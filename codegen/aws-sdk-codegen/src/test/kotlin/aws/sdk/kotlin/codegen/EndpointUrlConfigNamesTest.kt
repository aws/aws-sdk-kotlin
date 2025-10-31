/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package aws.sdk.kotlin.codegen

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

class EndpointUrlConfigNamesTest {
    // these don't appear to exist anymore - we don't have models (and therefore no generated clients) for them
    val ignoredIds = listOf("ImportExport", "SimpleDB")

    data class TestCase(
        val sdkId: String,
        val expectedConfigKey: String,
        val expectedEnvSuffix: String,
        val expectedSysPropSuffix: String,
    ) {
        companion object {
            fun fromJson(case: JsonObject, javaClientNames: JsonObject): TestCase {
                val sdkId = case["service_id"]!!.jsonPrimitive.content
                val javaClientName = javaClientNames[sdkId]?.jsonPrimitive?.content
                    ?: throw IllegalStateException("missing $sdkId in java client names")

                return TestCase(
                    sdkId,
                    case["services_section_name"]!!.jsonPrimitive.content,
                    case["service_envvar_name"]!!.jsonPrimitive.content.removePrefix("AWS_ENDPOINT_URL_"),
                    javaClientName.removeSuffix("Client"),
                )
            }
        }
    }

    @Test
    fun runTestSuite() {
        val javaClientNames = Json.parseToJsonElement(JAVA_CLIENT_NAMES_JSON).jsonObject
        val testCases = Json.parseToJsonElement(TEST_SUITE_JSON).jsonArray
            .filter { !ignoredIds.contains(it.jsonObject["service_id"]!!.jsonPrimitive.content) }
            .map { TestCase.fromJson(it.jsonObject, javaClientNames) }

        testCases.forEach { case ->
            val actualNames = case.sdkId.toEndpointUrlConfigNames()

            assertEquals(case.expectedConfigKey, actualNames.sharedConfigKey)
            assertEquals(case.expectedEnvSuffix, actualNames.envSuffix)
            assertEquals(case.expectedSysPropSuffix, actualNames.sysPropSuffix)
        }
    }
}

