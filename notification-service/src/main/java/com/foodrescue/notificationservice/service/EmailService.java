package com.foodrescue.notificationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException; // ⬅️ Import spécifique
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            log.info("E-mail envoyé avec succès à : {}", to);

        } catch (MailException e) { // ⬅️ On attrape l'exception spécifique du mail
            // ⬅️ On ajoute "e" à la fin pour logger la stack trace complète
            log.warn("Impossible d'envoyer l'e-mail via SMTP (Mode dégradé/simulation). Erreur: {}", e.getMessage(), e);
            log.info("--- SIMULATION ENVOI EMAIL ---");
            log.info("À : {}", to);
            log.info("Sujet : {}", subject);
            log.info("Contenu : \n{}", body);
            log.info("------------------------------");
        }
    }
}