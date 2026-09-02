package dev.vkazulkin.agentcore.runtime;


import dev.vkazulkin.ConventionalDefaults;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.bedrock.agentcore.alpha.AgentRuntimeArtifact;
import software.amazon.awscdk.services.bedrock.agentcore.alpha.ProtocolType;
import software.amazon.awscdk.services.bedrock.agentcore.alpha.Runtime;
import software.amazon.awscdk.services.bedrock.agentcore.alpha.RuntimeAuthorizerConfiguration;
import software.amazon.awscdk.services.iam.Role;
import software.constructs.Construct;

public class RuntimeStack extends Stack {

	private static final String RUNTIME_NAME = "RuntimeForAuthorContentAgenticSearch";
	
    public RuntimeStack(Construct scope, String appName,  StackProps stackProps) {
    	var id=ConventionalDefaults.stackName(appName, "agentcore-runtime");
        super(scope, id, stackProps);   
        System.out.println(" stack id "+id);
        
        var ecrImageURI=ConventionalDefaults.getContextVariableValueWithReplacedAccountId(this, "ecrImageURI");     		
        var roleArnForTheAgentCoreRuntime=ConventionalDefaults.getContextVariableValueWithReplacedAccountId(this, "roleArnForTheAgentCoreRuntime");
       
        // The runtime, by default, creates ECR permissions only for the repository available in the account where the stack is being deployed
        var agentRuntimeArtifact = AgentRuntimeArtifact.fromImageUri(ecrImageURI);
        var role= Role.fromRoleArn(this,"roleArnForTheAgentCoreRuntimeRole", roleArnForTheAgentCoreRuntime);
     
        // Create runtime using the built image
        var runtime = Runtime.Builder.create(this, RUNTIME_NAME+"_ID")
                .runtimeName(RUNTIME_NAME)
                .authorizerConfiguration(RuntimeAuthorizerConfiguration.usingIAM())
                .description("AgentCore Runtime with HTTP protocol for running authro content search app")
                .protocolConfiguration(ProtocolType.HTTP)
                .agentRuntimeArtifact(agentRuntimeArtifact)
                .executionRole(role)
                .build();
        
        CfnOutput.Builder.create(this, "RuntimeIdOutput").value(runtime.getAgentRuntimeId()).build();
        CfnOutput.Builder.create(this, "RuntimeArnOutput").value(runtime.getAgentRuntimeArn()).build();
     }  
  
}