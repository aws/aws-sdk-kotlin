#!/bin/bash

error_exit() {
  echo "$1" 1>&2
  exit 1
}

PROJECT_NAME=gh-aws-sdk-kotlin-svc-check-batch
# get the source version to be built (defaults to main branch if not specified)
SOURCE_VERSION=main
GITHUB_RELEASE=""
GITHUB_PULL_REQUEST_NUMBER=""
GITHUB_REPOSITORY_NO_ORG=""
EXTERNAL_CONTRIBUTOR_SDK_PR=""
EXTERNAL_CONTRIBUTOR_SMITHY_PR=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --project)
          if [[ "$2" == --* ]]; then
            shift 1
          else
            PROJECT_NAME="$2"
            shift 2
          fi
          ;;
        --source)
          if [[ "$2" == --* ]]; then
            shift 1
          else
            SOURCE_VERSION="$2"
            shift 2
          fi
          ;;
        --release)
          if [[ "$2" == --* ]]; then
            shift 1
          else
            GITHUB_RELEASE="$2"
            shift 2
          fi
          ;;
        --pr-number)
          if [[ "$2" == --* ]]; then
            shift 1
          else
            GITHUB_PULL_REQUEST_NUMBER="$2"
            shift 2
          fi
          ;;
        --repository)
          if [[ "$2" == --* ]]; then
            shift 1
          else
            GITHUB_REPOSITORY_NO_ORG="$2"
            shift 2
          fi
          ;;
        --external-contributor-sdk-pr-number)
          if [[ "$2" == --* ]]; then
            shift 1
          else
            EXTERNAL_CONTRIBUTOR_SDK_PR="$2"
            shift 2
          fi
          ;;
        --external-contributor-smithy-pr-number)
          if [[ "$2" == --* || -z "$2" ]]; then
            shift 1
          else
            EXTERNAL_CONTRIBUTOR_SMITHY_PR="$2"
            shift 2
          fi
          ;;
        *)
          echo "Unknown option: $1"
          exit 1
          ;;
    esac
done

export GITHUB_RELEASE
export GITHUB_PULL_REQUEST_NUMBER
export GITHUB_REPOSITORY_NO_ORG
export EXTERNAL_CONTRIBUTOR_SDK_PR
export EXTERNAL_CONTRIBUTOR_SMITHY_PR

echo "Starting CodeBuild project ${PROJECT_NAME}"

# dump all GITHUB_* & *_PR environment variables to file and pass to start job
jq -n 'env | to_entries | [.[] | select((.key | startswith("GITHUB_")) or (.key | endswith("_PR")))] | [.[] | {name: .key, value:.value, type:"PLAINTEXT"}]' >/tmp/gh_env_vars.json

START_RESULT=$(
  aws codebuild start-build-batch \
    --project-name ${PROJECT_NAME} \
    --source-version $SOURCE_VERSION \
    --environment-variables-override file:///tmp/gh_env_vars.json \
    --source-location-override "https://github.com/aws/$GITHUB_REPOSITORY_NO_ORG.git"
)

if [ "$?" != "0" ]; then
  echo "Start result was: $START_RESULT"
  error_exit "Could not start project. Exiting."
else
  echo "Build started successfully."
fi

BUILD_ID=$(echo ${START_RESULT} | jq '.buildBatch.id' -r)
echo "Build id $BUILD_ID"
echo "aws-build-id=$BUILD_ID" >> "$GITHUB_OUTPUT"

BUILD_STATUS="IN_PROGRESS"
while [ "$BUILD_STATUS" == "IN_PROGRESS" ]; do
  sleep 10

  BUILD=$(aws codebuild batch-get-build-batches --ids ${BUILD_ID})
  BUILD_STATUS=$(echo $BUILD | jq '.buildBatches[0].buildBatchStatus' -r)

  JOBS=$(echo $BUILD | jq '.buildBatches[0].buildGroups | [.[] | { identifier: .identifier, status: .currentBuildSummary.buildStatus } ]')
  TOTAL_JOBS=$(echo $JOBS | jq 'length')

  SUCCEEDED_CNT=$(echo $JOBS | jq '[.[] | select(.status == "SUCCEEDED")] | length')
  IN_PROGRESS_CNT=$(echo $JOBS | jq '[.[] | select(.status == "IN_PROGRESS")] | length')

  FAILED_CNT=$(($TOTAL_JOBS - $SUCCEEDED_CNT - $IN_PROGRESS_CNT))

  if [ "$BUILD_STATUS" == "IN_PROGRESS" ]; then
    echo "Build is still in progress (failed=$FAILED_CNT; in_progress=$IN_PROGRESS_CNT; succeeded=$SUCCEEDED_CNT; total=$TOTAL_JOBS), waiting..."
  fi
done

if [ "$BUILD_STATUS" != "SUCCEEDED" ]; then
  BUILD=$(aws codebuild batch-get-build-batches --ids ${BUILD_ID})
  FAILED_BUILDS=$(echo $BUILD | jq '.buildBatches[0].buildGroups | [.[] | {identifier: .identifier, status: .currentBuildSummary.buildStatus} | select(.status == "FAILED")]')
  echo "Failed builds in batch"
  echo $FAILED_BUILDS
  error_exit "Build failed, please review job output"
else
  echo "Build succeeded."
fi
