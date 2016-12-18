package org.sitenv.service.ccda.smartscorecard.processor;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jsoup.Jsoup;
import org.sitenv.service.ccda.smartscorecard.model.CCDAScoreCardRubrics;
import org.sitenv.service.ccda.smartscorecard.model.CCDAXmlSnippet;
import org.sitenv.service.ccda.smartscorecard.model.Category;
import org.sitenv.service.ccda.smartscorecard.model.ReferenceError;
import org.sitenv.service.ccda.smartscorecard.model.ReferenceResult;
import org.sitenv.service.ccda.smartscorecard.model.ResponseTO;
import org.sitenv.service.ccda.smartscorecard.model.Results;
import org.sitenv.service.ccda.smartscorecard.model.ReferenceTypes.ReferenceInstanceType;
import org.sitenv.service.ccda.smartscorecard.util.ApplicationConstants.ErrorMessages;
import org.sitenv.service.ccda.smartscorecard.util.ApplicationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.SAXException;

import com.lowagie.text.DocumentException;

@Service
@Component
public class GeneratePDfFromJson {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	public void createPDF(ResponseTO scorecardResponse,String outputFolder,String pdfFileName,String ccdaFile)
	{
		logger.info("Generating PDF Response for CCDAFile {} with name {}",ccdaFile,pdfFileName );
		String response = ensureLogicalParseTreeInHTML(convertReportToHTML(scorecardResponse));
		OutputStream out = null;
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setNamespaceAware(false);
			factory.setValidating(false);

