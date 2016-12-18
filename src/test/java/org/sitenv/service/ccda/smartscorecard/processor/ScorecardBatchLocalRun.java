package org.sitenv.service.ccda.smartscorecard.processor;

import org.springframework.boot.SpringApplication;

public class ScorecardBatchLocalRun {
	
	public static void main(String[] args) {
	    System.setProperty("configFileLocation", "C:/var/opt/scorecard/scorecard-batch.config");
	    
		SpringApplication.run(ScorecardBatchProcessor.class, args);
	}

}
