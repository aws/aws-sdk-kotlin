/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import aws.smithy.kotlin.runtime.testing.TempDirCleanupMode
import aws.smithy.kotlin.runtime.testing.withTempDir
import kotlinx.coroutines.runBlocking
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaGeneratorPluginTest {

    private fun getResource(resourceName: String): String = checkNotNull(this::class.java.getResource(resourceName)?.readText()) { "Could not read $resourceName" }
    private val kotlinVersion = getResource("kotlin-version.txt")
    private val sdkVersion = getResource("sdk-version.txt")
    private val smithyKotlinVersion = getResource("smithy-kotlin-version.txt")

    private fun withTestProject(block: TestProject.() -> Unit) = runBlocking {
        withTempDir(TempDirCleanupMode.ON_SUCCESS) { dir ->
            val testProjectDir = File(dir.toString())
            val settingsFile = File(testProjectDir, "settings.gradle.kts").also { it.writeText("") }
            val buildFile = File(testProjectDir, "build.gradle.kts").also { it.writeText("") }

            val buildFileContent = """
                repositories {
                    mavenCentral()
                    mavenLocal()
                }
                
                plugins {
                    id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
                    id("aws.sdk.kotlin.hll.dynamodbmapper.schema.generator")
                }
                
                dependencies {
                    implementation("aws.sdk.kotlin:dynamodb-mapper:$sdkVersion")
                    implementation("aws.sdk.kotlin:dynamodb-mapper-annotations:$sdkVersion")
                    implementation("aws.sdk.kotlin:dynamodb-mapper-schema-generator-plugin:$sdkVersion")
                }
                
            """.trimIndent()
            buildFile.writeText(buildFileContent)

            val runner = GradleRunner
                .create()
                .withProjectDir(testProjectDir)
                .withPluginClasspath()
                .forwardOutput()
                .withArguments("--info", "build")

            TestProject(testProjectDir, settingsFile, buildFile, runner).block()
        }
    }

    private inner class TestProject(
        val testProjectDir: File,
        val settingsFile: File,
        val buildFile: File,
        val runner: GradleRunner,
    ) {
        fun File.prependText(text: String) {
            val existingContent = readText()
            writeText(text)
            appendText(existingContent)
        }

        fun createClassFile(className: String, path: String = "src/main/kotlin/org/example") {
            val classFile = File(testProjectDir, "$path/$className.kt")
            classFile.ensureParentDirsCreated()
            classFile.createNewFile()
            classFile.writeText(getResource("/$className.kt"))
        }
    }

    @Test
    fun testDefaultOptions() = withTestProject {
        createClassFile("User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Builder
        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public class UserBuilder {
                    @GeneratedApi
                    public var id: Int? = null
                    @GeneratedApi
                    public var givenName: String? = null
                    @GeneratedApi
                    public var surname: String? = null
                    @GeneratedApi
                    public var age: Int? = null

                    @GeneratedApi
                    public fun build(): User {
                        val id = requireNotNull(id) { "Missing value for id" }
                        val givenName = requireNotNull(givenName) { "Missing value for givenName" }
                        val surname = requireNotNull(surname) { "Missing value for surname" }
                        val age = requireNotNull(age) { "Missing value for age" }

                        return User(
                            id,
                            givenName,
                            surname,
                            age,
                        )
                    }
                }
            """.trimIndent(),
        )

        // Converter
        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public object UserConverter : ItemConverter<User> by SimpleItemConverter(
                    builderFactory = ::UserBuilder,
                    build = UserBuilder::build,
                    descriptors = arrayOf(
                        AttributeDescriptor(
                            "id",
                            User::id,
                            UserBuilder::id::set,
                            NumberValueConverters.Int,
                        ),
                        AttributeDescriptor(
                            "fName",
                            User::givenName,
                            UserBuilder::givenName::set,
                            StringValueConverter,
                        ),
                        AttributeDescriptor(
                            "lName",
                            User::surname,
                            UserBuilder::surname::set,
                            StringValueConverter,
                        ),
                        AttributeDescriptor(
                            "age",
                            User::age,
                            UserBuilder::age::set,
                            NumberValueConverters.Int,
                        ),
                    ),
                )
            """.trimIndent(),
        )

        // Schema
        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public object UserSchema : ItemSchema.PartitionKey<User, KeyType.Key1<Int>> {
                    override val converter: UserConverter = UserConverter
                    override val partitionKey: KeySpec.Key1<Int> = KeySpec.int("id")
                    override val attributes: Attributes = emptyAttributes()
                }
            """.trimIndent(),
        )

        // GetTable
        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public fun DynamoDbMapper.getUserTable(name: String): Table.PartitionKey<User, KeyType.Key1<Int>> = getTable(name, UserSchema)
            """.trimIndent(),
        )
    }

    @Test
    fun testBuilderNotRequired() = withTestProject {
        createClassFile("BuilderNotRequired")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/BuilderNotRequiredSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Assert a builder is not generated, because it contains all mutable members with default values and a zero-arg constructor
        assertFalse(schemaContents.contains("public class BuilderNotRequiredBuilder {"))

        // Assert that the class itself is used as a builder
        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public object BuilderNotRequiredConverter : ItemConverter<BuilderNotRequired> by SimpleItemConverter(
                    builderFactory = { BuilderNotRequired() },
                    build = { this },
                    descriptors = arrayOf(
                        AttributeDescriptor(
                            "id",
                            BuilderNotRequired::id,
                            BuilderNotRequired::id::set,
                            NumberValueConverters.Int,
                        ),
                        AttributeDescriptor(
                            "fName",
                            BuilderNotRequired::givenName,
                            BuilderNotRequired::givenName::set,
                            StringValueConverter,
                        ),
                        AttributeDescriptor(
                            "lName",
                            BuilderNotRequired::surname,
                            BuilderNotRequired::surname::set,
                            StringValueConverter,
                        ),
                        AttributeDescriptor(
                            "age",
                            BuilderNotRequired::age,
                            BuilderNotRequired::age::set,
                            NumberValueConverters.Int,
                        ),
                    ),
                )
            """.trimIndent(),
        )
    }

    @Test
    fun testGenerateBuilderOption() = withTestProject {
        val pluginConfiguration = """
            import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.GenerateBuilderClasses
            
            dynamoDbMapper {
                generateBuilderClasses = GenerateBuilderClasses.ALWAYS
            }
            
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("BuilderNotRequired")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/BuilderNotRequiredSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Assert a builder is still generated, because we configured GenerateBuilderClasses.ALWAYS
        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public class BuilderNotRequiredBuilder {
                    @GeneratedApi
                    public var id: Int? = null
                    @GeneratedApi
                    public var givenName: String? = null
                    @GeneratedApi
                    public var surname: String? = null
                    @GeneratedApi
                    public var age: Int? = null

                    @GeneratedApi
                    public fun build(): BuilderNotRequired {
                        val id = requireNotNull(id) { "Missing value for id" }
                        val givenName = requireNotNull(givenName) { "Missing value for givenName" }
                        val surname = requireNotNull(surname) { "Missing value for surname" }
                        val age = requireNotNull(age) { "Missing value for age" }

                        return BuilderNotRequired(
                            id,
                            givenName,
                            surname,
                            age,
                        )
                    }
                }
            """.trimIndent(),
        )
    }

    @Test
    fun testVisibilityOption() = withTestProject {
        val pluginConfiguration = """
            import aws.sdk.kotlin.hll.codegen.rendering.Visibility
            
            dynamoDbMapper {
                visibility = Visibility.INTERNAL
            }
            
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // All codegenerated constructs should be `internal`
        assertContains(schemaContents, "internal class UserBuilder")
        assertContains(schemaContents, "internal object UserConverter")
        assertContains(schemaContents, "internal object UserSchema")
        assertContains(schemaContents, "internal fun DynamoDbMapper.getUserTable")
    }

    @Test
    fun testGenerateGetTableFunctionOption() = withTestProject {
        val pluginConfiguration = """
            dynamoDbMapper {
                generateGetTableExtension = false
            }
            
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // getUserTable should not be generated
        assertContains(schemaContents, "public class UserBuilder")
        assertContains(schemaContents, "public object UserConverter")
        assertContains(schemaContents, "public object UserSchema")
        assertFalse(schemaContents.contains("public fun DynamoDbMapper.getUserTable"))
    }

    @Test
    fun testRelativeDestinationPackage() = withTestProject {
        val pluginConfiguration = """
            import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
            
            dynamoDbMapper {
                destinationPackage = DestinationPackage.Relative("hello.moto")
            }
            
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/hello/moto/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(schemaContents, "package org.example.hello.moto")
    }

    @Test
    fun testAbsoluteDestinationPackage() = withTestProject {
        val pluginConfiguration = """
            import aws.sdk.kotlin.hll.dynamodbmapper.codegen.annotations.DestinationPackage
            
            dynamoDbMapper {
                destinationPackage = DestinationPackage.Absolute("absolutely.my.`package`")
            }
            
        """.trimIndent()
        buildFile.prependText(pluginConfiguration)

        createClassFile("User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/absolutely/my/`package`/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(schemaContents, "package absolutely.my.`package`")
    }

    @Test
    fun testGeneratedItemConverter() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test")) 
                }

            """.trimIndent(),
        )

        createClassFile("User")

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/UserTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/tests/UserTest.kt"))

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)
        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testDynamoDbIgnore() = withTestProject {
        createClassFile("IgnoredProperty")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/IgnoredPropertySchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public class IgnoredPropertyBuilder {
                    @GeneratedApi
                    public var id: Int? = null
                    @GeneratedApi
                    public var givenName: String? = null
                    @GeneratedApi
                    public var surname: String? = null
                    @GeneratedApi
                    public var age: Int? = null
                
                    @GeneratedApi
                    public fun build(): IgnoredProperty {
                        val id = requireNotNull(id) { "Missing value for id" }
                        val givenName = requireNotNull(givenName) { "Missing value for givenName" }
                        val surname = requireNotNull(surname) { "Missing value for surname" }
                        val age = requireNotNull(age) { "Missing value for age" }
                
                        return IgnoredProperty(
                            id,
                            givenName,
                            surname,
                            age,
                        )
                    }
                }
            """.trimIndent(),
        )

        // ssn is annotated with DynamoDbIgnore
        assertFalse(schemaContents.contains("public var ssn: String? = null"))
    }

    @Test
    fun testDynamoDbItemConverter() = withTestProject {
        createClassFile("custom-item-converter/CustomUser")
        createClassFile("custom-item-converter/CustomItemConverter", "src/main/kotlin/my/custom/item/converter")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/CustomUserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()
        assertFalse(schemaContents.contains("public object CustomUserItemConverter : ItemConverter<CustomUser> by SimpleItemConverter"))
        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public object CustomUserSchema : ItemSchema.PartitionKey<CustomUser, KeyType.Key1<Int>> {
                    override val converter: MyCustomUserConverter = MyCustomUserConverter
                    override val partitionKey: KeySpec.Key1<Int> = KeySpec.int("id")
                    override val attributes: Attributes = emptyAttributes()
                }
            """.trimIndent(),
        )
    }

    @Test
    fun testPrimitives() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    implementation("aws.smithy.kotlin:runtime-core:$smithyKotlinVersion")
                    testImplementation(kotlin("test")) 
                }
            """.trimIndent(),
        )

        createClassFile("standard-item-converters/src/Primitives")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)
        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/PrimitivesSchema.kt")
        assertTrue(schemaFile.exists())

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/standard-item-converters/test/PrimitivesTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/standard-item-converters/test/PrimitivesTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testNullableTypes() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    implementation("aws.smithy.kotlin:runtime-core:$smithyKotlinVersion")
                    testImplementation(kotlin("test")) 
                }
            """.trimIndent(),
        )

        createClassFile("standard-item-converters/src/NullableItem")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)
        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/NullableItemSchema.kt")
        assertTrue(schemaFile.exists())

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/standard-item-converters/test/NullableItemTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/standard-item-converters/test/NullableItemTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testLists() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test")) 
                }
            """.trimIndent(),
        )

        createClassFile("standard-item-converters/src/Lists")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)
        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/ListsSchema.kt")
        assertTrue(schemaFile.exists())

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/standard-item-converters/test/ListsTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/standard-item-converters/test/ListsTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testSets() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test")) 
                }
            """.trimIndent(),
        )

        createClassFile("standard-item-converters/src/Sets")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)
        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/SetsSchema.kt")
        assertTrue(schemaFile.exists())

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/standard-item-converters/test/SetsTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/standard-item-converters/test/SetsTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testMaps() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test")) 
                }
            """.trimIndent(),
        )

        createClassFile("standard-item-converters/src/Maps")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)
        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/MapsSchema.kt")
        assertTrue(schemaFile.exists())

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/standard-item-converters/test/MapsTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/standard-item-converters/test/MapsTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testRenamedPartitionKey() = withTestProject {
        createClassFile("RenamedPartitionKey")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/RenamedPartitionKeySchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Schema should use the renamed partition key
        assertContains(
            schemaContents,
            """
                @GeneratedApi
                public object RenamedPartitionKeySchema : ItemSchema.PartitionKey<RenamedPartitionKey, KeyType.Key1<Int>> {
                    override val converter: RenamedPartitionKeyConverter = RenamedPartitionKeyConverter
                    override val partitionKey: KeySpec.Key1<Int> = KeySpec.int("user_id")
                    override val attributes: Attributes = emptyAttributes()
                }
            """.trimIndent(),
        )
    }

    @Test
    fun testDynamoDbAttributeConverter() = withTestProject {
        createClassFile("attribute-converter/Employee")
        createClassFile("attribute-converter/HealthcareConverter")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/EmployeeSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        assertContains(schemaContents, "import org.example.OccupationConverter")
        assertContains(
            schemaContents,
            """        AttributeDescriptor(
            "occupation",
            Employee::occupation,
            Employee::occupation::set,
            OccupationConverter(),
        ),""",
        )

        // Test cross-package converter
        assertContains(schemaContents, "import a.different.pkg.HealthcareConverter")
        assertContains(
            schemaContents,
            """        AttributeDescriptor(
            "healthcare",
            Employee::healthcare,
            Employee::healthcare::set,
            HealthcareConverter(),
        ),""",
        )
    }

    @Test
    fun testDynamoDbTtlSeconds() = withTestProject {
        createClassFile("ttl/User")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/ttl/dynamodbmapper/generatedschemas/UserSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Ensure that TTL field is set
        assertContains(
            schemaContents,
            """
            public object UserSchema : ItemSchema.PartitionKey<User, KeyType.Key1<Int>> {
                override val converter: UserConverter = UserConverter
                override val partitionKey: KeySpec.Key1<Int> = KeySpec.int("id")
                private val ttlFields = mapOf("expiresAt" to 86400L)
                override val attributes: Attributes = attributesOf {
                    SchemaAttributes.TtlFields to ttlFields
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testInvalidDynamoDbTtlSeconds() = withTestProject {
        createClassFile("ttl/InvalidTtlLifetime")

        val result = runner.buildAndFail()
        assertContains(result.output, "@DynamoDbTtlSeconds must be positive, got -5 seconds on property expiresAt")
    }

    @Test
    fun testMultipleTtlAnnotations() = withTestProject {
        createClassFile("ttl/MultipleTtlAnnotations")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/ttl/dynamodbmapper/generatedschemas/MultipleTtlAnnotationsSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Ensure that both TTL fields are set
        assertContains(
            schemaContents,
            """
            public object MultipleTtlAnnotationsSchema : ItemSchema.PartitionKey<MultipleTtlAnnotations, KeyType.Key1<Int>> {
                override val converter: MultipleTtlAnnotationsConverter = MultipleTtlAnnotationsConverter
                override val partitionKey: KeySpec.Key1<Int> = KeySpec.int("id")
                private val ttlFields = mapOf("expiresAt" to 3600L, "actuallyExpiresAt" to 7200L)
                override val attributes: Attributes = attributesOf {
                    SchemaAttributes.TtlFields to ttlFields
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testInvalidTtlExpression() = withTestProject {
        createClassFile("ttl/InvalidTtlExpression")

        val result = runner.buildAndFail()
        assertContains(result.output, "@DynamoDbTtlSeconds annotation argument on property expiresAt could not be evaluated at compile time. Use a literal value like @DynamoDbTtlSeconds(3600) instead of expressions like @DynamoDbTtlSeconds(1.hours.inWholeSeconds).")
    }

    @Test
    fun testDynamoDbCounter() = withTestProject {
        createClassFile("counter/UserWithCounter")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(testProjectDir, "build/generated/ksp/main/kotlin/org/example/counter/dynamodbmapper/generatedschemas/UserWithCounterSchema.kt")
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()

        // Ensure that counter fields are set
        assertContains(
            schemaContents,
            """
            public object UserWithCounterSchema : ItemSchema.PartitionKey<UserWithCounter, KeyType.Key1<Int>> {
                override val converter: UserWithCounterConverter = UserWithCounterConverter
                override val partitionKey: KeySpec.Key1<Int> = KeySpec.int("id")
                private val ttlFields = mapOf("expiresAt" to 3600L)
                override val attributes: Attributes = attributesOf {
                    SchemaAttributes.TtlFields to ttlFields
                    SchemaAttributes.CounterFields to setOf("accessCount", "updateCount")
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testDynamoDbCounterInvalidType() = withTestProject {
        createClassFile("counter/UserWithInvalidCounter")
        val result = runner.buildAndFail()
        assertContains(result.output, "Property 'accessCount' annotated with @DynamoDbCounter must be of type Int or Long, but was String")
    }

    @Test
    fun testOverriddenAttributeConverter() = withTestProject {
        createClassFile("override-item-converters/User")
        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)
    }

    @Test
    fun testNestedItems() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test"))
                }
            """.trimIndent(),
        )

        createClassFile("nested/src/NestedItem")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)

        val basePath = "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas"

        // The table item (@DynamoDbItem) generates a full schema and references the nested value converter
        val personSchema = File(testProjectDir, "$basePath/PersonSchema.kt")
        assertTrue(personSchema.exists())
        val personContents = personSchema.readText()
        assertContains(personContents, "public object PersonSchema : ItemSchema.PartitionKey<Person, KeyType.Key1<Int>>")
        assertContains(personContents, "AddressValueConverter")

        // The nested-only type (@DynamoDbMappable) generates a converter + value converter but NO schema or table accessor
        val addressSchema = File(testProjectDir, "$basePath/AddressSchema.kt")
        assertTrue(addressSchema.exists())
        val addressContents = addressSchema.readText()
        assertContains(addressContents, "public object AddressConverter : ItemConverter<Address>")
        assertContains(addressContents, "public val AddressValueConverter:")
        assertFalse(addressContents.contains("ItemSchema.PartitionKey"))
        assertFalse(addressContents.contains("getAddressTable"))

        // Round-trip conversion
        val testFile = File(testProjectDir, "src/test/kotlin/org/example/NestedItemTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/nested/test/NestedItemTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testCrossPackageNesting() = withTestProject {
        createClassFile("nested/crosspackage/Coordinates")
        createClassFile("nested/crosspackage/Place")

        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        // Nested type in org.example.geo generates its value converter in that package's generated schemas
        val coordinatesSchema = File(
            testProjectDir,
            "build/generated/ksp/main/kotlin/org/example/geo/dynamodbmapper/generatedschemas/CoordinatesSchema.kt",
        )
        assertTrue(coordinatesSchema.exists())
        assertContains(coordinatesSchema.readText(), "public val CoordinatesValueConverter:")

        // The referencing table item in org.example must reference the converter in the nested type's package
        val placeSchema = File(
            testProjectDir,
            "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/PlaceSchema.kt",
        )
        assertTrue(placeSchema.exists())
        assertContains(
            placeSchema.readText(),
            "import org.example.geo.dynamodbmapper.generatedschemas.CoordinatesValueConverter",
        )
    }

    @Test
    fun testSelfReferentialNestingFails() = withTestProject {
        createClassFile("nested/src/SelfReferential")

        val result = runner.buildAndFail()
        assertContains(result.output, "Cyclic nesting detected involving type 'org.example.TreeNode'")
    }

    @Test
    fun testNestedTableItem() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test")) 
                }
            """.trimIndent(),
        )

        createClassFile("nested/src/NestedTableItem")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)

        val basePath = "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas"

        // The parent @DynamoDbItem generates a full schema and references the nested item's value converter
        val productSchema = File(testProjectDir, "$basePath/ProductSchema.kt")
        assertTrue(productSchema.exists())
        val productContents = productSchema.readText()
        assertContains(productContents, "public object ProductSchema : ItemSchema.PartitionKey<Product, KeyType.Key1<String>>")
        assertContains(productContents, "ManufacturerValueConverter")

        // The nested type is itself a @DynamoDbItem, so it ALSO generates its own schema, table accessor, and value converter
        val manufacturerSchema = File(testProjectDir, "$basePath/ManufacturerSchema.kt")
        assertTrue(manufacturerSchema.exists())
        val manufacturerContents = manufacturerSchema.readText()
        assertContains(manufacturerContents, "public object ManufacturerSchema : ItemSchema.PartitionKey<Manufacturer, KeyType.Key1<Int>>")
        assertContains(manufacturerContents, "public fun DynamoDbMapper.getManufacturerTable")
        assertContains(manufacturerContents, "public val ManufacturerValueConverter:")

        // Round-trip conversion, including the nested item's key field serialized as a plain attribute
        val testFile = File(testProjectDir, "src/test/kotlin/org/example/NestedTableItemTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/nested/test/NestedTableItemTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testNestedTableItemTtlAndCounterAreInert() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test")) 
                }
            """.trimIndent(),
        )

        createClassFile("nested/src/NestedItemWithTtlCounter")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)

        val basePath = "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas"

        // The nested type's TTL/counter are captured only on its OWN schema attributes
        val sessionSchema = File(testProjectDir, "$basePath/SessionSchema.kt").readText()
        assertContains(sessionSchema, "SchemaAttributes.TtlFields to ttlFields")
        assertContains(sessionSchema, "SchemaAttributes.CounterFields to setOf(\"accessCount\")")

        // They do NOT propagate to the parent schema, so the TTL/counter interceptors (which read the operation's
        // schema attributes) never act on the nested fields
        val accountSchema = File(testProjectDir, "$basePath/AccountSchema.kt").readText()
        assertContains(accountSchema, "override val attributes: Attributes = emptyAttributes()")
        assertFalse(accountSchema.contains("TtlFields"))
        assertFalse(accountSchema.contains("CounterFields"))

        // Round-trip shows the nested TTL/counter fields serialize as ordinary, unchanged attributes
        val testFile = File(testProjectDir, "src/test/kotlin/org/example/NestedItemWithTtlCounterTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/nested/test/NestedItemWithTtlCounterTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testDeeplyNestedMappable() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test")) 
                }
            """.trimIndent(),
        )

        createClassFile("nested/src/DeepNesting")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)

        // The deeply-nested reference is detected and the nested value converter is wired through List/Map/List
        val holderSchema = File(
            testProjectDir,
            "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/DeepListHolderSchema.kt",
        ).readText()
        assertContains(holderSchema, "ListValueConverter(MapValueConverter(")
        assertContains(holderSchema, "ListValueConverter(LeafValueConverter)")

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/DeepNestingTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.createNewFile()
        testFile.writeText(getResource("/nested/test/DeepNestingTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testDeeplyNestedSetOfMappableFails() = withTestProject {
        createClassFile("nested/negative/DeepSetNesting")

        // Set<SetLeaf> is a set of maps, which is unsupported even when nested inside List<Map<String, ...>>
        val result = runner.buildAndFail()
        assertContains(result.output, "Unsupported set element")
    }

    @Test
    fun testMutualCyclicNestingFails() = withTestProject {
        createClassFile("nested/negative/MutualCycle")

        val result = runner.buildAndFail()
        assertContains(result.output, "Cyclic nesting detected")
    }

    @Test
    fun testIgnoredAndPrivateSelfReferencesAreNotCyclic() = withTestProject {
        createClassFile("nested/src/IgnoredCycle")

        // Self-references exist only through an ignored and a private field, so this must build successfully.
        val result = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), result.task(":build")?.outcome)

        val schemaFile = File(
            testProjectDir,
            "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas/IgnoredCycleSchema.kt",
        )
        assertTrue(schemaFile.exists())

        val schemaContents = schemaFile.readText()
        // Neither the ignored nor the private self-referential field should be mapped
        assertFalse(schemaContents.contains("ignoredParent"))
        assertFalse(schemaContents.contains("privateParent"))
    }

    @Test
    fun testMappableOnAnnotationClassFails() = withTestProject {
        createClassFile("nested/negative/MappableAnnotation")

        val result = runner.buildAndFail()
        assertContains(result.output, "@DynamoDbMappable cannot be applied to annotation classes")
    }

    @Test
    fun testNestedWithKeyFails() = withTestProject {
        createClassFile("nested/negative/NestedWithKey")

        val result = runner.buildAndFail()
        assertContains(
            result.output,
            "is annotated with @DynamoDbPartitionKey. These annotations are only valid on a @DynamoDbItem table item",
        )
    }

    @Test
    fun testUnannotatedNestedTypeFails() = withTestProject {
        createClassFile("nested/negative/UnannotatedNested")

        val result = runner.buildAndFail()
        assertContains(
            result.output,
            "If 'org.example.PlainThing' should be stored as a nested item, annotate it with @DynamoDbMappable",
        )
    }

    @Test
    fun testSetOfNestedTypeFails() = withTestProject {
        createClassFile("nested/negative/SetOfNested")

        val result = runner.buildAndFail()
        assertContains(result.output, "Unsupported set element")
    }

    @Test
    fun testMissingPartitionKeyFails() = withTestProject {
        createClassFile("keys/NoPartitionKey")

        val result = runner.buildAndFail()
        assertContains(result.output, "Expected between 1 and 4 properties annotated with @DynamoDbPartitionKey, found 0")
    }

    @Test
    fun testTooManyPartitionKeysFails() = withTestProject {
        createClassFile("keys/TooManyPartitionKeys")

        val result = runner.buildAndFail()
        assertContains(result.output, "Expected between 1 and 4 properties annotated with @DynamoDbPartitionKey, found 5")
    }

    @Test
    fun testTooManySortKeysFails() = withTestProject {
        createClassFile("keys/TooManySortKeys")

        val result = runner.buildAndFail()
        assertContains(result.output, "Expected between 0 and 4 properties annotated with @DynamoDbSortKey, found 5")
    }

    @Test
    fun testUnsupportedMapKeyTypeFails() = withTestProject {
        createClassFile("keys/UnsupportedMapKey")

        val result = runner.buildAndFail()
        assertContains(result.output, "Unsupported key type:")
    }

    @Test
    fun testUnsupportedKeyAttributeTypeFails() = withTestProject {
        createClassFile("keys/UnsupportedKeyAttributeType")

        val result = runner.buildAndFail()
        assertContains(result.output, "Unsupported key attribute type")
    }

    @Test
    fun testAllProblemsAreReported() = withTestProject {
        createClassFile("keys/MultipleProblems")

        val result = runner.buildAndFail()
        assertContains(result.output, "Expected between 1 and 4 properties annotated with @DynamoDbPartitionKey, found 0")
        assertContains(result.output, "@DynamoDbTtlSeconds must be positive, got -5 seconds on property expiresAt")
        assertContains(result.output, "Property 'accessCount' annotated with @DynamoDbCounter must be of type Int or Long, but was String")
    }

    @Test
    fun testHeterogeneousSealedFamily() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test"))
                }
            """.trimIndent(),
        )

        createClassFile("heterogeneous/src/Vehicles")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)

        val basePath = "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas"
        val vehicleContents = File(testProjectDir, "$basePath/VehicleSchema.kt").readText()

        // Registrations are ordered by name (no member descends from another); the nested family is one registration
        assertContains(
            vehicleContents,
            """
                @GeneratedApi
                public object VehicleConverter : ItemConverter<Vehicle> by HeterogeneousItemConverter<Vehicle>(
                    typeAttribute = "type",
                    block = {
                        instanceOf<Bike>("bike", BikeConverter)
                        instanceOf<Car>("car", CarConverter)
                        instanceOf<Truck>("truck", TruckConverter)
                        instanceOf<Unicycle>("unicycle", UnicycleConverter)
                    },
                )
            """.trimIndent(),
        )
        assertContains(vehicleContents, "public val VehicleValueConverter: ValueConverter<Vehicle> = ConverterChain(VehicleConverter, ItemValueConverter)")
        assertContains(
            vehicleContents,
            """
                @GeneratedApi
                public object VehicleSchema : ItemSchema.PartitionKey<Vehicle, KeyType.Key1<Int>> {
                    override val converter: VehicleConverter = VehicleConverter
                    override val partitionKey: KeySpec.Key1<Int> = KeySpec.int("id")
                    override val attributes: Attributes = emptyAttributes()
                }
            """.trimIndent(),
        )
        assertContains(vehicleContents, "public fun DynamoDbMapper.getVehicleTable(name: String): Table.PartitionKey<Vehicle, KeyType.Key1<Int>> = getTable(name, VehicleSchema)")

        val truckContents = File(testProjectDir, "$basePath/TruckSchema.kt").readText()
        assertContains(truckContents, "public object TruckConverter : ItemConverter<Truck> by HeterogeneousItemConverter<Truck>(")
        assertContains(truckContents, "typeAttribute = \"truckType\",")
        assertFalse(truckContents.contains("TruckSchema :"))

        // Members get converters but no schemas of their own
        val carContents = File(testProjectDir, "$basePath/CarSchema.kt").readText()
        assertContains(carContents, "public object CarConverter : ItemConverter<Car> by SimpleItemConverter(")
        assertFalse(carContents.contains("CarSchema :"))

        val unicycleContents = File(testProjectDir, "$basePath/UnicycleSchema.kt").readText()
        assertContains(unicycleContents, "builderFactory = { Unicycle },")
        assertFalse(unicycleContents.contains("UnicycleBuilder"))

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/VehiclesTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.writeText(getResource("/heterogeneous/test/VehiclesTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testHeterogeneousOpenAndAdHocFamilies() = withTestProject {
        buildFile.appendText(
            """
                dependencies {
                    testImplementation(kotlin("test"))
                }
            """.trimIndent(),
        )

        createClassFile("heterogeneous/src/Animals")
        createClassFile("heterogeneous/src/Entities")

        val buildResult = runner.build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), buildResult.task(":build")?.outcome)

        val basePath = "build/generated/ksp/main/kotlin/org/example/dynamodbmapper/generatedschemas"

        // A subtype is registered before its supertype, and a self-registering base always comes last
        val animalContents = File(testProjectDir, "$basePath/AnimalSchema.kt").readText()
        val registrations = listOf(
            "exactType<Puppy>(\"puppy\", PuppyConverter)",
            "instanceOf<Dog>(\"dog\", DogConverter)",
            "instanceOf<Animal>(\"animal\", AnimalBaseConverter)",
        )
        val positions = registrations.map { animalContents.indexOf(it) }
        assertTrue(positions.all { it >= 0 } && positions == positions.sorted(), "Unexpected registrations in:\n$animalContents")
        val animalBaseContents = File(testProjectDir, "$basePath/AnimalBaseConverter.kt").readText()
        assertContains(animalBaseContents, "public object AnimalBaseConverter : ItemConverter<Animal> by SimpleItemConverter(")

        // An ad-hoc family's value type is Any; TTL and counter attributes are merged across its members
        val entitiesContents = File(testProjectDir, "$basePath/EntitiesSchema.kt").readText()
        assertContains(entitiesContents, "public object EntitiesConverter : ItemConverter<Any> by HeterogeneousItemConverter<Any>(")
        assertContains(entitiesContents, "public object EntitiesSchema : ItemSchema.CompositeKey<Any, KeyType.Key1<String>, KeyType.Key1<String>> {")
        assertContains(entitiesContents, "private val ttlFields = mapOf(\"expiresAt\" to 3600L)")
        assertContains(entitiesContents, "SchemaAttributes.CounterFields to setOf(\"version\")")

        val auditContents = File(testProjectDir, "$basePath/AuditRecordSchema.kt").readText()
        assertContains(auditContents, "EntitiesValueConverter,")
        assertContains(auditContents, "ListValueConverter(EntitiesValueConverter),")

        val testFile = File(testProjectDir, "src/test/kotlin/org/example/OpenAndAdHocFamiliesTest.kt")
        testFile.ensureParentDirsCreated()
        testFile.writeText(getResource("/heterogeneous/test/OpenAndAdHocFamiliesTest.kt"))

        val testResult = runner.withArguments("test").build()
        assertContains(setOf(TaskOutcome.SUCCESS, TaskOutcome.UP_TO_DATE), testResult.task(":test")?.outcome)
    }

    @Test
    fun testHeterogeneousFamilyProblemsAreReported() = withTestProject {
        createClassFile("heterogeneous/negative/FamilyProblems")

        val result = runner.buildAndFail()
        assertContains(result.output, "'org.example.Square' is a concrete subtype of sealed family 'org.example.Shape' but is not matched by any @DynamoDbSubtype registration")
        assertContains(result.output, "Subtype 'org.example.DocB' has keys (partition [id: Int], sort []) but 'org.example.DocA' has keys (partition [id: String], sort [])")
        assertContains(result.output, "Property 'kind' of 'org.example.Fish' maps to attribute 'kind', which collides with a type attribute of its family")
        assertContains(result.output, "Type name 'pome' of 'org.example.Pear' is already registered in family 'org.example.Fruit' by 'org.example.Apple'")
        assertContains(result.output, "typeName of subtype 'org.example.Plum' must not be blank")
        assertContains(result.output, "Subtype 'org.example.Hammer' uses EXACT_TYPE but is abstract or an interface, so it can never match")
        assertContains(result.output, "No @DynamoDbHeterogeneousType family found for subtype 'org.example.Thing'")
        assertContains(result.output, "Family 'org.example.Things' must have at least one @DynamoDbSubtype")
        assertContains(result.output, "Property 'n' is annotated with @DynamoDbHeterogeneousAttribute(org.example.Things::class) but holds no value of type 'kotlin.Any'")
        assertContains(result.output, "TTL lifetime of property 'expiresAt' conflicts across family 'org.example.Session'")
        assertContains(result.output, "@DynamoDbHeterogeneousType cannot be applied to 'org.example.Sealed' because it cannot have subtypes")
    }
}