			DocumentBuilder builder = factory.newDocumentBuilder();
			Document refineddoc = builder.parse(new ByteArrayInputStream(
					response.getBytes("UTF-8")));

			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocument(refineddoc, null);
			renderer.layout();

			
			out = new FileOutputStream(new File(outputFolder.concat(pdfFileName)));
			renderer.createPDF(out);
			logger.info("Generating PDF Response for CCDAFile {} completed ", ccdaFile);
		} catch (ParserConfigurationException pcE) {
			pcE.printStackTrace();
		} catch (SAXException saxE) {
			saxE.printStackTrace();
		} catch (DocumentException docE) {
			docE.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (out != null) {
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	public String ensureLogicalParseTreeInHTML(String htmlReport) {
		org.jsoup.nodes.Document doc = Jsoup.parse(htmlReport);
		String cleanHtmlReport = doc.toString();
		return cleanHtmlReport;
	}

	public String convertReportToHTML(ResponseTO report) {
		StringBuffer sb = new StringBuffer();
		appendOpeningHtml(sb);

		if (report == null) {
			report = new ResponseTO();
			report.setResults(null);
			report.setSuccess(false);
			report.setErrorMessage(ErrorMessages.GENERIC_WITH_CONTACT);
			appendErrorMessageFromReport(sb, report,
					ErrorMessages.RESULTS_ARE_NULL);
		} else {
			// report != null
			if (report.getResults() != null) {
				if (report.isSuccess()) {
					Results results = report.getResults();
					List<Category> categories = results.getCategoryList();
					List<ReferenceResult> referenceResults = report
							.getReferenceResults();

					appendHeader(sb, report, results);
					appendHorizontalRuleWithBreaks(sb);

					appendTopLevelResults(sb, results, categories,
							referenceResults);
					appendHorizontalRuleWithBreaks(sb);

					appendDetailedResults(sb, categories, referenceResults);
				}
			} else {
				// report.getResults() == null
				if (!report.isSuccess()) {
					appendErrorMessageFromReport(sb, report,
							ErrorMessages.IS_SUCCESS_FALSE);
				} else {
					appendErrorMessageFromReport(sb, report);
				}
			}
		}

		appendClosingHtml(sb);
		return sb.toString();
	}

	private void appendOpeningHtml(StringBuffer sb) {
		sb.append("<!DOCTYPE html>");
		sb.append("<html>");
		sb.append("<head>");
		sb.append("<title>SITE C-CDA Scorecard Report</title>");
		sb.append("</head>");
		sb.append("<body style='font-family: \"Helvetica Neue\",Helvetica,Arial,sans-serif;'>");
	}

	private void appendHeader(StringBuffer sb, ResponseTO report,
			Results results) {
		sb.append("<header id='topOfScorecard'>");
		final String logoPath = "https://devportal.sitenv.org/site-portal-responsivebootstrap-theme/images/site/site-header.png";
		sb.append("<center>");
		sb.append("<img src='" + logoPath + "' alt='SITE logo' width='100%'>");

		sb.append("<br />");
		appendHorizontalRuleWithBreaks(sb);

		sb.append("<h1>" + "C-CDA " + results.getDocType() + " Scorecard For: ");
		sb.append("<h2>" + report.getFilename() + "</h2>");
		sb.append("</center>");
		sb.append("</header>");
	}

	private void appendTopLevelResults(StringBuffer sb, Results results,
			List<Category> categories, List<ReferenceResult> referenceResults) {

		sb.append("<h3>Scorecard Grade: " + results.getFinalGrade() + "</h3>");
		sb.append("<ul><li>");
		sb.append("<p>Your document scored a " + "<b>"
				+ results.getFinalGrade() + "</b>"
				+ " compared to an industry average of " + "<b>"
				+ results.getIndustryAverageGrade() + "</b>" + ".</p>");
		sb.append("</ul></li>");

		sb.append("<h3>Scorecard Score: " + results.getFinalNumericalGrade()
				+ "</h3>");
		sb.append("<ul><li>");
		sb.append("<p>Your document scored " + "<b>"
				+ +results.getFinalNumericalGrade() + "</b>" + " out of "
				+ "<b>" + " 100 " + "</b>" + " total possible points.</p>");
		sb.append("</ul></li>");

		boolean isSingular = results.getNumberOfIssues() == 1;
		appendSummaryRow(sb, results.getNumberOfIssues(), "Scorecard Issues",
				null, isSingular ? "Scorecard Issue" : "Scorecard Issues",
				isSingular);
		sb.append("</ul></li>");

		String messageSuffix = null;
		if (ApplicationUtil.isEmpty(referenceResults)) {
			for (ReferenceInstanceType refType : ReferenceInstanceType.values()) {
				if (refType == ReferenceInstanceType.CERTIFICATION_2015) {
					messageSuffix = "results";
				}
				appendSummaryRow(sb, 0, refType.getTypePrettyName(),
						messageSuffix, refType.getTypePrettyName(), false);
				sb.append("</ul></li>");
			}
		} else {
			for (ReferenceResult refResult : referenceResults) {
				int refErrorCount = refResult.getTotalErrorCount();
				isSingular = refErrorCount == 1;
				String refTypeName = refResult.getType().getTypePrettyName();
				String messageSubject = "";
				if (refResult.getType() == ReferenceInstanceType.IG_CONFORMANCE) {
					messageSubject = isSingular ? refTypeName.substring(0,
							refTypeName.length() - 1) : refTypeName;
				} else if (refResult.getType() == ReferenceInstanceType.CERTIFICATION_2015) {
					messageSuffix = isSingular ? "result" : "results";
					messageSubject = refTypeName;
				}
				appendSummaryRow(sb, refErrorCount, refTypeName, messageSuffix,
						messageSubject, isSingular);
				sb.append("</ul></li>");
			}
		}

		appendHorizontalRuleWithBreaks(sb);

		sb.append("<span id='heatMap'>" + "</span>");
		for (Category curCategory : categories) {
			sb.append("<h3>" + "<a href='#" + curCategory.getCategoryName()
					+ "-category" + "'>" + curCategory.getCategoryName()
					+ "</a>" + "</h3>");

			sb.append("<ul>");
			if (curCategory.getCategoryNumericalScore() != -1) {
				sb.append("<li>" + "Section Grade: " + "<b>"
						+ curCategory.getCategoryGrade() + "</b>" + "</li>"
						+ "<li>" + "Number of Issues: " + "<b>"
						+ curCategory.getNumberOfIssues() + "</b>" + "</li>");
			} else {
				sb.append("<li>"
						+ "This category was not scored as it contains "
						+ "<b>" + "Conformance Errors" + "</b>" + "</li>");
			}
			sb.append("</ul></li>");
		}

	}

	private void appendSummaryRow(StringBuffer sb, int result, String header,
			String messageSuffix, String messageSubject, boolean isSingular) {
		sb.append("<h3>"
				+ header
				+ ": "
				+ ("Scorecard Issues".equals(header) || result < 1 ? result
						: ("<a href=\"#" + header + "-category\">" + result + "</a>"))
				+ "</h3>");
		sb.append("<ul><li>");
		sb.append("<p>There " + (isSingular ? "is" : "are") + " " + "<b>"
				+ result + "</b>" + " " + messageSubject
				+ (messageSuffix != null ? " " + messageSuffix : "")
				+ " in your document.</p>");
	}

	private void appendDetailedResults(StringBuffer sb,
			List<Category> categories, List<ReferenceResult> referenceResults) {

		sb.append("<h2>" + "Detailed Results" + "</h2>");

		if (!ApplicationUtil.isEmpty(referenceResults)) {
			for (ReferenceResult curRefInstance : referenceResults) {

				ReferenceInstanceType refType = curRefInstance.getType();
				String refTypeName = refType.getTypePrettyName();
				sb.append("<h3 id=\"" + refTypeName + "-category\">"
						+ refTypeName + "</h3>");

				sb.append("<ul>"); // START curRefInstance ul
				sb.append("<li>"
						+ "Number of "
						+ (refType == ReferenceInstanceType.CERTIFICATION_2015 ? "Results:"
								: "Errors:") + " "
						+ curRefInstance.getTotalErrorCount() + "</li>");

				sb.append("<ol>"); // START reference errors ol

				if (curRefInstance.getTotalErrorCount() > 0) {
					for (ReferenceError curRefError : curRefInstance
							.getReferenceErrors()) {

						sb.append("<li>"
								+ (refType == ReferenceInstanceType.CERTIFICATION_2015 ? "Feedback:"
										: "Error:") + " "
								+ curRefError.getDescription() + "</li>");

						sb.append("<ul>"); // START ul within the curRefError
						if (!ApplicationUtil.isEmpty(curRefError
								.getSectionName())) {
							sb.append("<li>" + "Related Section: "
									+ curRefError.getSectionName() + "</li>");
						}
						sb.append("<li>"
								+ "Document Line Number (approximate): "
								+ curRefError.getDocumentLineNumber() + "</li>");
						sb.append("<li>"
								+ "xPath: "
								+ "<xmp style='font-family: Consolas, monaco, monospace;'>"
								+ curRefError.getxPath() + "</xmp>" + "</li>");
						sb.append("</ul>"); // END ul within the curRefError
					}
				}
				sb.append("</ol>"); // END reference errors ol
				sb.append("</ul>"); // END curRefInstance ul
				appendBackToTopWithBreaks(sb);

			}
		}

		for (Category curCategory : categories) {
			sb.append("<h3 id='" + curCategory.getCategoryName() + "-category"
					+ "'>" + curCategory.getCategoryName() + "</h3>");

			if (curCategory.getCategoryNumericalScore() != -1) {
				sb.append("<ul>"); // START curCategory ul
				sb.append("<li>" + "Section Grade: "
						+ curCategory.getCategoryGrade() + "</li>" + "<li>"
						+ "Number of Issues: "
						+ curCategory.getNumberOfIssues() + "</li>");

				if (curCategory.getNumberOfIssues() > 0) {
					sb.append("<ol>"); // START rules ol
					for (CCDAScoreCardRubrics curRubric : curCategory
							.getCategoryRubrics()) {
						if (curRubric.getNumberOfIssues() > 0) {
							sb.append("<li>" + "Rule: " + curRubric.getRule()
									+ "</li>");
							if (curRubric.getDescription() != null) {
								sb.append("<ul>" + "<li>" + "Description"
										+ "</li>" + "<ul>" + "<li>"
										+ curRubric.getDescription() + "</li>"
										+ "</ul>" + "</ul>");
								sb.append("<br />");

								sb.append("<ol>"); // START snippets ol
								for (CCDAXmlSnippet curSnippet : curRubric
										.getIssuesList()) {
									sb.append("<li>"
											+ "XML at line number "
											+ curSnippet.getLineNumber()
											+ "</li>"
											+ "<br /><xmp style='font-family: Consolas, monaco, monospace;'>"
											+ curSnippet.getXmlString()
											+ "</xmp><br /><br />");
								}
								sb.append("</ol>"); // END snippets ol
							}
						} else {
							// don't display rules without occurrences
							sb.append("</ol>");
						}
					}
					sb.append("</ol>"); // END rules ol
				}
				sb.append("</ul>"); // END curCategory ul
			}
			appendBackToTopWithBreaks(sb);
		}

	}

	private void appendClosingHtml(StringBuffer sb) {
		sb.append("</body>");
		sb.append("</html>");
	}

	private void appendHorizontalRuleWithBreaks(StringBuffer sb) {
		sb.append("<br />");
		sb.append("<hr />");
		sb.append("<br />");
	}

	private void appendBackToTopWithBreaks(StringBuffer sb) {
		// sb.append("<br />");
		sb.append("<a href='#topOfScorecard'>Back to Top</a>");
		// sb.append("<br />");
		// A PDF conversion bug is not processing this valid HTML so commenting
		// out until time to address
		// sb.append("<a href='#heatMap'>Back to Section List</a>");
		sb.append("<br />");
		// sb.append("<br />");
	}

	private void appendErrorMessage(StringBuffer sb, String errorMessage) {
		sb.append("<h2 style='color:red; background-color: #ffe6e6'>");
		sb.append(errorMessage);
		sb.append("</h2>");
		sb.append("<p>" + ErrorMessages.CONTACT + "</p>");
	}

	private void appendGenericErrorMessage(StringBuffer sb) {
		sb.append("<p>" + ErrorMessages.JSON_TO_JAVA_JACKSON + "<br />"
				+ ErrorMessages.CONTACT + "</p>");
	}

	private void appendErrorMessageFromReport(StringBuffer sb, ResponseTO report) {
		appendErrorMessageFromReport(sb, report, null);
	}

	private void appendErrorMessageFromReport(StringBuffer sb,
			ResponseTO report, String extraMessage) {
		if (report.getErrorMessage() != null
				&& !report.getErrorMessage().isEmpty()) {
			appendErrorMessage(sb, report.getErrorMessage());
		} else {
			appendGenericErrorMessage(sb);
		}
		if (extraMessage != null && !extraMessage.isEmpty()) {
			sb.append("<p>" + extraMessage + "</p>");
		}
	}

}
