# AWS Lambda Durable Functions as AI Agents Orchestrator with S3 Files — Java 25

A serverless AI agents orchestrator built with AWS Lambda Durable Functions, Amazon S3 Files, and Java 25. The orchestrator extracts author content (YouTube videos and upcoming talks) in parallel using AI agents and persists results to an S3-backed filesystem.

## Architecture

```
API Gateway → AuthorContentExtractor (Durable Orchestrator)
                    ├── [parallel] YouTubeVideosExtractor (AI Agent)
                    ├── [parallel] UpcomingTalksExtractor (AI Agent)
                    └── WriteAuthorContentToFile → S3 Files (NFS mount)

API Gateway → GetAuthorContentResult → S3 Bucket (read result)
```

### Lambda Functions

| Function | Handler | Role |
|---|---|---|
| `AuthorContentExtractorAIAgentsOrchestrator` | `AuthorContentExtractor` | Durable orchestrator — fans out to agents in parallel, then writes result |
| `YouTubeVideosAIAgentExtractor` | `YouTubeVideosExtractor` | AI agent — extracts YouTube videos for an author |
| `UpcomingTalksAIAgentExtractor` | `UpcomingTalksExtractor` | AI agent — extracts upcoming conference talks for an author |
| `WriteAIGeneratedAuthorContentToFile` | `WriteAuthorContentToFile` | Writes aggregated JSON to S3 Files NFS mount |
| `GetAuthorContentResultAIAgentsOrchestrator` | `GetAuthorContentResult` | Reads result JSON from S3 via API Gateway |

## Prerequisites

- Java 25
- Maven 3.9+
- AWS SAM CLI
- AWS account with permissions for Lambda, API Gateway, S3, S3 Files, IAM, VPC

## Build

```bash
mvn clean package
```

## Deploy

```bash
sam deploy
```

Uses `samconfig.toml` defaults: stack `AWSLambdaDurableFunctionsAIOrchS3FilesJava25`, region `us-east-1`.

## API

API Gateway requires an API key header: `x-api-key: a6ZbcDefQW12BN56VwsGeS`

### Retrieve author content result

```
GET /prod/author/content/result/{firstname}/{lastname}
```

Returns the JSON file written by the orchestrator for the given author.

## Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `aws-durable-execution-sdk-java` | 2.0.0 | Lambda Durable Functions SDK |
| `software.amazon.awssdk:bedrockagentcore` | 2.46.17 (BOM) | Bedrock Agent Core |
| `software.amazon.awssdk:s3` | 2.46.17 (BOM) | S3 client |
| `jackson-databind` (tools.jackson) | 3.2.0 | JSON serialization |

## How It Works

1. A client triggers `AuthorContentExtractor` (durable orchestrator) with an `Author` payload.
2. The orchestrator fans out two parallel branches using `ctx.parallel()`:
   - `YouTubeVideosExtractor` — fetches YouTube videos for the author
   - `UpcomingTalksExtractor` — fetches upcoming conference talks
3. Once both branches complete, the orchestrator invokes `WriteAuthorContentToFile`, which writes the aggregated `AuthorContent` as JSON to an S3 Files NFS mount (`/mnt/workspace`).
4. The file is stored in the S3 bucket `vadym-s3-files-ai-agents-orchestrator-workspace` under the `lambda/` prefix.
5. The result can be retrieved via `GET /author/content/result/{firstname}/{lastname}`.
