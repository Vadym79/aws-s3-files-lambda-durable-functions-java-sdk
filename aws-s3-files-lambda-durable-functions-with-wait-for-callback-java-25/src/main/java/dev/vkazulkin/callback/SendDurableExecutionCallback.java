package dev.vkazulkin.callback;

import dev.vkazulkin.entity.UpcomingTalkApprovalStatus;
import dev.vkazulkin.entity.UpcomingTalks;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import tools.jackson.databind.ObjectMapper;

public class SendDurableExecutionCallback {
	
	private final static String CALLBACK_ID
	="Ab9hZXiuYXJuOmF3czpsYW1iZGE6dXMtZWFzdC0xOjI2NTYzNDI1NzYxMDpmdW5jdGlvbjpBc3luY0F1dGhvckNvbnRlbnRFeHRyYWN0b3I6JExBVEVTVC9kdXJhYmxlLWV4ZWN1dGlvbi9lOGJjNzY2NS03NWMwLTQzYjUtYTJkZS1mNzRiMDAzM2JhYWEvZjhlYmQ1MzctZDQxMy0zYzVhLThlMWQtMWFkMDk3NTI3NWZmYWl4JDI0MDI3NjdlLTQxMTctNGNjYi1hNTY4LTY2ZGQ4MmFmYjUxZf8";
	
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