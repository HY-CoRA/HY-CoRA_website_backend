package com.hycora.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(BackendApplication.class, args);
		Environment env = ctx.getEnvironment();
		String port = env.getProperty("server.port", "8080");
		System.out.println("\n========================================");
		System.out.println("  API 서버: http://localhost:" + port);
		System.out.println("  Swagger:  http://localhost:" + port + "/swagger-ui/index.html");
		System.out.println("========================================\n");
	}

}
