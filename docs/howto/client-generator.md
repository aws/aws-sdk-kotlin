# Generating a Kotlin Client from a Smithy Model

## Overview

This page covers how to generate a Kotlin client from any valid Smithy model.  This generated Kotlin client could then be used to interact with a service instance from a Kotlin program.  The Kotlin codegen is under active development and there remains several details that must be implemented before this approach can be used to create a fully functional client.  However, using the code generator can give insights into how API models translate into working Kotlin code that service customers may interact with in the future.

### Limitations

* The generated project requires some additions to the Gradle build file in order to successfully build the project (covered below).
* AWS-specific logic will be included in the generated client that should be disregarded.  In a future release this will be fixed.  For now please ignore AWS specific logic such as AWS regions.
* You'll need to include all model dependencies directly in a local project.

### Prerequisites

* Intellij IDE installed locally
* Kotlin AWS SDK installed locally (local maven repo)
* A valid Smithy model

## Steps

The following steps will create a project that will codegen a client when built. Either follow these steps or check out the example provided in `/examples/client-generator`.

1. Create a new Intellij Kotlin project, accepting defaults.
2. Add the following lines to the already generated Gradle `build.gradle.kts` file:
   ```kotlin
   plugins {
      ...
      id("software.amazon.smithy").version("0.5.3")
   }
   ```
   ```kotlin
   dependencies {
       ...
       // NOTE: More smithy dependencies may be required depending on what's referenced by your API models.
       implementation("aws.smithy.kotlin:aws-codegen:<latest version>")
   }
   ```
3. Add your model file(s) into a new directory at the root of your project called `model`.
   1. Add a `smithy-build.json` file into the root of your project.  Substitute `<namespace>`, `<service-name>`, and `<version>` for literals from your model and service:
      ```json
      {
           "version": "1.0",
           "plugins": {
               "kotlin-codegen": {
               "service": "<namespace>#<service-name>",
               "package": {
                   "name": "<namespace>",
                   "version": "<version>",
                   "description": "<service-name>"
               },
               "sdkId": "<service-name>",
               "build": {
                   "generateDefaultBuildFiles": true,
                   "optInAnnotations": [
                       "aws.smithy.kotlin.runtime.util.InternalApi",
                       "aws.sdk.kotlin.runtime.InternalSdkApi"
                   ],
                   "rootProject": true
               }
           }}
      }
      ```
4. Run the `build` Gradle task to generate the service client.
5. Look in `build/smithyprojections/<project name>/source/kotlin-codegen` for a generated Gradle project of the Kotlin client.
