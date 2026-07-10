package dev.vkazulkin.callback;

import dev.vkazulkin.entity.UpcomingTalkApprovalStatus;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import tools.jackson.databind.ObjectMapper;

public class SendDurableExecutionCallback {
	
	private final static String CALLBACK_ID
	="Ab9hZXiWYXJuOmF3czpsYW1iZGE6dXMtZWFzdC0xOjI2NTYzNDI1NzYxMDpmdW5jdGlvbjpBdXRob3JDb250ZW50RXh0cmFjdG9yOiRMQVRFU1QvZHVyYWJsZS1leGVjdXRpb24vc2VhcmNoRm9yVmFkeW0xMDUvNzYwZjdiMmMtZGFjZC0zMzA5LWJkNzEtZDlkM2JkN2JlNzliYWl4JGIzNjAxOTQyLTRkYWYtNGJhMS1hMjIyLTM3NTk2NmIwNDViYv8";
	
	private final static LambdaClient LAMBDA_CLIENT = LambdaClient.builder().region(Region.US_EAST_1).build();
	
	private final static ObjectMapper MAPPER= new ObjectMapper();

	public static void main(String[] args) {
		var approvedStatus= MAPPER.writeValueAsString(new UpcomingTalkApprovalStatus("approved"));
		System.out.println("approved status : "+approvedStatus);
		
		var response=LAMBDA_CLIENT.sendDurableExecutionCallbackSuccess(builder -> 
		  builder.callbackId(CALLBACK_ID)
		  .result(SdkBytes.fromUtf8String(approvedStatus))
		  .build());
		
	   System.out.println("response: "+response);
	}
}