package org.sitenv.service.ccda.smartscorecard.model;

public class ScorecardBatchConfig {
	
	private String ccdaFileLocation;
	private String scorecardOutputFolder;
	private String scorecardUrl;
	public String getCcdaFileLocation() {
		return ccdaFileLocation;
	}
	public void setCcdaFileLocation(String ccdaFileLocation) {
		this.ccdaFileLocation = ccdaFileLocation;
	}
	public String getScorecardOutputFolder() {
		return scorecardOutputFolder;
	}
	public void setScorecardOutputFolder(String scorecardOutputFolder) {
		this.scorecardOutputFolder = scorecardOutputFolder;
	}
	public String getScorecardUrl() {
		return scorecardUrl;
	}
	public void setScorecardUrl(String scorecardUrl) {
		this.scorecardUrl = scorecardUrl;
	}
}
