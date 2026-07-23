# AWS Lambda Durable Functions + S3 Files + Spring AI + AgentCore Gateway Web Search Tool

A serverless Java application that uses **AWS Lambda Durable Functions** to orchestrate parallel AI-powered web searches via **Amazon Bedrock AgentCore Gateway** (MCP protocol), persisting results to **Amazon S3 Files** (S3-backed file system).

## Architecture Overview

```
API Gateway (API Key auth)
       │
       ▼
AuthorContentExtractor (Durable Lambda Orchestrator)
       │
       ├──[parallel]──► YouTubeVideosWebSearchExtractor ──► AgentCore Gateway (MCP) ──► Bedrock Nova Pro
       │
       ├──[parallel]──► UpcomingTalksWebSearchExtractor ──► AgentCore Gateway (MCP) ──► Bedrock Nova Pro
       │
       └──────────────► WriteContentToFile ──► S3 Files (NFS mount via VPC)
                                                      │
GetAuthorContentResult ◄──────────────────────────── S3 Bucket
```

## Key Technologies

| Technology | Purpose |
|---|---|
| AWS Lambda Durable Functions SDK | Orchestrate long-running, stateful workflows with parallel branches |
| Amazon Bedrock AgentCore Gateway | MCP server exposing web search tool to Lambda functions |
| Spring AI (Bedrock Converse + MCP Client) | AI chat client with MCP tool integration |
| AWS S3 Files | S3-backed NFS file system mounted into Lambda via VPC |
| Amazon Cognito | OAuth2 client credentials flow for AgentCore Gateway auth |
| Spring Boot 4 + aws-serverless-java-container | Spring Boot running inside Lambda |
| Java 25 / Maven | Build toolchain |

## Lambda Functions

| Function | Handler | Description |
|---|---|---|
| `AuthorContentWebSearchExtractor` | `AuthorContentExtractor` | Durable orchestrator — fans out parallel web searches, then writes results to file |
| `YouTubeVideosWebSearchExtractor` | `YouTubeVideosStreamLambdaHandler` | Spring Boot Lambda — searches YouTube videos via AgentCore MCP Gateway |
| `UpcomingTalksWebSearchExtractor` | `UpcomingTalksStreamLambdaHandler` | Spring Boot Lambda — searches upcoming conference talks via AgentCore MCP Gateway |
| `WriteContentFromWebSearchToFile` | `WriteContentToFile` | Writes aggregated author content to S3 Files NFS mount (`/mnt/workspace`) |
| `GetAuthorContentWebSearchResult` | `GetAuthorContentResult` | Reads result JSON from S3 bucket and returns it via API Gateway |

## API Endpoints

All endpoints require the `x-api-key` header.

| Method | Path | Description |
|---|---|---|
| `POST` | `/author/content/youtubeVideos` | Trigger YouTube video search for an author |
| `POST` | `/author/content/upcomingTalks` | Trigger upcoming talks search for an author |
| `GET` | `/author/content/result/{firstname}/{lastname}` | Retrieve previously extracted author content |

### Request Body (POST endpoints)

```json
{
  "firstName": "John",
  "lastName": "Doe"
}
```

## Prerequisites

- AWS CLI configured
- AWS SAM CLI installed
- Java 25 + Maven
- Amazon Bedrock AgentCore Gateway deployed with a web search MCP tool
- Amazon Cognito User Pool (`UserPoolForAgentCoreMCP`) with a machine-to-machine client (`UserPoolClientWithUserAndPasswordForAgentCoreMCP`)

## Build & Deploy

```bash
# Build
mvn clean package

# Deploy (first time)
sam deploy --guided

# Subsequent deploys
sam deploy
```

SAM stack name: `AWSLambdaDurableFunctionsS3FilesSpringAIWebSearchOnAgentCore`  
Default region: `us-east-1`

## Configuration (`application.properties`)

| Property | Description |
|---|---|
| `spring.ai.bedrock.converse.chat.options.model` | Bedrock model for web search (`amazon.nova-lite-v1:0` default, `amazon.nova-pro-v1:0` used at runtime) |
| `amazon.bedrock.agentcore.gateway.base.url` | AgentCore Gateway base URL |
| `amazon.bedrock.agentcore.gateway.endpoint` | MCP endpoint path (`/mcp`) |
| `cognito.user.pool.name` | Cognito User Pool name for auth token retrieval |
| `cognito.user.pool.client.name` | Cognito App Client name |
| `cognito.auth.token.resource.server.id` | OAuth2 resource server ID for scope |

## Infrastructure Resources (SAM)

- **VPC** — private subnets + security groups (nested `network.yaml` stack)
- **S3 Bucket** — `vadym-s3files-web-search-on-agentcore-workspace` (AES256, versioning, no public access)
- **S3 Files FileSystem** — S3-backed NFS, mounted at `/mnt/workspace` in `WriteContentToFile` Lambda
- **API Gateway** — REST API with API key auth, usage plan (100 req/day, 10 RPS rate, 50 burst)

## How It Works

1. Client calls `AuthorContentWebSearchExtractor` (durable orchestrator) with an `Author` payload.
2. The orchestrator fans out two parallel branches using the Durable Functions SDK.
3. Each branch invokes a Spring Boot Lambda that connects to AgentCore Gateway via MCP (Streamable HTTP transport), authenticating with a Cognito OAuth2 token.
4. Spring AI sends a structured prompt to Amazon Bedrock Nova Pro, which calls the web search MCP tool and returns JSON-structured results.
5. The orchestrator collects both results, then invokes `WriteContentToFile` which writes the aggregated `AuthorContent` JSON to the S3 Files NFS mount.
6. The file is immediately available in S3 and can be retrieved via `GetAuthorContentResult`.
