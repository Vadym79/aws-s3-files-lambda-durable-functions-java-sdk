# Spring AI 2.0 Author Content Search with Amazon Bedrock AgentCore

An AI agent built with **Spring AI 2.0** and **Spring Boot 4.1** that performs author content search (YouTube videos, upcoming talks) by connecting to an **Amazon Bedrock AgentCore Gateway** via MCP (Model Context Protocol). The agent is designed to run on the **AgentCore Runtime**.

## Architecture

```
Client → AgentCore Runtime → Agent (this app)
                                  ↓
                         Amazon Nova Pro (Bedrock)
                                  ↓
                    AgentCore Gateway (MCP over HTTP)
                         (secured by Cognito)
```

- The agent exposes an `@AgentCoreInvocation` endpoint consumed by the AgentCore Runtime
- On each invocation, it fetches a Cognito OAuth2 token (client credentials flow) and connects to the AgentCore Gateway MCP endpoint
- Spring AI's `SyncMcpToolCallbackProvider` bridges MCP tools into the chat client
- Short-term conversation memory is maintained via `MessageChatMemoryAdvisor`

## Prerequisites

- Java 25
- Maven
- AWS account with:
  - Amazon Bedrock access (`amazon.nova-pro-v1:0` model enabled in `us-east-1`)
  - Amazon Cognito User Pool: `UserPoolForAuthorContentAgenticSearch`
  - AgentCore Gateway deployed and accessible

## Configuration

Edit `src/main/resources/application.properties`:

| Property | Description |
|---|---|
| `spring.ai.bedrock.aws.region` | AWS region (default: `us-east-1`) |
| `spring.ai.bedrock.converse.chat.options.model` | Bedrock model ID |
| `cognito.user.pool.name` | Cognito User Pool name |
| `cognito.user.pool.client.name` | Cognito App Client name |
| `cognito.auth.token.resource.server.id` | Cognito Resource Server ID (used as OAuth2 scope prefix) |
| `amazon.bedrock.agentcore.gateway.base.url` | AgentCore Gateway base URL |
| `amazon.bedrock.agentcore.gateway.endpoint` | MCP endpoint path (default: `/mcp`) |

## Build & Run

```bash
# Build
./mvnw clean package

# Run locally
./mvnw spring-boot:run
```

## Docker

```bash
# Build image
docker build -t author-content-search-agent .

# Run container (pass AWS credentials via environment)
docker run -e AWS_REGION=us-east-1 \
           -e AWS_ACCESS_KEY_ID=<access_key> \
           -e AWS_SECRET_ACCESS_KEY=<secret_key> \
           author-content-search-agent
```

## Key Dependencies

| Dependency | Purpose |
|---|---|
| `spring-ai-agentcore-runtime-starter` | AgentCore Runtime integration (`@AgentCoreInvocation`) |
| `spring-ai-starter-model-bedrock-converse` | Amazon Bedrock chat model |
| `spring-ai-starter-mcp-client-webflux` | MCP client (Streamable HTTP transport) |
| `software.amazon.awssdk:cognitoidentityprovider` | Cognito token retrieval |
| `software.amazon.awssdk:bedrockagentcore` | AgentCore SDK |

## How It Works

1. AgentCore Runtime invokes the agent with a `PromptRequest` (prompt + expected result type)
2. The agent fetches a Cognito `client_credentials` OAuth2 token
3. An MCP client connects to the AgentCore Gateway using the Bearer token
4. Available MCP tools (e.g., search YouTube videos, upcoming talks) are discovered and registered
5. Spring AI's chat client calls Amazon Nova Pro with the tools; the model decides which tools to invoke
6. The structured response (e.g., `YouTubeVideos`, `UpcomingTalks`) is returned to the caller
