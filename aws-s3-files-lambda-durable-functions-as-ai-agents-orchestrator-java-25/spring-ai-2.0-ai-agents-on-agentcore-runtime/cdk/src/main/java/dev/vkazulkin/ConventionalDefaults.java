package dev.vkazulkin;

import software.amazon.awscdk.Stack;

public interface ConventionalDefaults {


    static String stackName(String appName,String stackName){
        return "%s-%s-stack".formatted(appName,stackName);
    }

    
    static String getContextVariableValueWithReplacedAccountId(Stack stack, String contextVariableName) {
      var awsAccountId=(String)stack.getNode().tryGetContext("awsAccountId");
      if(awsAccountId == null || awsAccountId.trim().isEmpty()) {
      	 System.out.println("please provide your aws account id as as content to the call, for example: cdk deploy -c awsAccountId=1234567890101");
      }
      var contextVariableValue= getContextVariableValue(stack, contextVariableName);
      return replaceAWSAccountID(contextVariableValue, awsAccountId);
    }
    
    
    static String getContextVariableValue(Stack stack, String contextVariableName) {
        return (String)stack.getNode().tryGetContext(contextVariableName);
      }

    
    
    private static String replaceAWSAccountID(String configParam, String awsAccountId) {
    	return configParam.replace("{AWS_ACCOUNT_ID}", awsAccountId);
    }
   
}
