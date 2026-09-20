package com.company.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class KnowledgePlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(KnowledgePlatformApplication.class, args);
	}

}
