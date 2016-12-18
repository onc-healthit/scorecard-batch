package org.sitenv.service.ccda.smartscorecard.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.sitenv.service.ccda.smartscorecard.model.ResponseTO;
import org.sitenv.service.ccda.smartscorecard.model.ScorecardBatchConfig;
import org.sitenv.service.ccda.smartscorecard.model.UnprocessedFile;
import org.sitenv.service.ccda.smartscorecard.util.ApplicationConstants.ErrorMessages;
import org.sitenv.service.ccda.smartscorecard.util.ApplicationConstants.FileExtensions;
import org.sitenv.service.ccda.smartscorecard.util.ApplicationConstants.ScorecardBatchDefaults;
import org.sitenv.service.ccda.smartscorecard.util.ApplicationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ScorecardBatchProcessing {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	@Qualifier("restTemplate")
	private RestTemplate restTemplate;
	
	@Autowired
	@Qualifier("scorecardBatchConfig")
	private ScorecardBatchConfig scorecardBatchConfig;
	
	@Autowired
	private GeneratePDfFromJson generatePDfFromJSon;

	@PostConstruct
	public void processCCdaFiles() {
		
		ObjectMapper objectMapper = new ObjectMapper();
		ResponseTO scorecardResponse=null;
		String scorecardUrl;
		List<UnprocessedFile> unprocessedFileList = new ArrayList<UnprocessedFile>();
		UnprocessedFile unprocessedFile = null;
		try {
			File directory = loadDirectory(scorecardBatchConfig.getCcdaFileLocation());
			File[] list = directory.listFiles();
			if(list.length == 0)
			{
				logger.info("No files to process in given CCDAFileLocation : " + scorecardBatchConfig.getCcdaFileLocation());
				return;
			}
			if(ApplicationUtil.isEmpty(scorecardBatchConfig.getScorecardOutputFolder()))
			{
				logger.info("scorecardBatch.outputFolderLocation property is not specified or empty, responses will be created under {} folder",ScorecardBatchDefaults.defaultOutputFolder);
				scorecardBatchConfig.setScorecardOutputFolder(ScorecardBatchDefaults.defaultOutputFolder);
			}
			for (File ccdaFile : list) {
				logger.info("Processing CCDAFile : " + ccdaFile.getName());
				LinkedMultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<String, Object>();
				File tempFile = File.createTempFile(FilenameUtils.getBaseName(ccdaFile.getName()), ".".concat(FilenameUtils.getExtension(ccdaFile.getName())));
				FileOutputStream out = new FileOutputStream(tempFile);
				IOUtils.copy(new FileInputStream(ccdaFile), out);
				requestMap.add("ccdaFile", new FileSystemResource(tempFile));

				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.MULTIPART_FORM_DATA);
				

				HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
						requestMap, headers);
				FormHttpMessageConverter formConverter = new FormHttpMessageConverter();
				formConverter.setCharset(Charset.forName("UTF8"));
				restTemplate.getMessageConverters().add(formConverter);
				restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
				scorecardUrl = getScorecardUrl(scorecardBatchConfig);
				try
				{
					scorecardResponse = restTemplate.postForObject(scorecardUrl,requestEntity, ResponseTO.class);
					if(scorecardResponse.isSuccess())
					{
						scorecardResponse.setFilename(ccdaFile.getName());
						FileUtils.writeStringToFile(new File(scorecardBatchConfig.getScorecardOutputFolder().concat(getOutputFileName(ccdaFile.getName(),FileExtensions.jsonFileExtension))), 
															objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(scorecardResponse));
						logger.info("Scorecard JSON response generated for CCDAFile : " + ccdaFile.getName());
					}else 
					{
						logger.error(ErrorMessages.GENERIC_WITH_CONTACT);
						unprocessedFile = new UnprocessedFile();
						unprocessedFile.setFileName(ccdaFile.getName());
						unprocessedFile.setReason(ErrorMessages.GENERIC_WITH_CONTACT);
						unprocessedFileList.add(unprocessedFile);
					}
				}catch(RestClientException exception){
					logger.error(exception.getMessage(),exception);
					unprocessedFile = new UnprocessedFile();
					unprocessedFile.setFileName(ccdaFile.getName());
					unprocessedFile.setReason(exception.getMessage());
					unprocessedFileList.add(unprocessedFile);
				}catch(Exception exce)
				{
					logger.error(exce.getMessage(),exce);
					unprocessedFile = new UnprocessedFile();
					unprocessedFile.setFileName(ccdaFile.getName());
					unprocessedFile.setReason(exce.getMessage());
					unprocessedFileList.add(unprocessedFile);
				}
				tempFile.delete();
				
				if(scorecardResponse!=null && scorecardResponse.isSuccess())
				{
					generatePDfFromJSon.createPDF(scorecardResponse,scorecardBatchConfig.getScorecardOutputFolder(), 
													getOutputFileName(ccdaFile.getName(),FileExtensions.pdfFileExtension),ccdaFile.getName());
				}
			}
			if(!ApplicationUtil.isEmpty(unprocessedFileList))
			{
				FileUtils.writeStringToFile(new File(scorecardBatchConfig.getScorecardOutputFolder().concat(ScorecardBatchDefaults.errorFileName)), 
														generateErrorReport(unprocessedFileList));
			}
			logger.info("************Scorecard Batch Execution Completed***********************" );
		} catch (IOException ioe) {
			logger.error(ioe.getLocalizedMessage(),ioe);
		}
	}
	
	private String generateErrorReport(List<UnprocessedFile> unprocessedFiles)
	{
		StringBuffer errorMeessage = new StringBuffer();
		for(UnprocessedFile unprocessedFile : unprocessedFiles)
		{
			errorMeessage.append(unprocessedFile.toString());
			errorMeessage.append(System.lineSeparator());
		}
		return errorMeessage.toString();
	}
	
	private String getOutputFileName(String fileName,String fileExtension)
	{
		return FilenameUtils.getBaseName(fileName).concat(fileExtension);
	}

	private File loadDirectory(String directory) throws IOException {
		File dir = new File(directory);
		if (dir.isFile()) {
			throw new IOException("CCDAFiles directoty location is invalid");
		}
		return dir;
	}
	
	private String getScorecardUrl(ScorecardBatchConfig scorecardBatchConfig)
	{
		if(ApplicationUtil.isEmpty(scorecardBatchConfig.getScorecardUrl()))
		{
			return ScorecardBatchDefaults.scorecardDefaultUrl;
		}else 
			return scorecardBatchConfig.getScorecardUrl();
	}
}
