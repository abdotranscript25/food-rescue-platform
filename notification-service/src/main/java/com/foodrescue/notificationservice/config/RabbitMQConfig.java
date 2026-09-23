package com.foodrescue.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "notification.queue";
    public static final String EXCHANGE_NAME = "notification.exchange";
    public static final String ROUTING_KEY = "notification.routingKey";

    // 1. Création de la file d'attente (Queue)
    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true); // true = durable (survit au redémarrage de RabbitMQ)
    }

    // 2. Création de l'échangeur (Exchange)
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    // 3. Liaison entre la Queue et l'Exchange via la Routing Key
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY);
    }

    // 4. Convertisseur JSON pour envoyer/recevoir des objets DTO lisibles
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}