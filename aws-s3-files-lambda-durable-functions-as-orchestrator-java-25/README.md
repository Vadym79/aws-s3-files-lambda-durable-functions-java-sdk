# AWS Lambda Durable Functions as Orchestrator with S3 Files — Java 25

A serverless orchestration demo using **AWS Lambda Durable Functions** to fan-out parallel Lambda invocations, aggregate results, and persist them via **Amazon S3 Files** (NFS-over-S3 mount). Built with Java 25 and deployed via AWS SAM.

---

## Architecture

```
API Gateway (GET /author/content/result/{firstname}/{lastname})
        │
        ▼
GetAuthorContentResult  ──────────────────────────────► S3 Bucket (read result)
        
POST trigger (direct invoke)
        │
        ▼
AuthorContentExtractor  (Durable Orchestrator)
        │
        ├──[parallel]──► UpcomingTalksExtractor  (Lambda activity)
        ├──[parallel]──► YouTubeVideosExtractor   (Lambda activity)
        │
        └──[sequential]─► WriteAuthorContentToFile      (Lambda activity, S3Files NFS mount)
                                │
                                ▼
                        /mnt/workspace/{firstname}-{lastname}.json
                        (backed by S3 Bucket via S3Files FileSystem)
```

### Lambda Functions

| Function | Handler | Role |
|---|---|---|
| `AuthorContentExtractorOrchestrator` | `AuthorContentExtractor` | Durable orchestrator — fans out parallel branches, then writes result |
| `UpcomingTalksExtractor` | `UpcomingTalksExtractor` | Activity — returns upcoming conference talks for an author |
| `YouTubeVideosExtractor` | `YouTubeVideosExtractor` | Activity — returns YouTube videos for an author |
| `WriteAuthorContentToFile` | `WriteAuthorContentToFile` | Activity — serializes `AuthorContent` to JSON and writes to S3Files mount |
| `GetAuthorContentResultOrchestrator` | `GetAuthorContentResult` | API handler — reads the result JSON from S3 and returns it |

---

## Prerequisites

- Java 25
- Apache Maven 3.9+
- AWS SAM CLI
- AWS account with permissions for Lambda, API Gateway, S3, S3Files, IAM, VPC

---

## Build & Deploy

```bash
# Build the fat JAR
mvn clean package

# Deploy (first time — guided)
sam deploy --guided

# Subsequent deploys
sam deploy
```

SAM stack name: `AWSLambdaDurableFunctionsOrchS3FilesJava25`  
Default region: `us-east-1`

---

## API

The API requires an API key (`x-api-key` header).

### Get Author Content Result

```
GET /prod/author/content/result/{firstname}/{lastname}
x-api-key: a6ZbcDefQW12BN56VwsqmK
```

Returns the JSON file written by the orchestration for the given author.

---

## Key AWS Resources

| Resource | Details |
|---|---|
| S3 Bucket | `vadym-s3-files-orchestrator-workspace` — AES256 encrypted, versioned, private |
| S3Files FileSystem | NFS-over-S3 mount backed by the bucket above |
| S3Files AccessPoint | Mounted at `/mnt/workspace` inside `WriteAuthorContentContentToFile` Lambda (path `/lambda`) |
| VPC | Private subnets A & B with mount targets and security groups (via nested `network.yaml` stack) |
| API Gateway | `AWSS3FilesLambdaDurableFunctionsOrchestratorJava25API` — API key required, 100 req/day quota |

---

## Environment Variables

| Variable | Description |
|---|---|
| `WORKSPACE_MOUNT` | Local NFS mount path inside Lambda (`/mnt/workspace`) |
| `S3_BUCKET_NAME` | S3 bucket name for result storage |
| `S3_FOLDER_NAME` | S3 key prefix (`lambda`) |
| `REGION` | AWS region |
| `UpcomingTalksExtractorFunctionArn` | ARN of the `UpcomingTalksExtractor` function |
| `YouTubeVideosExtractorFunctionArn` | ARN of the `YouTubeVideosExtractor` function |
| `WriteAuthorContentToFileFunctionArn` | ARN of the `WriteAuthorContentToFile` function |

---

## Dependencies

- `aws-durable-execution-sdk-java` 2.0.0 — Durable Functions SDK
- `software.amazon.awssdk:s3` (BOM 2.46.17) — AWS SDK v2 S3 client
- `aws-lambda-java-core` 1.4.0 / `aws-lambda-java-events` 3.16.1
- `jackson-databind` 3.2.0 (tools.jackson)
- `slf4j-simple` 2.0.17
