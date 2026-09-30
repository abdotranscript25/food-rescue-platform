package com.foodrescue.authservice.service;

import com.foodrescue.authservice.client.UserServiceClient;
import com.foodrescue.authservice.dto.AuthResponse;
import com.foodrescue.authservice.dto.LoginRequest;
import com.foodrescue.authservice.dto.MfaSetupResponse;
import com.foodrescue.authservice.dto.MfaValidateRequest;
import com.foodrescue.authservice.dto.MfaVerifyRequest;
import com.foodrescue.authservice.dto.RegisterRequest;
import com.foodrescue.authservice.entity.Role;
import com.foodrescue.authservice.entity.User;
import com.foodrescue.authservice.entity.UserStatus;
import com.foodrescue.authservice.repository.UserRepository;
import com.foodrescue.authservice.util.TestMfaHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestMfaHelper testMfaHelper;

    @MockitoBean
    private UserServiceClient userServiceClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        Mockito.reset(userServiceClient);
    }

    // ==========================================
    // Test 1 : Inscription d'un nouvel utilisateur
    // ==========================================
    @Test
    @DisplayName("Register : doit créer un utilisateur CONSUMER avec un token")
    void register_shouldCreateUser() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Alice")
                .lastName("Dupont")
                .email("alice@test.com")
                .password("motDePasse123")
                .role(Role.CONSUMER)
                .phone("+33612345678")
                .address("1 rue de Paris")
                .city("Paris")
                .build();

        AuthResponse response = authService.register(request);

        assertNotNull(response.getToken());
        assertEquals("alice@test.com", response.getEmail());
        assertEquals("CONSUMER", response.getRole());
        assertEquals("Alice", response.getFirstName());
        assertEquals("Dupont", response.getLastName());
        assertFalse(response.isMfaRequired());

        User savedUser = userRepository.findByEmail("alice@test.com").orElse(null);
        assertNotNull(savedUser);
        assertEquals(UserStatus.ACTIVE, savedUser.getStatus());
        assertNotNull(savedUser.getCreatedAt());
        assertFalse(savedUser.isMfaEnabled());

        Mockito.verify(userServiceClient, Mockito.times(1))
                .createProfile(ArgumentMatchers.any());
    }

    // ==========================================
    // Test 2 : Inscription avec email existant
    // ==========================================
    @Test
    @DisplayName("Register : doit échouer si l'email existe déjà")
    void register_shouldFailIfEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Alice").lastName("Dupont")
                .email("alice@test.com").password("pass123")
                .role(Role.CONSUMER).build();

        authService.register(request);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authService.register(request));

        assertTrue(exception.getMessage().contains("existe déjà"));
    }

    // ==========================================
    // Test 3 : Connexion sans MFA
    // ==========================================
    @Test
    @DisplayName("Login : doit retourner un token complet pour un user sans MFA")
    void login_shouldReturnTokenWithoutMfa() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Bob").lastName("Martin")
                .email("bob@test.com").password("pass123")
                .role(Role.CONSUMER).build();
        authService.register(registerRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("bob@test.com").password("pass123").build();

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response.getToken());
        assertFalse(response.isMfaRequired());
        assertNull(response.getPartialToken());
    }

    // ==========================================
    // Test 4 : Connexion avec mauvais mot de passe
    // ==========================================
    @Test
    @DisplayName("Login : doit échouer si le mot de passe est incorrect")
    void login_shouldFailWithWrongPassword() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Bob").lastName("Martin")
                .email("bob@test.com").password("pass123")
                .role(Role.CONSUMER).build();
        authService.register(registerRequest);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("bob@test.com").password("MAUVAIS").build();

        assertThrows(Exception.class, () -> authService.login(loginRequest));
    }

    // ==========================================
    // Test 5 : Setup MFA (génération de secret)
    // ==========================================
    @Test
    @DisplayName("MFA Setup : doit générer un secret et un QR code")
    void mfaSetup_shouldGenerateSecret() {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Charlie").lastName("Durand")
                .email("charlie@test.com").password("pass123")
                .role(Role.CONSUMER).build();
        authService.register(registerRequest);

        MfaSetupResponse response = authService.mfaSetup("charlie@test.com");

        assertNotNull(response.getSecret());
        assertNotNull(response.getQrCodeImage());
        assertNotNull(response.getOtpauthUri());
        assertTrue(response.getOtpauthUri().startsWith("otpauth://totp/"));
        assertTrue(response.getOtpauthUri().contains("charlie"));

        User user = userRepository.findByEmail("charlie@test.com").orElse(null);
        assertNotNull(user);
        assertEquals(response.getSecret(), user.getMfaSecret());
        assertFalse(user.isMfaEnabled());
    }

    // ==========================================
    // Test 6 : Mot de passe haché avec BCrypt
    // ==========================================
    @Test
    @DisplayName("Register : le mot de passe doit être haché avec BCrypt")
    void register_shouldHashPassword() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Hash").lastName("Test")
                .email("hash@test.com").password("motDePasseClair123")
                .role(Role.CONSUMER).build();

        authService.register(request);

        User user = userRepository.findByEmail("hash@test.com").orElseThrow();
        assertNotEquals("motDePasseClair123", user.getPassword());
        assertTrue(user.getPassword().startsWith("$2a$") || user.getPassword().startsWith("$2b$"),
                "Le mot de passe doit être haché avec BCrypt");
    }

    // ==========================================
    // Test 7 : Flux MFA complet (setup → verify → login → validate)
    // ==========================================
    @Test
    @DisplayName("MFA Flow : setup → verify → login → validate")
    void mfaFullFlow_shouldWork() {
        // 1. Créer un utilisateur
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Dora").lastName("Explorer")
                .email("dora@test.com").password("pass123")
                .role(Role.CONSUMER).build();
        authService.register(registerRequest);

        // 2. Setup MFA
        MfaSetupResponse setup = authService.mfaSetup("dora@test.com");
        String secret = setup.getSecret();
        assertNotNull(secret);

        // 3. Verify (code généré côté test, PAS en prod)
        String codeForVerify = testMfaHelper.generateCurrentCode(secret, 30);
        authService.mfaVerify("dora@test.com", new MfaVerifyRequest(codeForVerify));

        User user = userRepository.findByEmail("dora@test.com").orElseThrow();
        assertTrue(user.isMfaEnabled(), "Le MFA doit être activé");

        // 4. Login → partialToken
        LoginRequest loginRequest = LoginRequest.builder()
                .email("dora@test.com").password("pass123").build();
        AuthResponse loginResponse = authService.login(loginRequest);

        assertTrue(loginResponse.isMfaRequired(), "MFA requis");
        assertNull(loginResponse.getToken(), "Pas de token final à cette étape");
        assertNotNull(loginResponse.getPartialToken(), "PartialToken présent");

        // 5. Validate → token final
        String codeForValidate = testMfaHelper.generateCurrentCode(secret, 30);
        AuthResponse finalResponse = authService.mfaValidate(
                new MfaValidateRequest(loginResponse.getPartialToken(), codeForValidate));

        assertNotNull(finalResponse.getToken(), "Token final présent");
        assertFalse(finalResponse.isMfaRequired(), "MFA validé");
        assertEquals("dora@test.com", finalResponse.getEmail());
    }

    // ==========================================
    // Test 8 : MFA Verify avec code invalide
    // ==========================================
    @Test
    @DisplayName("MFA Verify : doit échouer avec un code invalide")
    void mfaVerify_shouldFailWithInvalidCode() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Invalid").lastName("Code")
                .email("invalid@test.com").password("pass123")
                .role(Role.CONSUMER).build();
        authService.register(request);
        authService.mfaSetup("invalid@test.com");

        MfaVerifyRequest verifyRequest = new MfaVerifyRequest("000000");

        assertThrows(RuntimeException.class,
                () -> authService.mfaVerify("invalid@test.com", verifyRequest));
    }

    // ==========================================
    // Test 9 : MFA Validate avec token invalide
    // ==========================================
    @Test
    @DisplayName("MFA Validate : doit échouer avec un partialToken invalide")
    void mfaValidate_shouldFailWithInvalidToken() {
        MfaValidateRequest request = new MfaValidateRequest("token_bidon_invalide", "123456");

        assertThrows(RuntimeException.class, () -> authService.mfaValidate(request));
    }

    // ==========================================
    // Test 10 : User SUSPENDED ne peut pas se connecter
    // ==========================================
    @Test
    @DisplayName("Login : un utilisateur SUSPENDED doit être bloqué")
    void login_shouldFailForSuspendedUser() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Sus").lastName("Pended")
                .email("sus@test.com").password("pass123")
                .role(Role.CONSUMER).build();
        authService.register(request);

        // Le suspendre
        User user = userRepository.findByEmail("sus@test.com").orElseThrow();
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);

        // Tentative de login
        LoginRequest loginRequest = LoginRequest.builder()
                .email("sus@test.com").password("pass123").build();

        assertThrows(Exception.class, () -> authService.login(loginRequest));
    }
}