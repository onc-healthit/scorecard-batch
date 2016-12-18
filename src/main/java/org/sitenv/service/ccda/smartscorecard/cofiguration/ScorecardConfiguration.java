package org.sitenv.service.ccda.smartscorecard.cofiguration;

import java.io.File;

import org.sitenv.service.ccda.smartscorecard.model.ScorecardBatchConfig;
import org.sitenv.service.ccda.smartscorecard.util.ApplicationUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;


@Configuration
@PropertySource("file:${configFileLocation}")
public class ScorecardConfiguration {
	
	@Autowired
	Environment environment;
	
	public SimpleClientHttpRequestFactory getRequestFactory(Integer timeOutInMillSecs) {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    	factory.setReadTimeout(timeOutInMillSecs);
    	factory.setConnectTimeout(timeOutInMillSecs);
		return factory;
	}
	
	@Bean(name="restTemplate")
	public RestTemplate getRestTemplate(@Value("${api.rest.timeout.ms}") Integer timeOutInMillSecs) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(getRequestFactory(timeOutInMillSecs));
		return restTemplate;
	}
	
	@Bean(name="scorecardBatchConfig")
	public ScorecardBatchConfig getScorecardBatchConfig(final Environment environment)
	{
		ScorecardBatchConfig scorecardBatchConfig = new ScorecardBatchConfig();
		scorecardBatchConfig.setCcdaFileLocation(environment.getProperty("scorecardBatch.ccdaFileLocation"));
		scorecardBatchConfig.setScorecardOutputFolder(checkTrailngSlashForOutputFolder(environment.getProperty("scorecardBatch.outputFolderLocation")));
		scorecardBatchConfig.setScorecardUrl(environment.getProperty("scorecardBatch.scorecardUrl"));
		return scorecardBatchConfig;
	}
	
	private String checkTrailngSlashForOutputFolder(String outputFolder)
	{
		if(!ApplicationUtil.isEmpty(outputFolder))
		{
			if(outputFolder.charAt(outputFolder.length()-1)!=File.separatorChar){
				outputFolder += File.separator;
			}
		}
		return outputFolder;
	}
}
