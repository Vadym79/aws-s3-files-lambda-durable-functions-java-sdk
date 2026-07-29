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
import tools.jackson.databind.ObjectMapper;

interface BaseContentExtract {

	   static final Logger LOGGER = LoggerFactory.getLogger(BaseContentExtract.class);
	   static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	   
	   default public <T> T search(Author author, String searchTopic, int maxNumberOfResults, Class<T> clazz)  throws Exception {
			
	        var payload = """
	        		{
					"prompt":"Do a web search for the %s authored by %s %s. Provide maximum %d results. Your response should be in JSON format. Do not include any explanations, only provide a RFC8259 compliant JSON response following this format without deviation.",
	                 "resultType": "%s"
	                }
	                """.formatted(searchTopic, author.firstName(), author.lastName()
	                		, maxNumberOfResults, clazz.getName());

	        LOGGER.info("payload: "+payload);
			var httpClient=ApacheHttpClient.builder()
				    .connectionTimeout(Duration.ofMinutes(5))
				    .socketTimeout(Duration.ofMinutes(5))
				    .build();
						
			var bedrockAgentCoreClient = BedrockAgentCoreClient.builder()			
					.region(Region.US_EAST_1)
					.httpClient(httpClient)
					.build();

			var AGENT_RUNTIME_ARN= System.getenv("AGENTCORE_RUNTIME_ARN");
			LOGGER.info("AgentCore Runtime ARN: "+AGENT_RUNTIME_ARN);
			
			var invokeAgentRuntimeRequest = InvokeAgentRuntimeRequest.builder()
					.agentRuntimeArn(AGENT_RUNTIME_ARN)				 
					.qualifier("DEFAULT").contentType("application/json").payload(SdkBytes.fromUtf8String(payload)).build();
			try (var responseStream = bedrockAgentCoreClient
					.invokeAgentRuntime(invokeAgentRuntimeRequest)) {
				var response = new String(responseStream.readAllBytes(), StandardCharsets.UTF_8);

				LOGGER.info("Response from AgentCore Runtime "+response);
				return OBJECT_MAPPER.readValue(response, clazz);
			}
	}
}