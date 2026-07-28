# Author Content Agent - AWS CDK Infrastructure

AWS CDK for Java that provisions the infrastructure for the Author Content Search AI agent (Spring AI 2.0 on Amazon Bedrock AgentCore Runtime).

## Architecture

```
Client → AgentCore Runtime (ECR image) → Agent
                                            ↓
                                   Amazon Nova Pro (Bedrock)
                                            ↓
                               AgentCore Gateway (MCP over HTTP)
                                   (JWT auth via Cognito)
```

Three stacks are deployed:

| Stack | Resources |
|---|---|
| `user-client-pool` | Cognito User Pool + App Client (client credentials OAuth2 flow) + Resource Server |
| `agentcore-runtime` | Bedrock AgentCore Runtime backed by a container image from ECR |
| `gateway-with-web-search-tool-mcp-target` | Bedrock AgentCore Gateway with JWT authorizer (Cognito) |

## Prerequisites

- Java 25
- Maven
- Node.js / AWS CDK CLI (`npm install -g aws-cdk`)
- Docker image pushed to ECR (see `agent/` module)
- IAM roles pre-created:
  - `spring-ai-2.0-author-content-search-on-agentcore-runtime-role`
  - `AmazonBedrockAgentCoreGatewayDefaultServiceRole...`

## Configuration

Edit `cdk.json` before deploying:

| Key | Description |
|---|---|
| `ecrImageURI` | Full ECR image URI for the agent container |
| `roleArnForTheAgentCoreRuntime` | IAM role ARN for the AgentCore Runtime |
| `roleArnForTheAgentCoreGateway` | IAM role ARN for the AgentCore Gateway |

Replace `{AWS_ACCOUNT_ID}` placeholders with your actual AWS account ID.

## Deploy

```bash
./buildAndDeploy.sh
```

This runs `mvn clean package` then `cdk deploy --all`.

## Destroy

```bash
./destroy.sh
```

## Stack Outputs

After deployment, the following values are printed and available in CloudFormation outputs:

- `CognitoUserPoolIdOutput` — User Pool ID
- `CognitoUserPoolClientIdOutput` — App Client ID (use in `agent/application.properties`)
- `CognitoDiscoveryURLOutput` — OIDC discovery URL (use in `agent/application.properties`)
- `RuntimeIdOutput` — AgentCore Runtime ID

## Key Dependencies

| Dependency | Version |
|---|---|
| `aws-cdk-lib` | 2.261.0 |
| `bedrock-agentcore-alpha` | 2.261.0-alpha.0 |
| `constructs` | 10.6.0 |
| Java | 25 |
