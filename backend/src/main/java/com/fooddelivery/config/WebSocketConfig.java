package com.fooddelivery.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket configuration for real-time order tracking.
 * 
 * Enables STOMP over WebSocket for pub-sub messaging:
 * - Customers subscribe to /user/queue/orders/{orderId} for their order updates
 * - Delivery agents subscribe to /user/queue/agent/orders for new assignments
 * - Restaurants subscribe to /user/queue/restaurant/orders for incoming orders
 * 
 * Message flow:
 * 1. Order placed → broadcast to restaurant
 * 2. Order confirmed → broadcast to customer & agent assignment
 * 3. Order picked up → broadcast to customer 
 * 4. Location updates → broadcast to customer
 * 5. Order delivered → broadcast to customer & agent
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple message broker with these destination prefixes
        config.enableSimpleBroker("/queue", "/topic");
        
        // Messages with this prefix are routed to @MessageMapping methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Enable user-specific destinations (for targeted messaging)
        config.setUserDestinationPrefix("/user");
        
        log.info("WebSocket message broker configured");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoint for WebSocket connections
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // Allow all origins in development
                .withSockJS(); // Enable SockJS fallback for browsers without WebSocket support
        
        log.info("WebSocket STOMP endpoint registered at /ws");
    }
}