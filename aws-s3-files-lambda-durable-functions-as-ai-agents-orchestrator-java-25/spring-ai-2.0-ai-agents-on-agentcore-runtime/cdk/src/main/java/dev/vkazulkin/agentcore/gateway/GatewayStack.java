package dev.vkazulkin.agentcore.gateway;

import java.util.List;

import dev.vkazulkin.ConventionalDefaults;
import dev.vkazulkin.cognito.CognitoStack;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.bedrock.agentcore.alpha.CustomJwtAuthorizer;
import software.amazon.awscdk.services.bedrock.agentcore.alpha.Gateway;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;

public class GatewayStack extends Stack {

	private static final String GATEWAY_NAME = "GatewayForAuthorContentAgenticWebSearch";
    public GatewayStack(Construct scope, String appName,  StackProps stackProps) {
    	var id=ConventionalDefaults.stackName(appName, "gateway-with-web-search-tool-mcp-target");
        super(scope, id, stackProps);
        System.out.println(" stack id "+id);
       
        var roleArnForTheAgentCoreRuntime=ConventionalDefaults.getContextVariableValueWithReplacedAccountId(this, "roleArnForTheAgentCoreGateway");
        
        IRole role= Role.fromRoleArn(this,"roleArnForTheAgentCoreGateway_ID", roleArnForTheAgentCoreRuntime);
      
        var gateway= Gateway.Builder.create(this, GATEWAY_NAME+"_ID")	
           .gatewayName(GATEWAY_NAME)
           .authorizerConfiguration(CustomJwtAuthorizer.Builder
        		   .create().allowedClients(List.of(CognitoStack.userPoolClient.getUserPoolClientId()))
        		   .discoveryUrl(CognitoStack.COGNITO_DISCOVERY_URL).build())
           .role(role)
           .description("AgentCore Gateway with Web Search as MCP tool").build();     
        }
   
}