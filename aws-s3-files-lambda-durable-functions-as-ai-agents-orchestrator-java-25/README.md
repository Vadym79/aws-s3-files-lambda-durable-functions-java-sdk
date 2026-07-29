# AWS AI Agents — Java 25

This repository contains two independent projects that demonstrate AI agent patterns on AWS using Java 25.

---

## Projects

### 1. [`durable-functions-as-ai-agents-orchestrator/`](./durable-functions-as-ai-agents-orchestrator)

A serverless AI agents orchestrator built with **AWS Lambda Durable Functions**, **Amazon S3 Files**, and **Java 25**.

**What it does:**
- Accepts an author name via API Gateway
- Fans out two parallel AI agents (YouTube videos extractor + upcoming talks extractor) using Lambda Durable Functions
- Aggregates results and persists them as JSON to an S3 Files NFS mount
- Exposes a second endpoint to retrieve the result from S3

**Key AWS services:** Lambda Durable Functions, API Gateway, Amazon S3 Files, Amazon Bedrock

**Deploy:** AWS SAM (`sam deploy`)

---

### 2. [`spring-ai-2.0-ai-agents-on-agentcore-runtime/`](./spring-ai-2.0-ai-agents-on-agentcore-runtime)

A Spring AI 2.0 / Spring Boot 4.1 AI agent that runs on **Amazon Bedrock AgentCore Runtime** and searches for author content via an **AgentCore Gateway** (MCP over HTTP).

Contains two modules:

| Module | Description |
|---|---|
| [`agent/`](./spring-ai-2.0-ai-agents-on-agentcore-runtime/agent) | Spring AI agent — connects to AgentCore Gateway via MCP, uses Amazon Nova Pro (Bedrock), packaged as a Docker image |
| [`cdk/`](./spring-ai-2.0-ai-agents-on-agentcore-runtime/cdk) | AWS CDK (Java) — provisions Cognito, AgentCore Runtime, and AgentCore Gateway stacks |

**Key AWS services:** Amazon Bedrock AgentCore Runtime, AgentCore Gateway, Amazon Cognito, Amazon ECR, Amazon Nova Pro

**Deploy:** `./buildAndDeploy.sh` (inside `cdk/`)

---

## Prerequisites (both projects)

- Java 25
- Maven 3.9+
- AWS account with Bedrock model access (`amazon.nova-pro-v1:0` in `us-east-1`)
- AWS SAM CLI (project 1) / AWS CDK CLI + Docker (project 2)
