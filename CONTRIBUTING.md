# Contributing Guidelines

Thank you for your interest in contributing to the AWS SDK for Kotlin. Whether it's a bug report, new feature, correction, or additional 
documentation, we greatly value feedback and contributions from our community.

Please read through this document before submitting any issues or pull requests to ensure we have all the necessary 
information to effectively respond to your bug report or contribution.

## The AWS SDK for Kotlin is built from multiple GitHub repositories

1. This repository ([aws/aws-sdk-kotlin](https://github.com/aws/aws-sdk-kotlin))
   
This repository contains code generation for AWS specific services, and a corresponding runtime to support the generated code.

AWS service clients are generated from [Smithy](https://awslabs.github.io/smithy/) models. Much of the code generation is AWS agnostic though.
As such the code in `aws-sdk-kotlin` is a layer on top of generic Smithy based code generation tooling.


2. Smithy Kotlin Codegen repo ([smithy-lang/smithy-kotlin](https://github.com/smithy-lang/smithy-kotlin))

The `smithy-kotlin` repository contains the generic Smithy code generation tools for Kotlin.

If you want to contribute by diving into the codegen machinery and helping develop the SDK please refer to the [contributing guide](https://github.com/smithy-lang/smithy-kotlin/blob/main/CONTRIBUTING.md) in that repo.


## Reporting Bugs/Feature Requests

We welcome you to use the GitHub issue tracker to report bugs or suggest features.

When filing an issue, please check [existing open](https://github.com/aws-samples/aws-sdk-kotlin/issues), or [recently closed](https://github.com/aws-samples/aws-sdk-kotlin/issues?utf8=%E2%9C%93&q=is%3Aissue%20is%3Aclosed%20), issues to make sure somebody else hasn't already 
reported the issue. Please try to include as much information as you can. Details like these are incredibly useful:

* A reproducible test case or series of steps
* The version of our code being used
* Any modifications you've made relevant to the bug
* Anything unusual about your environment or deployment


## Contributing via Pull Requests
Contributions via pull requests are much appreciated. Before sending us a pull request, please ensure that:

1. You are working against the latest source on the *main* branch.
2. You check existing open, and recently merged, pull requests to make sure someone else hasn't addressed the problem already.
3. You open an issue to discuss any significant work - we would hate for your time to be wasted.

To send us a pull request, please:

1. Fork the repository.
2. Modify the source; please focus on a specific change in each pull request. If you incorporate multiple unrelated
   features or reformat all the code, it will be hard for us to focus on your change.
   * Ensure your modifications are accompanied by a [changelog entry](#Changelog) where necessary.
3. Ensure existing local tests pass. Add additional tests to cover the change.
4. Commit to your fork using clear commit messages.
5. Send us a pull request, answering any default questions in the pull request interface.
6. Pay attention to any automated CI failures reported in the pull request, and stay involved in the conversation.

GitHub provides additional document on [forking a repository](https://help.github.com/articles/fork-a-repo/) and 
[creating a pull request](https://help.github.com/articles/creating-a-pull-request/).

### API and style guidelines

The AWS SDK for Kotlin is a multiplatform library intended to work for a wide variety of environments and use cases.
Please see JetBrains's Kotlin-specific
[library creators' guidelines](https://kotlinlang.org/docs/jvm-api-guidelines-introduction.html) for general guidance,
in particular the section on
[backward compatibility](https://kotlinlang.org/docs/jvm-api-guideliens-backward-compatibility.html). These guidelines
are not necessarily firm requirements which all contributions must follow, but you should be prepared to discuss PRs
which deviate from the guidelines.

### Changelog
Merges to this repository must include one or more changelog entries which describe the modifications made.

Entries are placed in the top-level `.changes/` directory. An entry is a file containing a JSON object with the
following fields:

| Field name                 | Type       | Required | Enum                                         | Description                                                                                                                                                                                                                                                                                                                                     |
|----------------------------|------------|----------|----------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `id`                       | `string`   | yes      |                                              | A unique identifier for this entry. We recommend you generate a UUID for this field.                                                                                                                                                                                                                                                            |
| `type`                     | `string`   | yes      | `bugfix`, `feature`, `documentation`, `misc` | The type of change being made.                                                                                                                                                                                                                                                                                                                  |
| `description`              | `string`   | yes      |                                              | A description of the change being made.<ul><li>Prefix with `**Breaking**:` if the change is breaking</li><li>Use the imperative present tense (e.g., "change" not "changed" nor "changes")</li><li>Capitalize first letter</li><li>No dot (.) at the end unless there are multiple sentences</li></ul>                                          |
| `issues`                   | `string[]` | no       |                                              | A list of references to any related issues in the relevant repositories. A reference can be specified in several ways:<ul><li>The issue number, if local to this repository (eg. `#12345`)</li><li>A fully-qualified issue ID (eg.`smithy-lang/smithy-kotlin#12345`)</li><li>A fully-qualified URL (eg. `https://issuetracker.com/12345`)</li></ul> |
| `module`                   | `string`   | no       |                                              | The area of the code affected by your changes. If unsure, leave this value unset.                                                                                                                                                                                                                                                               |
| `requiresMinorVersionBump` | `boolean`  | no       |                                              | Indicates the change will require a new minor version. This is usually the case after a breaking change. Defaults to false if flag is not included.                                                                                                                                                                                             |

The filename of an entry is arbitrary. We recommend `<id>.json`, where `<id>` corresponds to the `id` field of the entry
itself.

Entries in the `.changes/` directory are automatically rolled into the main `CHANGELOG.md` file in every release.

If you believe that your modifications do not warrant a changelog entry, you can add the `no-changelog` label to your
pull request. The label will suppress the CI that blocks merging in the absence of a changelog, though the reviewer(s)
of your request may disagree and ask that you add one anyway.

#### Example
```json
{
  "id": "263ea6ab-4b75-41a8-9c37-821c30d7b9e5",
  "type": "feature",
  "description": "Add multiplatform support for URL parsing",
  "issues": [
    "aws/aws-sdk-kotlin#12345"
  ]
}
```

### Git Commit Guidelines
This project uses [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/) for its commit message format and expects all contributors to follow these guidelines.

Each commit message consists of a **header**, a **body** (optional), and a **footer** (optional). The header has a special format that includes a **type**, a **scope** and a **subject**:

```
<type>(<scope>): <subject>
<BLANK LINE>
<body>
<BLANK LINE>
<footer>
```

Any line of the commit message should not be longer 100 characters. This allows the message to be easier to read on GitHub as well as in various git tools.

#### Type

Must be one of the following:

- **feat**: A new feature
- **fix**: A bug fix
- **docs**: Documentation only changes
- **style**: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
- **refactor**: A code change that neither fixes a bug or adds a feature
- **perf**: A code change that improves performance
- **test**: Adding missing tests
- **chore**: Changes to the build process or auxiliary tools and libraries such as documentation generation
- **ci**: Changes to CI/CD scripts and tooling

#### Scope

The scope is optional but should be included when possible and refer to a module that is being touched. Examples:

- codegen
- rt (optionally the target platform e.g. rt-android)

#### Subject

The subject contains succinct description of the change:

- Use the imperative present tense (e.g., "change" not "changed" nor "changes")
- Don't capitalize first letter
- No dot (.) at the end

#### Body (optional)

Just as in the **subject**, use the imperative, present tense: "change" not "changed" nor "changes". The body should include the motivation for the change and contrast this with previous behavior.

#### Footer (optional)

The footer should contain any information about **Breaking Changes** and is also the place to reference GitHub issues that this commit **Closes**.

The last line of commits introducing breaking changes should be in the form `BREAKING CHANGE: <desc>`

Breaking changes should also add an exclamation mark `!` after the type/scope (e.g. `refactor(rt)!: drop support for Android API < 20`)

### Automated PR checks

A number of automated workflows run when a PR is submitted. Generally speaking, each of these must pass before the PR is
allowed to be merged. If your PR fails one of the checks, please attempt to address the problem and push a new commit to
the PR. If you need help understanding or resolving a PR check failure, please reach out via a PR comment or a GitHub
discussion. Please file a new issue if you believe there's a pre-existing bug in a PR check.

#### Lint

This repo uses [**ktlint**](https://github.com/pinterest/ktlint) (via the
[ktlint Gradle plugin](https://github.com/JLLeitschuh/ktlint-gradle)). To run a lint check locally, run
`./gradlew ktlint`.

#### CI linux/macos/windows-compat

To verify cross-OS compatibility, we run protocol tests on Linux, MacOS, and Windows runners provided by GitHub. Running
these checks independently requires access to hosts with those operating systems. On a host with the correct operating
system, run `./gradlew build publishToMavenlocal testAllProtocols`.

#### AWS CodeBuild BuildBatch

To verify that every service client behaves as expected, we codegen, compile, and test all services. Compiling every
service client takes a long time and is dispatched to a build fleet when run as a PR check on GitHub. To run this check
locally, run `./gradlew :codegen:sdk:bootstrap build`.

It is recommended to build only a subset of the services when testing locally, typically one or two directly affected by
the change under review. To codegen only select services, pass the `-Paws.services` argument with one or more services,
comma-delimited and prefixed by `+`. For example:

```shell
./gradlew :codegen:sdk:bootstrap -Paws.services=+s3,+dynamodb,+sqs
./gradlew build
```

See [the **:coddegen:sdk** build file](codegen/sdk/build.gradle.kts) for more details.

#### Changelog verification

This check enforces the changelog requirements [described above](#Changelog).

#### Binary Compatibility Validation
This repository uses [Kotlin's binary compatibility validator plugin](https://github.com/Kotlin/binary-compatibility-validator)
to help preserve backwards compatibility across releases.

The plugin will automatically run during a build with no extra steps needed from the developer.
If a backwards incompatible change is introduced, the build will fail.

If the backwards incompatibility is expected, the appropriate `.api` files must be updated as part of the PR.
The `.api` files can be updated by running `./gradlew apiDump`.

The binary compatibility validator can also be run manually using `./gradlew apiCheck`.

#### Release readiness

This checks for cross repo changes by looking at matching branch names across repos.
It verifies that downstream changes are ready to be released. If your change spans
multiple repos (aws-sdk-kotlin, smithy-kotlin, aws-crt-kotlin), the upstream 
changes must be merged and released first. For release assistance ask the team 
to run a release for you.

If this check is failing, and you have cross repo changes that do not rely on 
each other, you can add the `ready-for-release` label to your PR to override it.

## Finding contributions to work on
Looking at the existing issues is a great way to find something to contribute on. As our projects, by default, use the default GitHub issue labels ((enhancement/bug/duplicate/help wanted/invalid/question/wontfix), looking at any ['help wanted'](https://github.com/aws-samples/aws-sdk-kotlin/labels/help%20wanted) issues is a great place to start. 


## Code of Conduct
This project has adopted the [Amazon Open Source Code of Conduct](https://aws.github.io/code-of-conduct). 
For more information see the [Code of Conduct FAQ](https://aws.github.io/code-of-conduct-faq) or contact 
opensource-codeofconduct@amazon.com with any additional questions or comments.

## Security Issues

If you discover a potential security issue in this project we ask that you **do not** create a public GitHub issue.

Please refer to our [security policy](https://github.com/aws/aws-sdk-kotlin/security/policy) for how to notify us.

## Licensing

See the [LICENSE](https://github.com/aws-samples/aws-sdk-kotlin/blob/main/LICENSE) file for our project's licensing. We will ask you confirm the licensing of your contribution.

We may ask you to sign a [Contributor License Agreement (CLA)](http://en.wikipedia.org/wiki/Contributor_License_Agreement) for larger changes.
