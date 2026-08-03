package com.fooddelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the Food Delivery Spring Boot 3 application.
 *
 * @EnableJpaAuditing activates @CreatedDate / @LastModifiedDate
 * auto-population on all audited entities.
 */
@SpringBootApplication
@EnableJpaAuditing
public class FoodDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodDeliveryApplication.class, args);
    }
}