// language=JSON
private const val TEST_SUITE_JSON = """
[
  {
    "service_id": "AccessAnalyzer",
    "services_section_name": "accessanalyzer",
    "service_envvar_name": "AWS_ENDPOINT_URL_ACCESSANALYZER"
  },
  {
    "service_id": "Account",
    "services_section_name": "account",
    "service_envvar_name": "AWS_ENDPOINT_URL_ACCOUNT"
  },
  {
    "service_id": "ACM",
    "services_section_name": "acm",
    "service_envvar_name": "AWS_ENDPOINT_URL_ACM"
  },
  {
    "service_id": "ACM PCA",
    "services_section_name": "acm_pca",
    "service_envvar_name": "AWS_ENDPOINT_URL_ACM_PCA"
  },
  {
    "service_id": "amp",
    "services_section_name": "amp",
    "service_envvar_name": "AWS_ENDPOINT_URL_AMP"
  },
  {
    "service_id": "Amplify",
    "services_section_name": "amplify",
    "service_envvar_name": "AWS_ENDPOINT_URL_AMPLIFY"
  },
  {
    "service_id": "AmplifyBackend",
    "services_section_name": "amplifybackend",
    "service_envvar_name": "AWS_ENDPOINT_URL_AMPLIFYBACKEND"
  },
  {
    "service_id": "AmplifyUIBuilder",
    "services_section_name": "amplifyuibuilder",
    "service_envvar_name": "AWS_ENDPOINT_URL_AMPLIFYUIBUILDER"
  },
  {
    "service_id": "API Gateway",
    "services_section_name": "api_gateway",
    "service_envvar_name": "AWS_ENDPOINT_URL_API_GATEWAY"
  },
  {
    "service_id": "ApiGatewayManagementApi",
    "services_section_name": "apigatewaymanagementapi",
    "service_envvar_name": "AWS_ENDPOINT_URL_APIGATEWAYMANAGEMENTAPI"
  },
  {
    "service_id": "ApiGatewayV2",
    "services_section_name": "apigatewayv2",
    "service_envvar_name": "AWS_ENDPOINT_URL_APIGATEWAYV2"
  },
  {
    "service_id": "AppConfig",
    "services_section_name": "appconfig",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPCONFIG"
  },
  {
    "service_id": "AppConfigData",
    "services_section_name": "appconfigdata",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPCONFIGDATA"
  },
  {
    "service_id": "Appflow",
    "services_section_name": "appflow",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPFLOW"
  },
  {
    "service_id": "AppIntegrations",
    "services_section_name": "appintegrations",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPINTEGRATIONS"
  },
  {
    "service_id": "Application Auto Scaling",
    "services_section_name": "application_auto_scaling",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPLICATION_AUTO_SCALING"
  },
  {
    "service_id": "Application Insights",
    "services_section_name": "application_insights",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPLICATION_INSIGHTS"
  },
  {
    "service_id": "ApplicationCostProfiler",
    "services_section_name": "applicationcostprofiler",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPLICATIONCOSTPROFILER"
  },
  {
    "service_id": "App Mesh",
    "services_section_name": "app_mesh",
    "service_envvar_name": "AWS_ENDPOINT_URL_APP_MESH"
  },
  {
    "service_id": "AppRunner",
    "services_section_name": "apprunner",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPRUNNER"
  },
  {
    "service_id": "AppStream",
    "services_section_name": "appstream",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPSTREAM"
  },
  {
    "service_id": "AppSync",
    "services_section_name": "appsync",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPSYNC"
  },
  {
    "service_id": "Athena",
    "services_section_name": "athena",
    "service_envvar_name": "AWS_ENDPOINT_URL_ATHENA"
  },
  {
    "service_id": "AuditManager",
    "services_section_name": "auditmanager",
    "service_envvar_name": "AWS_ENDPOINT_URL_AUDITMANAGER"
  },
  {
    "service_id": "Auto Scaling",
    "services_section_name": "auto_scaling",
    "service_envvar_name": "AWS_ENDPOINT_URL_AUTO_SCALING"
  },
  {
    "service_id": "Auto Scaling Plans",
    "services_section_name": "auto_scaling_plans",
    "service_envvar_name": "AWS_ENDPOINT_URL_AUTO_SCALING_PLANS"
  },
  {
    "service_id": "Backup",
    "services_section_name": "backup",
    "service_envvar_name": "AWS_ENDPOINT_URL_BACKUP"
  },
  {
    "service_id": "Backup Gateway",
    "services_section_name": "backup_gateway",
    "service_envvar_name": "AWS_ENDPOINT_URL_BACKUP_GATEWAY"
  },
  {
    "service_id": "Batch",
    "services_section_name": "batch",
    "service_envvar_name": "AWS_ENDPOINT_URL_BATCH"
  },
  {
    "service_id": "billingconductor",
    "services_section_name": "billingconductor",
    "service_envvar_name": "AWS_ENDPOINT_URL_BILLINGCONDUCTOR"
  },
  {
    "service_id": "Braket",
    "services_section_name": "braket",
    "service_envvar_name": "AWS_ENDPOINT_URL_BRAKET"
  },
  {
    "service_id": "Budgets",
    "services_section_name": "budgets",
    "service_envvar_name": "AWS_ENDPOINT_URL_BUDGETS"
  },
  {
    "service_id": "Cost Explorer",
    "services_section_name": "cost_explorer",
    "service_envvar_name": "AWS_ENDPOINT_URL_COST_EXPLORER"
  },
  {
    "service_id": "Chime",
    "services_section_name": "chime",
    "service_envvar_name": "AWS_ENDPOINT_URL_CHIME"
  },
  {
    "service_id": "Chime SDK Identity",
    "services_section_name": "chime_sdk_identity",
    "service_envvar_name": "AWS_ENDPOINT_URL_CHIME_SDK_IDENTITY"
  },
  {
    "service_id": "Chime SDK Media Pipelines",
    "services_section_name": "chime_sdk_media_pipelines",
    "service_envvar_name": "AWS_ENDPOINT_URL_CHIME_SDK_MEDIA_PIPELINES"
  },
  {
    "service_id": "Chime SDK Meetings",
    "services_section_name": "chime_sdk_meetings",
    "service_envvar_name": "AWS_ENDPOINT_URL_CHIME_SDK_MEETINGS"
  },
  {
    "service_id": "Chime SDK Messaging",
    "services_section_name": "chime_sdk_messaging",
    "service_envvar_name": "AWS_ENDPOINT_URL_CHIME_SDK_MESSAGING"
  },
  {
    "service_id": "Cloud9",
    "services_section_name": "cloud9",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUD9"
  },
  {
    "service_id": "CloudControl",
    "services_section_name": "cloudcontrol",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDCONTROL"
  },
  {
    "service_id": "CloudDirectory",
    "services_section_name": "clouddirectory",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDDIRECTORY"
  },
  {
    "service_id": "CloudFormation",
    "services_section_name": "cloudformation",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDFORMATION"
  },
  {
    "service_id": "CloudFront",
    "services_section_name": "cloudfront",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDFRONT"
  },
  {
    "service_id": "CloudHSM",
    "services_section_name": "cloudhsm",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDHSM"
  },
  {
    "service_id": "CloudHSM V2",
    "services_section_name": "cloudhsm_v2",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDHSM_V2"
  },
  {
    "service_id": "CloudSearch",
    "services_section_name": "cloudsearch",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDSEARCH"
  },
  {
    "service_id": "CloudSearch Domain",
    "services_section_name": "cloudsearch_domain",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDSEARCH_DOMAIN"
  },
  {
    "service_id": "CloudTrail",
    "services_section_name": "cloudtrail",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDTRAIL"
  },
  {
    "service_id": "CloudTrail Data",
    "services_section_name": "cloudtrail_data",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDTRAIL_DATA"
  },
  {
    "service_id": "CloudWatch",
    "services_section_name": "cloudwatch",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDWATCH"
  },
  {
    "service_id": "codeartifact",
    "services_section_name": "codeartifact",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODEARTIFACT"
  },
  {
    "service_id": "CodeBuild",
    "services_section_name": "codebuild",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODEBUILD"
  },
  {
    "service_id": "CodeCommit",
    "services_section_name": "codecommit",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODECOMMIT"
  },
  {
    "service_id": "CodeDeploy",
    "services_section_name": "codedeploy",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODEDEPLOY"
  },
  {
    "service_id": "CodeGuru Reviewer",
    "services_section_name": "codeguru_reviewer",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODEGURU_REVIEWER"
  },
  {
    "service_id": "CodeGuruProfiler",
    "services_section_name": "codeguruprofiler",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODEGURUPROFILER"
  },
  {
    "service_id": "CodePipeline",
    "services_section_name": "codepipeline",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODEPIPELINE"
  },
  {
    "service_id": "CodeStar connections",
    "services_section_name": "codestar_connections",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODESTAR_CONNECTIONS"
  },
  {
    "service_id": "codestar notifications",
    "services_section_name": "codestar_notifications",
    "service_envvar_name": "AWS_ENDPOINT_URL_CODESTAR_NOTIFICATIONS"
  },
  {
    "service_id": "Cognito Identity",
    "services_section_name": "cognito_identity",
    "service_envvar_name": "AWS_ENDPOINT_URL_COGNITO_IDENTITY"
  },
  {
    "service_id": "Cognito Identity Provider",
    "services_section_name": "cognito_identity_provider",
    "service_envvar_name": "AWS_ENDPOINT_URL_COGNITO_IDENTITY_PROVIDER"
  },
  {
    "service_id": "Cognito Sync",
    "services_section_name": "cognito_sync",
    "service_envvar_name": "AWS_ENDPOINT_URL_COGNITO_SYNC"
  },
  {
    "service_id": "Comprehend",
    "services_section_name": "comprehend",
    "service_envvar_name": "AWS_ENDPOINT_URL_COMPREHEND"
  },
  {
    "service_id": "ComprehendMedical",
    "services_section_name": "comprehendmedical",
    "service_envvar_name": "AWS_ENDPOINT_URL_COMPREHENDMEDICAL"
  },
  {
    "service_id": "Compute Optimizer",
    "services_section_name": "compute_optimizer",
    "service_envvar_name": "AWS_ENDPOINT_URL_COMPUTE_OPTIMIZER"
  },
  {
    "service_id": "Config Service",
    "services_section_name": "config_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_CONFIG_SERVICE"
  },
  {
    "service_id": "Connect",
    "services_section_name": "connect",
    "service_envvar_name": "AWS_ENDPOINT_URL_CONNECT"
  },
  {
    "service_id": "Connect Contact Lens",
    "services_section_name": "connect_contact_lens",
    "service_envvar_name": "AWS_ENDPOINT_URL_CONNECT_CONTACT_LENS"
  },
  {
    "service_id": "ConnectCampaigns",
    "services_section_name": "connectcampaigns",
    "service_envvar_name": "AWS_ENDPOINT_URL_CONNECTCAMPAIGNS"
  },
  {
    "service_id": "ConnectCases",
    "services_section_name": "connectcases",
    "service_envvar_name": "AWS_ENDPOINT_URL_CONNECTCASES"
  },
  {
    "service_id": "ConnectParticipant",
    "services_section_name": "connectparticipant",
    "service_envvar_name": "AWS_ENDPOINT_URL_CONNECTPARTICIPANT"
  },
  {
    "service_id": "ControlTower",
    "services_section_name": "controltower",
    "service_envvar_name": "AWS_ENDPOINT_URL_CONTROLTOWER"
  },
  {
    "service_id": "Cost and Usage Report Service",
    "services_section_name": "cost_and_usage_report_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_COST_AND_USAGE_REPORT_SERVICE"
  },
  {
    "service_id": "Customer Profiles",
    "services_section_name": "customer_profiles",
    "service_envvar_name": "AWS_ENDPOINT_URL_CUSTOMER_PROFILES"
  },
  {
    "service_id": "DataBrew",
    "services_section_name": "databrew",
    "service_envvar_name": "AWS_ENDPOINT_URL_DATABREW"
  },
  {
    "service_id": "DataExchange",
    "services_section_name": "dataexchange",
    "service_envvar_name": "AWS_ENDPOINT_URL_DATAEXCHANGE"
  },
  {
    "service_id": "Data Pipeline",
    "services_section_name": "data_pipeline",
    "service_envvar_name": "AWS_ENDPOINT_URL_DATA_PIPELINE"
  },
  {
    "service_id": "DataSync",
    "services_section_name": "datasync",
    "service_envvar_name": "AWS_ENDPOINT_URL_DATASYNC"
  },
  {
    "service_id": "DAX",
    "services_section_name": "dax",
    "service_envvar_name": "AWS_ENDPOINT_URL_DAX"
  },
  {
    "service_id": "Detective",
    "services_section_name": "detective",
    "service_envvar_name": "AWS_ENDPOINT_URL_DETECTIVE"
  },
  {
    "service_id": "Device Farm",
    "services_section_name": "device_farm",
    "service_envvar_name": "AWS_ENDPOINT_URL_DEVICE_FARM"
  },
  {
    "service_id": "DevOps Guru",
    "services_section_name": "devops_guru",
    "service_envvar_name": "AWS_ENDPOINT_URL_DEVOPS_GURU"
  },
  {
    "service_id": "Direct Connect",
    "services_section_name": "direct_connect",
    "service_envvar_name": "AWS_ENDPOINT_URL_DIRECT_CONNECT"
  },
  {
    "service_id": "Application Discovery Service",
    "services_section_name": "application_discovery_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_APPLICATION_DISCOVERY_SERVICE"
  },
  {
    "service_id": "DLM",
    "services_section_name": "dlm",
    "service_envvar_name": "AWS_ENDPOINT_URL_DLM"
  },
  {
    "service_id": "Database Migration Service",
    "services_section_name": "database_migration_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_DATABASE_MIGRATION_SERVICE"
  },
  {
    "service_id": "DocDB",
    "services_section_name": "docdb",
    "service_envvar_name": "AWS_ENDPOINT_URL_DOCDB"
  },
  {
    "service_id": "drs",
    "services_section_name": "drs",
    "service_envvar_name": "AWS_ENDPOINT_URL_DRS"
  },
  {
    "service_id": "Directory Service",
    "services_section_name": "directory_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_DIRECTORY_SERVICE"
  },
  {
    "service_id": "DynamoDB",
    "services_section_name": "dynamodb",
    "service_envvar_name": "AWS_ENDPOINT_URL_DYNAMODB"
  },
  {
    "service_id": "DynamoDB Streams",
    "services_section_name": "dynamodb_streams",
    "service_envvar_name": "AWS_ENDPOINT_URL_DYNAMODB_STREAMS"
  },
  {
    "service_id": "EBS",
    "services_section_name": "ebs",
    "service_envvar_name": "AWS_ENDPOINT_URL_EBS"
  },
  {
    "service_id": "EC2",
    "services_section_name": "ec2",
    "service_envvar_name": "AWS_ENDPOINT_URL_EC2"
  },
  {
    "service_id": "EC2 Instance Connect",
    "services_section_name": "ec2_instance_connect",
    "service_envvar_name": "AWS_ENDPOINT_URL_EC2_INSTANCE_CONNECT"
  },
  {
    "service_id": "ECR",
    "services_section_name": "ecr",
    "service_envvar_name": "AWS_ENDPOINT_URL_ECR"
  },
  {
    "service_id": "ECR PUBLIC",
    "services_section_name": "ecr_public",
    "service_envvar_name": "AWS_ENDPOINT_URL_ECR_PUBLIC"
  },
  {
    "service_id": "ECS",
    "services_section_name": "ecs",
    "service_envvar_name": "AWS_ENDPOINT_URL_ECS"
  },
  {
    "service_id": "EFS",
    "services_section_name": "efs",
    "service_envvar_name": "AWS_ENDPOINT_URL_EFS"
  },
  {
    "service_id": "EKS",
    "services_section_name": "eks",
    "service_envvar_name": "AWS_ENDPOINT_URL_EKS"
  },
  {
    "service_id": "ElastiCache",
    "services_section_name": "elasticache",
    "service_envvar_name": "AWS_ENDPOINT_URL_ELASTICACHE"
  },
  {
    "service_id": "Elastic Beanstalk",
    "services_section_name": "elastic_beanstalk",
    "service_envvar_name": "AWS_ENDPOINT_URL_ELASTIC_BEANSTALK"
  },
  {
    "service_id": "Elastic Transcoder",
    "services_section_name": "elastic_transcoder",
    "service_envvar_name": "AWS_ENDPOINT_URL_ELASTIC_TRANSCODER"
  },
  {
    "service_id": "Elastic Load Balancing",
    "services_section_name": "elastic_load_balancing",
    "service_envvar_name": "AWS_ENDPOINT_URL_ELASTIC_LOAD_BALANCING"
  },
  {
    "service_id": "Elastic Load Balancing v2",
    "services_section_name": "elastic_load_balancing_v2",
    "service_envvar_name": "AWS_ENDPOINT_URL_ELASTIC_LOAD_BALANCING_V2"
  },
  {
    "service_id": "EMR",
    "services_section_name": "emr",
    "service_envvar_name": "AWS_ENDPOINT_URL_EMR"
  },
  {
    "service_id": "EMR containers",
    "services_section_name": "emr_containers",
    "service_envvar_name": "AWS_ENDPOINT_URL_EMR_CONTAINERS"
  },
  {
    "service_id": "EMR Serverless",
    "services_section_name": "emr_serverless",
    "service_envvar_name": "AWS_ENDPOINT_URL_EMR_SERVERLESS"
  },
  {
    "service_id": "Elasticsearch Service",
    "services_section_name": "elasticsearch_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_ELASTICSEARCH_SERVICE"
  },
  {
    "service_id": "EventBridge",
    "services_section_name": "eventbridge",
    "service_envvar_name": "AWS_ENDPOINT_URL_EVENTBRIDGE"
  },
  {
    "service_id": "Evidently",
    "services_section_name": "evidently",
    "service_envvar_name": "AWS_ENDPOINT_URL_EVIDENTLY"
  },
  {
    "service_id": "finspace",
    "services_section_name": "finspace",
    "service_envvar_name": "AWS_ENDPOINT_URL_FINSPACE"
  },
  {
    "service_id": "finspace data",
    "services_section_name": "finspace_data",
    "service_envvar_name": "AWS_ENDPOINT_URL_FINSPACE_DATA"
  },
  {
    "service_id": "Firehose",
    "services_section_name": "firehose",
    "service_envvar_name": "AWS_ENDPOINT_URL_FIREHOSE"
  },
  {
    "service_id": "fis",
    "services_section_name": "fis",
    "service_envvar_name": "AWS_ENDPOINT_URL_FIS"
  },
  {
    "service_id": "FMS",
    "services_section_name": "fms",
    "service_envvar_name": "AWS_ENDPOINT_URL_FMS"
  },
  {
    "service_id": "forecast",
    "services_section_name": "forecast",
    "service_envvar_name": "AWS_ENDPOINT_URL_FORECAST"
  },
  {
    "service_id": "forecastquery",
    "services_section_name": "forecastquery",
    "service_envvar_name": "AWS_ENDPOINT_URL_FORECASTQUERY"
  },
  {
    "service_id": "FraudDetector",
    "services_section_name": "frauddetector",
    "service_envvar_name": "AWS_ENDPOINT_URL_FRAUDDETECTOR"
  },
  {
    "service_id": "FSx",
    "services_section_name": "fsx",
    "service_envvar_name": "AWS_ENDPOINT_URL_FSX"
  },
  {
    "service_id": "GameLift",
    "services_section_name": "gamelift",
    "service_envvar_name": "AWS_ENDPOINT_URL_GAMELIFT"
  },
  {
    "service_id": "GameSparks",
    "services_section_name": "gamesparks",
    "service_envvar_name": "AWS_ENDPOINT_URL_GAMESPARKS"
  },
  {
    "service_id": "Glacier",
    "services_section_name": "glacier",
    "service_envvar_name": "AWS_ENDPOINT_URL_GLACIER"
  },
  {
    "service_id": "Global Accelerator",
    "services_section_name": "global_accelerator",
    "service_envvar_name": "AWS_ENDPOINT_URL_GLOBAL_ACCELERATOR"
  },
  {
    "service_id": "Glue",
    "services_section_name": "glue",
    "service_envvar_name": "AWS_ENDPOINT_URL_GLUE"
  },
  {
    "service_id": "grafana",
    "services_section_name": "grafana",
    "service_envvar_name": "AWS_ENDPOINT_URL_GRAFANA"
  },
  {
    "service_id": "Greengrass",
    "services_section_name": "greengrass",
    "service_envvar_name": "AWS_ENDPOINT_URL_GREENGRASS"
  },
  {
    "service_id": "GreengrassV2",
    "services_section_name": "greengrassv2",
    "service_envvar_name": "AWS_ENDPOINT_URL_GREENGRASSV2"
  },
  {
    "service_id": "GroundStation",
    "services_section_name": "groundstation",
    "service_envvar_name": "AWS_ENDPOINT_URL_GROUNDSTATION"
  },
  {
    "service_id": "GuardDuty",
    "services_section_name": "guardduty",
    "service_envvar_name": "AWS_ENDPOINT_URL_GUARDDUTY"
  },
  {
    "service_id": "Health",
    "services_section_name": "health",
    "service_envvar_name": "AWS_ENDPOINT_URL_HEALTH"
  },
  {
    "service_id": "HealthLake",
    "services_section_name": "healthlake",
    "service_envvar_name": "AWS_ENDPOINT_URL_HEALTHLAKE"
  },
  {
    "service_id": "IAM",
    "services_section_name": "iam",
    "service_envvar_name": "AWS_ENDPOINT_URL_IAM"
  },
  {
    "service_id": "identitystore",
    "services_section_name": "identitystore",
    "service_envvar_name": "AWS_ENDPOINT_URL_IDENTITYSTORE"
  },
  {
    "service_id": "imagebuilder",
    "services_section_name": "imagebuilder",
    "service_envvar_name": "AWS_ENDPOINT_URL_IMAGEBUILDER"
  },
  {
    "service_id": "ImportExport",
    "services_section_name": "importexport",
    "service_envvar_name": "AWS_ENDPOINT_URL_IMPORTEXPORT"
  },
  {
    "service_id": "Inspector",
    "services_section_name": "inspector",
    "service_envvar_name": "AWS_ENDPOINT_URL_INSPECTOR"
  },
  {
    "service_id": "Inspector2",
    "services_section_name": "inspector2",
    "service_envvar_name": "AWS_ENDPOINT_URL_INSPECTOR2"
  },
  {
    "service_id": "IoT",
    "services_section_name": "iot",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOT"
  },
  {
    "service_id": "IoT Data Plane",
    "services_section_name": "iot_data_plane",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOT_DATA_PLANE"
  },
  {
    "service_id": "IoT Jobs Data Plane",
    "services_section_name": "iot_jobs_data_plane",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOT_JOBS_DATA_PLANE"
  },
  {
    "service_id": "IoTAnalytics",
    "services_section_name": "iotanalytics",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOTANALYTICS"
  },
  {
    "service_id": "IotDeviceAdvisor",
    "services_section_name": "iotdeviceadvisor",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOTDEVICEADVISOR"
  },
  {
    "service_id": "IoT Events",
    "services_section_name": "iot_events",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOT_EVENTS"
  },
  {
    "service_id": "IoT Events Data",
    "services_section_name": "iot_events_data",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOT_EVENTS_DATA"
  },
  {
    "service_id": "IoTFleetWise",
    "services_section_name": "iotfleetwise",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOTFLEETWISE"
  },
  {
    "service_id": "IoTSecureTunneling",
    "services_section_name": "iotsecuretunneling",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOTSECURETUNNELING"
  },
  {
    "service_id": "IoTSiteWise",
    "services_section_name": "iotsitewise",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOTSITEWISE"
  },
  {
    "service_id": "IoTThingsGraph",
    "services_section_name": "iotthingsgraph",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOTTHINGSGRAPH"
  },
  {
    "service_id": "IoTTwinMaker",
    "services_section_name": "iottwinmaker",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOTTWINMAKER"
  },
  {
    "service_id": "IoT Wireless",
    "services_section_name": "iot_wireless",
    "service_envvar_name": "AWS_ENDPOINT_URL_IOT_WIRELESS"
  },
  {
    "service_id": "ivs",
    "services_section_name": "ivs",
    "service_envvar_name": "AWS_ENDPOINT_URL_IVS"
  },
  {
    "service_id": "ivschat",
    "services_section_name": "ivschat",
    "service_envvar_name": "AWS_ENDPOINT_URL_IVSCHAT"
  },
  {
    "service_id": "Kafka",
    "services_section_name": "kafka",
    "service_envvar_name": "AWS_ENDPOINT_URL_KAFKA"
  },
  {
    "service_id": "KafkaConnect",
    "services_section_name": "kafkaconnect",
    "service_envvar_name": "AWS_ENDPOINT_URL_KAFKACONNECT"
  },
  {
    "service_id": "kendra",
    "services_section_name": "kendra",
    "service_envvar_name": "AWS_ENDPOINT_URL_KENDRA"
  },
  {
    "service_id": "Keyspaces",
    "services_section_name": "keyspaces",
    "service_envvar_name": "AWS_ENDPOINT_URL_KEYSPACES"
  },
  {
    "service_id": "Kinesis",
    "services_section_name": "kinesis",
    "service_envvar_name": "AWS_ENDPOINT_URL_KINESIS"
  },
  {
    "service_id": "Kinesis Video Archived Media",
    "services_section_name": "kinesis_video_archived_media",
    "service_envvar_name": "AWS_ENDPOINT_URL_KINESIS_VIDEO_ARCHIVED_MEDIA"
  },
  {
    "service_id": "Kinesis Video Media",
    "services_section_name": "kinesis_video_media",
    "service_envvar_name": "AWS_ENDPOINT_URL_KINESIS_VIDEO_MEDIA"
  },
  {
    "service_id": "Kinesis Video Signaling",
    "services_section_name": "kinesis_video_signaling",
    "service_envvar_name": "AWS_ENDPOINT_URL_KINESIS_VIDEO_SIGNALING"
  },
  {
    "service_id": "Kinesis Analytics",
    "services_section_name": "kinesis_analytics",
    "service_envvar_name": "AWS_ENDPOINT_URL_KINESIS_ANALYTICS"
  },
  {
    "service_id": "Kinesis Analytics V2",
    "services_section_name": "kinesis_analytics_v2",
    "service_envvar_name": "AWS_ENDPOINT_URL_KINESIS_ANALYTICS_V2"
  },
  {
    "service_id": "Kinesis Video",
    "services_section_name": "kinesis_video",
    "service_envvar_name": "AWS_ENDPOINT_URL_KINESIS_VIDEO"
  },
  {
    "service_id": "KMS",
    "services_section_name": "kms",
    "service_envvar_name": "AWS_ENDPOINT_URL_KMS"
  },
  {
    "service_id": "LakeFormation",
    "services_section_name": "lakeformation",
    "service_envvar_name": "AWS_ENDPOINT_URL_LAKEFORMATION"
  },
  {
    "service_id": "Lambda",
    "services_section_name": "lambda",
    "service_envvar_name": "AWS_ENDPOINT_URL_LAMBDA"
  },
  {
    "service_id": "Lex Model Building Service",
    "services_section_name": "lex_model_building_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_LEX_MODEL_BUILDING_SERVICE"
  },
  {
    "service_id": "Lex Runtime Service",
    "services_section_name": "lex_runtime_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_LEX_RUNTIME_SERVICE"
  },
  {
    "service_id": "Lex Models V2",
    "services_section_name": "lex_models_v2",
    "service_envvar_name": "AWS_ENDPOINT_URL_LEX_MODELS_V2"
  },
  {
    "service_id": "Lex Runtime V2",
    "services_section_name": "lex_runtime_v2",
    "service_envvar_name": "AWS_ENDPOINT_URL_LEX_RUNTIME_V2"
  },
  {
    "service_id": "License Manager",
    "services_section_name": "license_manager",
    "service_envvar_name": "AWS_ENDPOINT_URL_LICENSE_MANAGER"
  },
  {
    "service_id": "License Manager User Subscriptions",
    "services_section_name": "license_manager_user_subscriptions",
    "service_envvar_name": "AWS_ENDPOINT_URL_LICENSE_MANAGER_USER_SUBSCRIPTIONS"
  },
  {
    "service_id": "Lightsail",
    "services_section_name": "lightsail",
    "service_envvar_name": "AWS_ENDPOINT_URL_LIGHTSAIL"
  },
  {
    "service_id": "Location",
    "services_section_name": "location",
    "service_envvar_name": "AWS_ENDPOINT_URL_LOCATION"
  },
  {
    "service_id": "CloudWatch Logs",
    "services_section_name": "cloudwatch_logs",
    "service_envvar_name": "AWS_ENDPOINT_URL_CLOUDWATCH_LOGS"
  },
  {
    "service_id": "LookoutEquipment",
    "services_section_name": "lookoutequipment",
    "service_envvar_name": "AWS_ENDPOINT_URL_LOOKOUTEQUIPMENT"
  },
  {
    "service_id": "m2",
    "services_section_name": "m2",
    "service_envvar_name": "AWS_ENDPOINT_URL_M2"
  },
  {
    "service_id": "Machine Learning",
    "services_section_name": "machine_learning",
    "service_envvar_name": "AWS_ENDPOINT_URL_MACHINE_LEARNING"
  },
  {
    "service_id": "Macie",
    "services_section_name": "macie",
    "service_envvar_name": "AWS_ENDPOINT_URL_MACIE"
  },
  {
    "service_id": "Macie2",
    "services_section_name": "macie2",
    "service_envvar_name": "AWS_ENDPOINT_URL_MACIE2"
  },
  {
    "service_id": "ManagedBlockchain",
    "services_section_name": "managedblockchain",
    "service_envvar_name": "AWS_ENDPOINT_URL_MANAGEDBLOCKCHAIN"
  },
  {
    "service_id": "Marketplace Catalog",
    "services_section_name": "marketplace_catalog",
    "service_envvar_name": "AWS_ENDPOINT_URL_MARKETPLACE_CATALOG"
  },
  {
    "service_id": "Marketplace Entitlement Service",
    "services_section_name": "marketplace_entitlement_service",
    "service_envvar_name": "AWS_ENDPOINT_URL_MARKETPLACE_ENTITLEMENT_SERVICE"
  },
  {
    "service_id": "Marketplace Commerce Analytics",
    "services_section_name": "marketplace_commerce_analytics",
    "service_envvar_name": "AWS_ENDPOINT_URL_MARKETPLACE_COMMERCE_ANALYTICS"
  },
  {
    "service_id": "MediaConnect",
    "services_section_name": "mediaconnect",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEDIACONNECT"
  },
  {
    "service_id": "MediaConvert",
    "services_section_name": "mediaconvert",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEDIACONVERT"
  },
  {
    "service_id": "MediaLive",
    "services_section_name": "medialive",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEDIALIVE"
  },
  {
    "service_id": "MediaPackage",
    "services_section_name": "mediapackage",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEDIAPACKAGE"
  },
  {
    "service_id": "MediaPackage Vod",
    "services_section_name": "mediapackage_vod",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEDIAPACKAGE_VOD"
  },
  {
    "service_id": "MediaStore",
    "services_section_name": "mediastore",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEDIASTORE"
  },
  {
    "service_id": "MediaStore Data",
    "services_section_name": "mediastore_data",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEDIASTORE_DATA"
  },
  {
    "service_id": "MediaTailor",
    "services_section_name": "mediatailor",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEDIATAILOR"
  },
  {
    "service_id": "MemoryDB",
    "services_section_name": "memorydb",
    "service_envvar_name": "AWS_ENDPOINT_URL_MEMORYDB"
  },
  {
    "service_id": "Marketplace Metering",
    "services_section_name": "marketplace_metering",
    "service_envvar_name": "AWS_ENDPOINT_URL_MARKETPLACE_METERING"
  },
  {
    "service_id": "Migration Hub",
    "services_section_name": "migration_hub",
    "service_envvar_name": "AWS_ENDPOINT_URL_MIGRATION_HUB"
  },
  {
    "service_id": "mgn",
    "services_section_name": "mgn",
    "service_envvar_name": "AWS_ENDPOINT_URL_MGN"
  },
  {
    "service_id": "Migration Hub Refactor Spaces",
    "services_section_name": "migration_hub_refactor_spaces",
    "service_envvar_name": "AWS_ENDPOINT_URL_MIGRATION_HUB_REFACTOR_SPACES"
  },
  {
    "service_id": "MigrationHub Config",
    "services_section_name": "migrationhub_config",
    "service_envvar_name": "AWS_ENDPOINT_URL_MIGRATIONHUB_CONFIG"
  },
  {
    "service_id": "MigrationHubOrchestrator",
    "services_section_name": "migrationhuborchestrator",
    "service_envvar_name": "AWS_ENDPOINT_URL_MIGRATIONHUBORCHESTRATOR"
  },
  {
    "service_id": "MigrationHubStrategy",
    "services_section_name": "migrationhubstrategy",
    "service_envvar_name": "AWS_ENDPOINT_URL_MIGRATIONHUBSTRATEGY"
  },
  {
    "service_id": "mq",
    "services_section_name": "mq",
    "service_envvar_name": "AWS_ENDPOINT_URL_MQ"
  },
  {
    "service_id": "MTurk",
    "services_section_name": "mturk",
    "service_envvar_name": "AWS_ENDPOINT_URL_MTURK"
  },
  {
    "service_id": "MWAA",
    "services_section_name": "mwaa",
    "service_envvar_name": "AWS_ENDPOINT_URL_MWAA"
  },
  {
    "service_id": "Neptune",
    "services_section_name": "neptune",
    "service_envvar_name": "AWS_ENDPOINT_URL_NEPTUNE"
  },
  {
    "service_id": "Network Firewall",
    "services_section_name": "network_firewall",
    "service_envvar_name": "AWS_ENDPOINT_URL_NETWORK_FIREWALL"
  },
  {
    "service_id": "NetworkManager",
    "services_section_name": "networkmanager",
    "service_envvar_name": "AWS_ENDPOINT_URL_NETWORKMANAGER"
  },
  {
    "service_id": "OpenSearch",
    "services_section_name": "opensearch",
    "service_envvar_name": "AWS_ENDPOINT_URL_OPENSEARCH"
  },
  {
    "service_id": "Organizations",
    "services_section_name": "organizations",
    "service_envvar_name": "AWS_ENDPOINT_URL_ORGANIZATIONS"
  },
  {
    "service_id": "Outposts",
    "services_section_name": "outposts",
    "service_envvar_name": "AWS_ENDPOINT_URL_OUTPOSTS"
  },
  {
    "service_id": "Panorama",
    "services_section_name": "panorama",
    "service_envvar_name": "AWS_ENDPOINT_URL_PANORAMA"
  },
  {
    "service_id": "Personalize",
    "services_section_name": "personalize",
    "service_envvar_name": "AWS_ENDPOINT_URL_PERSONALIZE"
  },
  {
    "service_id": "Personalize Events",
    "services_section_name": "personalize_events",
    "service_envvar_name": "AWS_ENDPOINT_URL_PERSONALIZE_EVENTS"
  },
  {
    "service_id": "Personalize Runtime",
    "services_section_name": "personalize_runtime",
    "service_envvar_name": "AWS_ENDPOINT_URL_PERSONALIZE_RUNTIME"
  },
  {
    "service_id": "PI",
    "services_section_name": "pi",
    "service_envvar_name": "AWS_ENDPOINT_URL_PI"
  },
  {
    "service_id": "Pinpoint",
    "services_section_name": "pinpoint",
    "service_envvar_name": "AWS_ENDPOINT_URL_PINPOINT"
  },
  {
    "service_id": "Pinpoint Email",
    "services_section_name": "pinpoint_email",
    "service_envvar_name": "AWS_ENDPOINT_URL_PINPOINT_EMAIL"
  },
  {
    "service_id": "Pinpoint SMS Voice",
    "services_section_name": "pinpoint_sms_voice",
    "service_envvar_name": "AWS_ENDPOINT_URL_PINPOINT_SMS_VOICE"
  },
  {
    "service_id": "Pinpoint SMS Voice V2",
    "services_section_name": "pinpoint_sms_voice_v2",
    "service_envvar_name": "AWS_ENDPOINT_URL_PINPOINT_SMS_VOICE_V2"
  },
  {
    "service_id": "Polly",
    "services_section_name": "polly",
    "service_envvar_name": "AWS_ENDPOINT_URL_POLLY"
  },
  {
    "service_id": "Pricing",
    "services_section_name": "pricing",
    "service_envvar_name": "AWS_ENDPOINT_URL_PRICING"
  },
  {
    "service_id": "Proton",
    "services_section_name": "proton",
    "service_envvar_name": "AWS_ENDPOINT_URL_PROTON"
  },
  {
    "service_id": "QuickSight",
    "services_section_name": "quicksight",
    "service_envvar_name": "AWS_ENDPOINT_URL_QUICKSIGHT"
  },
  {
    "service_id": "RAM",
    "services_section_name": "ram",
    "service_envvar_name": "AWS_ENDPOINT_URL_RAM"
  },
  {
    "service_id": "rbin",
    "services_section_name": "rbin",
    "service_envvar_name": "AWS_ENDPOINT_URL_RBIN"
  },
  {
    "service_id": "RDS",
    "services_section_name": "rds",
    "service_envvar_name": "AWS_ENDPOINT_URL_RDS"
  },
  {
    "service_id": "RDS Data",
    "services_section_name": "rds_data",
    "service_envvar_name": "AWS_ENDPOINT_URL_RDS_DATA"
  },
  {
    "service_id": "Redshift",
    "services_section_name": "redshift",
    "service_envvar_name": "AWS_ENDPOINT_URL_REDSHIFT"
  },
  {
    "service_id": "Redshift Data",
    "services_section_name": "redshift_data",
    "service_envvar_name": "AWS_ENDPOINT_URL_REDSHIFT_DATA"
  },
  {
    "service_id": "Redshift Serverless",
    "services_section_name": "redshift_serverless",
    "service_envvar_name": "AWS_ENDPOINT_URL_REDSHIFT_SERVERLESS"
  },
  {
    "service_id": "Rekognition",
    "services_section_name": "rekognition",
    "service_envvar_name": "AWS_ENDPOINT_URL_REKOGNITION"
  },
  {
    "service_id": "resiliencehub",
    "services_section_name": "resiliencehub",
    "service_envvar_name": "AWS_ENDPOINT_URL_RESILIENCEHUB"
  },
  {
    "service_id": "Resource Groups",
    "services_section_name": "resource_groups",
    "service_envvar_name": "AWS_ENDPOINT_URL_RESOURCE_GROUPS"
  },
  {
    "service_id": "Resource Groups Tagging API",
    "services_section_name": "resource_groups_tagging_api",
    "service_envvar_name": "AWS_ENDPOINT_URL_RESOURCE_GROUPS_TAGGING_API"
  },
  {
    "service_id": "RolesAnywhere",
    "services_section_name": "rolesanywhere",
    "service_envvar_name": "AWS_ENDPOINT_URL_ROLESANYWHERE"
  },
  {
    "service_id": "Route 53",
    "services_section_name": "route_53",
    "service_envvar_name": "AWS_ENDPOINT_URL_ROUTE_53"
  },
  {
    "service_id": "Route53 Recovery Cluster",
    "services_section_name": "route53_recovery_cluster",
    "service_envvar_name": "AWS_ENDPOINT_URL_ROUTE53_RECOVERY_CLUSTER"
  },
  {
    "service_id": "Route53 Recovery Control Config",
    "services_section_name": "route53_recovery_control_config",
    "service_envvar_name": "AWS_ENDPOINT_URL_ROUTE53_RECOVERY_CONTROL_CONFIG"
  },
  {
    "service_id": "Route53 Recovery Readiness",
    "services_section_name": "route53_recovery_readiness",
    "service_envvar_name": "AWS_ENDPOINT_URL_ROUTE53_RECOVERY_READINESS"
  },
  {
    "service_id": "Route 53 Domains",
    "services_section_name": "route_53_domains",
    "service_envvar_name": "AWS_ENDPOINT_URL_ROUTE_53_DOMAINS"
  },
  {
    "service_id": "Route53Resolver",
    "services_section_name": "route53resolver",
    "service_envvar_name": "AWS_ENDPOINT_URL_ROUTE53RESOLVER"
  },
  {
    "service_id": "RUM",
    "services_section_name": "rum",
    "service_envvar_name": "AWS_ENDPOINT_URL_RUM"
  },
  {
    "service_id": "S3",
    "services_section_name": "s3",
    "service_envvar_name": "AWS_ENDPOINT_URL_S3"
  },
  {
    "service_id": "S3 Control",
    "services_section_name": "s3_control",
    "service_envvar_name": "AWS_ENDPOINT_URL_S3_CONTROL"
  },
  {
    "service_id": "S3Outposts",
    "services_section_name": "s3outposts",
    "service_envvar_name": "AWS_ENDPOINT_URL_S3OUTPOSTS"
  },
  {
    "service_id": "SageMaker",
    "services_section_name": "sagemaker",
    "service_envvar_name": "AWS_ENDPOINT_URL_SAGEMAKER"
  },
  {
    "service_id": "SageMaker A2I Runtime",
    "services_section_name": "sagemaker_a2i_runtime",
    "service_envvar_name": "AWS_ENDPOINT_URL_SAGEMAKER_A2I_RUNTIME"
  },
  {
    "service_id": "Sagemaker Edge",
    "services_section_name": "sagemaker_edge",
    "service_envvar_name": "AWS_ENDPOINT_URL_SAGEMAKER_EDGE"
  },
  {
    "service_id": "SageMaker FeatureStore Runtime",
    "services_section_name": "sagemaker_featurestore_runtime",
    "service_envvar_name": "AWS_ENDPOINT_URL_SAGEMAKER_FEATURESTORE_RUNTIME"
  },
  {
    "service_id": "SageMaker Runtime",
    "services_section_name": "sagemaker_runtime",
    "service_envvar_name": "AWS_ENDPOINT_URL_SAGEMAKER_RUNTIME"
  },
  {
    "service_id": "savingsplans",
    "services_section_name": "savingsplans",
    "service_envvar_name": "AWS_ENDPOINT_URL_SAVINGSPLANS"
  },
  {
    "service_id": "schemas",
    "services_section_name": "schemas",
    "service_envvar_name": "AWS_ENDPOINT_URL_SCHEMAS"
  },
  {
    "service_id": "SimpleDB",
    "services_section_name": "simpledb",
    "service_envvar_name": "AWS_ENDPOINT_URL_SIMPLEDB"
  },
  {
    "service_id": "Secrets Manager",
    "services_section_name": "secrets_manager",
    "service_envvar_name": "AWS_ENDPOINT_URL_SECRETS_MANAGER"
  },
  {
    "service_id": "SecurityHub",
    "services_section_name": "securityhub",
    "service_envvar_name": "AWS_ENDPOINT_URL_SECURITYHUB"
  },
  {
    "service_id": "ServerlessApplicationRepository",
    "services_section_name": "serverlessapplicationrepository",
    "service_envvar_name": "AWS_ENDPOINT_URL_SERVERLESSAPPLICATIONREPOSITORY"
  },
  {
    "service_id": "Service Quotas",
    "services_section_name": "service_quotas",
    "service_envvar_name": "AWS_ENDPOINT_URL_SERVICE_QUOTAS"
  },
  {
    "service_id": "Service Catalog",
    "services_section_name": "service_catalog",
    "service_envvar_name": "AWS_ENDPOINT_URL_SERVICE_CATALOG"
  },
  {
    "service_id": "Service Catalog AppRegistry",
    "services_section_name": "service_catalog_appregistry",
    "service_envvar_name": "AWS_ENDPOINT_URL_SERVICE_CATALOG_APPREGISTRY"
  },
  {
    "service_id": "ServiceDiscovery",
    "services_section_name": "servicediscovery",
    "service_envvar_name": "AWS_ENDPOINT_URL_SERVICEDISCOVERY"
  },
  {
    "service_id": "SES",
    "services_section_name": "ses",
    "service_envvar_name": "AWS_ENDPOINT_URL_SES"
  },
  {
    "service_id": "SESv2",
    "services_section_name": "sesv2",
    "service_envvar_name": "AWS_ENDPOINT_URL_SESV2"
  },
  {
    "service_id": "Shield",
    "services_section_name": "shield",
    "service_envvar_name": "AWS_ENDPOINT_URL_SHIELD"
  },
  {
    "service_id": "signer",
    "services_section_name": "signer",
    "service_envvar_name": "AWS_ENDPOINT_URL_SIGNER"
  },
  {
    "service_id": "Pinpoint SMS Voice",
    "services_section_name": "pinpoint_sms_voice",
    "service_envvar_name": "AWS_ENDPOINT_URL_PINPOINT_SMS_VOICE"
  },
  {
    "service_id": "Snow Device Management",
    "services_section_name": "snow_device_management",
    "service_envvar_name": "AWS_ENDPOINT_URL_SNOW_DEVICE_MANAGEMENT"
  },
  {
    "service_id": "Snowball",
    "services_section_name": "snowball",
    "service_envvar_name": "AWS_ENDPOINT_URL_SNOWBALL"
  },
  {
    "service_id": "SNS",
    "services_section_name": "sns",
    "service_envvar_name": "AWS_ENDPOINT_URL_SNS"
  },
  {
    "service_id": "SQS",
    "services_section_name": "sqs",
    "service_envvar_name": "AWS_ENDPOINT_URL_SQS"
  },
  {
    "service_id": "SSM",
    "services_section_name": "ssm",
    "service_envvar_name": "AWS_ENDPOINT_URL_SSM"
  },
  {
    "service_id": "SSM Contacts",
    "services_section_name": "ssm_contacts",
    "service_envvar_name": "AWS_ENDPOINT_URL_SSM_CONTACTS"
  },
  {
    "service_id": "SSM Incidents",
    "services_section_name": "ssm_incidents",
    "service_envvar_name": "AWS_ENDPOINT_URL_SSM_INCIDENTS"
  },
  {
    "service_id": "SSO",
    "services_section_name": "sso",
    "service_envvar_name": "AWS_ENDPOINT_URL_SSO"
  },
  {
    "service_id": "SSO Admin",
    "services_section_name": "sso_admin",
    "service_envvar_name": "AWS_ENDPOINT_URL_SSO_ADMIN"
  },
  {
    "service_id": "SSO OIDC",
    "services_section_name": "sso_oidc",
    "service_envvar_name": "AWS_ENDPOINT_URL_SSO_OIDC"
  },
  {
    "service_id": "SFN",
    "services_section_name": "sfn",
    "service_envvar_name": "AWS_ENDPOINT_URL_SFN"
  },
  {
    "service_id": "Storage Gateway",
    "services_section_name": "storage_gateway",
    "service_envvar_name": "AWS_ENDPOINT_URL_STORAGE_GATEWAY"
  },
  {
    "service_id": "STS",
    "services_section_name": "sts",
    "service_envvar_name": "AWS_ENDPOINT_URL_STS"
  },
  {
    "service_id": "Support",
    "services_section_name": "support",
    "service_envvar_name": "AWS_ENDPOINT_URL_SUPPORT"
  },
  {
    "service_id": "Support App",
    "services_section_name": "support_app",
    "service_envvar_name": "AWS_ENDPOINT_URL_SUPPORT_APP"
  },
  {
    "service_id": "SWF",
    "services_section_name": "swf",
    "service_envvar_name": "AWS_ENDPOINT_URL_SWF"
  },
  {
    "service_id": "synthetics",
    "services_section_name": "synthetics",
    "service_envvar_name": "AWS_ENDPOINT_URL_SYNTHETICS"
  },
  {
    "service_id": "Textract",
    "services_section_name": "textract",
    "service_envvar_name": "AWS_ENDPOINT_URL_TEXTRACT"
  },
  {
    "service_id": "Timestream Query",
    "services_section_name": "timestream_query",
    "service_envvar_name": "AWS_ENDPOINT_URL_TIMESTREAM_QUERY"
  },
  {
    "service_id": "Timestream Write",
    "services_section_name": "timestream_write",
    "service_envvar_name": "AWS_ENDPOINT_URL_TIMESTREAM_WRITE"
  },
  {
    "service_id": "Transcribe",
    "services_section_name": "transcribe",
    "service_envvar_name": "AWS_ENDPOINT_URL_TRANSCRIBE"
  },
  {
    "service_id": "Transfer",
    "services_section_name": "transfer",
    "service_envvar_name": "AWS_ENDPOINT_URL_TRANSFER"
  },
  {
    "service_id": "Translate",
    "services_section_name": "translate",
    "service_envvar_name": "AWS_ENDPOINT_URL_TRANSLATE"
  },
  {
    "service_id": "Voice ID",
    "services_section_name": "voice_id",
    "service_envvar_name": "AWS_ENDPOINT_URL_VOICE_ID"
  },
  {
    "service_id": "WAF",
    "services_section_name": "waf",
    "service_envvar_name": "AWS_ENDPOINT_URL_WAF"
  },
  {
    "service_id": "WAF Regional",
    "services_section_name": "waf_regional",
    "service_envvar_name": "AWS_ENDPOINT_URL_WAF_REGIONAL"
  },
  {
    "service_id": "WAFV2",
    "services_section_name": "wafv2",
    "service_envvar_name": "AWS_ENDPOINT_URL_WAFV2"
  },
  {
    "service_id": "WellArchitected",
    "services_section_name": "wellarchitected",
    "service_envvar_name": "AWS_ENDPOINT_URL_WELLARCHITECTED"
  },
  {
    "service_id": "Wisdom",
    "services_section_name": "wisdom",
    "service_envvar_name": "AWS_ENDPOINT_URL_WISDOM"
  },
  {
    "service_id": "WorkDocs",
    "services_section_name": "workdocs",
    "service_envvar_name": "AWS_ENDPOINT_URL_WORKDOCS"
  },
  {
    "service_id": "WorkMail",
    "services_section_name": "workmail",
    "service_envvar_name": "AWS_ENDPOINT_URL_WORKMAIL"
  },
  {
    "service_id": "WorkMailMessageFlow",
    "services_section_name": "workmailmessageflow",
    "service_envvar_name": "AWS_ENDPOINT_URL_WORKMAILMESSAGEFLOW"
  },
  {
    "service_id": "WorkSpaces",
    "services_section_name": "workspaces",
    "service_envvar_name": "AWS_ENDPOINT_URL_WORKSPACES"
  },
  {
    "service_id": "WorkSpaces Web",
    "services_section_name": "workspaces_web",
    "service_envvar_name": "AWS_ENDPOINT_URL_WORKSPACES_WEB"
  },
  {
    "service_id": "XRay",
    "services_section_name": "xray",
    "service_envvar_name": "AWS_ENDPOINT_URL_XRAY"
  }
]
"""

