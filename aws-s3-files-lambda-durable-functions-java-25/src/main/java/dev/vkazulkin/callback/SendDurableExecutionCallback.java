package dev.vkazulkin.callback;

import dev.vkazulkin.entity.UpcomingTalkApprovalStatus;
import dev.vkazulkin.entity.UpcomingTalks;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import tools.jackson.databind.ObjectMapper;

public class SendDurableExecutionCallback {
	
	private final static String CALLBACK_ID
	="Ab9hZXiWYXJuOmF3czpsYW1iZGE6dXMtZWFzdC0xOjI2NTYzNDI1NzYxMDpmdW5jdGlvbjpBdXRob3JDb250ZW50RXh0cmFjdG9yOiRMQVRFU1QvZHVyYWJsZS1leGVjdXRpb24vc2VhcmNoRm9yVmFkeW0xMzQvYjEyMTQxZTItZDM5OS0zNWM0LWIyOWQtYjUwYTU1MzVkNTM5YWl4JDlhYjBjNzMyLWY4N2UtNDMxNC05Njc3LWQ1NzM3ZWU0YWVhN/8";
	
	private final static LambdaClient LAMBDA_CLIENT = LambdaClient.builder().region(Region.US_EAST_1).build();
	
	private final static ObjectMapper MAPPER= new ObjectMapper();

	public static void main(String[] args) {
		var approvedStatus= MAPPER.writeValueAsString(
				new UpcomingTalkApprovalStatus(UpcomingTalks.getDefaultUpcomingTalks(), "approved"));
		System.out.println("approved status : "+approvedStatus);
		
		var response=LAMBDA_CLIENT.sendDurableExecutionCallbackSuccess(builder -> 
		  builder.callbackId(CALLBACK_ID)
		  .result(SdkBytes.fromUtf8String(approvedStatus))
		  .build());
		
	   System.out.println("response: "+response);
	}
}