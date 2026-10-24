package com.aiagentos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aiagentos")
public class AiagentosApplication {
  public static void main(String[] args) {
    SpringApplication.run(AiagentosApplication.class, args);
  }
}
