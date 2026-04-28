/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package aws.sdk.kotlin.hll.dynamodbmapper.plugins

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SchemaGeneratorPluginTest {
    @TempDir(cleanup = CleanupMode.ON_SUCCESS)
    lateinit var testProjectDir: File

    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    private lateinit var runner: GradleRunner

    private fun getResource(resourceName: String): String = checkNotNull(this::class.java.getResource(resourceName)?.readText()) { "Could not read $resourceName" }
    private val kotlinVersion = getResource("kotlin-version.txt")
    private val sdkVersion = getResource("sdk-version.txt")
    private val smithyKotlinVersion = getResource("smithy-kotlin-version.txt")

    @BeforeEach
    fun setup() {
        settingsFile = File(testProjectDir, "settings.gradle.kts").also { it.writeText("") }

        buildFile = File(testProjectDir, "build.gradle.kts").also { it.writeText("") }

        // Apply the plugin and necessary dependencies
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

        runner = GradleRunner
            .create()
            .withProjectDir(testProjectDir)
            .withPluginClasspath()
            .forwardOutput()
            .withArguments("--info", "build")
    }

    private fun File.prependText(text: String) {
        val existingContent = readText()
        writeText(text)
        appendText(existingContent)
    }

    private fun createClassFile(className: String, path: String = "src/main/kotlin/org/example") {
        val classFile = File(testProjectDir, "$path/$className.kt")
        classFile.ensureParentDirsCreated()
        classFile.createNewFile()
        classFile.writeText(getResource("/$className.kt"))
    }

    @Test
    fun testDefaultOptions() {
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
    fun testBuilderNotRequired() {
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
    fun testGenerateBuilderOption() {
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
    fun testVisibilityOption() {
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
    fun testGenerateGetTableFunctionOption() {
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
    fun testRelativeDestinationPackage() {
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
    fun testAbsoluteDestinationPackage() {
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
    fun testGeneratedItemConverter() {
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
    fun testDynamoDbIgnore() {
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
    fun testDynamoDbItemConverter() {
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
    fun testPrimitives() {
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
    fun testNullableTypes() {
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
    fun testLists() {
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
    fun testSets() {
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
    fun testMaps() {
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
    fun testRenamedPartitionKey() {
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
    fun testDynamoDbAttributeConverter() {
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
            org.example.OccupationConverter(),
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
            a.different.pkg.HealthcareConverter(),
        ),""",
        )
    }

    @Test
    fun testDynamoDbTtlSeconds() {
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
                override val attributes: Attributes = attributesOf {
                    SchemaAttributes.TtlFields to setOf(Pair("expiresAt", 86400L))
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testInvalidDynamoDbTtlSeconds() {
        createClassFile("ttl/InvalidTtlLifetime")

        val result = runner.buildAndFail()
        assertContains(result.output, "@DynamoDbTtlSeconds must be positive, got -5 seconds on property expiresAt")
    }

    @Test
    fun testMultipleTtlAnnotations() {
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
                override val attributes: Attributes = attributesOf {
                    SchemaAttributes.TtlFields to setOf(Pair("expiresAt", 3600L), Pair("actuallyExpiresAt", 7200L))
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testInvalidTtlExpression() {
        createClassFile("ttl/InvalidTtlExpression")

        val result = runner.buildAndFail()
        assertContains(result.output, "@DynamoDbTtlSeconds annotation argument on property expiresAt could not be evaluated at compile time. Use a literal value like @DynamoDbTtlSeconds(3600) instead of expressions like @DynamoDbTtlSeconds(1.hours.inWholeSeconds).")
    }

    @Test
    fun testDynamoDbCounter() {
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
                override val attributes: Attributes = attributesOf {
                    SchemaAttributes.TtlFields to setOf(Pair("expiresAt", 3600L))
                    SchemaAttributes.CounterFields to setOf("accessCount", "updateCount")
                }
            }
            """.trimIndent(),
        )
    }

    @Test
    fun testDynamoDbCounterInvalidType() {
        createClassFile("counter/UserWithInvalidCounter")
        val result = runner.buildAndFail()
        assertContains(result.output, "Property 'accessCount' annotated with @DynamoDbCounter must be of type Int or Long, but was String")
    }
}
