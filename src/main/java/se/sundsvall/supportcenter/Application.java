package se.sundsvall.supportcenter;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import se.sundsvall.dept44.ServiceApplication;

import static org.springframework.boot.SpringApplication.run;

@ServiceApplication
@EnableFeignClients
@EnableCaching
public class Application {
	public static void main(String... args) {
		run(Application.class, args);
	}
}
