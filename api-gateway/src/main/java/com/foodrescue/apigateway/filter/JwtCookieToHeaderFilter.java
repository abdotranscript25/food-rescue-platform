package com.foodrescue.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtCookieToHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // 1. Vérifier si le cookie 'jwt' existe dans la requête entrante du navigateur
        HttpCookie jwtCookie = request.getCookies().getFirst("jwt");

        if (jwtCookie != null) {
            String token = jwtCookie.getValue();

            // 2. Cloner la requête et injecter l'en-tête Authorization: Bearer <token>
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("Authorization", "Bearer " + token)
                    .build();

            // 3. Poursuivre le flux avec la requête modifiée
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        }

        // Si aucun cookie n'est présent, on laisse passer la requête telle quelle (pour les routes publiques)
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // S'assure que ce filtre s'exécute en priorité haute dans la chaîne de l'API Gateway
        return -100;
    }
}