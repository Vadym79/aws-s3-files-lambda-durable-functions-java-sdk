package dev.vkazulkin.cognito;


import java.util.List;

import dev.vkazulkin.ConventionalDefaults;
import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.cognito.AuthFlow;
import software.amazon.awscdk.services.cognito.CognitoDomainOptions;
import software.amazon.awscdk.services.cognito.OAuthFlows;
import software.amazon.awscdk.services.cognito.OAuthScope;
import software.amazon.awscdk.services.cognito.OAuthSettings;
import software.amazon.awscdk.services.cognito.ResourceServerScope;
import software.amazon.awscdk.services.cognito.UserPool;
import software.amazon.awscdk.services.cognito.UserPoolClient;
import software.amazon.awscdk.services.cognito.UserPoolDomainOptions;
import software.amazon.awscdk.services.cognito.UserPoolResourceServerOptions;
import software.constructs.Construct;

public class CognitoStack extends Stack {
	
	public static String COGNITO_DISCOVERY_URL;
	public static UserPoolClient userPoolClient;
	
	private static final String USER_POOL_NAME="UserPoolForAuthorContentAgenticSearch";
	private static final String USER_CLIENT_POOL_NAME="UserClientPoolForAuthorContentAgenticSearch";
	private static final String AUTH_TOKEN_RESOURCE_SERVER_ID="ResourceServerIdAuthorContentAgenticSearch";

    public CognitoStack(Construct scope, String appName,  StackProps stackProps) {
    	var id=ConventionalDefaults.stackName(appName, "user-client-pool");
        super(scope, id, stackProps);   
        System.out.println(" stack id "+id);
        var region=Stack.of(this).getRegion();
        
        var userPool=UserPool.Builder.create(this, USER_POOL_NAME+"_ID").userPoolName(USER_POOL_NAME).build();
       
        var fullAccessScope = ResourceServerScope.Builder.create().scopeName("*").scopeDescription("Full access").build();
        var userServer = userPool.addResourceServer(AUTH_TOKEN_RESOURCE_SERVER_ID+"_ID", UserPoolResourceServerOptions.builder()
                 .identifier(AUTH_TOKEN_RESOURCE_SERVER_ID)
                 .scopes(List.of(fullAccessScope))
                 .build());
   
           
        var userPoolId= userPool.getUserPoolId();
        
        // doesn't work, because userPoolId is a token at the cdk synth time. And it fails at synth time with
        //Caused by: java.lang.RuntimeException: DomainPrefixCognitoDomainContain: domainPrefix for cognitoDomain can contain only lowercase alphabets, numbers and hyphens
        // see the created issues https://github.com/aws/aws-cdk/issues/37514
        /* 
        userPool.addDomain("UserPoolForAgentCoreMCPDomain", UserPoolDomainOptions.builder()
           .cognitoDomain(CognitoDomainOptions.builder()
        		   .domainPrefix(userPoolId.replace("_", "").toLowerCase()).build()).build());
        */
              
                
        COGNITO_DISCOVERY_URL = "https://cognito-idp."+region+".amazonaws.com/"+userPoolId+"/.well-known/openid-configuration";
              
        userPoolClient=UserPoolClient.Builder.create(this, USER_CLIENT_POOL_NAME+"_ID")
        		.authFlows(AuthFlow.builder().userPassword(false).userSrp(false)
        				.adminUserPassword(false).custom(false).user(false)
        				.build())       		
        		.oAuth(OAuthSettings.builder()
        				  .flows(OAuthFlows.builder().clientCredentials(true)
        						  .implicitCodeGrant(false)
        						  .authorizationCodeGrant(false).build())
        				 .scopes(List.of(OAuthScope.resourceServer(userServer, fullAccessScope)))
        				 .build())
        		.userPoolClientName(USER_CLIENT_POOL_NAME)
        		.generateSecret(true)		
        		.userPool(userPool).build();
        
        
        
        var cognitoDomainPrefix=ConventionalDefaults.getContextVariableValue(this, "cognitoDomainPrefix");
        userPool.addDomain("UserPoolForAgentCoreMCPDomain", UserPoolDomainOptions.builder()
                .cognitoDomain(CognitoDomainOptions.builder()
             	      .domainPrefix(cognitoDomainPrefix.replace("_", "").toLowerCase())
                	  //.domainPrefix(cognitoDomainPrefix)
             	     .build()).build());
        
        CfnOutput.Builder.create(this, "CognitoUserPoolIdOutput").value(userPoolId).build();
        CfnOutput.Builder.create(this, "CognitoUserPoolClientIdOutput").value(userPoolClient.getUserPoolClientId()).build();
        //CfnOutput.Builder.create(this, "CognitoUserPoolClientSecretOutput").value(userPoolClient.getUserPoolClientSecret().toString()).build();
        CfnOutput.Builder.create(this, "CognitoDiscoveryURLOutput").value(COGNITO_DISCOVERY_URL).build();
        CfnOutput.Builder.create(this, "CognitoUserPoolName").value(USER_POOL_NAME);
        CfnOutput.Builder.create(this, "CognitoUserClientPoolName").value(USER_CLIENT_POOL_NAME);
        CfnOutput.Builder.create(this, "CognitoAuthTokenResourceServerId").value(AUTH_TOKEN_RESOURCE_SERVER_ID);
    }  
}