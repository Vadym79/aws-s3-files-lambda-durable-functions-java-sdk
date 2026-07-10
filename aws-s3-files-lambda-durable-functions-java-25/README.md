# AWS S3 Files + Lambda Durable Functions — Java 25

A sample project demonstrating **AWS Lambda Durable Functions** with **Amazon S3 Files** (S3-backed filesystem) using **Java 25** and the AWS SAM framework.

## Overview

The project extracts author content (upcoming talks and YouTube videos) using durable Lambda functions that survive restarts and support long-running workflows with human-in-the-loop approval steps. Results are persisted to an S3-backed filesystem and later retrieved via an API Gateway endpoint.

## Architecture

```
API Gateway (API Key auth)
    └── GET /result/{firstname}/{lastname}  →  GetAuthorContentResult (Lambda)
                                                    └── Reads from S3 bucket

AuthorContentExtractor (Durable Lambda)       ← sync steps
AsyncAuthorContentExtractor (Durable Lambda)  ← parallel async steps
    ├── step: searchForUpcomingTalks
    ├── step: searchForYouTubeVideos
    ├── writes result to S3Files mount (/mnt/workspace)
    └── waitForCallback: upcoming talks approval
```

## Lambda Functions

| Function | Handler | Description |
|---|---|---|
| `AuthorContentExtractor` | `dev.vkazulkin.handler.AuthorContentExtractor` | Runs steps sequentially with durable execution |
| `AsyncAuthorContentExtractor` | `dev.vkazulkin.handler.AsyncAuthorContentExtractor` | Runs steps in parallel using `stepAsync` |
| `GetAuthorContentResult` | `dev.vkazulkin.handler.GetAuthorContentResult` | Reads result JSON from S3 via API Gateway |

## Key AWS Services

- **AWS Lambda Durable Functions** — long-running, resumable executions (up to 1 hour timeout)
- **Amazon S3 Files** (`AWS::S3Files::FileSystem`) — S3-backed POSIX filesystem mounted at `/mnt/workspace`
- **Amazon API Gateway** — REST API with API key authentication and usage plan
- **Amazon S3** — stores extracted author content as JSON files
- **Amazon VPC** — Lambda functions run in a private VPC to access the S3Files mount targets

## Prerequisites

- Java 25
- Apache Maven
- AWS SAM CLI
- AWS account with access to Lambda Durable Functions and S3 Files (preview features)

## Build

```bash
mvn clean package
```

## Deploy

```bash
sam deploy
```

Uses `samconfig.toml` defaults: region `us-east-1`, stack `AWSS3FilesLambdaDurableFunctionsJava25`.

## Usage

### Invoke a durable extractor

Invoke `AuthorContentExtractor` or `AsyncAuthorContentExtractor` directly with an `Author` payload:

```json
{ "firstName": "Vadym", "lastName": "Kazulkin" }
```

The function writes `{firstName}-{lastName}.json` to the S3Files mount, which is synced to the S3 bucket under the `lambda/` prefix.

### Retrieve the result

```
GET https://{api-id}.execute-api.us-east-1.amazonaws.com/prod/result/{firstname}/{lastname}
x-api-key: a6ZbcDefQW12BN56VwsqmK
```

### Send a durable callback (approval)

Run `SendDurableExecutionCallback.main()` with the `callbackId` from the waiting execution to approve upcoming talks and resume the durable function.

## Project Structure

```
src/main/java/dev/vkazulkin/
├── handler/
│   ├── AbstractAuthorContentExtractor.java  # shared steps & callback logic
│   ├── AuthorContentExtractor.java          # sync durable handler
│   ├── AsyncAuthorContentExtractor.java     # async parallel durable handler
│   └── GetAuthorContentResult.java          # API Gateway result handler
├── callback/
│   └── SendDurableExecutionCallback.java    # utility to send approval callback
└── entity/
    ├── Author.java
    ├── AuthorContent.java
    ├── UpcomingTalk.java / UpcomingTalks.java
    ├── UpcomingTalkApprovalStatus.java
    └── YouTubeVideo.java / YouTubeVideos.java
src/main/resources/stacks/
    └── network.yaml                         # VPC / subnets / security groups nested stack
template.yaml                                # SAM template
samconfig.toml                               # SAM deploy defaults
```

## Key Dependencies

| Dependency | Version |
|---|---|
| `aws-durable-execution-sdk-java` | 2.0.0 |
| `aws-lambda-java-core` | 1.4.0 |
| `aws-lambda-java-events` | 3.16.1 |
| `software.amazon.awssdk` BOM | 2.46.17 |
| `jackson-databind` (tools.jackson) | 3.2.0 |
