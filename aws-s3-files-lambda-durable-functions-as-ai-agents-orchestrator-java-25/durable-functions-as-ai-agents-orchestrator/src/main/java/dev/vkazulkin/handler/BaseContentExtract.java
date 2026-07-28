package dev.vkazulkin.handler;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.vkazulkin.entity.Author;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockagentcore.BedrockAgentCoreClient;
import software.amazon.awssdk.services.bedrockagentcore.model.InvokeAgentRuntimeRequest;
import software.amazon.awssdk.services.sts.StsClient;
import tools.jackson.databind.ObjectMapper;

interface BaseContentExtract {

	   static final Logger LOGGER = LoggerFactory.getLogger(BaseContentExtract.class);
	   static final String AGENT_RUNTIME_ARN="arn:aws:bedrock-agentcore:us-east-1:{AWS_ACCOUNT_ID}:runtime/RuntimeForAuthorContentAgenticSearch-LBaMJZGakW";
	   static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	   
	   default public <T> T search(Author author, String searchTopic, int maxNumberOfResults, Class<T> clazz)  throws Exception {
			
	        var payload = """
	        		{
					"prompt":
	                "Search for the %s given by %s %s. Provide maximum %d results.
	                 Your response should be in JSON format.
	                 Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.",
	                 "resultType": %s
	                }
	                """.formatted(searchTopic, author.firstName(), author.lastName()
	                		, maxNumberOfResults, clazz.getName());

			IO.println("payload: "+payload);
			var httpClient=ApacheHttpClient.builder()
				    .connectionTimeout(Duration.ofMinutes(5))
				    .socketTimeout(Duration.ofMinutes(5))
				    .build();
						
			var bedrockAgentCoreClient = BedrockAgentCoreClient.builder()			
					.region(Region.US_EAST_1)
					.httpClient(httpClient)
					.build();

			var invokeAgentRuntimeRequest = InvokeAgentRuntimeRequest.builder()
					.agentRuntimeArn(replaceAWSAccountID(AGENT_RUNTIME_ARN))				 
					.qualifier("DEFAULT").contentType("application/json").payload(SdkBytes.fromUtf8String(payload)).build();
			try (var responseStream = bedrockAgentCoreClient
					.invokeAgentRuntime(invokeAgentRuntimeRequest)) {
				var response = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);

				IO.println(response);
				return OBJECT_MAPPER.readValue(response, clazz);
			}
		}

	    private static String replaceAWSAccountID(String arn ) {
	    	var replacedArn = arn.replace("{AWS_ACCOUNT_ID}", getAccountId());
	    	IO.println("replaced runtime arn "+replacedArn);
	    	return replacedArn;
	    }

		private static String getAccountId() {
			var stsClient = StsClient.builder().region(Region.US_EAST_1).build();
		    var awsAccountId= stsClient.getCallerIdentity().account();
		    IO.println("AWS Account Id "+awsAccountId);
		    return awsAccountId;
		}

}