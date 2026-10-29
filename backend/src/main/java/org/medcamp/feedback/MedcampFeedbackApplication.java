package org.medcamp.feedback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MedcampFeedbackApplication {

	public static void main(String[] args) {
		SpringApplication.run(MedcampFeedbackApplication.class, args);
	}

}
