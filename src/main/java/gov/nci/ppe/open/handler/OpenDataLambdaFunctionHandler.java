package gov.nci.ppe.open.handler;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

import gov.nci.ppe.open.data.entity.dto.OpenRequestDTO;

public class OpenDataLambdaFunctionHandler implements RequestHandler<Object, String> {

	@Override
	public String handleRequest(Object input, Context context) {
		context.getLogger().log("Input: " + input);

		String jsonOutputAsString = callOpenURLForData(context);
		context.getLogger().log(jsonOutputAsString);
		String ppeResponse = callNCIPPEWithData(jsonOutputAsString);
		context.getLogger().log(ppeResponse);
		return ppeResponse;
	}

	/* Get data from OPEN using a REST API call */
	private String callOpenURLForData(Context context) {
		RestTemplate restTemplate = new RestTemplateBuilder().build();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Content-Type", "application/json");
		httpHeaders.set("Authorization", getEnvironmentValue("OPEN_AUTHORIZATION"));
		context.getLogger().log("Input: " + getEnvironmentValue("START_DATE") + "," + getEnvironmentValue("END_DATE"));
		OffsetDateTime toTime = getEndTime(context);
		OffsetDateTime fromTime = getStartTime(toTime, context);
		OpenRequestDTO openRequestObj = new OpenRequestDTO();
		openRequestObj.setPatientId("");
		openRequestObj.setProtocolNumber(getEnvironmentValue("OPEN_PROTOCOL"));
		openRequestObj.setToDate(toTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
		openRequestObj.setFromDate(fromTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

		context.getLogger().log("Request Param: " + openRequestObj.toString());
		HttpEntity<String> request = new HttpEntity<String>(openRequestObj.toString(), httpHeaders);
		return restTemplate.postForObject(getEnvironmentValue("OPEN_ENDPOINT"), request, String.class);
	}

	private String getEnvironmentValue(String envVariable) {
		return System.getenv(envVariable).strip();
	}

	private OffsetDateTime getStartTime(OffsetDateTime endTime, Context context) {
		String startTimeString = getEnvironmentValue("START_DATE");

		OffsetDateTime startDate;
		try {
			startDate = OffsetDateTime.parse(startTimeString);
		} catch (Exception ex) {
			context.getLogger().log(ex.getMessage());
			startDate = endTime.minusMinutes(Integer.parseInt(System.getenv("OPEN_POLLING_FREQUENCY")));
		}
		return startDate;
	}

	private OffsetDateTime getEndTime(Context context) {
		String endTimeString = getEnvironmentValue("END_DATE");
		OffsetDateTime endDate;
		try {
			endDate = OffsetDateTime.parse(endTimeString);
		} catch (Exception ex) {
			context.getLogger().log(ex.getMessage());
			endDate = OffsetDateTime.now();
		}

		return endDate;
	}

	/**
	 * Insert Patient Data into PPE using a REST API call
	 * 
	 * @param jsonData - Data for patients returned from OPEN
	 */
	private String callNCIPPEWithData(String jsonData) {
		RestTemplate restTemplate = new RestTemplateBuilder().build();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Content-Type", "application/json");
		HttpEntity<String> request = new HttpEntity<String>(jsonData, httpHeaders);

		return restTemplate.postForObject(getEnvironmentValue("NCIPPE_API_ENDPOINT"), request, String.class);
	}

}
