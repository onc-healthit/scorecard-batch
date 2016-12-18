package org.sitenv.service.ccda.smartscorecard.util;

public class ApplicationConstants {
	
	public static class ErrorMessages {
		public static final String CONTACT = "Please report this issue to TestingServices@sitenv.org.";
		public static final String GENERIC = "An Unknown error has occurred. ";
		public static final String GENERIC_WITH_CONTACT = "An Unknown error has occurred. "
				+ CONTACT;
		public static final String GENERIC_DIFFERENT_FILE_OR_TIME = "Please try a different file or try again at another time.";
		public static final String JSON_TO_JAVA_JACKSON = "An error occurred while converting the Scorecard service JSON response to a Java object via the Jackson API.";
		public static final String RESULTS_ARE_NULL = "Note for the developers: The ResponseTO results are null.";
		public static final String IS_SUCCESS_FALSE = "Note for the developers: isSuccess is equal to false.";
		public static final String NULL_RESULT_ON_SAVESCORECARDSERVICEBACKEND_CALL = "Error: savescorecardservicebackend did not receive any results (null) from ccdascorecardservice."
				+ " " + GENERIC_DIFFERENT_FILE_OR_TIME;
		public static final String UNSTRUCTURED_DOCUMENT = "The supplied C-CDA XML document has been identified as an Unstructured Document "
				+ "urn:hl7ii:2.16.840.1.113883.10.20.22.1.10. The C-CDA Scorecard tool does not score this document type. "
				+ "Please try submitting another document type for review such as a Continuity of Care Document, Care Plan, etc.";
		public static final String SCHEMA_ERRORS_GENERIC = "Schema errors must be addressed before a score can be provided.";
	}
	
	public static class FileExtensions{
		public static final String jsonFileExtension = ".json";
		public static final String pdfFileExtension = ".pdf";
	}
	
	public static class ScorecardBatchDefaults{
		public static final String scorecardDefaultUrl = "https://devccda.sitenv.org/scorecard/ccdascorecardservice2";
		public static final String defaultOutputFolder = "/scorecardBatchOutput/";
		public static final String errorFileName = "UnprocessedFiles.log";
	}

}
