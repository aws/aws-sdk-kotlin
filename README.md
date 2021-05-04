# AWS SDK for Kotlin

## License

This library is licensed under the Apache 2.0 License. 


## Getting Started

See the [Getting Started Guide](docs/GettingStarted.md)


## Development

### Generate SDK(s)

Generated sources are not checked into the repository, you first have to generate the clients before you can build them.


```sh
./gradlew :codegen:sdk:bootstrap
```

NOTE: This task will respect the AWS services specified by project properties. See options below.
NOTE: To re-run codegen for the same set of services multiple times add the `--rerun-tasks` flag.


After generating the services you care about they are available to build:

e.g.
```sh
./gradlew :services:lambda:build
```


Where the task follows the pattern: `:services:SERVICE:build`

To see list of all projects run `./gradlew projects`

##### Generating a single service
See the local.properties definition above to specify this in a config file.

```sh
./gradlew -Paws.services=lambda  :codegen:sdk:bootstrap
```

##### Testing Locally
Testing generated services generally requires publishing artifacts (e.g. client-runtime) of `smithy-kotlin`, `aws-crt-kotlin`, and `aws-sdk-kotin` to maven local.

#### Generating API Documentation

API documentation is generated using [Dokka](http://kotlin.github.io/dokka) which is the official documentation tool maintained by JetBrains for documenting Kotlin code.

Unlike Java, Kotlin uses it's own [KDoc](https://kotlinlang.org/docs/kotlin-doc.html) format.


To generate API reference documentation for the AWS Kotlin SDK:


```sh
./gradlew --no-daemon --no-parallel dokkaHtmlMultiModule
```

This will output HTML formatted documentation to `build/dokka/htmlMultiModule`

NOTE: You currently need an HTTP server to view the documentation in browser locally. You can either use the builtin server in Intellij or use your favorite local server (e.g. `python3 -m http.server`). See [Kotlin/dokka#1795](https://github.com/Kotlin/dokka/issues/1795)

### Build properties

You can define a `local.properties` config file at the root of the project to modify build behavior. 

An example config with the various properties is below:

```
# comma separated list of paths to `includeBuild()`
# This is useful for local development of smithy-kotlin in particular 
compositeProjects=../smithy-kotlin

# comma separated list of services to generate from codegen/sdk/aws-models. When not specified all services are generated
# service names match the filenames in the models directory `service.VERSION.json`
aws.services=lambda
```


