package co.edu.unicauca.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada de la aplicación Spring Boot del sistema Al Toro Gastrobar.
 */
@SpringBootApplication
public class BackendApplication {

	/**
	 * Arranca el contexto de Spring y lanza el servidor embebido.
	 *
	 * @param args argumentos de línea de comandos pasados al contexto de Spring
	 */
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
