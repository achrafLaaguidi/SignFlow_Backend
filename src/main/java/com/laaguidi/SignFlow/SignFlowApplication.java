package com.laaguidi.SignFlow;

import com.laaguidi.SignFlow.bootstrap.CentralDatabaseInitializer;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
public class SignFlowApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

		CentralDatabaseInitializer.createDatabaseAndTablesIfNotExists();

		SpringApplication.run(SignFlowApplication.class, args);
	}
}