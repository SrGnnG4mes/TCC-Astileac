package br.edu.ifac.astileac.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint temporario so para validar que o toolchain (Java + Maven +
 * Spring Boot) esta funcionando de ponta a ponta. Remover quando os
 * primeiros endpoints reais (RF01 em diante) forem implementados.
 */
@RestController
public class HelloController {

	@GetMapping("/hello")
	public String helloMundo() {
		return "Ola, mundo! ASTILEAC backend funcionando.";
	}

}
