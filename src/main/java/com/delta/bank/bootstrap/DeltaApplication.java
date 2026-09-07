package com.delta.bank.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.delta.bank")
@org.springframework.data.jpa.repository.config.EnableJpaRepositories(basePackages = "com.delta.bank.domain")
@org.springframework.boot.autoconfigure.domain.EntityScan(basePackages = "com.delta.bank.domain")
public class DeltaApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeltaApplication.class, args);
    }
}
