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
        assertContains(schemaContents, "public class UserBuilder")
        assertContains(schemaContents, "public var id: Int? = null")
        assertContains(schemaContents, "public var givenName: String? = null")
        assertContains(schemaContents, "public var surname: String? = null")
        assertContains(schemaContents, "public var age: Int? = null")
        assertContains(schemaContents, "public fun build(): User")

        // Converter
        assertContains(
            schemaContents,
            """
        object UserConverter : ItemConverter<User> by SimpleItemConverter(
            builderFactory = ::UserBuilder,
            build = UserBuilder::build,
            descriptors = arrayOf(
                AttributeDescriptor(
                    "id",
                    User::id,
                    UserBuilder::id::set,
                    IntConverter,
                ),
                AttributeDescriptor(
                    "fName",
                    User::givenName,
                    UserBuilder::givenName::set,
                    StringConverter,
                ),
                AttributeDescriptor(
                    "lName",
                    User::surname,
                    UserBuilder::surname::set,
                    StringConverter,
                ),
                AttributeDescriptor(
                    "age",
                    User::age,
                    UserBuilder::age::set,
                    IntConverter,
                ),
            ),
        )
            """.trimIndent(),
        )

        // Schema
        assertContains(
            schemaContents,
            """
        object UserSchema : ItemSchema.PartitionKey<User, Int> {
            override val converter: UserConverter = UserConverter
            override val partitionKey: KeySpec<Number> = aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Number("id")
        }
            """.trimIndent(),
        )

        // GetTable
        assertContains(schemaContents, "fun DynamoDbMapper.getUserTable(name: String): Table.PartitionKey<User, Int> = getTable(name, UserSchema)".trimIndent())
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
        object BuilderNotRequiredConverter : ItemConverter<BuilderNotRequired> by SimpleItemConverter(
            builderFactory = { BuilderNotRequired() },
            build = { this },
            descriptors = arrayOf(
                AttributeDescriptor(
                    "id",
                    BuilderNotRequired::id,
                    BuilderNotRequired::id::set,
                    IntConverter,
                ),
                AttributeDescriptor(
                    "fName",
                    BuilderNotRequired::givenName,
                    BuilderNotRequired::givenName::set,
                    StringConverter,
                ),
                AttributeDescriptor(
                    "lName",
                    BuilderNotRequired::surname,
                    BuilderNotRequired::surname::set,
                    StringConverter,
                ),
                AttributeDescriptor(
                    "age",
                    BuilderNotRequired::age,
                    BuilderNotRequired::age::set,
                    IntConverter,
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
        import aws.smithy.kotlin.runtime.ExperimentalApi
        
        @OptIn(ExperimentalApi::class)
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
        assertContains(schemaContents, "public class BuilderNotRequiredBuilder")
        assertContains(schemaContents, "public var id: Int? = null")
        assertContains(schemaContents, "public var givenName: String? = null")
        assertContains(schemaContents, "public var surname: String? = null")
        assertContains(schemaContents, "public var age: Int? = null")
        assertContains(schemaContents, "public fun build(): BuilderNotRequired")
    }

    @Test
    fun testVisibilityOption() {
        val pluginConfiguration = """
        import aws.sdk.kotlin.hll.codegen.rendering.Visibility
        import aws.smithy.kotlin.runtime.ExperimentalApi
        
        @OptIn(ExperimentalApi::class)
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
        import aws.smithy.kotlin.runtime.ExperimentalApi
        
        @OptIn(ExperimentalApi::class)
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
        import aws.smithy.kotlin.runtime.ExperimentalApi
        
        @OptIn(ExperimentalApi::class)
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
        import aws.smithy.kotlin.runtime.ExperimentalApi
        
        @OptIn(ExperimentalApi::class)
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

        assertContains(schemaContents, "public class IgnoredProperty")
        assertContains(schemaContents, "public var id: Int? = null")
        assertContains(schemaContents, "public var givenName: String? = null")
        assertContains(schemaContents, "public var surname: String? = null")
        assertContains(schemaContents, "public var age: Int? = null")
        assertContains(schemaContents, "public fun build(): IgnoredProperty")

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
            public object CustomUserSchema : ItemSchema.PartitionKey<CustomUser, Int> {
                override val converter: MyCustomUserConverter = MyCustomUserConverter
                override val partitionKey: KeySpec<Number> = aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Number("id")
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
        object RenamedPartitionKeySchema : ItemSchema.PartitionKey<RenamedPartitionKey, Int> {
            override val converter: RenamedPartitionKeyConverter = RenamedPartitionKeyConverter
            override val partitionKey: KeySpec<Number> = aws.sdk.kotlin.hll.dynamodbmapper.items.KeySpec.Number("user_id")
        }
            """.trimIndent(),
        )
    }
}
