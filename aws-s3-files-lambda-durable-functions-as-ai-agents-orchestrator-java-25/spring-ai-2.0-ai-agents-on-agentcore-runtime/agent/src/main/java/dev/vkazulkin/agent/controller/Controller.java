package dev.vkazulkin.agent.controller;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;


import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.agentcore.annotation.AgentCoreInvocation;
import org.springaicommunity.agentcore.context.AgentCoreContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.DescribeUserPoolClientRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientDescription;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolClientType;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserPoolDescriptionType;


@RestController
public class Controller {

	@Value("${cognito.user.pool.name}")
	private String USER_POOL_NAME;
	
	@Value("${cognito.user.pool.client.name}")
	private String USER_POOL_CLIENT_NAME;
	
	@Value("${cognito.auth.token.resource.server.id}")
	private String RESOURCE_SERVER_ID;

	@Value("${amazon.bedrock.agentcore.gateway.base.url}")
	private String AGENTCORE_GATEWAY_BASE_URL;
	
	@Value("${amazon.bedrock.agentcore.gateway.endpoint}")
	private String AGENTCORE_GATEWAY_ENDPOINT;
		
	private final ChatClient chatClient;

	private final CognitoIdentityProviderClient cognitoClient;
	  
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Controller.class);

	/**
	 * use this constructor to inject the short-term memory (or no memory)
	 * @param builder
	 * @param chatMemory
	 */
	public Controller(ChatClient.Builder builder, ChatMemory chatMemory, @Value("${aws.region}") String awsRegion) {
		var options = ToolCallingChatOptions.builder()
				 .model("amazon.nova-pro-v1:0")
				//.model("us.anthropic.claude-sonnet-4-6")
				.model("amazon.nova-pro-v1:0")
				.maxTokens(2000);

		this.chatClient = builder.defaultOptions(options)
				//.defaultSystem(SYSTEM_PROMPT)
				//short term memory
				.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())	
				.build();
				
		cognitoClient = CognitoIdentityProviderClient.builder().region(Region.of(awsRegion)).build();
	}
	

	/**
	 * POST method which has a prompt as an input parameter and outputs the agent response synchronously
	 * 
	 * @param prompt - prompt
	 * @return agent answer
	 */
	@AgentCoreInvocation
	public Object invokeSync(PromptRequest promptRequest, AgentCoreContext agentCoreContext) throws Exception {
		LOGGER.info("invocations endpoint with prompt: " + promptRequest.prompt());
		var clazz = Class.forName(promptRequest.resultType());
		LOGGER.info("result type: " + clazz);
		var token = getAuthTokenViaHttpClient();
		try (var client = McpClient.sync(getMcpClientTransport(token)).build()) {
			client.initialize();

			client.listTools().tools().forEach(tool -> LOGGER.info("tool found: " + tool));
			
			var syncMcpToolCallbackProvider = SyncMcpToolCallbackProvider.builder().mcpClients(client).build();
			
			var response= this.chatClient.prompt()
					.user(promptRequest.prompt())
					.tools(syncMcpToolCallbackProvider.getToolCallbacks())
					.call()
					.entity(clazz);
			 LOGGER.info("response: " + response);
	         return response;
		}
	}

		
	/**
	 * returns streamable http mcp client transport
	 * 
	 * @param token -bearer authorization token
	 * @return streamable http mcp client transport
	 */
    private McpClientTransport getMcpClientTransport(String token) {
        var headerValue = "Bearer " + token;
	    var httpRequestBuilder = HttpRequest.newBuilder().header("Authorization", headerValue);
	    
        return HttpClientStreamableHttpTransport.builder(AGENTCORE_GATEWAY_BASE_URL)
                .connectTimeout(Duration.ofMinutes(3))
                .endpoint(AGENTCORE_GATEWAY_ENDPOINT)
                .requestBuilder(httpRequestBuilder)
                .build();
    }
	
	
	/**
	 * returns authorization token required by the mcp client
	 * @return authorization token
	 */
	private String getAuthTokenViaHttpClient() {
		var userPool = getUserPool();
		LOGGER.info("user pool " + userPool);
		if(userPool == null) {
			throw new RuntimeException("cognito user pool with the name "+USER_POOL_NAME+ " is not found");
		}
		var userPoolClient = getUserPoolClient(userPool);
		LOGGER.info("user pool " + userPoolClient);
		
		if(userPoolClient == null) {
			throw new RuntimeException("cognito user pool client with the name "+USER_POOL_CLIENT_NAME+ " is not found");
		}

		var userPoolClientType = describeUserPoolClient(userPoolClient);
		LOGGER.info("user pool client type " + userPoolClientType);
		
		if(userPoolClientType == null) {
			throw new RuntimeException("cognito user client type for the client "+USER_POOL_CLIENT_NAME+ " is not found");
		}
		var userPoolId = userPool.id();
		userPoolId = userPoolId.replace("_", "").toLowerCase();
		var url = "https://" + userPoolId + ".auth." + Region.US_EAST_1.id() + ".amazoncognito.com/oauth2/token";
		LOGGER.info("url: " + url);

		var SCOPE_STRING = RESOURCE_SERVER_ID + "/*";
		
		var entity = "grant_type=client_credentials&" + "client_id=" + userPoolClientType.clientId() + "&"
				+ "client_secret=" + userPoolClientType.clientSecret() + "&" + "scope=" + SCOPE_STRING;

		LOGGER.info("entity " + entity);
		try (var httpClient = HttpClients.createDefault()) {
			var httpPost = ClassicRequestBuilder.post(url)
					.setHeader("Content-Type", "application/x-www-form-urlencoded").setEntity(entity).build();
			return httpClient.execute(httpPost, new AuthTokenResponseHandler());
			
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("error occured with the message: ", e.getMessage());
		}
		return null;
	}

	
	/**
	 * returns cognito user pool with specific user name
	 * 
	 * @return cognito user pool with specific user name
	 */
	private UserPoolDescriptionType getUserPool() {
		try {
			var request = ListUserPoolsRequest.builder().maxResults(10).build();
			var response = cognitoClient.listUserPools(request);
			for (var userPool : response.userPools()) {
				LOGGER.info("User pool " + userPool.name() + ", User ID " + userPool.id());
				if (userPool.name().equals(USER_POOL_NAME)) {
					return userPool;
				}
			}

		} catch (CognitoIdentityProviderException e) {
			LOGGER.error("error occured with the message: ", e.getMessage());
		}
		return null;
	}

	/**
	 * returns cognito user pool client for the given cognito user pool
	 * 
	 * @param userPool - cognito user pool
	 * @return cognito user pool client for the given cognito user pool
	 */
	private UserPoolClientDescription getUserPoolClient(UserPoolDescriptionType userPool) {
		try {
			var request = ListUserPoolClientsRequest.builder().userPoolId(userPool.id()).maxResults(10).build();

			var response = cognitoClient.listUserPoolClients(request);
			for (var userPoolClient : response.userPoolClients()) {
				LOGGER.info("User Pool Client Name " + userPoolClient.clientName() + ", User Pool Client ID "
						+ userPoolClient.clientId());
				if (userPoolClient.clientName().equals(USER_POOL_CLIENT_NAME)) {
					return userPoolClient;
				}
			}
		} catch (CognitoIdentityProviderException e) {
			LOGGER.error("error occured with the message: ", e.getMessage());
		}
		return null;
	}

	

	/** returns cognito user pool client type for the given cognito user pool client
	 * 
	 * @param userPoolClient- cognito user pool client
	 * @return cognito user pool client type for the given cognito user pool client
	 */
	private UserPoolClientType describeUserPoolClient(UserPoolClientDescription userPoolClient) {
		var request = DescribeUserPoolClientRequest.builder()
				.userPoolId(userPoolClient.userPoolId()).clientId(userPoolClient.clientId()).build();
		var response = cognitoClient.describeUserPoolClient(request);
		var optionalType = response.getValueForField("UserPoolClient",
				UserPoolClientType.class);
		if(optionalType.isEmpty()) {
			return null;
		}
		return optionalType.get();
	}
	
	private class AuthTokenResponseHandler implements HttpClientResponseHandler<String> {
		@Override
		public String handleResponse(ClassicHttpResponse response) throws HttpException, IOException {
			var inputStream = response.getEntity().getContent();
			var responseString = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
			LOGGER.info("response: " + responseString);

			var responseMap = OBJECT_MAPPER.readValue(responseString, new TypeReference<Map<String, Object>>() {});
			var token = (String) responseMap.get("access_token");
			LOGGER.info("token : " + token);
			
			var expiresInSeconds = (Integer) responseMap.get("expires_in");
			LOGGER.info("token expires in seconds : " + expiresInSeconds);
			// add handling of the auth token expiration

			return token;
		}
	}
	
}