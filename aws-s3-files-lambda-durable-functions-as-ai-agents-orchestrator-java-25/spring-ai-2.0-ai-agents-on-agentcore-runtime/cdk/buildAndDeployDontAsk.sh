#!/bin/sh
set -e
mvn clean package && cdk deploy --all --require-approval=never -c awsAccountId={YOUR_AWS_ACCOUINT_ID} --all