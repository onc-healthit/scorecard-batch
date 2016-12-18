package org.sitenv.service.ccda.smartscorecard.model;

public class UnprocessedFile {

	private String fileName;
	private String reason;
	
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	@Override
	public String toString() {
		return fileName + ":" + reason;
	}
}
