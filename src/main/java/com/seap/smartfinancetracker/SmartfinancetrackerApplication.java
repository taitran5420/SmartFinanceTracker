package com.seap.smartfinancetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SmartfinancetrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartfinancetrackerApplication.class, args);
	}

}
