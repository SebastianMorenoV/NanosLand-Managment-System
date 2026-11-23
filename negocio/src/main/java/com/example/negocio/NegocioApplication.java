package com.example.negocio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.mycompany.persistencia")
@EntityScan(basePackages = "com.mycompany.persistencia")
public class NegocioApplication {

	public static void main(String[] args) {
		SpringApplication.run(NegocioApplication.class, args);
	}

}
