package dev.vkazulkin;

import dev.vkazulkin.agentcore.gateway.GatewayStack;
import dev.vkazulkin.agentcore.runtime.RuntimeStack;
import dev.vkazulkin.cognito.CognitoStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public interface CDKApp {
    
    String appName = "spring-ai-author-content-search-ai-agents-on-bedrock-agentcore";

    static void main(String... args) {

        var app = new App();
        new CognitoStack(app, appName, stackProperties());
        new RuntimeStack(app, appName, stackProperties());
        new GatewayStack(app, appName, stackProperties());
        app.synth();  
    }
    
    public static StackProps stackProperties() {
        var env = Environment
                .builder()
                .region("us-east-1")
                .build();
        return StackProps
                .builder()
                .env(env)
                .build();
    }
}

