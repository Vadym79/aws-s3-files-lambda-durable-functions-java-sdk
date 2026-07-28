#!/bin/sh
set -e
mvn clean package && cdk deploy -c awsAccountId={YOUR_AWS_ACCOUINT_ID} --all