// language=JSON
private const val JAVA_CLIENT_NAMES_JSON = """
{
  "AppRunner": "AppRunnerClient",
  "EC2": "Ec2Client",
  "Lex Models V2": "LexModelsV2Client",
  "Account": "AccountClient",
  "tnb": "TnbClient",
  "CodeGuru Security": "CodeGuruSecurityClient",
  "finspace data": "FinspaceDataClient",
  "SageMaker": "SageMakerClient",
  "Athena": "AthenaClient",
  "CodeGuru Reviewer": "CodeGuruReviewerClient",
  "ACM": "AcmClient",
  "STS": "StsClient",
  "DataExchange": "DataExchangeClient",
  "Kinesis Video Media": "KinesisVideoMediaClient",
  "Marketplace Catalog": "MarketplaceCatalogClient",
  "Route 53": "Route53Client",
  "EFS": "EfsClient",
  "Migration Hub Refactor Spaces": "MigrationHubRefactorSpacesClient",
  "RAM": "RamClient",
  "SSO Admin": "SsoAdminClient",
  "XRay": "XRayClient",
  "Personalize Events": "PersonalizeEventsClient",
  "API Gateway": "ApiGatewayClient",
  "ApiGatewayManagementApi": "ApiGatewayManagementApiClient",
  "RolesAnywhere": "RolesAnywhereClient",
  "HealthLake": "HealthLakeClient",
  "Resource Groups Tagging API": "ResourceGroupsTaggingApiClient",
  "CodeCommit": "CodeCommitClient",
  "Data Pipeline": "DataPipelineClient",
  "Transcribe": "TranscribeClient",
  "forecastquery": "ForecastqueryClient",
  "IVS RealTime": "IvsRealTimeClient",
  "Elasticsearch Service": "ElasticsearchClient",
  "Personalize Runtime": "PersonalizeRuntimeClient",
  "SWF": "SwfClient",
  "CloudWatch Events": "CloudWatchEventsClient",
  "AppStream": "AppStreamClient",
  "Auto Scaling Plans": "AutoScalingPlansClient",
  "RDS": "RdsClient",
  "Polly": "PollyClient",
  "Marketplace Metering": "MarketplaceMeteringClient",
  "Device Farm": "DeviceFarmClient",
  "Pricing": "PricingClient",
  "Rekognition": "RekognitionClient",
  "Cognito Identity Provider": "CognitoIdentityProviderClient",
  "Chime SDK Voice": "ChimeSdkVoiceClient",
  "Connect": "ConnectClient",
  "WorkSpaces Web": "WorkSpacesWebClient",
  "SFN": "SfnClient",
  "ElastiCache": "ElastiCacheClient",
  "Comprehend": "ComprehendClient",
  "kendra": "KendraClient",
  "CloudFormation": "CloudFormationClient",
  "OpenSearchServerless": "OpenSearchServerlessClient",
  "SecurityHub": "SecurityHubClient",
  "Backup": "BackupClient",
  "Greengrass": "GreengrassClient",
  "AppConfigData": "AppConfigDataClient",
  "IoT Jobs Data Plane": "IotJobsDataPlaneClient",
  "Timestream Query": "TimestreamQueryClient",
  "Lex Model Building Service": "LexModelBuildingClient",
  "Medical Imaging": "MedicalImagingClient",
  "Auto Scaling": "AutoScalingClient",
  "SES": "SesClient",
  "Firehose": "FirehoseClient",
  "ivschat": "IvschatClient",
  "Global Accelerator": "GlobalAcceleratorClient",
  "Lex Runtime Service": "LexRuntimeClient",
  "Customer Profiles": "CustomerProfilesClient",
  "DocDB": "DocDbClient",
  "DAX": "DaxClient",
  "ACM PCA": "AcmPcaClient",
  "GuardDuty": "GuardDutyClient",
  "CleanRooms": "CleanRoomsClient",
  "codeartifact": "CodeartifactClient",
  "CloudDirectory": "CloudDirectoryClient",
  "Snow Device Management": "SnowDeviceManagementClient",
  "MediaPackage Vod": "MediaPackageVodClient",
  "Lex Runtime V2": "LexRuntimeV2Client",
  "ServerlessApplicationRepository": "ServerlessApplicationRepositoryClient",
  "CodeStar connections": "CodeStarConnectionsClient",
  "Pinpoint SMS Voice V2": "PinpointSmsVoiceV2Client",
  "IoTSiteWise": "IotSiteWiseClient",
  "ivs": "IvsClient",
  "InternetMonitor": "InternetMonitorClient",
  "mq": "MqClient",
  "Direct Connect": "DirectConnectClient",
  "Panorama": "PanoramaClient",
  "IoT Events": "IotEventsClient",
  "WAF Regional": "WafRegionalClient",
  "WAF": "WafClient",
  "ManagedBlockchain": "ManagedBlockchainClient",
  "SSO OIDC": "SsoOidcClient",
  "Budgets": "BudgetsClient",
  "Detective": "DetectiveClient",
  "m2": "M2Client",
  "Lambda": "LambdaClient",
  "Ssm Sap": "SsmSapClient",
  "billingconductor": "BillingconductorClient",
  "ServiceDiscovery": "ServiceDiscoveryClient",
  "DLM": "DlmClient",
  "finspace": "FinspaceClient",
  "CodeDeploy": "CodeDeployClient",
  "AmplifyBackend": "AmplifyBackendClient",
  "IoT 1Click Projects": "Iot1ClickProjectsClient",
  "CloudHSM V2": "CloudHsmV2Client",
  "savingsplans": "SavingsplansClient",
  "Batch": "BatchClient",
  "MigrationHubStrategy": "MigrationHubStrategyClient",
  "Kafka": "KafkaClient",
  "Backup Gateway": "BackupGatewayClient",
  "MigrationHub Config": "MigrationHubConfigClient",
  "AppSync": "AppSyncClient",
  "ApiGatewayV2": "ApiGatewayV2Client",
  "DataBrew": "DataBrewClient",
  "amp": "AmpClient",
  "drs": "DrsClient",
  "DevOps Guru": "DevOpsGuruClient",
  "EKS": "EksClient",
  "KafkaConnect": "KafkaConnectClient",
  "ConnectCampaigns": "ConnectCampaignsClient",
  "Textract": "TextractClient",
  "Support": "SupportClient",
  "identitystore": "IdentitystoreClient",
  "Kinesis Analytics": "KinesisAnalyticsClient",
  "Snowball": "SnowballClient",
  "Route53 Recovery Control Config": "Route53RecoveryControlConfigClient",
  "Redshift Serverless": "RedshiftServerlessClient",
  "MTurk": "MTurkClient",
  "AuditManager": "AuditManagerClient",
  "SSO": "SsoClient",
  "Cloud9": "Cloud9Client",
  "Network Firewall": "NetworkFirewallClient",
  "IAM": "IamClient",
  "Keyspaces": "KeyspacesClient",
  "MediaStore": "MediaStoreClient",
  "Redshift Data": "RedshiftDataClient",
  "mgn": "MgnClient",
  "Wisdom": "WisdomClient",
  "ConnectParticipant": "ConnectParticipantClient",
  "ApplicationCostProfiler": "ApplicationCostProfilerClient",
  "Compute Optimizer": "ComputeOptimizerClient",
  "SQS": "SqsClient",
  "Chime SDK Identity": "ChimeSdkIdentityClient",
  "AppConfig": "AppConfigClient",
  "SageMaker FeatureStore Runtime": "SageMakerFeatureStoreRuntimeClient",
  "PI": "PiClient",
  "EMR Serverless": "EmrServerlessClient",
  "EMR containers": "EmrContainersClient",
  "Chime SDK Messaging": "ChimeSdkMessagingClient",
  "Application Insights": "ApplicationInsightsClient",
  "MediaTailor": "MediaTailorClient",
  "Resource Explorer 2": "ResourceExplorer2Client",
  "ManagedBlockchain Query": "ManagedBlockchainQueryClient",
  "MediaPackageV2": "MediaPackageV2Client",
  "AmplifyUIBuilder": "AmplifyUiBuilderClient",
  "ConnectCases": "ConnectCasesClient",
  "Evidently": "EvidentlyClient",
  "GameLift": "GameLiftClient",
  "Appflow": "AppflowClient",
  "CloudTrail": "CloudTrailClient",
  "Pipes": "PipesClient",
  "Marketplace Entitlement Service": "MarketplaceEntitlementClient",
  "Redshift": "RedshiftClient",
  "WellArchitected": "WellArchitectedClient",
  "Kinesis Video WebRTC Storage": "KinesisVideoWebRtcStorageClient",
  "Amplify": "AmplifyClient",
  "EBS": "EbsClient",
  "IoT": "IotClient",
  "MigrationHubOrchestrator": "MigrationHubOrchestratorClient",
  "Service Catalog AppRegistry": "ServiceCatalogAppRegistryClient",
  "CloudControl": "CloudControlClient",
  "Application Discovery Service": "ApplicationDiscoveryClient",
  "App Mesh": "AppMeshClient",
  "schemas": "SchemasClient",
  "codestar notifications": "CodestarNotificationsClient",
  "Cost and Usage Report Service": "CostAndUsageReportClient",
  "Directory Service": "DirectoryClient",
  "FSx": "FSxClient",
  "Machine Learning": "MachineLearningClient",
  "CodePipeline": "CodePipelineClient",
  "AppFabric": "AppFabricClient",
  "Kendra Ranking": "KendraRankingClient",
  "CloudWatch Logs": "CloudWatchLogsClient",
  "Elastic Load Balancing v2": "ElasticLoadBalancingV2Client",
  "Kinesis Video Signaling": "KinesisVideoSignalingClient",
  "CloudSearch": "CloudSearchClient",
  "Route53Resolver": "Route53ResolverClient",
  "Connect Contact Lens": "ConnectContactLensClient",
  "Chime SDK Media Pipelines": "ChimeSdkMediaPipelinesClient",
  "Route53 Recovery Cluster": "Route53RecoveryClusterClient",
  "ECS": "EcsClient",
  "WorkSpaces": "WorkSpacesClient",
  "Elastic Load Balancing": "ElasticLoadBalancingClient",
  "IoT Events Data": "IotEventsDataClient",
  "Storage Gateway": "StorageGatewayClient",
  "grafana": "GrafanaClient",
  "rbin": "RbinClient",
  "S3Outposts": "S3OutpostsClient",
  "Application Auto Scaling": "ApplicationAutoScalingClient",
  "EntityResolution": "EntityResolutionClient",
  "Database Migration Service": "DatabaseMigrationClient",
  "DynamoDB": "DynamoDbClient",
  "resiliencehub": "ResiliencehubClient",
  "DynamoDB Streams": "DynamoDbStreamsClient",
  "ECR": "EcrClient",
  "Chime SDK Meetings": "ChimeSdkMeetingsClient",
  "Resource Groups": "ResourceGroupsClient",
  "Route 53 Domains": "Route53DomainsClient",
  "Macie2": "Macie2Client",
  "Scheduler": "SchedulerClient",
  "Support App": "SupportAppClient",
  "Braket": "BraketClient",
  "Neptune": "NeptuneClient",
  "VerifiedPermissions": "VerifiedPermissionsClient",
  "Elastic Beanstalk": "ElasticBeanstalkClient",
  "SimSpaceWeaver": "SimSpaceWeaverClient",
  "Transfer": "TransferClient",
  "SageMaker Metrics": "SageMakerMetricsClient",
  "CloudSearch Domain": "CloudSearchDomainClient",
  "Migration Hub": "MigrationHubClient",
  "Glacier": "GlacierClient",
  "Lightsail": "LightsailClient",
  "imagebuilder": "ImagebuilderClient",
  "WorkMailMessageFlow": "WorkMailMessageFlowClient",
  "Kinesis Video Archived Media": "KinesisVideoArchivedMediaClient",
  "AccessAnalyzer": "AccessAnalyzerClient",
  "Route53 Recovery Readiness": "Route53RecoveryReadinessClient",
  "ComprehendMedical": "ComprehendMedicalClient",
  "IoTThingsGraph": "IotThingsGraphClient",
  "IoTFleetWise": "IotFleetWiseClient",
  "RUM": "RumClient",
  "OpenSearch": "OpenSearchClient",
  "EC2 Instance Connect": "Ec2InstanceConnectClient",
  "Health": "HealthClient",
  "CloudFront": "CloudFrontClient",
  "WAFV2": "Wafv2Client",
  "Cost Explorer": "CostExplorerClient",
  "IoTSecureTunneling": "IotSecureTunnelingClient",
  "Kinesis Analytics V2": "KinesisAnalyticsV2Client",
  "CodeCatalyst": "CodeCatalystClient",
  "RDS Data": "RdsDataClient",
  "Location": "LocationClient",
  "Marketplace Commerce Analytics": "MarketplaceCommerceAnalyticsClient",
  "synthetics": "SyntheticsClient",
  "SSM Incidents": "SsmIncidentsClient",
  "VPC Lattice": "VpcLatticeClient",
  "Inspector": "InspectorClient",
  "Translate": "TranslateClient",
  "FMS": "FmsClient",
  "S3 Control": "S3ControlClient",
  "MemoryDB": "MemoryDbClient",
  "Payment Cryptography": "PaymentCryptographyClient",
  "Cognito Identity": "CognitoIdentityClient",
  "OSIS": "OsisClient",
  "Proton": "ProtonClient",
  "ECR PUBLIC": "EcrPublicClient",
  "CloudTrail Data": "CloudTrailDataClient",
  "CloudHSM": "CloudHsmClient",
  "Voice ID": "VoiceIdClient",
  "fis": "FisClient",
  "GameSparks": "GameSparksClient",
  "Config Service": "ConfigClient",
  "GroundStation": "GroundStationClient",
  "Inspector2": "Inspector2Client",
  "IoTAnalytics": "IotAnalyticsClient",
  "SageMaker A2I Runtime": "SageMakerA2IRuntimeClient",
  "neptunedata": "NeptunedataClient",
  "Pinpoint Email": "PinpointEmailClient",
  "Sagemaker Edge": "SagemakerEdgeClient",
  "Macie": "MacieClient",
  "GreengrassV2": "GreengrassV2Client",
  "DataSync": "DataSyncClient",
  "SNS": "SnsClient",
  "MediaLive": "MediaLiveClient",
  "MediaConvert": "MediaConvertClient",
  "MediaPackage": "MediaPackageClient",
  "WorkDocs": "WorkDocsClient",
  "NetworkManager": "NetworkManagerClient",
  "Omics": "OmicsClient",
  "Cognito Sync": "CognitoSyncClient",
  "Outposts": "OutpostsClient",
  "Personalize": "PersonalizeClient",
  "SESv2": "SesV2Client",
  "ARC Zonal Shift": "ArcZonalShiftClient",
  "EMR": "EmrClient",
  "AppIntegrations": "AppIntegrationsClient",
  "S3": "S3Client",
  "Service Quotas": "ServiceQuotasClient",
  "LookoutEquipment": "LookoutEquipmentClient",
  "CloudWatch": "CloudWatchClient",
  "Pca Connector Ad": "PcaConnectorAdClient",
  "Glue": "GlueClient",
  "SecurityLake": "SecurityLakeClient",
  "Pinpoint SMS Voice": "PinpointSmsVoiceClient",
  "EventBridge": "EventBridgeClient",
  "FraudDetector": "FraudDetectorClient",
  "QuickSight": "QuickSightClient",
  "Elastic Transcoder": "ElasticTranscoderClient",
  "WorkMail": "WorkMailClient",
  "Secrets Manager": "SecretsManagerClient",
  "Service Catalog": "ServiceCatalogClient",
  "SageMaker Geospatial": "SageMakerGeospatialClient",
  "MWAA": "MwaaClient",
  "signer": "SignerClient",
  "SageMaker Runtime": "SageMakerRuntimeClient",
  "License Manager User Subscriptions": "LicenseManagerUserSubscriptionsClient",
  "MediaConnect": "MediaConnectClient",
  "ControlTower": "ControlTowerClient",
  "KMS": "KmsClient",
  "LakeFormation": "LakeFormationClient",
  "SSM": "SsmClient",
  "License Manager": "LicenseManagerClient",
  "MediaStore Data": "MediaStoreDataClient",
  "Shield": "ShieldClient",
  "Kinesis": "KinesisClient",
  "Organizations": "OrganizationsClient",
  "Pinpoint": "PinpointClient",
  "CodeGuruProfiler": "CodeGuruProfilerClient",
  "Kinesis Video": "KinesisVideoClient",
  "Payment Cryptography Data": "PaymentCryptographyDataClient",
  "Chime": "ChimeClient",
  "IoT 1Click Devices Service": "Iot1ClickDevicesClient",
  "Timestream Write": "TimestreamWriteClient",
  "CodeBuild": "CodeBuildClient",
  "DocDB Elastic": "DocDbElasticClient",
  "IoTTwinMaker": "IotTwinMakerClient",
  "IotDeviceAdvisor": "IotDeviceAdvisorClient",
  "License Manager Linux Subscriptions": "LicenseManagerLinuxSubscriptionsClient",
  "forecast": "ForecastClient",
  "IoT Wireless": "IotWirelessClient",
  "IoT Data Plane": "IotDataPlaneClient",
  "SSM Contacts": "SsmContactsClient",
  "OAM": "OamClient"
}
"""
