package com.suvidha.connections;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class ConnectionsApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConnectionsApplication.class, args);
    }
}
