package co.edu.unicauca.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requiere PostgreSQL en ejecución — ejecutar solo con infraestructura disponible (mvn test -Dspring.profiles.active=dev)")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
