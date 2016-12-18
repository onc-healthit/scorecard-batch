package org.sitenv.service.ccda.smartscorecard.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("org.sitenv.service.ccda.smartscorecard")
public class ScorecardBatchProcessor {
	
	public static void main(String[] args) {
		SpringApplication.run(ScorecardBatchProcessor.class, args);
	}
}
