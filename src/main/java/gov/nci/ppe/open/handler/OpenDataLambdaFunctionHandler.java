package gov.nci.ppe.open.handler;

import org.joda.time.LocalDateTime;
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
		String output = "Hello from Lambda!";
		context.getLogger().log(output);
		String jsonOutputAsString = callOpenURLForData(); 
		context.getLogger().log(jsonOutputAsString);
		String ppeResponse = callNCIPPEWithData(jsonOutputAsString);
		context.getLogger().log(ppeResponse);
		return output;
    }
    
    /* Get data from OPEN using a REST API call */
    private String callOpenURLForData() {
    	RestTemplate restTemplate = new RestTemplateBuilder().build();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Content-Type", "application/json");
		httpHeaders.set("Authorization", "Basic cHBlOnBwZTEj");
		// TODO the time below needs to be corrected.
		LocalDateTime toTime = LocalDateTime.now();
		LocalDateTime fromTime = toTime.minusMinutes(3);
		OpenRequestDTO openRequestObj = new OpenRequestDTO();
		openRequestObj.setPatientId("");
		openRequestObj.setProtocolNumber("");
		openRequestObj.setToDate(toTime.toString().substring(0, 19));
		openRequestObj.setFromDate(fromTime.toString().substring(0, 19));
		HttpEntity<String> request = new HttpEntity<String>(openRequestObj.toString(), httpHeaders);
		return restTemplate.postForObject(
				"https://test-cews.ctsu.org/cews/api/open/v1/moonshot/ppe", request, String.class);
    }
    
    /**
     *  Insert Patient Data into PPE using a REST API call
     * @param jsonData - Data for patients returned from OPEN
     */
    private String callNCIPPEWithData(String jsonData) {
    	RestTemplate restTemplate = new RestTemplateBuilder().build();
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.set("Content-Type", "application/json");
		HttpEntity<String> request = new HttpEntity<String>(jsonData, httpHeaders);
		System.out.println("http://devintg.ncippe.publicissapient.tech/api/v1/user/insert-open-data");
		return restTemplate.postForObject("http://devintg.ncippe.publicissapient.tech/api/v1/user/insert-open-data", request, String.class);
    }

}
