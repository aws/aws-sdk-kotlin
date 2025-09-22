# Changelog

## [1.5.43] - 09/19/2025

### Features
* (**bedrockagentcorecontrol**) Add tagging and VPC support to AgentCore Runtime, Code Interpreter, and Browser resources. Add support for configuring request headers in Runtime. Fix AgentCore Runtime shape names.
* (**configservice**) Add UNKNOWN state to RemediationExecutionState and add IN_PROGRESS/EXITED/UNKNOWN states to RemediationExecutionStepState.
* (**connect**) This release adds a persistent connection field to UserPhoneConfig that maintains agent's softphone media connection for faster call connections.
* (**licensemanagerusersubscriptions**) Added support for cross-account Active Directories.
* (**medialive**) Add MinBitrate for QVBR mode under H264/H265/AV1 output codec. Add GopBReference, GopNumBFrames, SubGopLength fields under H265 output codec.

### Documentation
* (**kendraranking**) Model whitespace change - no client difference
* (**sqs**) Update invalid character handling documentation for SQS SendMessage API

## [1.5.42] - 09/18/2025

### Features
* (**bedrock**) Release includes an increase to the maximum policy build document size, an update to DeleteAutomatedReasoningPolicyBuildWorkflow to add ResourceInUseException, and corrections to UpdateAutomatedReasoningPolicyTestCaseRequest.
* (**budgets**) Added BillingViewHealthStatus Exception which is thrown when a Budget is created or updated with a Billing View that is not in the HEALTHY status
* (**chimesdkmessaging**) Amazon Chime SDK Messaging GetMessagingSessionEndpoint API now returns dual-stack WebSocket endpoints supporting IPv4/IPv6.
* (**ec2**) Allowed AMIs adds support for four new parameters - marketplaceProductCodes, deprecationTimeCondition, creationDateCondition and imageNames

## [1.5.41] - 09/17/2025

### Features
* (**ec2**) Add mac-m4.metal and mac-m4pro.metal instance types.
* (**networkfirewall**) Network Firewall now prevents TLS handshakes with the target server until after the Server Name Indication (SNI) has been seen and verified. The monitoring dashboard now provides deeper insights into PrivateLink endpoint candidates and offers filters based on IP addresses and protocol.
* (**pcs**) Add support for Amazon EC2 Capacity Blocks for ML

## [1.5.40] - 09/16/2025

### Features
* (**budgets**) Add support for custom time periods in budget configuration
* (**cloudwatchlogs**) Cloudwatch Logs added support for 2 new API parameters in metric and subscription filter APIs to filter log events based on system field values and emit system field values as dimensions and send them to customer destination as additional metadata.
* (**ivsrealtime**) IVS now offers customers the ability to control the positioning of participants in both grid and PiP layouts based on custom attribute values in participant tokens.
* (**osis**) Adds support for cross-account ingestion for push-based sources. This includes resource policies for sharing pipelines across accounts and features for managing pipeline endpoints which enable accessing pipelines across different VPCs, including VPCs in other accounts.

## [1.5.39] - 09/15/2025

### Features
* (**costexplorer**) Added endpoint support for eusc-de-east-1 region.
* (**medicalimaging**) Added support for OpenID Connect (OIDC) custom authorizer
* (**observabilityadmin**) CloudWatch Observability Admin adds the ability to enable telemetry centralization in customers' Organizations. The release introduces new APIs to manage centralization rules, which define settings to replicate telemetry data to a central destination in the customers' Organization.
* (**s3control**) Introduce three new encryption filters: EncryptionType (SSE-S3, SSE-KMS, DSSE-KMS, SSE-C, NOT-SSE), KmsKeyArn (for SSE-KMS and DSSE-KMS), and BucketKeyEnabled (for SSE-KMS).

## [1.5.38] - 09/12/2025

### Features
* (**paymentcryptography**) Add support for certificates to be signed by 3rd party certificate authorities.  New API GetCertificateSigningRequest API and support for providing certificates at run-time for tr-34 import/export

### Documentation
* Improve documentation for [`AwsRetryPolicy`](https://docs.aws.amazon.com/sdk-for-kotlin/api/latest/aws-http/aws.sdk.kotlin.runtime.http.retries/-aws-retry-policy/)

## [1.5.37] - 09/11/2025

### Features
* (**amp**) Add Vended Logs APIs for Amazon Prometheus Managed Collector
* (**datazone**) adding IAM principal id to IAM user profile details
* (**ecs**) This release supports hook details for Amazon ECS lifecycle hooks.
* (**emrcontainers**) Added nodeLabel support in container provider to aid hardware isolation support for virtual cluster and security configuration.
* (**evs**) CreateEnvironment API now supports parameters (isHcxPublic & hcxNetworkAclId) for HCX migration via public internet, adding flexibility for migration scenarios. New APIs have been added for associating (AssociateEipToVlan) & disassociating (DisassociateEipFromVlan) Elastic IP (EIP) addresses.
* (**guardduty**) Updated Java SDK implementation of entity set status in GuardDuty API.
* (**medialive**) AWS Elemental MediaLive adds a new feature in MediaPackage output group that enables MediaPackage V2 users to control HLS-related parameters directly in MediaLive. These parameter settings are then reflected in MediaPackage outputs, providing more streamlined control over HLS configurations.
* (**quicksight**) This release adds support for Account level custom permissions, additional Dashboard Options, and Null support for Q&A.
* (**rds**) Adds support for end-to-end IAM authentication in RDS Proxy for MySQL, MariaDB, and PostgreSQL engines.

## [1.5.36] - 09/10/2025

### Features
* (**networkflowmonitor**) Added new enum value (AWS::Region) for type field under MonitorLocalResource and MonitorRemoteResource. Workload Insights and Monitor top contributors queries now support a new DestinationCategory (INTER_REGION).
* (**paymentcryptography**) AWS Payment Cryptography Service now supports Multi-Region key replication. Customers can choose to automatically distribute keys across AWS Regions.

## [1.5.34] - 09/08/2025

### Features
* (**iotsitewise**) Add ComputationModelVersion support in IoT SiteWise APIs
* (**s3**) This release includes backward compatibility work on the "Expires" parameter.
* (**securityhub**) This release adds the RESOURCE_NOT_FOUND error code as a possible value in responses to the following operations: BatchGetStandardsControlAssociations, BatchUpdateStandardsControlAssociations, and BatchGetSecurityControls.

## [1.5.33] - 09/05/2025

### Features
* (**sagemaker**) Release IPv6 support with dualstack in SageMaker Notebooks, Tiered Storage Checkpointing Support in SageMaker HyperPod and P5.4xlarge instance type for SageMaker Hosting.

### Documentation
* (**ecs**) This is a documentation only release that adds additional information for Amazon ECS Availability Zone rebalancing.
* (**pcs**) Documentation-only update to add AccountingStorageEnforce to SlurmCustomSetting.

## [1.5.32] - 09/04/2025

### Features
* (**cleanrooms**) Add support for configurable compute sizes for PySpark jobs.
* (**cloudformation**) ListHookResults API now supports retrieving invocation results for all CloudFormation Hooks (previously limited to create change set and Cloud Control operations) with new optional parameters for filtering by Hook status and ARN.
* (**ec2**) Add m8i, m8i-flex and i8ge instance types.
* (**opensearchserverless**) Add support for Federal Information Processing Standards (FIPS) and Federal Risk and Authorization Management Program (FedRAMP) compliance
* (**rds**) Added new EndpointNetworkType and TargetConnectionNetworkType fields in Proxy APIs to support IPv6
* (**verifiedpermissions**) Amazon Verified Permissions / Features : Adds support for datetime and duration attribute values.

## [1.5.31] - 09/03/2025

### Features
* (**cleanrooms**) Added support for adding new data provider members to an existing collaboration.
* (**cleanroomsml**) AWS Clean Rooms ML adds log sanitization for privacy-enhanced error summaries, supports new instance types for custom models providing better performance and lower costs, and deprecates P3-series instances.
* (**cloudfront**) Adding an optional field IpAddressType in CustomOriginConfig
* (**mq**) Add CONFIG_MANAGED as a supported AuthenticationStrategy for Amazon MQ for RabbitMQ brokers. Make username and password optional on broker creation for CONFIG_MANAGED brokers.
* (**rds**) This release adds support for MasterUserAuthenticationType parameter on CreateDBInstance, ModifyDBInstance, CreateDBCluster, and ModifyDBCluster operations.
* (**route53**) Amazon Route 53 now supports the Asia Pacific (New Zealand) Region (ap-southeast-6) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.
* (**route53domains**) Added new ExtraParams AU_ELIGIBILITY_TYPE, AU_POLICY_REASON, and AU_REGISTRANT_NAME
* (**transcribestreaming**) This release adds support for automatic language identification in AWS Transcribe Call Analytics

## [1.5.30] - 09/02/2025

### Features
* (**ec2**) MaximumEbsAttachments and AttachmentLimitType fields added to DescribeInstanceTypesResponse. G6f, Gr6f, R8i, R8i-flex and p5.4xlarge instance types added to InstanceTypes enum.
* (**neptune**) Removed the deprecated marker from publiclyAccessible parameter from DbInstance, CreateDbInstance and ModifyDbInstance and added relevant usage information for the parameter.
* (**notifications**) Added Org support for notifications:  - `ListMemberAccounts` gets member accounts list, `AssociateOrganizationalUnit` links OU to notification configuration, `DisassociateOrganizationalUnit` removes OU from notification configuration, `ListOrganizationalUnits` shows OUs configured for notifications.
* (**workmail**) Make RoleArn an optional parameter for the PutEmailMonitoringConfiguration API, and add UnsupportedOperationException to RegisterToWorkMail.

### Miscellaneous
* Remove SMS service

## [1.5.29] - 08/29/2025

### Features
* (**bedrockruntime**) Fixed stop sequence limit for converse API.
* (**ec2**) Release shows new route types such as filtered and advertisement.
* (**xray**) AWS X-Ray Features: Support Sampling Rate Boost On Anomaly

## [1.5.28] - 08/28/2025

### Features
* (**amplify**) Remove incorrect endpoint tests
* (**applicationautoscaling**) Remove incorrect endpoint tests
* (**applicationcostprofiler**) Remove incorrect endpoint tests
* (**autoscalingplans**) Remove incorrect endpoint tests
* (**bedrockruntime**) Remove incorrect endpoint tests
* (**cloud9**) Remove incorrect endpoint tests
* (**cloudhsmv2**) Remove incorrect endpoint tests
* (**cloudsearchdomain**) Remove incorrect endpoint tests
* (**codedeploy**) Remove incorrect endpoint tests
* (**codegurureviewer**) Remove incorrect endpoint tests
* (**connect**) AgentStatusDrillDown feature in GetCurrentMetricData API. Adding AGENT_STATUS as filter and grouping in GetCurrentMetricData API
* (**connectcampaigns**) Remove incorrect endpoint tests
* (**databrew**) Remove incorrect endpoint tests
* (**dynamodb**) Remove incorrect endpoint tests
* (**ec2**) This release adds support for copying Amazon EBS snapshot and AMIs to and from Local Zones.
* (**ec2instanceconnect**) Remove incorrect endpoint tests
* (**ecr**) Remove incorrect endpoint tests
* (**emr**) Remove incorrect endpoint tests
* (**entityresolution**) Remove incorrect endpoint tests
* (**eventbridge**) Remove incorrect endpoint tests
* (**fis**) Remove incorrect endpoint tests
* (**fms**) Remove incorrect endpoint tests
* (**geomaps**) Remove incorrect endpoint tests
* (**geoplaces**) Remove incorrect endpoint tests
* (**glue**) Adding support to fetch TargetDatabase field during GetDatabases with AttributesToGet
* (**healthlake**) Add ValidationLevel parameter to StartFHIRImportJob API, allowing users to specify a FHIR validation level for their asynchronous import jobs.
* (**inspectorscan**) Remove incorrect endpoint tests
* (**internetmonitor**) Remove incorrect endpoint tests
* (**kinesis**) Remove incorrect endpoint tests
* (**licensemanagerusersubscriptions**) Remove incorrect endpoint tests
* (**machinelearning**) Remove incorrect endpoint tests
* (**managedblockchainquery**) Remove incorrect endpoint tests
* (**marketplacedeployment**) Remove incorrect endpoint tests
* (**mediastoredata**) Remove incorrect endpoint tests
* (**mturk**) Remove incorrect endpoint tests
* (**networkfirewall**) Remove incorrect endpoint tests
* (**oam**) Remove incorrect endpoint tests
* (**omics**) Adds Amazon ECR pull through cache support to AWS HealthOmics, so you can more easily use container images from external sources.
* (**opensearch**) Remove incorrect endpoint tests
* (**osis**) Remove incorrect endpoint tests
* (**pcs**) Remove incorrect endpoint tests
* (**pinpointsmsvoice**) Remove incorrect endpoint tests
* (**pinpointsmsvoicev2**) Remove incorrect endpoint tests
* (**pipes**) Remove incorrect endpoint tests
* (**quicksight**) Remove incorrect endpoint tests
* (**rds**) Added RDS HTTP Endpoint feature support flag to DescribeOrderableDBInstanceOptions API
* (**rekognition**) Remove incorrect endpoint tests
* (**route53domains**) Remove incorrect endpoint tests
* (**savingsplans**) Remove incorrect endpoint tests
* (**scheduler**) Remove incorrect endpoint tests
* (**servicequotas**) Remove incorrect endpoint tests
* (**ses**) Remove incorrect endpoint tests
* (**sesv2**) Remove incorrect endpoint tests
* (**ssmguiconnect**) Remove incorrect endpoint tests
* (**ssmincidents**) Remove incorrect endpoint tests
* (**ssmsap**) Added support for Configuration Checks on SAP HANA Applications.
* (**sso**) Remove incorrect endpoint tests
* (**swf**) Remove incorrect endpoint tests
* (**timestreamquery**) Remove incorrect endpoint tests
* (**waf**) Remove incorrect endpoint tests
* (**workmailmessageflow**) Remove incorrect endpoint tests
* (**xray**) Remove incorrect endpoint tests

### Documentation
* (**apprunner**) Doc only updates for APIs and and datatypes related to IPAddressType and Subnets for IPv6 dualstack support.

## [1.5.27] - 08/27/2025

### Features
* (**acmpca**) Remove incorrect endpoint tests
* (**amplifybackend**) Remove incorrect endpoint tests
* (**amplifyuibuilder**) Remove incorrect endpoint tests
* (**appconfigdata**) Remove incorrect endpoint tests
* (**applicationdiscoveryservice**) Remove incorrect endpoint tests
* (**applicationinsights**) Remove incorrect endpoint tests
* (**apptest**) Remove incorrect endpoint tests
* (**bedrockagentcore**) Remove incorrect endpoint tests
* (**chime**) Remove incorrect endpoint tests
* (**chimesdkidentity**) Remove incorrect endpoint tests
* (**chimesdkmeetings**) Remove incorrect endpoint tests
* (**chimesdkvoice**) Remove incorrect endpoint tests
* (**cleanroomsml**) Remove incorrect endpoint tests
* (**codebuild**) Remove incorrect endpoint tests
* (**codeconnections**) Remove incorrect endpoint tests
* (**codeguruprofiler**) Remove incorrect endpoint tests
* (**computeoptimizer**) Remove incorrect endpoint tests
* (**datapipeline**) Remove incorrect endpoint tests
* (**directoryservice**) Add APIs for CA AutoEnrollment support: DescribeCAEnrollmentPolicy, EnableCAEnrollmentPolicy and DisableCAEnrollmentPolicy.
* (**eks**) Add support for on-demand refresh of EKS cluster insights
* (**elasticache**) Remove incorrect endpoint tests
* (**evidently**) Remove incorrect endpoint tests
* (**frauddetector**) Remove incorrect endpoint tests
* (**inspector**) Remove incorrect endpoint tests
* (**iot**) Remove incorrect endpoint tests
* (**iotfleetwise**) Remove incorrect endpoint tests
* (**iotwireless**) Remove incorrect endpoint tests
* (**ivschat**) Remove incorrect endpoint tests
* (**ivsrealtime**) Remove incorrect endpoint tests
* (**kafkaconnect**) Remove incorrect endpoint tests
* (**kinesisvideo**) Remove incorrect endpoint tests
* (**kinesisvideomedia**) Remove incorrect endpoint tests
* (**kms**) Remove incorrect endpoint tests
* (**lakeformation**) Remove incorrect endpoint tests
* (**lexmodelbuildingservice**) Remove incorrect endpoint tests
* (**marketplacereporting**) Remove incorrect endpoint tests
* (**medicalimaging**) Remove incorrect endpoint tests
* (**mgn**) Remove incorrect endpoint tests
* (**migrationhubconfig**) Remove incorrect endpoint tests
* (**neptune**) Remove incorrect endpoint tests
* (**neptunegraph**) Add StartGraph and StopGraph operations to Neptune Analytics
* (**omics**) Remove incorrect endpoint tests
* (**paymentcryptographydata**) Remove incorrect endpoint tests
* (**personalize**) Remove incorrect endpoint tests
* (**pi**) Remove incorrect endpoint tests
* (**qapps**) Remove incorrect endpoint tests
* (**qldbsession**) Remove incorrect endpoint tests
* (**redshift**) Remove incorrect endpoint tests
* (**robomaker**) Remove incorrect endpoint tests
* (**s3tables**) Remove incorrect endpoint tests
* (**sagemaker**) This release adds support for AutoScaling on SageMaker HyperPod.
* (**schemas**) Remove incorrect endpoint tests
* (**snowdevicemanagement**) Remove incorrect endpoint tests
* (**sns**) Remove incorrect endpoint tests
* (**supplychain**) Remove incorrect endpoint tests
* (**textract**) Remove incorrect endpoint tests
* (**timestreamwrite**) Remove incorrect endpoint tests
* (**voiceid**) Remove incorrect endpoint tests
* (**vpclattice**) Remove incorrect endpoint tests
* (**workdocs**) Remove incorrect endpoint tests
* (**workmail**) Remove incorrect endpoint tests

### Documentation
* (**batch**) Added ECS_AL2023_NVIDIA as an option for Ec2Configuration.imageType.

## [1.5.26] - 08/26/2025

### Features
* (**appconfig**) Remove incorrect endpoint tests
* (**appmesh**) Remove incorrect endpoint tests
* (**apprunner**) Remove incorrect endpoint tests
* (**arczonalshift**) This release adds new API options to enable allowed windows and multiple alarms for practice runs.
* (**bedrockagentcorecontrol**) Remove incorrect endpoint tests
* (**cleanrooms**) Remove incorrect endpoint tests
* (**cloudwatch**) Remove incorrect endpoint tests
* (**codecommit**) Remove incorrect endpoint tests
* (**codestarconnections**) Remove incorrect endpoint tests
* (**comprehendmedical**) Remove incorrect endpoint tests
* (**connectcontactlens**) Remove incorrect endpoint tests
* (**connectparticipant**) Amazon Connect Participant Service: Remove unused fields from WebRTCConnection
* (**controlcatalog**) Remove incorrect endpoint tests
* (**datasync**) Remove incorrect endpoint tests
* (**dlm**) Remove incorrect endpoint tests
* (**drs**) Remove incorrect endpoint tests
* (**ec2**) Add new APIs for viewing how your shared AMIs are used by other accounts, and identify resources in your account that are dependent on particular AMIs
* (**elastictranscoder**) Remove incorrect endpoint tests
* (**evs**) Remove incorrect endpoint tests
* (**finspace**) Remove incorrect endpoint tests
* (**gamelift**) Remove incorrect endpoint tests
* (**georoutes**) Added RouteFerryNotice PotentialViolatedVehicleRestrictionUsage value for CalculateRoutes. This value indicates when the Route is potentially forbidden for the given vehicle profile.
* (**glacier**) Remove incorrect endpoint tests
* (**globalaccelerator**) Remove incorrect endpoint tests
* (**greengrass**) Remove incorrect endpoint tests
* (**identitystore**) Remove incorrect endpoint tests
* (**imagebuilder**) Remove incorrect endpoint tests
* (**iotjobsdataplane**) Remove incorrect endpoint tests
* (**iotsecuretunneling**) Remove incorrect endpoint tests
* (**iottwinmaker**) Remove incorrect endpoint tests
* (**kafka**) Remove incorrect endpoint tests
* (**licensemanagerlinuxsubscriptions**) Remove incorrect endpoint tests
* (**lookoutvision**) Remove incorrect endpoint tests
* (**m2**) Remove incorrect endpoint tests
* (**migrationhub**) Remove incorrect endpoint tests
* (**migrationhubrefactorspaces**) Remove incorrect endpoint tests
* (**mq**) Remove incorrect endpoint tests
* (**neptunedata**) Remove incorrect endpoint tests
* (**pcaconnectorscep**) Remove incorrect endpoint tests
* (**personalizeevents**) Remove incorrect endpoint tests
* (**pinpoint**) Remove incorrect endpoint tests
* (**polly**) Remove incorrect endpoint tests
* (**ram**) Remove incorrect endpoint tests
* (**rbin**) Remove incorrect endpoint tests
* (**rdsdata**) Remove incorrect endpoint tests
* (**resourceexplorer2**) Remove incorrect endpoint tests
* (**route53**) Remove incorrect endpoint tests
* (**route53recoverycluster**) Remove incorrect endpoint tests
* (**sagemakergeospatial**) Remove incorrect endpoint tests
* (**secretsmanager**) Remove incorrect endpoint tests
* (**securityhub**) Remove incorrect endpoint tests
* (**servicecatalogappregistry**) Remove incorrect endpoint tests
* (**signer**) Remove incorrect endpoint tests
* (**socialmessaging**) Remove incorrect endpoint tests
* (**ssm**) Remove incorrect endpoint tests
* (**ssmsap**) Remove incorrect endpoint tests
* (**ssooidc**) Remove incorrect endpoint tests
* (**tnb**) Remove incorrect endpoint tests
* (**transfer**) Remove incorrect endpoint tests
* (**trustedadvisor**) Remove incorrect endpoint tests
* (**wafregional**) Remove incorrect endpoint tests
* (**workspaces**) Remove incorrect endpoint tests

### Fixes
* Stop erroneously using `ProfileCredentialsProvider.profileName` to initialize IMDS instance profile

### Miscellaneous
* Remove OpsWorks and OpsWorks-CM service clients from SDK because the [service has reached End Of Life](https://docs.aws.amazon.com/opsworks/latest/userguide/stacks-eol-faqs.html)

## [1.5.25] - 08/25/2025

### Features
* (**acm**) Remove incorrect endpoint tests
* (**aiops**) Remove incorrect endpoint tests
* (**appflow**) Remove incorrect endpoint tests
* (**appsync**) Remove incorrect endpoint tests
* (**athena**) Remove incorrect endpoint tests
* (**autoscaling**) Remove incorrect endpoint tests
* (**b2bi**) Updated APIs to support custom validation rules.
* (**bedrock**) Remove incorrect endpoint tests
* (**bedrockagent**) Remove incorrect endpoint tests
* (**bedrockdataautomation**) Remove incorrect endpoint tests
* (**bedrockdataautomationruntime**) Remove incorrect endpoint tests
* (**braket**) Remove incorrect endpoint tests
* (**chatbot**) Remove incorrect endpoint tests
* (**chimesdkmediapipelines**) Remove incorrect endpoint tests
* (**chimesdkmessaging**) Remove incorrect endpoint tests
* (**cloudcontrol**) Remove incorrect endpoint tests
* (**cloudformation**) Remove incorrect endpoint tests
* (**cloudsearch**) Remove incorrect endpoint tests
* (**cloudtraildata**) Remove incorrect endpoint tests
* (**cloudwatchevents**) Remove incorrect endpoint tests
* (**cloudwatchlogs**) Remove incorrect endpoint tests
* (**codeartifact**) Remove incorrect endpoint tests
* (**codestarnotifications**) Remove incorrect endpoint tests
* (**connectparticipant**) Remove incorrect endpoint tests
* (**controltower**) Remove incorrect endpoint tests
* (**costandusagereportservice**) Remove incorrect endpoint tests
* (**datazone**) Releasing the following features - Asset classification that lets users use restricted terms for classifying assets if they have the right permissions. Also adding a new enum value "Moving" to project status.
* (**dax**) Remove incorrect endpoint tests
* (**devicefarm**) Remove incorrect endpoint tests
* (**devopsguru**) Remove incorrect endpoint tests
* (**directconnect**) Remove incorrect endpoint tests
* (**docdbelastic**) Remove incorrect endpoint tests
* (**ec2**) Added IPv6 support for AWS Client VPN.
* (**eks**) Remove incorrect endpoint tests
* (**emrcontainers**) Remove incorrect endpoint tests
* (**finspacedata**) Remove incorrect endpoint tests
* (**forecast**) Remove incorrect endpoint tests
* (**fsx**) Remove incorrect endpoint tests
* (**groundstation**) Remove incorrect endpoint tests
* (**health**) Remove incorrect endpoint tests
* (**iotdataplane**) Remove incorrect endpoint tests
* (**iotfleethub**) Remove incorrect endpoint tests
* (**iotsitewise**) Remove incorrect endpoint tests
* (**kendra**) Remove incorrect endpoint tests
* (**keyspaces**) Remove incorrect endpoint tests
* (**kinesisanalytics**) Remove incorrect endpoint tests
* (**kinesisvideoarchivedmedia**) Remove incorrect endpoint tests
* (**lambda**) Remove incorrect endpoint tests
* (**lexmodelsv2**) Remove incorrect endpoint tests
* (**lexruntimeservice**) Remove incorrect endpoint tests
* (**licensemanager**) Remove incorrect endpoint tests
* (**lookoutequipment**) Remove incorrect endpoint tests
* (**mailmanager**) Remove incorrect endpoint tests
* (**marketplacecatalog**) Remove incorrect endpoint tests
* (**mediaconvert**) This release adds support for input rendition selection for HLS input, adds new Share API to enable sharing jobs with AWS Support for support investigations, and adds INCLUDE_AS_TS to iFrameOnlyManifest setting for HLS outputs.
* (**mediapackagevod**) Remove incorrect endpoint tests
* (**mediastore**) Remove incorrect endpoint tests
* (**migrationhubstrategy**) Remove incorrect endpoint tests
* (**odb**) Remove incorrect endpoint tests
* (**opensearchserverless**) Remove incorrect endpoint tests
* (**opsworkscm**) Remove incorrect endpoint tests
* (**outposts**) Remove incorrect endpoint tests
* (**pcaconnectorad**) Remove incorrect endpoint tests
* (**personalizeruntime**) Remove incorrect endpoint tests
* (**qbusiness**) The Amazon Q Business GetDocumentContent operation now supports retrieval of the extracted text content in JSON format.
* (**redshiftserverless**) Remove incorrect endpoint tests
* (**repostspace**) Remove incorrect endpoint tests
* (**resiliencehub**) Remove incorrect endpoint tests
* (**resourcegroupstaggingapi**) Remove incorrect endpoint tests
* (**sagemakeredge**) Remove incorrect endpoint tests
* (**sagemakerfeaturestoreruntime**) Remove incorrect endpoint tests
* (**sagemakerruntime**) Remove incorrect endpoint tests
* (**serverlessapplicationrepository**) Remove incorrect endpoint tests
* (**servicecatalog**) Remove incorrect endpoint tests
* (**sfn**) Remove incorrect endpoint tests
* (**shield**) Remove incorrect endpoint tests
* (**snowball**) Remove incorrect endpoint tests
* (**ssoadmin**) Remove incorrect endpoint tests
* (**storagegateway**) Remove incorrect endpoint tests
* (**support**) Remove incorrect endpoint tests
* (**supportapp**) Remove incorrect endpoint tests
* (**taxsettings**) Remove incorrect endpoint tests
* (**timestreaminfluxdb**) Add MAINTENANCE status for DbInstance and DbCluster
* (**transcribe**) Remove incorrect endpoint tests
* (**transcribestreaming**) Remove incorrect endpoint tests
* (**translate**) Remove incorrect endpoint tests
* (**wellarchitected**) Remove incorrect endpoint tests
* (**workspacesthinclient**) Remove incorrect endpoint tests

### Documentation
* (**codegurusecurity**) Documentation update to notify users of the discontinuation of Amazon CodeGuru Security.
* (**sqs**) Documentation update for Amazon SQS Supports Large Payload Message feature

## [1.5.24] - 08/22/2025

### Features
* (**account**) Remove incorrect endpoint tests
* (**apigateway**) Remove incorrect endpoint tests
* (**apigatewayv2**) Remove incorrect endpoint tests
* (**b2bi**) Remove incorrect endpoint tests
* (**clouddirectory**) Remove incorrect endpoint tests
* (**codepipeline**) Remove incorrect endpoint tests
* (**cognitosync**) Remove incorrect endpoint tests
* (**dataexchange**) Remove incorrect endpoint tests
* (**docdb**) Remove incorrect endpoint tests
* (**elasticloadbalancing**) Remove incorrect endpoint tests
* (**emrserverless**) Remove incorrect endpoint tests
* (**firehose**) Remove incorrect endpoint tests
* (**healthlake**) Remove incorrect endpoint tests
* (**iotanalytics**) Remove incorrect endpoint tests
* (**launchwizard**) Remove incorrect endpoint tests
* (**location**) Remove incorrect endpoint tests
* (**macie2**) Remove incorrect endpoint tests
* (**marketplacecommerceanalytics**) Remove incorrect endpoint tests
* (**medialive**) AWS Elemental MediaLive now has a field called "SubtitleRows" for controlling subtitle row count for DVB-Sub and Burn-In captions outputs
* (**memorydb**) Remove incorrect endpoint tests
* (**migrationhuborchestrator**) Remove incorrect endpoint tests
* (**networkmonitor**) Remove incorrect endpoint tests
* (**organizations**) Remove incorrect endpoint tests
* (**paymentcryptography**) Remove incorrect endpoint tests
* (**pricing**) Remove incorrect endpoint tests
* (**proton**) Remove incorrect endpoint tests
* (**qconnect**) Releasing model ID support for UpdateAIPrompt
* (**qldb**) Remove incorrect endpoint tests
* (**redshiftdata**) Remove incorrect endpoint tests
* (**rolesanywhere**) Remove incorrect endpoint tests
* (**route53recoveryreadiness**) Remove incorrect endpoint tests
* (**rum**) Remove incorrect endpoint tests
* (**sagemaker**) Launch SageMaker Notebook Instances support for AL2023 along with P6-B200 instance type and Rootless Docker support for SageMaker Studio.
* (**sagemakera2iruntime**) Remove incorrect endpoint tests
* (**sagemakermetrics**) Remove incorrect endpoint tests
* (**simspaceweaver**) Remove incorrect endpoint tests
* (**sqs**) Remove incorrect endpoint tests
* (**synthetics**) Added multi browser support for synthetics canaries, Increased ephemeral storage limit from 5GB to 10GB
* (**wafv2**) test and verified, safe to release

### Documentation
* (**rds**) Updates Amazon RDS documentation for Db2 read-only replicas.
* (**servicediscovery**) Updating SDK examples after launch of shared AWS Cloud Map namespaces

## [1.5.23] - 08/21/2025

### Features
* (**accessanalyzer**) Remove incorrect endpoint tests
* (**amp**) Remove incorrect endpoint tests
* (**apigatewaymanagementapi**) Remove incorrect endpoint tests
* (**appfabric**) Remove incorrect endpoint tests
* (**appintegrations**) Remove incorrect endpoint tests
* (**appstream**) Remove incorrect endpoint tests
* (**artifact**) Remove incorrect endpoint tests
* (**auditmanager**) Remove incorrect endpoint tests
* (**backup**) Remove incorrect endpoint tests
* (**backupgateway**) Remove incorrect endpoint tests
* (**bedrockagentruntime**) Remove incorrect endpoint tests
* (**billingconductor**) Remove incorrect endpoint tests
* (**cloudfront**) Remove incorrect endpoint tests
* (**cloudhsm**) Remove incorrect endpoint tests
* (**cloudtrail**) Remove incorrect endpoint tests
* (**cognitoidentity**) Remove incorrect endpoint tests
* (**comprehend**) Remove incorrect endpoint tests
* (**configservice**) Remove incorrect endpoint tests
* (**connect**) Remove incorrect endpoint tests
* (**connectcampaignsv2**) Remove incorrect endpoint tests
* (**connectcases**) Remove incorrect endpoint tests
* (**costoptimizationhub**) Remove incorrect endpoint tests
* (**customerprofiles**) Remove incorrect endpoint tests
* (**databasemigrationservice**) Remove incorrect endpoint tests
* (**deadline**) Remove incorrect endpoint tests
* (**directoryservice**) Remove incorrect endpoint tests
* (**directoryservicedata**) Remove incorrect endpoint tests
* (**dynamodbstreams**) Remove incorrect endpoint tests
* (**ebs**) Remove incorrect endpoint tests
* (**ecrpublic**) Remove incorrect endpoint tests
* (**elasticbeanstalk**) Remove incorrect endpoint tests
* (**elasticloadbalancingv2**) Remove incorrect endpoint tests
* (**elasticsearchservice**) Remove incorrect endpoint tests
* (**forecastquery**) Remove incorrect endpoint tests
* (**gameliftstreams**) The default application in a stream group can now be changed at any time using UpdateStreamGroup to update the DefaultApplicationIdentifier.
* (**glue**) Added support for preprocessing queries in Data Quality operations through new DataQualityGlueTable structure.
* (**grafana**) Remove incorrect endpoint tests
* (**greengrassv2**) Remove incorrect endpoint tests
* (**guardduty**) Remove Pattern trait from email field
* (**inspector2**) Remove incorrect endpoint tests
* (**iotdeviceadvisor**) Remove incorrect endpoint tests
* (**iotevents**) Remove incorrect endpoint tests
* (**ioteventsdata**) Remove incorrect endpoint tests
* (**iotthingsgraph**) Remove incorrect endpoint tests
* (**ivs**) Remove incorrect endpoint tests
* (**kinesisvideosignaling**) Remove incorrect endpoint tests
* (**kinesisvideowebrtcstorage**) Remove incorrect endpoint tests
* (**lexruntimev2**) Remove incorrect endpoint tests
* (**lightsail**) Remove incorrect endpoint tests
* (**lookoutmetrics**) Remove incorrect endpoint tests
* (**managedblockchain**) Remove incorrect endpoint tests
* (**marketplaceagreement**) Remove incorrect endpoint tests
* (**mediaconnect**) Remove incorrect endpoint tests
* (**mediaconvert**) Remove incorrect endpoint tests
* (**mediapackage**) Remove incorrect endpoint tests
* (**mediapackagev2**) Remove incorrect endpoint tests
* (**mediatailor**) Remove incorrect endpoint tests
* (**mwaa**) Remove incorrect endpoint tests
* (**observabilityadmin**) Remove incorrect endpoint tests
* (**panorama**) Remove incorrect endpoint tests
* (**pinpointemail**) Remove incorrect endpoint tests
* (**resourcegroups**) Remove incorrect endpoint tests
* (**route53profiles**) Remove incorrect endpoint tests
* (**route53resolver**) Remove incorrect endpoint tests
* (**s3outposts**) Remove incorrect endpoint tests
* (**securitylake**) Remove incorrect endpoint tests
* (**ssmquicksetup**) Remove incorrect endpoint tests
* (**sts**) Remove incorrect endpoint tests
* (**timestreaminfluxdb**) Remove incorrect endpoint tests
* (**verifiedpermissions**) Remove incorrect endpoint tests
* (**wisdom**) Remove incorrect endpoint tests
* (**workspacesweb**) Remove incorrect endpoint tests

### Documentation
* (**ecs**) This is a documentation only release that adds additional information for the update-service request parameters.
* (**ssmcontacts**) Doc-only updates for Incident Manager Contacts August 2025

## [1.5.22] - 08/20/2025

### Features
* (**bedrockruntime**) Launch CountTokens API to allow token counting
* (**billing**) Clarify IPv4 and IPv6 endpoints
* (**budgets**) Remove incorrect endpoint tests
* (**cognitoidentityprovider**) This release adds support for the new Terms APIs which allow displaying Terms of Use and Privacy Policy on the Managed Login user-registration page.
* (**costexplorer**) Remove incorrect endpoint tests
* (**datazone**) This release supports policy grant identifier for cloud formation integration
* (**detective**) Remove incorrect endpoint tests
* (**dynamodb**) Remove incorrect endpoint tests
* (**efs**) Remove incorrect endpoint tests
* (**eks**) EKS Add-ons Custom Namespace Support
* (**iam**) Remove incorrect endpoint tests
* (**kinesis**) Remove incorrect endpoint tests
* (**kinesisanalyticsv2**) Adds Key Management Service (KMS) support allowing customer-managed key (CMK) encryption for Flink application data.
* (**marketplaceentitlementservice**) Remove incorrect endpoint tests
* (**marketplacemetering**) Remove incorrect endpoint tests
* (**networkmanager**) Remove incorrect endpoint tests
* (**pinpointsmsvoicev2**) This change added InternationalSendingEnbaled as part of describe/Update/Request phone number API response, and as part of update/Request phone number API request
* (**route53recoverycontrolconfig**) Remove incorrect endpoint tests
* (**sagemaker**) This release adds 1/ Launch ml.p5.4xlarge instance in Processing jobs, Training jobs and Training Plan 2/ Makes S3Uri to be required for S3FileSystem and S3FileSystemConfig.
* (**sesv2**) Remove incorrect endpoint tests

## [1.5.21] - 08/19/2025

### Features
* (**cleanrooms**) Support error message configuration for analysis templates
* (**ec2**) Add support for "warning" volume status.
* (**polly**) Added support for new output format - Opus.

## [1.5.20] - 08/18/2025

### Features
* (**bcmdashboards**) Billing and Cost Management Dashboards enables users to create dashboards that combine multiple visualizations of cost and usage data. Users can create, manage, and share dashboards. Tags are also available for dashboards.
* (**connect**) Amazon Connect Service Feature: Add support to enable multi-user in-app, web, and video calling.
* (**connectparticipant**) Amazon Connect Service Feature: Add support to enable multi-user in-app, web, and video calling.
* (**s3control**) Adds support for Compute checksum functionality in Amazon S3 Batch Operations. You can now calculate checksums for a list of objects using supported algorithms in Amazon S3, without requiring a restore or download
* (**sagemaker**) Customer managed keys now available for volume encryption of SageMaker HyperPod clusters.

### Documentation
* (**batch**) Added default_x86_64 and default_arm64 as values to the instanceTypes field.

## [1.5.19] - 08/15/2025

### Features
* (**amp**) Add Resource-based Policy APIs for Amazon Prometheus
* (**bedrockagent**) This release adds support for saving Bedrock Flows while node configuration is still in progress, even if the Flow is not yet able to be invoked
* (**glue**) AWS Glue Zero ETL now supports On-demand snapshot load

## [1.5.18] - 08/14/2025

### Features
* (**arcregionswitch**) Endpoint rule test and documentation update.
* (**bcmrecommendedactions**) Initial SDK release for AWS Billing and Cost Management Recommended Actions.
* (**directconnect**) Added pagination support for DescribeHostedConnections, DescribeVirtualInterfaces, DescribeConnections, DescribeInterconnects, DescribeLags. Added asnLong support for BGP peer operations which supports a large range.
* (**dynamodb**) This release 1/ Adds support for throttled keys mode for CloudWatch Contributor Insights, 2/ Adds throttling reasons to exceptions across dataplane APIs. 3/ Explicitly models ThrottlingException as a class in statically typed languages. Refer to the launch day blog post for more details.
* (**ec2**) This release adds ModifyInstanceConnectEndpoint API to update configurations on existing EC2 Instance Connect Endpoints and improves IPv6 support through dualstack DNS names for EC2 Instance Connect Endpoints.
* (**fsx**) Amazon FSx for NetApp ONTAP 2nd generation file systems now support decreasing SSD storage capacity.
* (**glue**) AWS Glue now supports Trusted Identity Propagation.
* (**guardduty**) Added support for entity lists.
* (**medialive**) CMAF Ingest output groups in MediaLive can now accept one additional destination url for single pipeline channels and up to two additional destination urls for standard channels.
* (**pcs**) Updated the regex pattern and description of iamInstanceProfileArn in the CreateComputeNodeGroup and UpdateComputeNodeGroup API actions. Name and path requirements apply to the ARN of the IAM role associated with the instance profile and not the ARN of the instance profile.
* (**servicediscovery**) Added support for cross account through Id parameter overloading with ARN and allow owner account for some APIs instead of ARN
* (**workspaces**) New APIs introduced to import WorkSpaces BYOL image using a new process that leveraged EC2 Image Builder. WorkSpaces tests and fixes your image's compatibility issues and supports customized VM images.

### Documentation
* (**qapps**) Documentation update for Amazon Q Apps API Reference

## [1.5.17] - 08/13/2025

### Features
* (**braket**) Add support for Braket program sets.
* (**datazone**) Adds support for account pools and project profile account decoupling
* (**fsx**) Add Dual-Stack support for Amazon FSx for OpenZFS file systems
* (**partnercentralselling**) Add Tagging Support for Opportunity resources
* (**sagemaker**) This release introduces compute quota for GPU, Trainium accelerators, vCPU, and vCPU memory utilization across teams in HyperPod clusters
* (**securityir**) Added support for Organizational Unit-level Membership configuration and the ability to resume a cancelled membership.

## [1.5.16] - 08/12/2025

### Features
* (**backupsearch**) Using recommended smithy trait to generate regional endpoints for Backup Search
* (**codebuild**) AWS CodeBuild now supports PullRequestBuildPolicy in webhook object.
* (**ec2**) Release to allow route table association with a PublicIpv4Pool.
* (**organizations**) This release introduces 2 new APIs in Organizations: 1. ListAccountsWithInvalidEffectivePolicy 2. ListEffectivePolicyValidationErrors
* (**sagemaker**) IAM Identity Center trusted identity propagation is now supported in SageMaker Studio.
* (**transcribe**) AWS HealthScribe now supports specifying preferred patient pronouns through the MedicalScribeContext parameter for use in the generated clinical notes.
* (**transcribestreaming**) AWS HealthScribe now supports specifying preferred patient pronouns through the MedicalScribeContext parameter for use in the generated clinical notes.

## [1.5.15] - 08/11/2025

### Features
* (**bedrock**) This release includes model updates and enhanced SDK documentation for union fields in automated reasoning policy components. Added docs cover policy definitions, mutations (add/update for rules/types/variables), build assets, workflow sources, test results, and tag exception handling.
* (**cognitoidentityprovider**) Remove SigV4 auth requirement for GetTokensFromRefreshToken
* (**connect**) Updating SearchUserHierarchyGroups API
* (**deadline**) Adds support for Wait and Save feature in service-managed fleets
* (**ec2**) This release adds AvailabilityZoneId support for CreateVolume, DescribeVolume, LaunchTemplates, RunInstances, DescribeInstances, CreateDefaultSubnet, SpotInstances, and CreateDefaultSubnet APIs.
* (**quicksight**) Add RowAxisDisplayOptions and ColumnAxisDisplayOptions to HeatMapConfiguration, add Actions to PluginVisual, increase limit for CalculatedFields list
* (**ssoadmin**) Added support for managing user background session for applications

### Documentation
* (**evs**) Update for general availability of Amazon Elastic VMware Service (EVS).
* (**lambda**) Doc-only update for Lambda that updates the maximum payload size for response streaming invocations to 200 MB.

## [1.5.14] - 08/08/2025

### Features
* (**applicationsignals**) Removed incorrect endpoint tests
* (**backupsearch**) Removed incorrect endpoint tests
* (**bcmpricingcalculator**) Removed incorrect endpoint tests
* (**billing**) Removed incorrect endpoint tests
* (**connect**) This release adds a new API GetContactMetrics for Amazon Connect.
* (**dsql**) Removed incorrect endpoint tests
* (**gameliftstreams**) Removed incorrect endpoint tests
* (**inspector2**) Add CVSSV4 to Vulnerability Search API and update enable/disable account id list length to 5
* (**invoicing**) Removed incorrect endpoint tests
* (**iotdataplane**) Adding DeleteConnection API to IoT Data Plane
* (**iotmanagedintegrations**) Removed incorrect endpoint tests
* (**keyspacesstreams**) Removed incorrect endpoint tests
* (**mpa**) Removed incorrect endpoint tests
* (**networkflowmonitor**) Removed incorrect endpoint tests
* (**notifications**) Removed incorrect endpoint tests
* (**notificationscontacts**) Removed incorrect endpoint tests
* (**partnercentralselling**) Removed incorrect endpoint tests
* (**s3vectors**) Removed incorrect endpoint tests
* (**sagemaker**) Adds support for GB200 UltraServers in Amazon SageMaker training jobs, training plans, and HyperPod clusters
* (**securityir**) Removed incorrect endpoint tests
* (**workspacesinstances**) Removed incorrect endpoint tests

### Documentation
* (**transcribe**) Update documentation to use key ARN only in OutputEncryptionKMSKeyId request parameter

## [1.5.13] - 08/07/2025

### Features
* (**cloudfront**) Added new viewer security policy, TLSv1.3_2025, for CloudFront.
* (**codebuild**) AWS CodeBuild now supports comment-based pull request control.
* (**glue**) AWS Glue Data Catalog now supports Iceberg Optimization settings at the Catalog level, and supports new options to control the optimization job run rate.
* (**guardduty**) Added support for VPC owner account ID associated with DNS request in the GuardDuty finding.

### Documentation
* (**batch**) This feature allows customers to use AWS Batch with Linux with ARM64 CPU Architecture with Fargate Spot compute support.
* (**gameliftstreams**) Adds Proton 9.0-2 to the list of runtime environment options available when creating an Amazon GameLift Streams application

## [1.5.12] - 08/06/2025

### Features
* (**budgets**) Adds support for billing views. Billing views let you control access to cost and usage data through an AWS resource, streamlining the process of sharing cost and usage data across account boundaries. With this release, you can now create and view budgets based on billing views.
* (**ec2**) Mark Elastic Inference Accelerators and Elastic Graphics Processor parameters as deprecated on the RunInstances and LaunchTemplate APIs.
* (**opensearchserverless**) Features: add Index APIs in OpenSearchServerless to support managed semantic enrichment
* (**qbusiness**) Amazon Q Business now supports the GetDocumentContent() API that enables customers to securely access the source documents through clickable citation links at query time

### Documentation
* (**appstream**) Added support for G6 instances

## [1.5.11] - 08/05/2025

### Features
* (**bedrock**) This release introduces Automated Reasoning checks for Amazon Bedrock Guardrails. The feature adds new APIs for policy building, refinement, version management, and testing. Guardrail APIs now support Automated Reasoning policy configuration and validation output.
* (**bedrockruntime**) This release adds support for Automated Reasoning checks output models for the Amazon Bedrock Guardrails ApplyGuardrail API.
* (**eks**) Add support for deletion protection on EKS clusters
* (**rds**) Adds a new Aurora Serverless v2 attribute to the DBCluster resource to expose the platform version. Also updates the attribute to be part of both the engine version and platform version descriptions.
* (**sagemaker**) Add support for SageMaker Hyperpod continuous scaling and custom AMI; Introduce new APIs: ListClusterEvents, DescribeClusterEvent, BatchAddClusterNodes

## [1.5.10] - 08/04/2025

### Features
* (**bedrockagentcore**) Remove superfluous field from API
* (**codeconnections**) New integration with Azure DevOps provider type.
* (**evs**) TagResource API now throws ServiceQuotaExceededException when the number of tags on the Amazon EVS resource exceeds the maximum allowed. TooManyTagsException is deprecated.
* (**iotsitewise**) Support Interface for IoT SiteWise Asset Modeling
* (**sagemaker**) This release adds the ability for customers to attach and detach their EBS volumes to EKS-orchestrated HyperPod cluster nodes.

## [1.5.9] - 08/01/2025

### Features
* (**aiops**) This release includes fix for InvestigationGroup timestamp conversion issue.
* (**arcregionswitch**) This is the initial SDK release for Region switch
* (**auditmanager**) Added a note to Framework APIs (CreateAssessmentFramework, GetAssessmentFramework, UpdateAssessmentFramework) clarifying that the Controls object returns a partial response when called through Framework APIs. Added documentation that the Framework's controlSources parameter is no longer supported.
* (**lightsail**) This release adds support for the Asia Pacific (Jakarta) (ap-southeast-3) Region.
* (**observabilityadmin**) CloudWatch Observability Admin adds the ability to enable telemetry on AWS resources such as Amazon VPCs (Flow Logs) in customers AWS Accounts and Organizations. The release introduces new APIs to manage telemetry rules, which define telemetry settings to be applied on AWS resources.
* (**pcs**) Add support for IPv6 Networking for Clusters.
* (**securityhub**) Release new resource detail type CodeRepository

### Documentation
* (**acmpca**) Doc-only update to add more information to GetCertificate action.
* (**sns**) Amazon SNS support for Amazon SQS fair queues

## [1.5.8] - 07/31/2025

### Features
* (**customerprofiles**) The release updates standard profile with 2 new fields that supports account-level engagement. Updated APIs include CreateProfile, UpdateProfile, MergeProfiles, SearchProfiles, BatchGetProfile, GetSegmentMembership, CreateSegmentDefinition, CreateSegmentEstimate.
* (**ec2**) Added support for the force option for the EC2 instance terminate command. This feature enables customers to recover resources associated with an instance stuck in the shutting-down state as a result of rare issues caused by a frozen operating system or an underlying hardware problem.
* (**entityresolution**) Add support for creating advanced rule-based matching workflows in AWS Entity Resolution.
* (**glue**) Added support for Route node, S3 Iceberg sources/targets, catalog Iceberg sources, DynamoDB ELT connector, AutoDataQuality evaluation, enhanced PII detection with redaction, Kinesis fan-out support, and new R-series worker types.
* (**inspector2**) Extend usage to include agentless hours and add CODE_REPOSITORY to aggregation resource type
* (**iot**) This release allows AWS IoT Core users to use their own AWS KMS keys for data protection
* (**opensearch**) Granular access control support for NEO-SAML with IAMFederation for AOS data source
* (**quicksight**) Added Impala connector support
* (**s3control**) Add Tags field to CreateAccessPoint
* (**sesv2**) This release introduces support for Multi-tenant management
* (**workspacesweb**) Added ability to log session activity on a portal to an S3 bucket.

### Documentation
* (**elasticloadbalancingv2**) This release enables secondary IP addresses for Network Load Balancers.

## [1.5.7] - 07/30/2025

### Features
* (**cloudfront**) This release adds new origin timeout options: 1) ResponseCompletionTimeout and 2) OriginReadTimeout (for S3 origins)
* (**directoryservice**) This release adds support for AWS Managed Microsoft AD Hybrid Edition, introducing new operations: StartADAssessment, DescribeADAssessment, ListADAssessments, DeleteADAssessment, CreateHybridAD, UpdateHybridAD, and DescribeHybridADUpdate; and updated existing operation: DescribeDirectories.
* (**docdb**) Add support for setting Serverless Scaling Configuration on clusters.
* (**ec2**) Release to show the next hop IP address for routes propagated by VPC Route Server into VPC route tables.
* (**iotwireless**) Added TxPowerIndexMin, TxPowerIndexMax, NbTransMin and NbTransMax params to ServiceProfile.

## [1.5.6] - 07/29/2025

### Features
* (**batch**) AWS Batch for SageMaker Training jobs feature support.  Includes new APIs for service job submission (e.g., SubmitServiceJob) and managing service environments (e.g., CreateServiceEnvironment) that enable queueing SageMaker Training jobs.
* (**cleanrooms**) This feature provides the ability to update the table reference and allowed columns on an existing configured table.
* (**deadline**) Adds support for tag management on monitors.
* (**location**) This release 1) adds support for multi-polygon geofences with disconnected territories, and 2) enables polygon exclusion zones within geofences for more accurate representation of real-world boundaries.
* (**opensearchserverless**) This is to support Granular access control support for SAML with IAMFedraton in AOSS

## [1.5.5] - 07/28/2025

### Features
* (**directconnect**) Enable MACSec support and features on Interconnects.
* (**iotsitewise**) Add support for native anomaly detection in IoT SiteWise using new Computation Model APIs
* (**osis**) Add Pipeline Role Arn as an optional parameter to the create / update pipeline APIs as an alternative to passing in the pipeline configuration body

## [1.5.4] - 07/25/2025

### Features
* (**appintegrations**) Amazon AppIntegrations introduces new configuration capabilities to enable customers to manage iframe permissions, control application refresh behavior (per contact or per browser/cross-contact), and run background applications (service).
* (**budgets**) Adds IPv6 and PrivateLink support for AWS Budgets in IAD.
* (**ec2**) Transit Gateway native integration with AWS Network Firewall. Adding new enum value for the new Transit Gateway Attachment type.
* (**mediapackagev2**) This release adds support for specifying a preferred input for channels using CMAF ingest.
* (**socialmessaging**) This release introduces new WhatsApp template management APIs that enable customers to programmatically create and submit templates for approval, monitor approval status, and manage the complete template lifecycle

### Documentation
* (**configservice**) Documentation improvements have been made to the EvaluationModel and DescribeConfigurationRecorders APIs.
* (**kms**) Doc only update: fixed grammatical errors.
* (**sqs**) Documentation updates for Amazon SQS fair queues feature.

## [1.5.3] - 07/24/2025

### Features
* (**datazone**) This release adds support for 1) highlighting relevant text in returned results for Search and SearchListings APIs and 2) returning aggregated counts of values for specified attributes for SearchListings API.
* (**omics**) Add Git integration and README support for HealthOmics workflows

## [1.5.2] - 07/23/2025

### Features
* (**ec2**) Added support for skip-os-shutdown option for the EC2 instance stop and terminate operations. This feature enables customers to bypass the graceful OS shutdown, supporting faster state transitions when instance data preservation isn't critical.
* (**glue**) AWS Glue now supports dynamic session policies for job executions. This feature allows you to specify custom, fine-grained permissions for each job run without creating multiple IAM roles.
* (**workspacesthinclient**) Added the lastUserId parameter to the ListDevices and GetDevice API.

## [1.5.1] - 07/22/2025

### Features
* (**ecr**) Add support for Image Tag Mutability Exception feature, allowing repositories to define wildcard-based patterns that override the default image tag mutability settings.
* (**emr**) This release adds new parameter 'ExtendedSupport' in AWS EMR RunJobFlow, ModifyCluster and DescribeCluster API.
* (**lambda**) This release migrated the model to Smithy keeping all features unchanged.
* (**neptunedata**) This release updates the supported regions for Neptune API to include current AWS regions.

## [1.5.0] - 07/21/2025

### Features
* (**cloudfront**) Add dualstack endpoint support
* (**deadline**) Add support for VPC resource endpoints in Service Managed Fleets
* (**ec2**) This release adds support for C8gn, F2 and P6e-GB200 Instance types
* (**sagemaker**) This release adds 1/ Support for S3FileSystem in CustomFileSystems 2/ The ability for a customer to select their preferred IpAddressType for use with private Workforces 3/ Support for p4de instance type in SageMaker Training Plans
* (**timestreaminfluxdb**) Timestream for InfluxDB adds support for db.influx.24xlarge instance type. This enhancement enables higher compute capacity for demanding workloads through CreateDbInstance, CreateDbCluster, UpdateDbInstance, and UpdateDbCluster APIs.
* Upgrade to Kotlin 2.2.0
* [#1413](https://github.com/awslabs/aws-sdk-kotlin/issues/1413) ⚠️ **IMPORTANT**: Refactor endpoint discoverer classes into interfaces so custom implementations may be provided
* [#1478](https://github.com/awslabs/aws-sdk-kotlin/issues/1478) Add `regionProvider` property to client config
* [#1413](https://github.com/awslabs/aws-sdk-kotlin/issues/1413) ⚠️ **IMPORTANT**: Add support for enabling/disabling endpoint discovery via [standard cross-SDK config mechanisms](https://docs.aws.amazon.com/sdkref/latest/guide/feature-endpoint-discovery.html)

### Fixes
* [#1413](https://github.com/awslabs/aws-sdk-kotlin/issues/1413) Favor `endpointUrl` instead of endpoint discovery if both are provided

## [1.4.125] - 07/18/2025

### Features
* (**auditmanager**) Updated error handling for RegisterOrganizationAdminAccount API to properly translate TooManyExceptions to HTTP 429 status code. This enhancement improves error handling consistency and provides clearer feedback when request limits are exceeded.
* (**cloudwatchlogs**) CloudWatchLogs launches GetLogObject API with streaming support for efficient log data retrieval. Logs added support for new AccountPolicy type METRIC_EXTRACTION_POLICY. For more information, see CloudWatch Logs API documentation
* (**mediaconvert**) This release adds support for TAMS server integration with MediaConvert inputs.
* (**outposts**) Add AWS Outposts API to surface customer billing information
* (**sesv2**) Added IP Visibility support for managed dedicated pools. Enhanced GetDedicatedIp and GetDedicatedIps APIs to return managed IP addresses.

### Documentation
* (**ssm**) July 2025 doc-only updates for Systems Manager.

## [1.4.124] - 07/17/2025

### Features
* (**cleanroomsml**) This release introduces Parquet result format support for ML Input Channel models in AWS Clean Rooms ML.
* (**ec2**) AWS Free Tier Version2 Support
* (**mailmanager**) Allow underscores in the local part of the input of the "Email recipients rewrite" action in rule sets.
* (**mediaconvert**) This release expands the range of supported audio outputs to include xHE, 192khz FLAC and the deprecation of dual mono for AC3.
* (**synthetics**) This feature allows AWS Synthetics customers to provide code dependencies using lambda layer while creating a canary

### Documentation
* (**cloudfront**) Doc only update for CloudFront that fixes some customer-reported issues
* (**keyspacesstreams**) Doc only update for the Amazon Keyspaces Streams API.
* (**sfn**) Align input with style guidelines.

## [1.4.123] - 07/16/2025

### Features
* (**bedrock**) This release adds support for on-demand custom model inference through CustomModelDeployment APIs for Amazon Bedrock.
* (**bedrockagentcore**) Initial release of Amazon Bedrock AgentCore SDK including Runtime, Built-In Tools, Memory, Gateway and Identity.
* (**bedrockagentcorecontrol**) Initial release of Amazon Bedrock AgentCore SDK including Runtime, Built-In Tools, Memory, Gateway and Identity.
* (**bedrockruntime**) document update to support on demand custom model.
* (**cloudwatchlogs**) CloudWatch Logs updates: Added X-Ray tracing for Amazon Bedrock Agent resources. Logs introduced Log Group level resource policies (managed through Put/Delete/Describe Resource Policy APIs). For more information, see CloudWatch Logs API documentation.
* (**datasync**) AWS DataSync now supports IPv6 address inputs and outputs in create, update, and describe operations for NFS, SMB, and Object Storage locations
* (**glue**) AWS Glue now supports schema, partition and sort management of Apache Iceberg tables using Glue SDK
* (**guardduty**) Add expectedBucketOwner parameter to ThreatIntel and IPSet APIs.
* (**iotwireless**) FuotaTaskId is not a valid IdentifierType for EventConfiguration and is being removed from possible IdentifierType values.
* (**mediapackagev2**) This release adds support for CDN Authentication using Static Headers in MediaPackage v2.
* (**networkflowmonitor**) Introducing 2 new scope status types - DEACTIVATING and DEACTIVATED.
* (**paymentcryptographydata**) Expand length of message data field for Mac generation and validation to 8192 characters.
* Add support for Bearer authentication using a token set in an environment variable for Bedrock services

### Documentation
* (**sfn**) Doc-only update to introduction, and edits to clarify input parameter and the set of control characters.

## [1.4.122] - 07/16/2025

### Features
* (**ecs**) This release removes hookDetails for the Amazon ECS native blue/green deployments.

## [1.4.121] - 07/15/2025

### Features
* (**bedrockagent**) Add support for S3 Vectors as a vector store.
* (**datazone**) Removing restriction of environment profile identifier as required field, S3 feature release
* (**dynamodbstreams**) Added support for optional shard filter parameter in DescribeStream api that allows customers to fetch child shards of a read_only parent shard.
* (**ec2**) This release adds support for volume initialization status, which enables you to monitor when the initialization process for an EBS volume is completed. This release also adds IPv6 support to EC2 Instance Connect Endpoints, allowing you to connect to your EC2 Instance via a private IPv6 address.
* (**ecs**) Amazon ECS supports native blue/green deployments, allowing you to validate new service revisions before directing production traffic to them.
* (**eventbridge**) Add customer-facing logging for the EventBridge Event Bus, enabling customers to better observe their events and extract insights about their EventBridge usage.
* (**opensearch**) AWS Opensearch adds support for enabling s3 vector engine options. After enabling this option, customers will be able to create indices with s3 vector engine.
* (**pricing**) This release adds support for new filter types in GetProducts API, including EQUALS, CONTAINS, ANY_OF, and NONE_OF.
* (**quicksight**) Introduced custom instructions for topics.
* (**repostspace**) This release introduces Channels functionality with CreateChannel, GetChannel, ListChannels, and UpdateChannel operations. Channels provide dedicated collaboration spaces where teams can organize discussions and knowledge by projects, business units, or areas of responsibility.
* (**s3**) Amazon S3 Metadata live inventory tables provide a queryable inventory of all the objects in your general purpose bucket so that you can determine the latest state of your data. To help minimize your storage costs, use journal table record expiration to set a retention period for your records.
* (**s3tables**) Adds table bucket type to ListTableBucket and GetTableBucket API operations
* (**s3vectors**) Amazon S3 Vectors provides cost-effective, elastic, and durable vector storage for queries based on semantic meaning and similarity.
* (**sagemaker**) This release adds support for a new Restricted instance group type to enable a specialized environment for running Nova customization jobs on SageMaker HyperPod clusters. This release also adds support for SageMaker pipeline versioning.

## [1.4.120] - 07/09/2025

### Features
* (**ec2**) Adds support to Capacity Blocks for ML for purchasing EC2 P6e-GB200 UltraServers. Customers can now purchase u-p6e-gb200x72 and u-p6e-gb200x36 UltraServers. Adds new DescribeCapacityBlocks andDescribeCapacityBlockStatus APIs. Adds support for CapacityBlockId to DescribeInstanceTopology.
* (**freetier**) This release introduces four new APIs: GetAccountPlanState and UpgradeAccountPlan for AWS account plan management; ListAccountActivities and GetAccountActivity that provide activity tracking capabilities.

### Documentation
* (**opsworkscm**) Removing content that refers to an S3 bucket that is no longer in use.

## [1.4.119] - 07/03/2025

### Features
* (**customerprofiles**) This release introduces capability of create Segments via importing a CSV file directly. This consumes the CSV file and creates/updates corresponding profiles for the particular domain.
* (**deadline**) Added fields for output manifest reporting and task chunking parameters
* (**ec2**) This release adds GroupOwnerId as a response member to the DescribeSecurityGroupVpcAssociations API and also adds waiters for SecurityGroupVpcAssociations (SecurityGroupVpcAssociationAssociated and SecurityGroupVpcAssociationDisassociated).
* (**mediapackagev2**) This release adds support for Microsoft Smooth Streaming (MSS) and allows users to exclude DRM segment Metadata in MediaPackage v2 Origin Endpoints
* (**route53**) Amazon Route 53 now supports the iso-e regions for private DNS Amazon VPCs and cloudwatch healthchecks.
* (**sagemaker**) Changes include new StartSession API for SageMaker Studio spaces and CreateHubContentPresignedUrls API for SageMaker JumpStart.

## [1.4.118] - 07/02/2025

### Features
* (**connectcases**) This release adds DeleteCase and DeleteRelatedItem APIs, which enable deleting cases and comments, undoing contact association, and removing service level agreements (SLAs) from cases. Contact center admins can use these APIs to delete cases when requested by customers and correct agent errors.
* (**ec2**) AWS Site-to-Site VPN now supports IPv6 addresses on outer tunnel IPs, making it easier for customers to build or transition to IPv6-only networks.
* (**s3**) Added support for directory bucket creation with tags and bucket ARN retrieval in CreateBucket, ListDirectoryBuckets, and HeadBucket operations
* (**s3control**) Added TagResource, UntagResource, and ListTagsForResource support for directory bucket

## [1.4.117] - 07/01/2025

### Features
* (**cleanroomsml**) This release introduces support for incremental training and distributed training for custom models in AWS Clean Rooms ML.
* (**datazone**) Add support for the new optional domain-unit-id parameter in the UpdateProject API.
* (**ec2**) Add Context to GetInstanceTypesFromInstanceRequirements API
* (**odb**) This release adds API operations for Oracle Database@AWS. You can use the APIs to create Exadata infrastructure, ODB networks, and Exadata and Autonomous VM clusters inside AWS data centers. The infrastructure is managed by OCI. You can integrate these resources with AWS services.
* (**qbusiness**) New ChatResponseConfiguration to Customize Q Business chat responses for specific use cases and communication needs. Updated Boosting capability allowing admins to provide preference on date attributes for recency and/or provide a preferred data source.
* (**sagemaker**) Updated field validation requirements for InstanceGroups.

### Documentation
* (**rds**) Amazon RDS Custom for Oracle now supports multi-AZ database instances.

## [1.4.116] - 06/30/2025

### Features
* (**arczonalshift**) Added support for on-demand practice runs and balanced capacity checks in ARC autoshift practice.
* (**b2bi**) Updated APIs to support inbound EDI split capabilities and additional Partnership-level configurations of generated EDI files' contents and format.
* (**bedrock**) Add support for API Keys, Re-Ranker, implicit filter for RAG / KB evaluation for Bedrock APIs.
* (**bedrockruntime**) Add API Key and document citations support for Bedrock Runtime APIs
* (**cloudformation**) Added support for UNKNOWN drift status.
* (**cloudwatchlogs**) Increase minimum length of queryId parameter to 1 character.
* (**configservice**) Updated ResourceType enum with new resource types onboarded by AWS Config as of June 2025
* (**connect**) This release introduces ChatMetrics to the model, providing comprehensive analytics insights for Amazon Connect chat conversations. Users can access these detailed metrics through the AWS Connect API by using the DescribeContact operation with their specific instance and contact IDs
* (**dataexchange**) This release updates resource Id with alphanumeric constraint, including Asset id, Revision id, Data Set id, Job id, and Event Action id.
* (**dynamodb**) This change adds support for witnesses in global tables. It also adds a new table status, REPLICATION_NOT_AUTHORIZED. This status will indicate scenarios where global replicas table can't be utilized for data plane operations.
* (**eventbridge**) Added support for EventBridge Dualstack endpoints in AWS GovCloud (US) regions (us-gov-east-1 and us-gov-west-1). The dualstack endpoints are identical for both FIPS and non-FIPS configurations, following the format: events.{region}.api.aws
* (**glue**) releasing source processing properties to support source properties for ODB integrations
* (**iam**) Updated IAM ServiceSpecificCredential support to include expiration, API Key output format instead of username and password for services that will support API keys, and the ability to list credentials for all users in the account for a given service configuration.
* (**medicalimaging**) Added new fields to support the concept of primary image sets within the storage hierarchy.
* (**networkflowmonitor**) Add ConflictExceptions to UpdateScope and DeleteScope operations for scopes being mutated.
* (**outposts**) Make ContactName and ContactPhoneNumber required fields when creating and updating Outpost Site Addresses.
* (**pcs**) Fixed the validation pattern for an instance profile Amazon Resource Name (ARN) in AWS PCS.
* (**quicksight**) Introduced custom permission capabilities for reporting content. Added menu option in exploration to preserve configuration data when textbox menu option is used. Added support for Athena trusted identity propagation.
* (**ssm**) Introduces AccessType, a new filter value for the DescribeSessions API.
* (**transfer**) Added support for dual-stack (IPv4 and IPv6) endpoints for SFTP public endpoints and VPC-internal endpoints (SFTP, FTPS, FTP, and AS2), enabling customers to configure new servers with IPv4 or dual-stack mode, convert existing servers to dual-stack, and use IPv6 with service APIs.

## [1.4.115] - 06/27/2025

### Features
* (**connect**) This release adds the following value to an InitiateAs enum: COMPLETED
* (**glue**) AWS Glue now supports schema, partition and sort management of Apache Iceberg tables using Glue SDK
* (**guardduty**) Update JSON target for Kubernetes workload resource type.
* (**qconnect**) Adding UnauthorizedException to public SDK
* (**rds**) StartDBCluster and StopDBCluster can now throw InvalidDBShardGroupStateFault.
* (**sesv2**) Added support for new SES regions

### Documentation
* (**configservice**) Added important considerations to the PutConformancePack and PutOrganizationConformancPack APIs.

## [1.4.114] - 06/26/2025

### Features
* (**deadline**) Added fields to track cumulative task retry attempts for steps and jobs
* (**ec2**) This release adds support for OdbNetworkArn as a target in VPC Route Tables
* (**iotmanagedintegrations**) Adding managed integrations APIs for IoT Device Management to onboard and control devices across different manufacturers, connectivity protocols and third party vendor clouds. APIs include managed thing operations, provisioning profile management, and cloud connector operations.
* (**keyspaces**) This release provides change data capture (CDC) streams support through updates to the Amazon Keyspaces API.
* (**keyspacesstreams**) This release adds change data capture (CDC) streams support through the new Amazon Keyspaces Streams API.
* (**qbusiness**) Added support for App level authentication for QBusiness DataAccessor using AWS IAM Identity center Trusted Token issuer
* (**workspaces**) Updated modifyStreamingProperties to support PrivateLink VPC endpoints for directories

### Fixes
* [#1626](https://github.com/awslabs/aws-sdk-kotlin/issues/1626) improve retry policies for ECS and IMDS credentials to handle common network errors
* [#1295](https://github.com/smithy-lang/smithy-kotlin/issues/1295) Enable building this project on JDK21 by setting -Xjdk-release flag

### Documentation
* (**kms**) This release updates AWS CLI examples for KMS APIs.

## [1.4.113] - 06/25/2025

### Features
* (**fsx**) Add support for the ability to create Amazon S3 Access Points for Amazon FSx for OpenZFS file systems.
* (**s3**) Adds support for additional server-side encryption mode and storage class values for accessing Amazon FSx data from Amazon S3 using S3 Access Points
* (**s3control**) Add support for the ability to use Amazon S3 Access Points with Amazon FSx for OpenZFS file systems.
* (**storagegateway**) This release adds IPv6 support to the Storage Gateway APIs. APIs that previously only accept or return IPv4 address will now accept or return both IPv4 and IPv6 addresses.
* (**textract**) Add RotationAngle field to Geometry of WORD blocks for Textract AnalyzeDocument API
* (**workspacesthinclient**) Remove Tags field from Get API responses

### Documentation
* (**ecs**) Updates for change to Amazon ECS default log driver mode from blocking to non-blocking

## [1.4.112] - 06/24/2025

### Features
* (**aiops**) Adds support for cross account investigations for CloudWatch investigations AI Operations (AIOps).
* (**batch**) Add userdataType to LaunchTemplateSpecification and LaunchTemplateSpecificationOverride.
* (**bedrock**) We are making ListFoundationModelAgreementOffers, DeleteFoundationModelAgreement, CreateFoundationModelAgreement, GetFoundationModelAvailability, PutUseCaseForModelAccess and GetUseCaseForModelAccess APIs public, previously they were console.
* (**ec2**) This release allows you to create and register AMIs while maintaining their underlying EBS snapshots within Local Zones.
* (**elasticloadbalancingv2**) Add Paginator for DescribeAccountLimits, and fix Paginators for DescribeTrustStoreAssociations, DescribeTrustStoreRevocations, and DescribeTrustStores
* (**gamelift**) Add support for UDP ping beacons to ListLocations API, including new PingBeacon and UDPEndpoint data types within its Locations return value. Use UDP ping beacon endpoints to help measure real-time network latency for multiplayer games.
* (**licensemanager**) AWS License Manager now supports license type conversions for AWS Marketplace products. Customers can provide Marketplace codes in the source license context or destination license context in the CreateLicenseConversionTaskForResource requests.
* (**rds**) Adding support for RDS on Dedicated Local Zones, including local backup target, snapshot availability zone and snapshot target
* (**route53resolver**) Add support for iterative DNS queries through the new INBOUND_DELEGATION endpoint. Add delegation support through the Outbound Endpoints with DELEGATE rules.
* (**transcribe**) This Feature Adds Support for the "et-EE" Locale for Batch Operations

## [1.4.111] - 06/23/2025

### Features
* (**glue**) AWS Glue now supports sort and z-order strategy for managed automated compaction for Iceberg tables in addition to binpack.
* (**s3tables**) S3 Tables now supports sort and z-order compaction strategies for Iceberg tables in addition to binpack.
* (**workspacesinstances**) Added support for Amazon WorkSpaces Instances API

## [1.4.110] - 06/21/2025

## [1.4.109] - 06/20/2025

### Features
* (**bedrock**) Add support for tiers in Content Filters and Denied Topics for Amazon Bedrock Guardrails.
* (**ecs**) Add ECS support for Windows Server 2025
* (**geoplaces**) Geocode, ReverseGeocode, and GetPlace APIs added Intersections and SecondaryAddresses. To use, add to the AdditionalFeatures list in your request. This provides info about nearby intersections and secondary addresses that are associated with a main address. Also added MainAddress and ParsedQuery.
* (**glue**) AWS Glue Data Quality now provides aggregated metrics in evaluation results when publishAggregatedMetrics with row-level results are enabled. These metrics include summary statistics showing total counts of processed, passed, and failed rows and rules in a single view.
* (**mediaconvert**) This release adds a new SPECIFIED_OPTIMAL option for handling DDS when using DVB-Sub with high resolution video.

## [1.4.108] - 06/19/2025

### Features
* (**bedrock**) This release of the SDK has the API and documentation for the createcustommodel API. This feature lets you copy a trained model into Amazon Bedrock for inference.
* (**emrserverless**) This release adds the capability to enable IAM IdentityCenter Trusted Identity Propagation for users running Interactive Sessions on EMR Serverless Applications.
* (**lambda**) Support Schema Registry feature for Kafka Event Source Mapping. Customers can now configure a Schema Registry to enable schema validation and filtering for Avro, Protobuf, and JSON-formatted events in Lambda for Kafka Event Source.
* (**paymentcryptography**) Additional support for managing HMAC keys that adheres to changes documented in X9.143-2021 and provides better interoperability for key import/export
* (**paymentcryptographydata**) Additional support for managing HMAC keys that adheres to changes documented in X9.143-2021 and provides better interoperability for key import/export
* (**sagemaker**) This release introduces alternative support for utilizing CFN templates from S3 for SageMaker Projects.

### Miscellaneous
* Upgrade to Dokka 2.0.0

## [1.4.107] - 06/18/2025

### Features
* (**aiops**) This is the initial SDK release for Amazon AI Operations (AIOps). AIOps is a generative AI-powered assistant that helps you respond to incidents in your system by scanning your system's telemetry and quickly surface suggestions that might be related to your issue.
* (**autoscaling**) Add IncludeInstances parameter to DescribeAutoScalingGroups API
* (**cloudwatchlogs**) Added CloudWatch Logs Transformer support for converting CloudTrail, VPC Flow, EKS Audit, AWS WAF and Route53 Resolver logs to OCSF v1.1 format.
* (**s3**) Added support for renaming objects within the same bucket using the new RenameObject API.
* (**sagemaker**) Add support for p6-b200 instance type for SageMaker Hyperpod

### Fixes
* [#1596](https://github.com/awslabs/aws-sdk-kotlin/issues/1596) Correctly convert key fields during paginated `Scan` and `Query` operations

## [1.4.106] - 06/17/2025

### Features
* (**accessanalyzer**) We are launching a new analyzer type, internal access analyzer. The new analyzer will generate internal access findings, which help customers understand who within their AWS organization or AWS Account has access to their critical AWS resources.
* (**acm**) Adds support for Exportable Public Certificates
* (**backup**) AWS Backup is adding support for integration of its logically air-gapped vaults with the AWS Organizations Multi-party approval capability.
* (**databasemigrationservice**) Add "Virtual" field to Data Provider as well as "S3Path" and "S3AccessRoleArn" fields to DataProvider settings
* (**guardduty**) Adding support for extended threat detection for EKS Audit Logs and EKS Runtime Monitoring.
* (**inspector2**) Add Code Repository Scanning as part of AWS InspectorV2
* (**mpa**) This release enables customers to create Multi-party approval teams and approval requests to protect supported operations.
* (**networkfirewall**) Release of Active Threat Defense in Network Firewall
* (**organizations**) Add support for policy operations on the SECURITYHUB_POLICY policy type.
* (**securityhub**) Adds operations, structures, and exceptions required for public preview release of Security Hub V2.
* (**sts**) The AWS Security Token Service APIs AssumeRoleWithSAML and AssumeRoleWithWebIdentity can now be invoked without pre-configured AWS credentials in the SDK configuration.
* (**wafv2**) AWS WAF can now suggest protection packs for you based on the application information you provide when you create a webACL.

### Documentation
* (**bedrock**) This release of the SDK has the API and documentation for the createcustommodel API. This feature lets you copy a trained model into Amazon Bedrock for inference.

## [1.4.105] - 06/16/2025

### Features
* (**bedrock**) This release of the SDK has the API and documentation for the createcustommodel API. This feature lets you copy a Amazon SageMaker trained Amazon Nova model into Amazon Bedrock for inference.
* (**ecr**) The `DescribeImageScanning` API now includes `lastInUseAt` and `InUseCount` fields that can be used to prioritize vulnerability remediation for images that are actively being used.
* (**networkfirewall**) You can now create firewalls using a Transit Gateway instead of a VPC, resulting in a TGW attachment.
* (**sagemaker**) This release 1) adds a new S3DataType Converse for SageMaker training 2)adds C8g R7gd M8g C6in P6 P6e instance type for SageMaker endpoint 3) adds m7i, r7i, c7i instance type for SageMaker Training and Processing.

## [1.4.104] - 06/12/2025

### Features
* (**connectcampaignsv2**) Added PutInstanceCommunicationLimits and GetInstanceCommunicationLimits APIs
* (**emrserverless**) This release adds support for retrieval of the optional executionIamPolicy field in the GetJobRun API response.
* (**iotfleetwise**) Add new status READY_FOR_CHECKIN used for vehicle synchronisation
* (**kms**) AWS KMS announces the support of ML-DSA key pairs that creates post-quantum safe digital signatures.
* (**pcs**) Fixed regex patterns for ARN fields.

### Documentation
* (**apigateway**) Documentation updates for Amazon API Gateway
* (**apigatewayv2**) Documentation updates for Amazon API Gateway
* (**ecs**) This Amazon ECS  release supports updating the capacityProviderStrategy parameter in update-service.

## [1.4.103] - 06/11/2025

### Features
* (**controlcatalog**) Introduced ListControlMappings API that retrieves control mappings. Added control aliases and governed resources fields in GetControl and ListControls APIs. New filtering capability in ListControls API, with implementation identifiers and implementation types.
* (**eks**) Release for EKS Pod Identity Cross Account feature and disableSessionTags flag.
* (**lexmodelsv2**) Add support for the Assisted NLU feature to improve bot performance
* (**networkmanager**) Add support for public DNS hostname resolution to private IP addresses across Cloud WAN-managed VPCs. Add support for security group referencing across Cloud WAN-managed VPCs.
* (**wafv2**) WAF now provides two DDoS protection options: resource-level monitoring for Application Load Balancers and the AWSManagedRulesAntiDDoSRuleSet managed rule group for CloudFront distributions.

### Documentation
* (**rds**) Updates Amazon RDS documentation for Amazon RDS for Db2 cross-Region replicas in standby mode.

## [1.4.102] - 06/10/2025

### Features
* [#1609](https://github.com/awslabs/aws-sdk-kotlin/issues/1609) (**s3**) Add support for presigning DeleteObject requests

### Documentation
* (**gameliftstreams**) Documentation updates for Amazon GameLift Streams to address formatting errors, correct resource ID examples, and update links to other guides

## [1.4.101] - 06/09/2025

### Features
* (**appsync**) Deprecate `atRestEncryptionEnabled` and `transitEncryptionEnabled` attributes in `CreateApiCache` action. Encryption is always enabled for new caches.
* (**costexplorer**) Support dual-stack endpoints for ce api
* (**customerprofiles**) This release introduces capability of Profile Explorer, using correct ingestion timestamp & using historical data for computing calculated attributes, and new standard objects for T&H as part of Amazon Connect Customer Profiles service.
* (**ec2**) Release to support Elastic VMware Service (Amazon EVS) Subnet and Amazon EVS Network Interface Types.
* (**efs**) Added support for Internet Protocol Version 6 (IPv6) on EFS Service APIs and mount targets.
* (**marketplacecatalog**) The ListEntities API now supports the EntityID, LastModifiedDate, ProductTitle, and Visibility filters for machine learning products. You can also sort using all of those filters.
* (**workspacesthinclient**) Add ConflictException to UpdateEnvironment API
* Optimize Kotlin API Reference documentation navigation sidebar

## [1.4.100] - 06/06/2025

### Features
* (**bedrockagentruntime**) This release introduces the `PromptCreationConfigurations` input parameter, which includes fields to control prompt population for `InvokeAgent` or `InvokeInlineAgent` requests.
* (**kms**) Remove unpopulated KeyMaterialId from Encrypt Response
* (**rds**) Include Global Cluster Identifier in DBCluster if the DBCluster is a Global Cluster Member.
* (**rekognition**) Adds support for defining an ordered preference list of different Rekognition Face Liveness challenge types when calling CreateFaceLivenessSession.
* (**route53**) Amazon Route 53 now supports the Asia Pacific (Taipei) Region (ap-east-2) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.
* (**s3tables**) S3 Tables now supports getting details about a table via its table ARN.

## [1.4.99] - 06/05/2025

### Features
* (**bcmpricingcalculator**) Updating the minimum for List APIs to be 1 (instead of 0)
* (**cloudformation**) Add new warning type 'EXCLUDED_PROPERTIES'
* (**kms**) AWS KMS announces the support for on-demand rotation of symmetric-encryption KMS keys with imported key material (EXTERNAL origin).
* (**wafv2**) AWS WAF adds support for ASN-based traffic filtering and support for ASN-based rate limiting.

## [1.4.98] - 06/04/2025

### Features
* (**evs**) Amazon Elastic VMware Service (Amazon EVS) allows you to run VMware Cloud Foundation (VCF) directly within your Amazon VPC including simplified self-managed migration experience with guided workflow in AWS console or via AWS CLI, get full access to their VCF deployment and VCF license portability.
* (**invoicing**) Added new Invoicing ListInvoiceSummaries API Operation
* (**mediaconnect**) This release updates the DescribeFlow API to show peer IP addresses. You can now identify the peer IP addresses of devices connected to your sources and outputs. This helps you to verify and troubleshoot your flow's active connections.
* (**mediaconvert**) This release includes support for embedding and signing C2PA content credentials in MP4 outputs.
* (**networkfirewall**) You can now monitor flow and alert log metrics from the Network Firewall console.
* (**sagemaker**) Added support for p6-b200 instance type in SageMaker Training Jobs and Training Plans.
* (**transcribe**) AWS Healthscribe now supports new templates for the clinical note summary: BIRP, SIRP, DAP, BEHAVIORAL_SOAP, and PHYSICAL_SOAP
* (**transcribestreaming**) AWS Healthscribe now supports new templates for the clinical note summary: BIRP, SIRP, DAP, BEHAVIORAL_SOAP, and PHYSICAL_SOAP

### Documentation
* (**amplify**) Update documentation for cacheConfig in CreateApp API

## [1.4.97] - 06/03/2025

### Features
* (**apigateway**) Adds support to set the routing mode for a custom domain name.
* (**apigatewayv2**) Adds support to create routing rules and set the routing mode for a custom domain name.
* (**emrserverless**) AWS EMR Serverless: Adds a new option in the CancelJobRun API in EMR 7.9.0+, to cancel a job with grace period. This feature is enabled by default with a 120-second grace period for streaming jobs and is not enabled by default for batch jobs.

## [1.4.96] - 06/02/2025

### Features
* (**athena**) Add support for the managed query result in the workgroup APIs. The managed query result configuration enables users to store query results to Athena owned storage.
* (**backup**) You can now subscribe to Amazon SNS notifications and Amazon EventBridge events for backup indexing. You can now receive notifications when a backup index is created, deleted, or fails to create, enhancing your ability to monitor and track your backup operations.
* (**bedrockagent**) This release adds the Agent Lifecycle Paused State feature to Amazon Bedrock agents. By using an agent's alias, you can temporarily suspend agent operations during maintenance, updates, or other situations.
* (**computeoptimizer**) This release enables AWS Compute Optimizer to analyze Amazon Aurora database clusters and generate Aurora I/O-Optimized recommendations.
* (**costoptimizationhub**) Support recommendations for Aurora instance and Aurora cluster storage.
* (**eks**) Add support for filtering ListInsights API calls on MISCONFIGURATION insight category
* (**entityresolution**) Add support for generating match IDs in near real-time.
* (**pcs**) Introduces SUSPENDING and SUSPENDED states for clusters, compute node groups, and queues.
* (**synthetics**) Support for Java runtime handler pattern.

### Documentation
* (**ecs**) Updates Amazon ECS documentation to include note for upcoming default log driver mode change.

## [1.4.95] - 05/30/2025

### Features
* (**emrserverless**) This release adds the capability for users to specify an optional Execution IAM policy in the StartJobRun action. The resulting permissions assumed by the job run is the intersection of the permissions in the Execution Role and the specified Execution IAM Policy.
* (**sagemaker**) Release new parameter CapacityReservationConfig in ProductionVariant

## [1.4.94] - 05/29/2025

### Features
* (**amplify**) Add support for customizable build instance sizes. CreateApp and UpdateApp operations now accept a new JobConfig parameter composed of BuildComputeType.
* (**autoscaling**) Add support for "apple" CpuManufacturer in ABIS
* (**bcmpricingcalculator**) Add AFTER_DISCOUNTS_AND_COMMITMENTS to Workload Estimate Rate Type. Set ListWorkLoadEstimateUsage maxResults range to minimum of 0 and maximum of 300.
* (**cloudtrail**) CloudTrail Feature Release: Support for Enriched Events with Configurable Context for Event Data Store
* (**connect**) Amazon Connect Service Feature: Email Recipient Limit Increase
* (**dataexchange**) This release adds Tag support for Event Action resource, through which customers can create event actions with Tags and retrieve event actions with Tags.
* (**datasync**) AgentArns field is made optional for Object Storage and Azure Blob location create requests. Location credentials are now managed via Secrets Manager, and may be encrypted with service managed or customer managed keys. Authentication is now optional for Azure Blob locations.
* (**fsx**) FSx API changes to support the public launch of new Intelligent Tiering storage class on Amazon FSx for Lustre
* (**ivsrealtime**) IVS Real-Time now offers customers the participant replication that allow customers to copy a participant from one stage to another.
* (**mwaa**) Amazon MWAA now lets you choose a worker replacement strategy when updating an environment. This release adds two worker replacement strategies: FORCED (default), which stops workers immediately, and GRACEFUL, which allows workers to finish current tasks before shutting down.
* (**s3**) Adding checksum support for S3 PutBucketOwnershipControls API.
* (**sagemaker**) Add maintenance status field to DescribeMlflowTrackingServer API response

## [1.4.93] - 05/28/2025

### Features
* (**costoptimizationhub**) This release allows customers to modify their preferred commitment term and payment options.
* (**ec2**) Enable the option to automatically delete underlying Amazon EBS snapshots when deregistering Amazon Machine Images (AMIs)
* (**eventbridge**) Allow for more than 2 characters for location codes in EventBridge ARNs
* (**networkfirewall**) You can now use VPC endpoint associations to create multiple firewall endpoints for a single firewall.
* (**synthetics**) Add support to change ephemeral storage. Add a new field "TestResult" under CanaryRunStatus.

## [1.4.92] - 05/27/2025

### Features
* (**costexplorer**) This release introduces Cost Comparison feature (GetCostAndUsageComparisons, GetCostComparisonDrivers) allowing you find cost variations across multiple dimensions and identify key drivers of spending changes.
* (**deadline**) AWS Deadline Cloud service-managed fleets now support storage profiles. With storage profiles, you can map file paths between a workstation and the worker hosts running the job.
* (**ec2**) This release adds three features - option to store AWS Site-to-Site VPN pre-shared keys in AWS Secrets Manager, GetActiveVpnTunnelStatus API to check the in-use VPN algorithms, and SampleType option in GetVpnConnectionDeviceSampleConfiguration API to get recommended sample configs for VPN devices.

## [1.4.91] - 05/23/2025

### Features
* (**ec2**) This release adds support for the C7i-flex, M7i-flex, I7i, I7ie, I8g, P6-b200, Trn2, C8gd, M8gd and R8gd instances
* (**securityir**) Update PrincipalId pattern documentation to reflect what user should receive back from the API call

## [1.4.90] - 05/22/2025

### Features
* (**amp**) Add QueryLoggingConfiguration APIs for Amazon Managed Prometheus
* (**auditmanager**) With this release, the AssessmentControl description field has been deprecated, as of May 19, 2025. Additionally, the UpdateAssessment API can now return a ServiceQuotaExceededException when applicable service quotas are exceeded.
* (**dsql**) Features: support for customer managed encryption keys
* (**glue**) This release supports additional ConversionSpec parameter as part of IntegrationPartition Structure in CreateIntegrationTableProperty API. This parameter is referred to apply appropriate column transformation for columns that are used for timestamp based partitioning

## [1.4.89] - 05/21/2025

### Features
* (**bedrockagentruntime**) Amazon Bedrock introduces asynchronous flows (in preview), which let you run flows for longer durations and yield control so that your application can perform other tasks and you don't have to actively monitor the flow's progress.
* (**cloudwatch**) Adds support for setting up Contributor Insight rules on logs transformed via Logs Transformation feature.
* (**ec2**) Release of Dualstack and Ipv6-only EC2 Public DNS hostnames
* (**partnercentralselling**) Modified validation to allow expectedCustomerSpend array with zero elements in Partner Opportunity operations.

### Documentation
* (**applicationautoscaling**) Doc only update that addresses a customer reported issue.

### Miscellaneous
* Removing private networks service due to deprecation

## [1.4.88] - 05/20/2025

### Features
* (**datasync**) Remove Discovery APIs from the DataSync service
* (**ec2**) This release expands the ModifyInstanceMaintenanceOptions API to enable or disable instance migration during customer-initiated reboots for EC2 Scheduled Reboot Events.
* (**glue**) Enhanced AWS Glue ListConnectionTypes API Model with additional metadata fields.
* (**inspector2**) This release adds GetClustersForImage API and filter updates as part of the mapping of container images to running containers feature.
* (**oam**) Add IncludeTags field to GetLink, GetSink and UpdateLink API
* (**rds**) This release introduces the new DescribeDBMajorEngineVersions API for describing the properties of specific major versions of database engines.

## [1.4.87] - 05/19/2025

### Features
* (**dsql**) CreateMultiRegionCluster and DeleteMultiRegionCluster APIs removed
* (**ec2**) This release includes new APIs for System Integrity Protection (SIP) configuration and automated root volume ownership delegation for EC2 Mac instances.
* (**mediapackagev2**) This release adds support for DVB-DASH, EBU-TT-D subtitle format, and non-compacted manifests for DASH in MediaPackage v2 Origin Endpoints.

## [1.4.86] - 05/16/2025

### Features
* (**bedrockdataautomation**) Add support for VIDEO modality to BlueprintType enum.
* (**bedrockdataautomationruntime**) Add AssetProcessingConfiguration for video segment to InputConfiguration
* (**codepipeline**) CodePipeline now supports new API ListDeployActionExecutionTargets that lists the deployment target details for deploy action executions.
* (**emr**) Added APIs for managing Application UIs: Access Persistent (serverless) UIs via CreatePersistentAppUI DescribePersistentAppUI & GetPersistentAppUIPresignedURL, and Cluster-based UIs through GetOnClusterAppUIPresignedURL. Supports Yarn, Spark History, and TEZ interfaces.
* (**glue**) Changes include (1) Excel as S3 Source type and XML and Tableau's Hyper as S3 Sink types, (2) targeted number of partitions parameter in S3 sinks and (3) new compression types in CSV/JSON and Parquet S3 sinks.
* (**neptune**) This release adds Global Cluster Switchover capability which enables you to change your global cluster's primary AWS Region, the region that serves writes, while preserving the replication between all regions in the global cluster.
* (**servicequotas**) This release introduces CreateSupportCase operation to SDK.

### Documentation
* (**ecs**) This is an Amazon ECs documentation only release to support the change of the container exit "reason" field from 255 characters to 1024 characters.

## [1.4.85] - 05/15/2025

### Features
* (**bedrockagent**) Amazon Bedrock Flows introduces DoWhile loops nodes, parallel node executions, and enhancements to knowledge base nodes.
* (**codebuild**) AWS CodeBuild now supports Docker Server capability
* (**databasemigrationservice**) Introduces Data Resync feature to describe-table-statistics and IAM database authentication for MariaDB, MySQL, and PostgreSQL.
* (**pcs**) This release adds support for Slurm accounting. For more information, see the Slurm accounting topic in the AWS PCS User Guide. Slurm accounting is supported for Slurm 24.11 and later. This release also adds 24.11 as a valid value for the version parameter of the Scheduler data type.
* (**workspaces**) Added the new AlwaysOn running mode for WorkSpaces Pools. Customers can now choose between AlwaysOn (for instant access, with hourly usage billing regardless of connection status), or AutoStop (to optimize cost, with a brief startup delay) for their pools.

### Documentation
* (**controltower**) Updated the descriptions for the AWS Control Tower Baseline APIs to make them more intuitive.

## [1.4.84] - 05/14/2025

### Features
* (**cloudwatchlogs**) This release adds a new API "ListLogGroups" and an improvement in API "DescribeLogGroups"
* (**cognitoidentityprovider**) Add exceptions to WebAuthn operations.
* (**firehose**) This release adds catalogARN support for s3 tables multi-catalog catalogARNs.
* (**mediaconvert**) This update enables cropping for video overlays and adds a new STL to Teletext upconversion toggle to preserve styling.

## [1.4.83] - 05/13/2025

### Features
* (**bedrock**) Enable cross-Region inference for Amazon Bedrock Guardrails by using the crossRegionConfig parameter when calling the CreateGuardrail or UpdateGuardrail operation.
* (**bedrockagentruntime**) Changes for enhanced metadata in trace
* (**controltower**) AWS Control Tower now reports the inheritance drift status for EnabledBaselines through the GetEnabledBaseline and ListEnabledBaselines APIs. You can now filter EnabledBaselines by their enablement and drift status using the ListEnabledBaselines API to view accounts and OUs that require attention.
* (**dsql**) CreateMultiRegionClusters and DeleteMultiRegionClusters APIs marked as deprecated. Introduced new multi-Region clusters creation experience through multiRegionProperties parameter in CreateCluster API.
* (**ecs**) This release extends functionality for Amazon EBS volumes attached to Amazon ECS tasks by adding support for the new EBS volumeInitializationRate parameter in ECS RunTask/StartTask/CreateService/UpdateService APIs.
* (**licensemanager**) Add Tagging feature to resources in the Managed Entitlements service. License and Grant resources can now be tagged.

## [1.4.82] - 05/12/2025

### Features
* (**deadline**) AWS Deadline Cloud service-managed fleets now support configuration scripts. Configuration scripts make it easy to install additional software, like plugins and packages, onto a worker.
* (**ec2**) EC2 - Adding support for AvailabilityZoneId
* (**iam**) Updating the endpoint list for the Identity and access management (IAM) service
* (**medialive**) Add support to the AV1 rate control mode
* (**mediatailor**) Documenting that EnabledLoggingStrategies is always present in responses of PlaybackConfiguration read operations.
* (**s3control**) Updates to support S3 Express zonal endpoints for directory buckets in AWS CLI
* (**sagemaker**) No API changes from previous release. This release migrated the model to Smithy keeping all features unchanged.
* (**supplychain**) Launch new AWS Supply Chain public APIs for DataIntegrationEvent, DataIntegrationFlowExecution and DatasetNamespace. Also add more capabilities to existing public APIs to support direct dataset event publish, data deduplication in DataIntegrationFlow, partition specification of custom datasets.

## [1.4.81] - 05/09/2025

### Features
* (**cloudwatchlogs**) We are pleased to announce limit increases to our grok processor logs transformation feature. Now you can define 20 Grok patterns in their configurations, with an expanded total pattern matching limit of 512 characters.
* (**synthetics**) Add support to retry a canary automatically after schedule run failures. Users can enable this feature by configuring the RetryConfig field when calling the CreateCanary or UpdateCanary API. Also includes changes in GetCanary and GetCanaryRuns to support retrieving retry configurations.
* (**workspaces**) Remove parameter EnableWorkDocs from WorkSpacesServiceModel due to end of support of Amazon WorkDocs service.

### Documentation
* (**athena**) Minor API documentation updates

## [1.4.80] - 05/08/2025

### Features
* (**codepipeline**) Add support for Secrets Manager and Plaintext environment variable types in Commands action
* (**ec2**) Launching the feature to support ENA queues offering flexibility to support multiple queues per Enhanced Network Interface (ENI)
* (**glue**) This new release supports customizable RefreshInterval for all Saas ZETL integrations from 15 minutes to 6 days.
* (**ssoadmin**) Update PutPermissionBoundaryToPermissionSet API's managedPolicyArn pattern to allow valid ARN only. Update ApplicationName to allow white spaces.

### Documentation
* (**cloudfront**) Doc-only update for CloudFront. These changes include customer-reported issues.
* (**guardduty**) Updated description of a data structure.

## [1.4.79] - 05/07/2025

### Features
* (**ec2**) This release adds API support for Path Component Exclusion (Filter Out ARN) for Reachability Analyzer
* (**medialive**) Enables Updating Anywhere Settings on a MediaLive Anywhere Channel.
* (**sagemaker**) SageMaker AI Studio users can now migrate to SageMaker Unified Studio, which offers a unified web-based development experience that integrates AWS data, analytics, artificial intelligence (AI), and machine learning (ML) services, as well as additional tools and resource
* (**synthetics**) Add support to test a canary update by invoking a dry run of a canary. This behavior can be used via the new StartCanaryDryRun API along with new fields in UpdateCanary to apply dry run changes. Also includes changes in GetCanary and GetCanaryRuns to support retrieving dry run configurations.

### Documentation
* (**imagebuilder**) Updated the CreateImageRecipeRequest ParentImage description to include all valid values as updated with the SSM Parameters project.

## [1.4.78] - 05/06/2025

### Features
* (**ec2**) This release adds support for Amazon EBS Provisioned Rate for Volume Initialization, which lets you specify a volume initialization rate to ensure that your EBS volumes are initialized in a predictable amount of time.
* (**servicecatalog**) ServiceCatalog's APIs (DeleteServiceAction, DisassociateServiceActionFromProvisioningArtifact, AssociateServiceActionWithProvisioningArtifact) now throw InvalidParametersException when IdempotencyToken is invalid.
* (**timestreamquery**) Add dualstack endpoints support and correct us-gov-west-1 FIPS endpoint.
* (**timestreamwrite**) Add dualstack endpoints support.

### Fixes
* [#1590](https://github.com/awslabs/aws-sdk-kotlin/issues/1590) (**dynamodb-mapper**) Fix schema generation for nullable lists and maps

## [1.4.77] - 05/05/2025

### Features
* (**datazone**) This release adds a new authorization policy to control the usage of custom AssetType when creating an Asset. Customer can now add new grant(s) of policyType USE_ASSET_TYPE for custom AssetTypes to apply authorization policy to projects members and domain unit owners.
* (**devicefarm**) Add an optional parameter to the GetDevicePoolCompatibility API to pass in project information to check device pool compatibility.
* (**ec2**) This update introduces API operations to manage and create local gateway VIF and VIF groups. It also includes API operations to describe Outpost LAGs and service link VIFs.
* (**mediaconvert**) This release adds an optional sidecar per-frame video quality metrics report and an ALL_PCM option for audio selectors. It also changes the data type for Probe API response fields related to video and audio bitrate from integer to double.

### Documentation
* (**ecs**) Add support to roll back an In_Progress ECS Service Deployment

## [1.4.76] - 05/02/2025

### Features
* (**bedrockdataautomation**) Added support for Custom output and blueprints for AUDIO data types.
* (**kinesis**) Marking ResourceARN as required for Amazon Kinesis Data Streams APIs TagResource, UntagResource, and ListTagsForResource.

### Documentation
* (**directoryservice**) Doc only update - fixed typos.

## [1.4.75] - 05/01/2025

### Features
* (**appconfig**) Adding waiter support for deployments and environments; documentation updates
* (**connect**) This release adds the following fields to DescribeContact: DisconnectReason, AgentInitiatedHoldDuration, AfterContactWorkStartTimestamp, AfterContactWorkEndTimestamp, AfterContactWorkDuration, StateTransitions, Recordings, ContactDetails, ContactEvaluations, Attributes
* (**sagemaker**) Feature - Adding support for Scheduled and Rolling Update Software in Sagemaker Hyperpod.
* (**verifiedpermissions**) Amazon Verified Permissions / Features : Adds support for tagging policy stores.

## [1.4.74] - 04/30/2025

### Features
* (**bedrock**) You can now specify a cross region inference profile as a teacher model for the CreateModelCustomizationJob API. Additionally, the GetModelCustomizationJob API has been enhanced to return the sub-task statuses of a customization job within the StatusDetails response field.
* (**bedrockagent**) Features:    Add inline code node to prompt flow
* (**bedrockagentruntime**) Support for Custom Orchestration within InlineAgents
* (**cleanrooms**) This release adds support for ProtectedQuery results to be delivered to more than one collaboration member via the new distribute output configuration in StartProtectedQuery.
* (**cloudwatchlogs**) CloudWatch Logs supports "DELIVERY" log class. This log class is used only for delivering AWS Lambda logs to Amazon S3 or Amazon Data Firehose.
* (**deadline**) Adds support for tag management on workers and tag inheritance from fleets to their associated workers.
* (**ec2**) Launch of cost distribution feature for IPAM owners to distribute costs to internal teams.
* (**ecr**) Adds dualstack support for Amazon Elastic Container Registry (Amazon ECR).
* (**ecrpublic**) Adds dualstack support for Amazon Elastic Container Registry Public (Amazon ECR Public).
* (**mailmanager**) Introducing new RuleSet rule PublishToSns action, which allows customers to publish email notifications to an Amazon SNS topic. New PublishToSns action enables customers to easily integrate their email workflows via Amazon SNS, allowing them to notify other systems about important email events.

## [1.4.73] - 04/29/2025

### Features
* (**connectcases**) Introduces CustomEntity as part of the UserUnion data type. This field is used to indicate the entity who is performing the API action.
* (**kinesis**) Amazon KDS now supports tagging and attribute-based access control (ABAC) for enhanced fan-out consumers.
* (**pinpointsmsvoicev2**) AWS End User Messaging has added MONITOR and FILTER functionality to SMS Protect.
* (**qbusiness**) Add support for anonymous user access for Q Business applications
* (**sagemaker**) Introduced support for P5en instance types on SageMaker Studio for JupyterLab and CodeEditor applications.
* (**sagemakermetrics**) SageMaker Metrics Service now supports FIPS endpoint in all US and Canada Commercial regions.
* (**ssm**) This release adds support for just-In-time node access in AWS Systems Manager. Just-in-time node access enables customers to move towards zero standing privileges by requiring operators to request access and obtain approval before remotely connecting to nodes managed by the SSM Agent.
* (**ssmguiconnect**) This release adds API support for the connection recording GUI Connect feature of AWS Systems Manager
* Add `authSchemePreference` configuration

## [1.4.72] - 04/28/2025

### Features
* (**acm**) Add support for file-based HTTP domain control validation, available through Amazon CloudFront.
* (**bedrockruntime**) This release adds native h2 support for the bedrock runtime API, the support is only limited to SDKs that support h2 requests natively.
* (**cloudfront**) Add distribution tenant, connection group, and multi-tenant distribution APIs to the CloudFront SDK.
* (**imagebuilder**) Add integration with SSM Parameter Store to Image Builder.

### Documentation
* (**dynamodb**) Doc only update for GSI descriptions.

## [1.4.71] - 04/25/2025

### Features
* (**bedrockruntime**) You can now reference images and documents stored in Amazon S3 when using InvokeModel and Converse APIs with Amazon Nova Lite and Nova Pro. This enables direct integration of S3-stored multimedia assets in your model requests without manual downloading or base64 encoding.

### Documentation
* (**ecs**) Documentation only release for Amazon ECS.
* (**marketplacedeployment**) Doc only update for the AWS Marketplace Deployment Service that fixes several customer-reported issues.

## [1.4.70] - 04/24/2025

### Features
* (**apprunner**) AWS App Runner adds Node.js 22 runtime.
* (**appsync**) Add data source support to Event APIs
* (**bedrockdataautomation**) Added support for modality routing and modality enablement on CreateDataAutomationProject and UpdateDataAutomationProject APIs
* (**codebuild**) Remove redundant validation check.
* (**dynamodb**) Add support for ARN-sourced account endpoint generation for TransactWriteItems. This will generate account endpoints for DynamoDB TransactWriteItems requests using ARN-sourced account ID when available.
* (**rds**) This Amazon RDS release adds support for managed master user passwords for Oracle CDBs.
* [#1577](https://github.com/awslabs/aws-sdk-kotlin/issues/1577) Add support for DynamoDB Mapper `getItem` overloads that specify primary key(s)

### Documentation
* (**ecs**) Documentation only release for Amazon ECS
* (**pcs**) Documentation-only update: added valid values for the version property of the Scheduler and SchedulerRequest data types.

## [1.4.69] - 04/23/2025

### Features
* (**codebuild**) Add support for custom instance type for reserved capacity fleets
* (**ecs**) Add support to roll back an In_Progress ECS Service Deployment

### Documentation
* (**resourceexplorer2**) Documentation-only update for CreateView option correction

## [1.4.68] - 04/22/2025

### Features
* (**account**) AWS Account Management now supports account name update via IAM principals.
* (**cognitoidentityprovider**) This release adds refresh token rotation.
* (**ec2**) Added support for  ClientRouteEnforcementOptions flag in CreateClientVpnEndpoint and ModifyClientVpnEndpoint requests and DescribeClientVpnEndpoints responses
* (**entityresolution**) To expand support for matching records using digital identifiers with TransUnion
* (**mq**) You can now delete Amazon MQ broker configurations using the DeleteConfiguration API. For more information, see Configurations in the Amazon MQ API Reference.
* (**redshiftserverless**) Provides new and updated API members to support the Redshift Serverless reservations feature.
* (**s3control**) Fix endpoint resolution test cases

### Fixes
* [#1214](https://github.com/awslabs/aws-sdk-kotlin/issues/1214) Bump **smithy-kotlin** version to pick up fix for connection polling

## [1.4.67] - 04/21/2025

### Features
* (**arczonalshift**) Updates to documentation and exception types for Zonal Autoshift
* (**budgets**) Releasing the new Budget FilterExpression and Metrics fields to support more granular filtering options. These new fields are intended to replace CostFilters and CostTypes, which are deprecated as of 2025/18/04.
* (**mediatailor**) Added support for Recurring Prefetch and Traffic Shaping on both Single and Recurring Prefetch. ListPrefetchSchedules now return single prefetchs by default and can be provided scheduleType of SINGLE, RECURRING, AND ALL.
* (**qbusiness**) The CheckDocumentAccess API for Amazon Q Business is a self-service debugging API that allows administrators to verify document access permissions and review Access Control List (ACL) configurations.

### Documentation
* (**firehose**) Documentation update regarding the number of streams you can create using the CreateDeliveryStream API.
* (**securityhub**) Minor documentation update for the GetConfigurationPolicyAssociation example

## [1.4.66] - 04/18/2025

### Features
* (**qconnect**) This release adds support for the following capabilities: Chunking generative answer replies from Amazon Q in Connect. Integration support for the use of additional LLM models with Amazon Q in Connect.
* (**sagemaker**) This release adds a new Neuron driver option in InferenceAmiVersion parameter for ProductionVariant. Additionally, it adds support for fetching model lifecycle status in the ListModelPackages API. Users can now use this API to view the lifecycle stage of models that have been shared with them.
* (**servicequotas**) Add new optional SupportCaseAllowed query parameter to the RequestServiceQuotaIncrease API

## [1.4.65] - 04/17/2025

### Features
* (**accessanalyzer**) Added new resource types to evaluate for public access in resource policies and added support for S3 directory bucket access points.
* (**amp**) Add Workspace Configuration APIs for Amazon Prometheus
* (**bedrock**) With this release, Bedrock Evaluation will now support custom metrics for evaluation.
* (**connect**) This release adds following capabilities to Contact Lens Rules APIs 1/ 'ASSIGN_SLA' action and  '$.Case.TemplateId' comparison value for 'OnCaseCreate' and 'OnCaseUpdate' event sources 2/ 'OnSlaBreach' Cases event source which supports '$.RelatedItem.SlaConfiguration.Name' comparison value
* (**ecs**) Adds a new AccountSetting - defaultLogDriverMode for ECS.
* (**iotfleetwise**) We've added stricter parameter validations to AWS IoT FleetWise signal catalog, model manifest, and decoder manifest APIs.
* (**memorydb**) Added support for IPv6 and dual stack for Valkey and Redis clusters. Customers can now launch new Valkey and Redis clusters with IPv6 and dual stack networking support.
* (**omics**) Add versioning for HealthOmics workflows
* Add support for fetching account ID from IMDS credentials on EC2

### Documentation
* (**autoscaling**) Doc only update for EC2 Auto Scaling.

## [1.4.64] - 04/16/2025

### Features
* (**connectcases**) This feature provides capabilities to help track and meet service level agreements (SLAs) on cases programmatically. It allows configuring a new related item of type `Sla` on a case using CreateRelatedItem API and provides the ability to search for this new related item using SearchRelatedItems API.
* (**dsql**) Added GetClusterEndpointService API. The new API allows retrieving endpoint service name specific to a cluster.
* (**eks**) Added support for new AL2023 ARM64 NVIDIA AMIs to the supported AMITypes.
* (**eventbridge**) Adding support for KmsKeyIdentifer in CreateConnection, UpdateConnection and DescribeConnection APIs
* (**resourcegroups**) Resource Groups: TagSyncTasks can be created with ResourceQuery
* (**s3tables**) S3 Tables now supports setting encryption configurations on table buckets and tables. Encryption configurations can use server side encryption using AES256 or KMS customer-managed keys.

### Documentation
* (**servicecatalog**) Updated default value for the access-level-filter in SearchProvisionedProducts API to Account. For access to userLevel or roleLevel, the user must provide access-level-filter parameter.

## [1.4.63] - 04/14/2025

### Features
* (**entityresolution**) This is to add new metrics to our GetIdMappingJob API and also update uniqueId naming for batchDeleteUniqueIds API to be more accurate
* (**taxsettings**) Indonesia SOR Tax Registration Launch

## [1.4.62] - 04/11/2025

### Features
* (**connectcontactlens**) Making sentiment optional for ListRealtimeContactAnalysisSegments Response depending on conversational analytics configuration
* (**datazone**) Raise hard limit of authorized principals per SubscriptionTarget from 10 to 20.
* (**detective**) Add support for Detective DualStack endpoints
* (**marketplaceentitlementservice**) Add support for Marketplace Entitlement Service dual-stack endpoints for CN and GOV regions
* (**marketplacemetering**) Add support for Marketplace Metering Service dual-stack endpoints for CN regions
* (**pcs**) Changed the minimum length of clusterIdentifier, computeNodeGroupIdentifier, and queueIdentifier to 3.
* (**verifiedpermissions**) Adds deletion protection support to policy stores. Deletion protection is disabled by default, can be enabled via the CreatePolicyStore or UpdatePolicyStore APIs, and is visible in GetPolicyStore.

### Documentation
* (**dynamodb**) Doc only update for API descriptions.

## [1.4.61] - 04/10/2025

### Features
* (**applicationautoscaling**) Application Auto Scaling now supports horizontal scaling for Elasticache Memcached self-designed clusters using target tracking scaling policies and scheduled scaling.
* (**elasticache**) AWS ElastiCache SDK now supports using MemcachedUpgradeConfig parameter with ModifyCacheCluster API to enable updating Memcached cache node types. Please refer to updated AWS ElastiCache public documentation for detailed information on API usage and implementation.
* (**m2**) Introduce three new APIs: CreateDataSetExportTask, GetDataSetExportTask and ListDataSetExportHistory. Add support for batch restart for Blu Age applications.
* (**medialive**) AWS Elemental MediaLive / Features : Add support for CMAF Ingest CaptionLanguageMappings, TimedMetadataId3 settings, and Link InputResolution.
* (**qbusiness**) Adds functionality to enable/disable a new Q Business Hallucination Reduction feature. If enabled, Q Business will detect and attempt to remove Hallucinations from certain Chat requests.
* (**quicksight**) Add support to analysis and sheet level highlighting in QuickSight.

## [1.4.60] - 04/09/2025

### Features
* (**controlcatalog**) The GetControl API now surfaces a control's Severity, CreateTime, and Identifier for a control's Implementation. The ListControls API now surfaces a control's Behavior, Severity, CreateTime, and Identifier for a control's Implementation.
* (**glue**) The TableOptimizer APIs in AWS Glue now return the DpuHours field in each TableOptimizerRun, providing clients visibility to the DPU-hours used for billing in managed Apache Iceberg table compaction optimization.
* (**groundstation**) Support tagging Agents and adjust input field validations
* (**transfer**) This launch includes 2 enhancements to SFTP connectors user-experience: 1) Customers can self-serve concurrent connections setting for their connectors, and 2) Customers can discover the public host key of remote servers using their SFTP connectors.

### Documentation
* (**dynamodb**) Documentation update for secondary indexes and Create_Table.

## [1.4.59] - 04/08/2025

### Features
* (**bedrockruntime**) This release introduces our latest bedrock runtime API, InvokeModelWithBidirectionalStream. The API supports both input and output streams and is supported by only HTTP2.0.
* (**costexplorer**) This release supports Pagination traits on Cost Anomaly Detection APIs.
* (**costoptimizationhub**) This release adds resource type "MemoryDbReservedInstances" and resource type "DynamoDbReservedCapacity" to the GetRecommendation, ListRecommendations, and ListRecommendationSummaries APIs to support new MemoryDB and DynamoDB RI recommendations.
* (**iotfleetwise**) This release adds the option to update the strategy of state templates already associated to a vehicle, without the need to remove and re-add them.
* (**storagegateway**) Added new ActiveDirectoryStatus value, ListCacheReports paginator, and support for longer pagination tokens.
* (**taxsettings**) Uzbekistan Launch on TaxSettings Page

### Documentation
* (**securityhub**) Documentation updates for AWS Security Hub.

## [1.4.58] - 04/08/2025

## [1.4.57] - 04/07/2025

⚠️ **IMPORTANT**: This version should not be used, as it depends on an unreleased snapshot version and will not work correctly. Please use version 1.4.58 or later.

### Features
* (**bedrock**) New options for how to handle harmful content detected by Amazon Bedrock Guardrails.
* (**bedrockruntime**) New options for how to handle harmful content detected by Amazon Bedrock Guardrails.
* (**codebuild**) AWS CodeBuild now offers an enhanced debugging experience.
* (**glue**) Add input validations for multiple Glue APIs
* (**medialive**) AWS Elemental MediaLive now supports SDI inputs to MediaLive Anywhere Channels in workflows that use AWS SDKs.
* (**personalize**) Add support for eventsConfig for CreateSolution, UpdateSolution, DescribeSolution, DescribeSolutionVersion. Add support for GetSolutionMetrics to return weighted NDCG metrics when eventsConfig is enabled for the solution.
* (**transfer**) This launch enables customers to manage contents of their remote directories, by deleting old files or moving files to archive folders in remote servers once they have been retrieved. Customers will be able to automate the process using event-driven architecture.

## [1.4.56] - 04/04/2025

### Features
* (**eventbridge**) Amazon EventBridge adds support for customer-managed keys on Archives and validations for two fields: eventSourceArn and kmsKeyIdentifier.

### Documentation
* (**directoryservicedata**) Doc only update - fixed broken links.
* (**ec2**) Doc-only updates for Amazon EC2
* (**s3control**) Updated max size of Prefixes parameter of Scope data type.

## [1.4.55] - 04/03/2025

### Features
* (**bedrockagent**) Added optional "customMetadataField" for Amazon Aurora knowledge bases, allowing single-column metadata. Also added optional "textIndexName" for MongoDB Atlas knowledge bases, enabling hybrid search support.
* (**chimesdkvoice**) Added FOC date as an attribute of PhoneNumberOrder, added AccessDeniedException as a possible return type of ValidateE911Address
* (**mailmanager**) Add support for Dual_Stack and PrivateLink types of IngressPoint. For configuration requests, SES Mail Manager will now accept both IPv4/IPv6 dual-stack endpoints and AWS PrivateLink VPC endpoints for email receiving.
* (**route53**) Added us-gov-east-1 and us-gov-west-1 as valid Latency Based Routing regions for change-resource-record-sets.
* (**sagemaker**) Adds support for i3en, m7i, r7i instance types for SageMaker Hyperpod
* (**sesv2**) This release enables customers to provide attachments in the SESv2 SendEmail and SendBulkEmail APIs.
* (**transcribe**) This Feature Adds Support for the "zh-HK" Locale for Batch Operations

### Documentation
* (**opensearch**) Improve descriptions for various API commands and data types.

## [1.4.54] - 04/02/2025

### Features
* (**applicationsignals**) Application Signals now supports creating Service Level Objectives on service dependencies. Users can now create or update SLOs on discovered service dependencies to monitor their standard application metrics.
* (**codebuild**) This release adds support for environment type WINDOWS_SERVER_2022_CONTAINER in ProjectEnvironment
* (**lexmodelsv2**) Release feature of errorlogging for lex bot, customer can config this feature in bot version to generate log for error exception which helps debug
* (**medialive**) Added support for SMPTE 2110 inputs when running a channel in a MediaLive Anywhere cluster. This feature enables ingestion of SMPTE 2110-compliant video, audio, and ancillary streams by reading SDP files that AWS Elemental MediaLive can retrieve from a network source.

### Documentation
* (**ecr**) Fix for customer issues related to AWS account ID and size limitation for token.
* (**ecs**) This is an Amazon ECS documentation only update to address various tickets.

## [1.4.53] - 04/01/2025

### Features
* (**cleanrooms**) This release adds support for updating the analytics engine of a collaboration.
* (**sagemaker**) Added tagging support for SageMaker notebook instance lifecycle configurations

## [1.4.52] - 03/31/2025

### Features
* (**bedrockruntime**) Add Prompt Caching support to Converse and ConverseStream APIs
* (**deadline**) With this release you can use a new field to specify the search term match type. Search term match types currently support fuzzy and contains matching.
* (**ec2**) Release VPC Route Server, a new feature allowing dynamic routing in VPCs.
* (**eks**) Add support for updating RemoteNetworkConfig for hybrid nodes on EKS UpdateClusterConfig API
* (**marketplaceentitlementservice**) Add support for Marketplace Entitlement Service dual-stack endpoints.
* (**outposts**) Enabling Asset Level Capacity Management feature, which allows customers to create a Capacity Task for a single Asset on their active Outpost.
* (**s3**) Amazon S3 adds support for S3 Access Points for directory buckets in AWS Dedicated Local Zones
* (**s3control**) Amazon S3 adds support for S3 Access Points for directory buckets in AWS Dedicated Local Zones
* (**sesv2**) Add dual-stack support to global endpoints.
* (**transfer**) Add WebAppEndpointPolicy support for WebApps

## [1.4.51] - 03/28/2025

### Features
* (**apigateway**) Adds support for setting the IP address type to allow dual-stack or IPv4 address types to invoke your APIs or domain names.
* (**apigatewayv2**) Adds support for setting the IP address type to allow dual-stack or IPv4 address types to invoke your APIs or domain names.
* (**bedrockruntime**) Launching Multi-modality Content Filter for Amazon Bedrock Guardrails.
* (**codebuild**) This release adds support for cacheNamespace in ProjectCache
* (**marketplacemetering**) Add support for Marketplace Metering Service dual-stack endpoints.
* (**networkmanager**) Add support for NetworkManager Dualstack endpoints.
* (**paymentcryptography**) The service adds support for transferring AES-256 and other keys between the service and other service providers and HSMs. This feature uses ECDH to derive a one-time key transport key to enable these secure key exchanges.
* (**quicksight**) RLS permission dataset with userAs: RLS_RULES flag, Q in QuickSight/Threshold Alerts/Schedules/Snapshots in QS embedding, toggle dataset refresh email alerts via API, transposed table with options: column width, type and index, toggle Q&A on dashboards, Oracle Service Name when creating data source.
* (**sagemaker**) TransformAmiVersion for Batch Transform and SageMaker Search Service Aggregate Search API Extension

### Documentation
* (**ecs**) This is an Amazon ECS documentation only release that addresses tickets.

## [1.4.50] - 03/27/2025

### Features
* (**batch**) This release will enable two features: Firelens log driver, and Execute Command on Batch jobs on ECS. Both features will be passed through to ECS.
* (**bcmpricingcalculator**) Added standaloneAccountRateTypeSelections for GetPreferences and UpdatePreferences APIs. Added STALE enum value to status attribute in GetBillScenario and UpdateBillScenario APIs.
* (**bedrockagentruntime**) bedrock flow now support node action trace.
* (**cloudformation**) Adding support for the new parameter "ScanFilters" in the CloudFormation StartResourceScan API. When this parameter is included, the StartResourceScan API will initiate a scan limited to the resource types specified by the parameter.
* (**datazone**) This release adds new action type of Create Listing Changeset for the Metadata Enforcement Rule feature.
* (**eks**) Added support for BOTTLEROCKET FIPS AMIs to AMI types in US regions.
* (**gamelift**) Amazon GameLift Servers add support for additional instance types.
* (**iam**) Update IAM dual-stack endpoints for BJS, IAD and PDT partitions
* (**sagemaker**) add: recovery mode for SageMaker Studio apps
* (**ssooidc**) This release adds AwsAdditionalDetails in the CreateTokenWithIAM API response.

## [1.4.49] - 03/26/2025

### Features
* (**arczonalshift**) Add new shiftType field for ARC zonal shifts.
* (**directconnect**) With this release, AWS Direct Connect allows you to tag your Direct Connect gateways. Tags are metadata that you can create and use to manage your Direct Connect gateways. For more information about tagging, see AWS Tagging Strategies.
* (**mediaconvert**) This release adds a configurable Quality Level setting for the top rendition of Auto ABR jobs
* (**mediatailor**) Add support for log filtering which allow customers to filter out selected event types from logs.
* (**polly**) Added support for the new voice - Jihye (ko-KR). Jihye is available as a Neural voice only.
* (**wafv2**) This release adds the ability to associate an AWS WAF v2 web ACL with an AWS Amplify App.

### Documentation
* (**rds**) Add note about the Availability Zone where RDS restores the DB cluster for the RestoreDBClusterToPointInTime operation.

## [1.4.48] - 03/25/2025

### Features
* (**bedrockagent**) Adding support for Amazon OpenSearch Managed clusters as a vector database in Knowledge Bases for Amazon Bedrock
* (**eks**) Added support to override upgrade-blocking readiness checks via force flag when updating a cluster.
* (**gameliftstreams**) Minor updates to improve developer experience.
* (**keyspaces**) Removing replication region limitation for Amazon Keyspaces Multi-Region Replication APIs.
* (**marketplaceentitlementservice**) This release enhances the GetEntitlements API to support new filter CUSTOMER_AWS_ACCOUNT_ID in request and CustomerAWSAccountId field in response.
* (**marketplacemetering**) This release enhances the BatchMeterUsage API to support new field CustomerAWSAccountId in request and response and making CustomerIdentifier optional. CustomerAWSAccountId or CustomerIdentifier must be provided in request but not both.
* (**sagemaker**) This release adds support for customer-managed KMS keys in Amazon SageMaker Partner AI Apps
* (**workspacesthinclient**) Deprecate tags field in Get API responses

## [1.4.47] - 03/24/2025

### Features
* (**iotwireless**) Mark EutranCid under LteNmr optional.
* (**pcs**) ClusterName/ClusterIdentifier, ComputeNodeGroupName/ComputeNodeGroupIdentifier, and QueueName/QueueIdentifier can now have 10 characters, and a minimum of 3 characters. The TagResource API action can now return ServiceQuotaExceededException.
* (**ssm**) This release adds the AvailableSecurityUpdatesComplianceStatus field to patch baseline operations, as well as the AvailableSecurityUpdateCount and InstancesWithAvailableSecurityUpdates to patch state operations. Applies to Windows Server managed nodes only.

### Documentation
* (**qconnect**) Provides the correct value for supported model ID.

## [1.4.46] - 03/21/2025

### Features
* (**bedrock**) A CustomModelUnit(CMU) is an abstract view of the hardware utilization that Bedrock needs to host a a single copy of your custom imported model. Bedrock determines the number of CMUs that a model copy needs when you import the custom model. You can use CMUs to estimate the cost of Inference's.
* (**datazone**) Add support for overriding selection of default AWS IAM Identity Center instance as part of Amazon DataZone domain APIs.
* (**route53recoverycontrolconfig**) Adds dual-stack (IPv4 and IPv6) endpoint support for route53-recovery-control-config operations, opt-in dual-stack addresses for cluster endpoints, and UpdateCluster API to update the network-type of clusters between IPv4 and dual-stack.
* (**sagemaker**) This release does the following: 1.) Adds DurationHours as a required field to the SearchTrainingPlanOfferings action in the SageMaker AI API; 2.) Adds support for G6e instance types for SageMaker AI inference optimization jobs.

## [1.4.45] - 03/20/2025

### Features
* (**amplify**) Added appId field to Webhook responses
* (**bedrock**) With this release, Bedrock Evaluation will now support bring your own inference responses.
* (**mailmanager**) Amazon SES Mail Manager. Extended rule string and boolean expressions to support analysis in condition evaluation. Extended ingress point string expression to support analysis in condition evaluation
* (**networkfirewall**) You can now use flow operations to either flush or capture traffic monitored in your firewall's flow table.

### Documentation
* (**controlcatalog**) Add ExemptAssumeRoot parameter to adapt for new AWS AssumeRoot capability.

## [1.4.44] - 03/19/2025

### Features
* (**bedrock**) Support custom prompt routers for evaluation jobs
* (**lambda**) Add Ruby 3.4 (ruby3.4) support to AWS Lambda.
* (**mediaconnect**) This release adds support for NDI flow outputs in AWS Elemental MediaConnect. You can now send content from your MediaConnect transport streams directly to your NDI environment using the new NDI output type.
* (**neptunegraph**) Update IAM Role ARN Validation to Support Role Paths
* (**sagemaker**) Added support for g6, g6e, m6i, c6i instance types in SageMaker Processing Jobs.

### Documentation
* (**ec2**) Doc-only updates for EC2 for March 2025.

## [1.4.43] - 03/18/2025

### Features
* (**appsync**) Providing Tagging support for DomainName in AppSync
* (**cleanrooms**) This release adds support for PySpark jobs. Customers can now analyze data by running jobs using approved PySpark analysis templates.
* (**mediaconvert**) This release adds support for AVC passthrough, the ability to specify PTS offset without padding, and an A/V segment matching feature.
* (**route53**) Amazon Route 53 now supports the iso-f regions for private DNS Amazon VPCs and cloudwatch healthchecks.

## [1.4.42] - 03/17/2025

### Features
* (**applicationsignals**) This release adds support for adding, removing, and listing SLO time exclusion windows with the BatchUpdateExclusionWindows and ListServiceLevelObjectiveExclusionWindows APIs.
* (**geomaps**) Provide support for vector map styles in the GetStaticMap operation.
* (**rum**) CloudWatch RUM now supports unminification of JS error stack traces.
* (**taxsettings**) Adjust Vietnam PaymentVoucherNumber regex and minor API change.
* (**wafv2**) AWS WAF now lets you inspect fragments of request URIs. You can specify the scope of the URI to inspect and narrow the set of URI fragments.

## [1.4.41] - 03/14/2025

### Features
* (**glue**) This release added AllowFullTableExternalDataAccess to glue catalog resource.
* (**lakeformation**) This release added "condition" to LakeFormation OptIn APIs, also added WithPrivilegedAccess flag to RegisterResource and DescribeResource.

### Documentation
* (**cognitoidentity**) Updated API model build artifacts for identity pools
* (**cognitoidentityprovider**) Minor description updates to API parameters

## [1.4.40] - 03/13/2025

### Features
* (**acmpca**) Private Certificate Authority service now supports P521 and RSA3072 key algorithms.
* (**amplify**) Introduced support for Skew Protection. Added enableSkewProtection field to createBranch and updateBranch API.
* (**cloudwatchlogs**) Updated CreateLogAnomalyDetector to accept only kms key arn
* (**codebuild**) AWS CodeBuild now supports webhook filtering by organization name
* (**datazone**) This release adds support to update projects and environments
* (**dynamodb**) Generate account endpoints for DynamoDB requests using ARN-sourced account ID when available
* (**ec2**) This release changes the CreateLaunchTemplate, CreateLaunchTemplateVersion, ModifyLaunchTemplate CLI and SDKs such that if you do not specify a client token, a randomly generated token is used for the request to ensure idempotency.
* (**ivsrealtime**) IVS Real-Time now offers customers the ability to adjust the participant & composition recording segment duration
* (**mediapackagev2**) This release adds the ResetChannelState and ResetOriginEndpointState operation to reset MediaPackage V2 channel and origin endpoint. This release also adds a new field, UrlEncodeChildManifest, for HLS/LL-HLS to allow URL-encoding child manifest query string based on the requirements of AWS SigV4.
* (**s3control**) Updating GetDataAccess response for S3 Access Grants to include the matched Grantee for the requested prefix

## [1.4.39] - 03/11/2025

### Features
* (**ec2**) This release adds the GroupLongName field to the response of the DescribeAvailabilityZones API.
* (**ecr**) This release adds Amazon ECR to Amazon ECR pull through cache rules support.
* (**inspector2**) Adding componentArn to network reachability details
* (**medialive**) Add an enum option DISABLED for Output Locking Mode under Global Configuration.

### Documentation
* (**ecs**) This is a documentation only update for Amazon ECS to address various tickets.

## [1.4.38] - 03/10/2025

### Features
* (**bedrockagent**) Add support for computer use tools
* (**bedrockagentruntime**) Add support for computer use tools
* (**connect**) Add support for contact transfers in external voice systems.
* (**costexplorer**) Releasing minor partition endpoint updates.
* (**medialive**) Adds defaultFontSize and defaultLineHeight as options in the EbuTtDDestinationSettings within the caption descriptions for an output stream.
* (**pcaconnectorad**) PrivateCA Connector for Active Directory now supports dual stack endpoints. This release adds the IpAddressType option to the VpcInformation on a Connector which determines whether the endpoint supports IPv4 only or IPv4 and IPv6 traffic.
* (**securityhub**) This release adds new StandardsControlsUpdatable field to the StandardsSubscription resource

### Documentation
* (**timestreaminfluxdb**) This release updates the default value of pprof-disabled from false to true.

## [1.4.37] - 03/07/2025

### Features
* (**bedrockagent**) Introduces support for Neptune Analytics as a vector data store and adds Context Enrichment Configurations, enabling use cases such as GraphRAG.
* (**bedrockagentruntime**) Support Multi Agent Collaboration within Inline Agents
* (**ec2**) Add serviceManaged field to DescribeAddresses API response.
* (**elasticloadbalancingv2**) This release adds support for assigning IP addresses to Application Load Balancers from VPC IP Address Manager pools.

### Documentation
* (**cloudfront**) Documentation updates for Amazon CloudFront.
* (**neptunegraph**) Several small updates to resolve customer requests.

## [1.4.36] - 03/06/2025

### Features
* (**bedrock**) This releases adds support for Custom Prompt Router
* (**ivsrealtime**) IVS Real-Time now offers customers the ability to merge fragmented recordings in the event of a participant disconnect.
* (**networkflowmonitor**) This release contains 2 changes. 1: DeleteScope/GetScope/UpdateScope operations now return 404 instead of 500 when the resource does not exist. 2: Expected string format for clientToken fields of CreateMonitorInput/CreateScopeInput/UpdateMonitorInput have been updated to be an UUID based string.
* (**redshiftdata**) This release adds support for ListStatements API to filter statements by ClusterIdentifier, WorkgroupName, and Database.
* (**wafv2**) You can now perform an exact match or rate limit aggregation against the web request's JA4 fingerprint.
* (**workspaces**) Added a new ModifyEndpointEncryptionMode API for managing endpoint encryption settings.

### Documentation
* (**cloudtrail**) Doc-only update for CloudTrail.

## [1.4.35] - 03/05/2025

### Features
* (**bedrockruntime**) This releases adds support for Custom Prompt Router ARN
* (**datasync**) AWS DataSync now supports modifying ServerHostname while updating locations SMB, NFS, and ObjectStorage.
* (**gameliftstreams**) New Service: Amazon GameLift Streams delivers low-latency game streaming from AWS global infrastructure to virtually any device with a browser at up to 1080p resolution and 60 fps.
* (**iotfleetwise**) This release adds floating point support for CAN/OBD signals and adds support for signed OBD signals.
* (**workspaces**) Added DeviceTypeWorkSpacesThinClient type to allow users to access their WorkSpaces through a WorkSpaces Thin Client.

## [1.4.34] - 03/04/2025

### Features
* (**iotmanagedintegrations**) Adding managed integrations APIs for IoT Device Management to setup and control devices across different manufacturers and connectivity protocols. APIs include managedthing operations, credential and provisioning profile management, notification configuration, and OTA update.
* (**iotsitewise**) AWS IoT SiteWise now supports MQTT-enabled, V3 gateways. Configure data destinations for real-time ingestion into AWS IoT SiteWise or buffered ingestion using Amazon S3 storage. You can also use path filters for precise data collection from specific MQTT topics.

### Documentation
* (**elasticache**) Doc only update, listing 'valkey7' and 'valkey8' as engine options for parameter groups.
* (**rds**) Note support for Database Insights for Amazon RDS.

## [1.4.33] - 03/03/2025

### Features
* (**cognitoidentityprovider**) Added the capacity to return available challenges in admin authentication and to set version 3 of the pre token generation event for M2M ATC.
* (**ec2**) Update the DescribeVpcs response
* (**qbusiness**) Adds support for the ingestion of audio and video files by Q Business, which can be configured with the mediaExtractionConfiguration parameter.
* (**rum**) Add support for PutResourcePolicy, GetResourcePolicy and DeleteResourcePolicy to support resource based policies for AWS CloudWatch RUM
* (**sagemaker**) Add DomainId to CreateDomainResponse

### Documentation
* (**transcribe**) Updating documentation for post call analytics job queueing.

## [1.4.32] - 02/28/2025

### Features
* (**bedrockagent**) This release lets Amazon Bedrock Flows support newer models by increasing the maximum length of output in a prompt configuration. This release also increases the maximum number of prompt variables to 20 and the maximum number of node inputs to 20.
* (**bedrockdataautomation**) Renamed and added new StandardConfiguration enums. Added support to update EncryptionConfiguration in UpdateBlueprint and UpdateDataAutomation APIs. Changed HttpStatus code for DeleteBlueprint and DeleteDataAutomationProject APIs to 200 from 204. Added APIs to support tagging.
* (**bedrockdataautomationruntime**) Added a mandatory parameter DataAutomationProfileArn to support for cross region inference for InvokeDataAutomationAsync API. Renamed DataAutomationArn to DataAutomationProjectArn. Added APIs to support tagging.
* (**databasemigrationservice**) Add skipped status to the Result Statistics of an Assessment Run
* (**eks**) Adding licenses to EKS Anywhere Subscription operations response.
* (**mediaconvert**) The AWS MediaConvert Probe API allows you to analyze media files and retrieve detailed metadata about their content, format, and structure.
* (**pricing**) Update GetProducts and DescribeServices API request input validations.

### Documentation
* (**ssm**) Systems Manager doc-only updates for Feb. 2025.

## [1.4.31] - 02/27/2025

### Features
* (**bedrockagentruntime**) Introduces Sessions (preview) to enable stateful conversations in GenAI applications.
* (**qbusiness**) This release supports deleting attachments from conversations.
* (**redshiftserverless**) Add track support for Redshift Serverless workgroup.
* (**sagemaker**) SageMaker HubService is introducing support for creating Training Jobs in Curated Hub (Private Hub). Additionally, it is introducing two new APIs: UpdateHubContent and UpdateHubContentReference.
* (**storagegateway**) This release adds support to invoke a process that cleans the specified file share's cache of file entries that are failing upload to Amazon S3.

### Documentation
* (**emr**) Definition update for EbsConfiguration.

## [1.4.30] - 02/26/2025

### Features
* (**applicationsignals**) This release adds API support for reading Service Level Objectives and Services from monitoring accounts, from SLO and Service-scoped operations, including ListServices and ListServiceLevelObjectives.
* (**batch**) AWS Batch: Resource Aware Scheduling feature support
* (**chime**) Removes the Amazon Chime SDK APIs from the "chime" namespace.  Amazon Chime SDK APIs continue to be available in the AWS SDK via the dedicated Amazon Chime SDK namespaces: chime-sdk-identity, chime-sdk-mediapipelines, chime-sdk-meetings, chime-sdk-messaging, and chime-sdk-voice.
* (**ec2**) Amazon EC2 Fleet customers can now override the Block Device Mapping specified in the Launch Template when creating a new Fleet request, saving the effort of creating and associating new Launch Templates to customize the Block Device Mapping.
* (**iotfleetwise**) This release adds an optional listResponseScope request parameter in certain list API requests to limit the response to metadata only.
* (**oam**) This release adds support for sharing AWS::ApplicationSignals::Service and AWS::ApplicationSignals::ServiceLevelObjective resources.
* (**sagemaker**) AWS SageMaker InferenceComponents now support rolling update deployments for Inference Components.

### Documentation
* (**cloudfront**) Documentation update for VPC origin config.

## [1.4.29] - 02/25/2025

### Features
* (**codebuild**) Adding "reportArns" field in output of BatchGetBuildBatches API. "reportArns" is an array that contains the ARNs of reports created by merging reports from builds associated with the batch build.
* (**devicefarm**) Add an optional configuration to the ScheduleRun and CreateRemoteAccessSession API to set a device level http/s proxy.
* (**ec2**) Adds support for time-based EBS-backed AMI copy operations. Time-based copy ensures that EBS-backed AMIs are copied within and across Regions in a specified timeframe.
* (**iot**) AWS IoT - AWS IoT Device Defender adds support for a new Device Defender Audit Check that monitors device certificate age and custom threshold configurations for both the new device certificate age check and existing device certificate expiry check.
* (**taxsettings**) PutTaxRegistration API changes for Egypt, Greece, Vietnam countries
* Add SigV4a support to the default AWS signer

## [1.4.28] - 02/24/2025

### Features
* (**bedrockagent**) This release improves support for newer models in Amazon Bedrock Flows.
* (**bedrockagentruntime**) Adding support for ReasoningContent fields in Pre-Processing, Post-Processing and Orchestration Trace outputs.
* (**bedrockruntime**) This release adds Reasoning Content support to Converse and ConverseStream APIs

### Documentation
* (**elasticache**) Documentation update, adding clarity and rephrasing.

### Miscellaneous
* Remove Elastic Inference Service

## [1.4.27] - 02/21/2025

### Features
* (**appstream**) Added support for Certificate-Based Authentication on AppStream 2.0 multi-session fleets.
* (**bedrockagent**) Introduce a new parameter which represents the user-agent header value used by the Bedrock Knowledge Base Web Connector.

## [1.4.26] - 02/20/2025

### Features
* (**codebuild**) Add webhook status and status message to AWS CodeBuild webhooks
* (**guardduty**) Reduce the minimum number of required attack sequence signals from 2 to 1
* (**sagemaker**) Added new capability in the UpdateCluster operation to remove instance groups from your SageMaker HyperPod cluster.
* (**workspacesweb**) Add support for toolbar configuration under user settings.

### Documentation
* (**licensemanagerusersubscriptions**) Updates entity to include Microsoft RDS SAL as a valid type of user subscription.
* (**rds**) CloudWatch Database Insights now supports Amazon RDS.

## [1.4.25] - 02/19/2025

### Features
* (**codepipeline**) Add environment variables to codepipeline action declaration.
* (**location**) Adds support for larger property maps for tracking and geofence positions changes. It increases the maximum number of items from 3 to 4, and the maximum value length from 40 to 150.
* (**mailmanager**) This release adds additional metadata fields in Mail Manager archive searches to show email source and details about emails that were archived when being sent with SES.
* (**networkfirewall**) This release introduces Network Firewall's Automated Domain List feature. New APIs include UpdateFirewallAnalysisSettings, StartAnalysisReport, GetAnalysisReportResults, and ListAnalysisReports. These allow customers to enable analysis on firewalls to identify and report frequently accessed domain.
* (**sagemaker**) Adds r8g instance type support to SageMaker Realtime Endpoints
* (**sesv2**) This release adds the ability for outbound email sent with SES to preserve emails to a Mail Manager archive.

### Documentation
* (**ecs**) This is a documentation only release for Amazon ECS that supports the CPU task limit increase.
* (**lightsail**) Documentation updates for Amazon Lightsail.

## [1.4.24] - 02/18/2025

### Features
* (**emrcontainers**) EMR on EKS StartJobRun Api will be supporting the configuration of log storage in AWS by using "managedLogs" under "MonitoringConfiguration".
* (**medialive**) Adds support for creating CloudWatchAlarmTemplates for AWS Elemental MediaTailor Playback Configuration resources.

### Documentation
* (**batch**) This documentation-only update corrects some typos.

## [1.4.23] - 02/17/2025

### Features
* (**amplify**) Add ComputeRoleArn to CreateApp, UpdateApp, CreateBranch, and UpdateBranch, allowing caller to specify a role to be assumed by Amplify Hosting for server-side rendered applications.
* (**databasemigrationservice**) Support replicationConfigArn in DMS DescribeApplicableIndividualAssessments API.
* (**timestreaminfluxdb**) This release introduces APIs to manage DbClusters and adds support for read replicas

## [1.4.22] - 02/14/2025

### Features
* (**codebuild**) Added test suite names to test case metadata
* (**connect**) Release Notes: 1) Analytics API enhancements: Added new ListAnalyticsDataLakeDataSets API. 2)  Onboarding API Idempotency: Adds ClientToken to instance creation and management APIs to support idempotency.
* (**databasemigrationservice**) Introduces premigration assessment feature to DMS Serverless API for start-replication and describe-replications
* (**rdsdata**) Add support for Stop DB feature.
* (**s3**) Added support for Content-Range header in HeadObject response.
* (**wafv2**) The WAFv2 API now supports configuring data protection in webACLs.
* (**workspacesthinclient**) Update Environment and Device name field definitions
* [#1521](https://github.com/awslabs/aws-sdk-kotlin/issues/1521) Improve the coding style of DynamoDB Mapper auto-generated schemas

### Fixes
* [#1413](https://github.com/awslabs/aws-sdk-kotlin/issues/1413) Bump the **smithy-kotlin** version to pick up a [bugfix for Endpoint Discovery](https://github.com/smithy-lang/smithy-kotlin/pull/1240)

## [1.4.21] - 02/14/2025

### Features
* (**codebuild**) Added test suite names to test case metadata
* (**connect**) Release Notes: 1) Analytics API enhancements: Added new ListAnalyticsDataLakeDataSets API. 2)  Onboarding API Idempotency: Adds ClientToken to instance creation and management APIs to support idempotency.
* (**databasemigrationservice**) Introduces premigration assessment feature to DMS Serverless API for start-replication and describe-replications
* (**rdsdata**) Add support for Stop DB feature.
* (**s3**) Added support for Content-Range header in HeadObject response.
* (**wafv2**) The WAFv2 API now supports configuring data protection in webACLs.
* (**workspacesthinclient**) Update Environment and Device name field definitions
* [#1521](https://github.com/awslabs/aws-sdk-kotlin/issues/1521) Improve the coding style of DynamoDB Mapper auto-generated schemas

### Fixes
* [#1413](https://github.com/awslabs/aws-sdk-kotlin/issues/1413) Bump the **smithy-kotlin** version to pick up a [bugfix for Endpoint Discovery](https://github.com/smithy-lang/smithy-kotlin/pull/1240)

## [1.4.20] - 02/13/2025

### Features
* (**accessanalyzer**) This release introduces the getFindingsStatistics API, enabling users to retrieve aggregated finding statistics for IAM Access Analyzer's external access and unused access analysis features. Updated service API and documentation.
* (**fis**) Adds auto-pagination for the following operations: ListActions, ListExperimentTemplates, ListTargetAccountConfigurations, ListExperiments, ListExperimentResolvedTargets, ListTargetResourceTypes. Reduces length constraints of prefixes for logConfiguration and experimentReportConfiguration.
* (**sagemaker**) Adds additional values to the InferenceAmiVersion parameter in the ProductionVariant data type.
* (**storagegateway**) This release adds support for generating cache reports on S3 File Gateways for files that fail to upload.

### Documentation
* (**acmpca**) Private Certificate Authority (PCA) documentation updates
* (**ecs**) This is a documentation only release to support migrating Amazon ECS service ARNs to the long ARN format.

## [1.4.19] - 02/12/2025

### Features
* (**b2bi**) Allow spaces in the following fields in the Partnership resource: ISA 06 - Sender ID, ISA 08 - Receiver ID, GS 02 - Application Sender Code, GS 03 - Application Receiver Code
* (**bedrockagent**) This releases adds the additionalModelRequestFields field to the CreateAgent and UpdateAgent operations. Use additionalModelRequestFields to specify  additional inference parameters for a model beyond the base inference parameters.
* (**bedrockagentruntime**) This releases adds the additionalModelRequestFields field to the InvokeInlineAgent operation. Use additionalModelRequestFields to specify  additional inference parameters for a model beyond the base inference parameters.
* (**fsx**) Support for in-place Lustre version upgrades
* (**medialive**) Adds a RequestId parameter to all MediaLive Workflow Monitor create operations.  The RequestId parameter allows idempotent operations.
* (**opensearchserverless**) Custom OpenSearchServerless Entity ID for SAML Config.
* (**polly**) Added support for the new voice - Jasmine (en-SG). Jasmine is available as a Neural voice only.

### Documentation
* (**codebuild**) Add note for the RUNNER_BUILDKITE_BUILD buildType.

## [1.4.18] - 02/11/2025

### Features
* (**acmpca**) Private Certificate Authority service now supports Partitioned CRL as a revocation configuration option.
* (**appsync**) Add support for operation level caching
* (**ec2**) Adding support for the new fullSnapshotSizeInBytes field in the response of the EC2 EBS DescribeSnapshots API. This field represents the size of all the blocks that were written to the source volume at the time the snapshot was created.

### Documentation
* (**pi**) Documentation only update for RDS Performance Insights dimensions for execution plans and locking analysis.

## [1.4.17] - 02/10/2025

### Features
* (**apigatewayv2**) Documentation updates for Amazon API Gateway
* (**databasemigrationservice**) New vendors for DMS Data Providers: DB2 LUW and DB2 for z/OS

### Documentation
* (**cloudfront**) Doc-only update that adds defaults for CloudFront VpcOriginEndpointConfig values.
* (**connect**) Updated the CreateContact API documentation to indicate that it only applies to EMAIL contacts.

## [1.4.16] - 02/07/2025

### Features
* (**ecr**) Adds support to handle the new basic scanning daily quota.
* (**eks**) Introduce versionStatus field to take place of status field in EKS DescribeClusterVersions API
* (**mediaconvert**) This release adds support for Animated GIF output, forced chroma sample positioning metadata, and Extensible Wave Container format
* (**pi**) Adds documentation for dimension groups and dimensions to analyze locks for Database Insights.
* (**transcribe**) This release adds support for the Clinical Note Template Customization feature for the AWS HealthScribe APIs within Amazon Transcribe.
* (**transcribestreaming**) This release adds support for the Clinical Note Template Customization feature for the AWS HealthScribe Streaming APIs within Amazon Transcribe.

## [1.4.15] - 02/06/2025

### Features
* (**cloudformation**) We added 5 new stack refactoring APIs: CreateStackRefactor, ExecuteStackRefactor, ListStackRefactors, DescribeStackRefactor, ListStackRefactorActions.
* (**connectcases**) This release adds the ability to conditionally require fields on a template. Check public documentation for more information.
* (**costoptimizationhub**) This release enables AWS Cost Optimization Hub to show cost optimization recommendations for Amazon Auto Scaling Groups, including those with single and mixed instance types.
* (**s3**) Updated list of the valid AWS Region values for the LocationConstraint parameter for general purpose buckets.

## [1.4.14] - 02/05/2025

### Documentation
* (**rds**) Documentation updates to clarify the description for the parameter AllocatedStorage for the DB cluster data type, the description for the parameter DeleteAutomatedBackups for the DeleteDBCluster API operation, and removing an outdated note for the CreateDBParameterGroup API operation.

## [1.4.13] - 02/04/2025

### Features
* (**databasemigrationservice**) Introduces TargetDataSettings with the TablePreparationMode option available for data migrations.
* (**iam**) This release adds support for accepting encrypted SAML assertions. Customers can now configure their identity provider to encrypt the SAML assertions it sends to IAM.
* (**neptunegraph**) Added argument to `list-export` to filter by graph ID
* (**qbusiness**) Adds functionality to enable/disable a new Q Business Chat orchestration feature. If enabled, Q Business can orchestrate over datasources and plugins without the need for customers to select specific chat modes.

### Documentation
* (**datasync**) Doc-only update to provide more information on using Kerberos authentication with SMB locations.
* (**sagemaker**) IPv6 support for Hyperpod clusters

## [1.4.12] - 02/03/2025

### Features
* (**mediatailor**) Add support for CloudWatch Vended Logs which allows for delivery of customer logs to CloudWatch Logs, S3, or Firehose.

## [1.4.11] - 01/31/2025

### Features
* (**amp**) Add support for sending metrics to cross account and CMCK AMP workspaces through RoleConfiguration on Create/Update Scraper.
* (**bedrockagentruntime**) This change is to deprecate the existing citation field under RetrieveAndGenerateStream API response in lieu of GeneratedResponsePart and RetrievedReferences
* (**codebuild**) Added support for CodeBuild self-hosted Buildkite runner builds
* (**georoutes**) The OptimizeWaypoints API now supports 50 waypoints per request (20 with constraints like AccessHours or AppointmentTime). It adds waypoint clustering via Clustering and ClusteringIndex for better optimization. Also, total distance validation is removed for greater flexibility.
* (**sagemaker**) This release introduces a new valid value in InstanceType parameter: p5en.48xlarge, in ProductionVariant.

### Documentation
* (**rds**) Updates to Aurora MySQL and Aurora PostgreSQL API pages with instance log type in the create and modify DB Cluster.

## [1.4.10] - 01/30/2025

### Features
* (**appstream**) Add support for managing admin consent requirement on selected domains for OneDrive Storage Connectors in AppStream2.0.
* (**bedrockagentruntime**) Add a 'reason' field to InternalServerException
* (**ecr**) Temporarily updating dualstack endpoint support
* (**ecrpublic**) Temporarily updating dualstack endpoint support
* (**mediatailor**) Adds options for configuring how MediaTailor conditions ads before inserting them into the content stream. Based on the new settings, MediaTailor will either transcode ads to match the content stream as it has in the past, or it will insert ads without first transcoding them.
* (**qbusiness**) Added APIs to manage QBusiness user subscriptions
* (**s3tables**) You can now use the CreateTable API operation to create tables with schemas by adding an optional metadata argument.
* (**verifiedpermissions**) Adds Cedar JSON format support for entities and context data in authorization requests

## [1.4.9] - 01/29/2025

### Features
* (**bcmpricingcalculator**) Added ConflictException error type in DeleteBillScenario, BatchDeleteBillScenarioCommitmentModification, BatchDeleteBillScenarioUsageModification, BatchUpdateBillScenarioUsageModification, and BatchUpdateBillScenarioCommitmentModification API operations.
* (**ecr**) Add support for Dualstack and Dualstack-with-FIPS Endpoints
* (**ecrpublic**) Add support for Dualstack Endpoints
* (**mailmanager**) This release includes a new feature for Amazon SES Mail Manager which allows customers to specify known addresses and domains and make use of those in traffic policies and rules actions to distinguish between known and unknown entries.
* (**s3**) Change the type of MpuObjectSize in CompleteMultipartUploadRequest from int to long.
* (**transcribestreaming**) This release adds support for AWS HealthScribe Streaming APIs within Amazon Transcribe.

### Fixes
* Upgrade **smithy-kotlin** version to pick up fixes for header signing

## [1.4.8] - 01/28/2025

### Features
* (**appsync**) Add stash and outErrors to EvaluateCode/EvaluateMappingTemplate response
* (**datasync**) AWS DataSync now supports the Kerberos authentication protocol for SMB locations.
* (**deadline**) feature: Deadline: Add support for limiting the concurrent usage of external resources, like floating licenses, using limits and the ability to constrain the maximum number of workers that work on a job
* (**ec2**) This release changes the CreateFleet CLI and SDK's such that if you do not specify a client token, a randomly generated token is used for the request to ensure idempotency.
* (**firehose**) For AppendOnly streams, Firehose will automatically scale to match your throughput.
* (**timestreaminfluxdb**) Adds 'allocatedStorage' parameter to UpdateDbInstance API that allows increasing the database instance storage size and 'dbStorageType' parameter to UpdateDbInstance API that allows changing the storage type of the database instance

## [1.4.7] - 01/27/2025

### Features
* (**bedrockagent**) Add support for the prompt caching feature for Bedrock Prompt Management
* (**iot**) Raised the documentParameters size limit to 30 KB for AWS IoT Device Management - Jobs.
* (**mediaconvert**) This release adds support for dynamic audio configuration and the ability to disable the deblocking filter for h265 encodes.
* (**s3control**) Minor fix to ARN validation for Lambda functions passed to S3 Batch Operations

## [1.4.6] - 01/24/2025

### Features
* (**cloudtrail**) This release introduces the SearchSampleQueries API that allows users to search for CloudTrail Lake sample queries.
* (**eks**) Adds support for UpdateStrategies in EKS Managed Node Groups.
* (**healthlake**) Added new authorization strategy value 'SMART_ON_FHIR' for CreateFHIRDatastore API to support Smart App 2.0
* (**transfer**) Added CustomDirectories as a new directory option for storing inbound AS2 messages, MDN files and Status files.

### Documentation
* (**ssm**) Systems Manager doc-only update for January, 2025.
* (**ssooidc**) Fixed typos in the descriptions.

## [1.4.5] - 01/23/2025

### Features
* (**ec2**) Added "future" allocation type for future dated capacity reservation

## [1.4.4] - 01/22/2025

### Features
* (**bedrockagentruntime**) Adds multi-turn input support for an Agent node in an Amazon Bedrock Flow
* (**medialive**) AWS Elemental MediaLive adds a new feature, ID3 segment tagging, in CMAF Ingest output groups. It allows customers to insert ID3 tags into every output segment, controlled by a newly added channel schedule action Id3SegmentTagging.

### Documentation
* (**glue**) Docs Update for timeout changes
* (**workspacesthinclient**) Rename WorkSpaces Web to WorkSpaces Secure Browser

## [1.4.3] - 01/21/2025

### Features
* (**cognitoidentityprovider**) corrects the dual-stack endpoint configuration for cognitoidp
* (**connect**) Added DeleteContactFlowVersion API and the CAMPAIGN flow type
* (**emrserverless**) Increasing entryPoint in SparkSubmit to accept longer script paths. New limit is 4kb.
* (**iotsitewise**) AWS IoT SiteWise now supports ingestion and querying of Null (all data types) and NaN (double type) values of bad or uncertain data quality. New partial error handling prevents data loss during ingestion. Enabled by default for new customers; existing customers can opt-in.
* (**quicksight**) Added `DigitGroupingStyle` in ThousandsSeparator to allow grouping by `LAKH`( Indian Grouping system ) currency. Support LAKH and `CRORE` currency types in Column Formatting.

### Documentation
* (**batch**) Documentation-only update: clarified the description of the shareDecaySeconds parameter of the FairsharePolicy data type, clarified the description of the priority parameter of the JobQueueDetail data type.
* (**cloudwatchlogs**) Documentation-only update to address doc errors
* (**sns**) This release adds support for the topic attribute FifoThroughputScope for SNS FIFO topics. For details, see the documentation history in the Amazon Simple Notification Service Developer Guide.

## [1.4.2] - 01/17/2025

### Features
* (**bedrockruntime**) Allow hyphens in tool name for Converse and ConverseStream APIs
* (**ec2**) Release u7i-6tb.112xlarge, u7i-8tb.112xlarge, u7inh-32tb.480xlarge, p5e.48xlarge, p5en.48xlarge, f2.12xlarge, f2.48xlarge, trn2.48xlarge instance types.
* (**notifications**) Added support for Managed Notifications, integration with AWS Organization and added aggregation summaries for Aggregate Notifications
* Add auth token generator for RDS and DSQL

### Documentation
* (**detective**) Doc only update for Detective documentation.
* (**sagemaker**) Correction of docs for  "Added support for ml.trn1.32xlarge instance type in Reserved Capacity Offering"

## [1.4.1] - 01/16/2025

### Features
* (**sagemaker**) Added support for ml.trn1.32xlarge instance type in Reserved Capacity Offering

### Documentation
* (**ecs**) The release addresses Amazon ECS documentation tickets.

## [1.4.0] - 01/15/2025

### Features
* (**bedrockagentruntime**) Now supports streaming for inline agents.
* (**cognitoidentity**) corrects the dual-stack endpoint configuration
* (**partnercentralselling**) Add Tagging support for ResourceSnapshotJob resources
* (**s3**) This change enhances integrity protections for new SDK requests to S3. S3 SDKs now support the CRC64NVME checksum algorithm, full object checksums for multipart S3 objects, and new default integrity protections for S3 requests.
* (**securityir**) Increase minimum length of Threat Actor IP 'userAgent' to 1.
* (**sesv2**) This release introduces a new recommendation in Virtual Deliverability Manager Advisor, which detects elevated complaint rates for customer sending identities.
* (**workspaces**) Added GeneralPurpose.4xlarge & GeneralPurpose.8xlarge ComputeTypes.
* (**workspacesthinclient**) Mark type in MaintenanceWindow as required.
* ⚠️ **IMPORTANT**: S3 client behavior is updated to always calculate a checksum by default for operations that support it (such as PutObject or UploadPart), or require it (such as DeleteObjects). The checksum algorithm used by default varies by SDK (CRC32 or CRC64, CRC32 for the Kotlin SDK). Checksum calculation behavior can be configured using `when_supported` and `when_required` options - in code using requestChecksumCalculation, in shared config using request_checksum_calculation, or as env variable using AWS_REQUEST_CHECKSUM_CALCULATION. The S3 client attempts to validate response checksums for all S3 API operations that support checksums. However, if the SDK has not implemented the specified checksum algorithm then this validation is skipped. Checksum validation behavior can be configured using `when_supported` and `when_required` options - in code using responseChecksumValidation, in shared config using response_checksum_validation, or as env variable using AWS_RESPONSE_CHECKSUM_VALIDATION.

### Fixes
* [#1321](https://github.com/awslabs/aws-sdk-kotlin/issues/1321) Include more information when retry strategy halts early due to token bucket capacity errors

### Documentation
* (**apigateway**) Documentation updates for Amazon API Gateway

### Miscellaneous
* The order of credentials resolution in the credentials provider chain has been updated to: system properties, environment variables, web identity tokens, profile, ECS, EC2
* ⚠️ **IMPORTANT**: Upgrade to Kotlin 2.1.0
* The order of credentials resolution in config files has been updated to: static credentials, assume role with source profile OR assume role with named provider, web identity token, SSO session, legacy SSO, process

## [1.3.112] - 01/14/2025

### Features
* (**gamelift**) Amazon GameLift releases a new game session placement feature: PriorityConfigurationOverride. You can now override how a game session queue prioritizes placement locations for a single StartGameSessionPlacement request.
* (**route53**) Amazon Route 53 now supports the Mexico (Central) Region (mx-central-1) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region

## [1.3.111] - 01/13/2025

### Features
* (**artifact**) Support resolving regional API calls to partition's leader region endpoint.
* (**bedrock**) With this release, Bedrock Evaluation will now support latency-optimized inference for foundation models.
* (**ec2**) Add support for DisconnectOnSessionTimeout flag in CreateClientVpnEndpoint and ModifyClientVpnEndpoint requests and DescribeClientVpnEndpoints responses
* (**kafkaconnect**) Support updating connector configuration via UpdateConnector API. Release Operations API to monitor the status of the connector operation.
* (**transcribe**) This update provides tagging support for Transcribe's Call Analytics Jobs and Call Analytics Categories.

## [1.3.110] - 01/10/2025

### Documentation
* (**redshift**) Additions to the PubliclyAccessible and Encrypted parameters clarifying what the defaults are.
* (**securitylake**) Doc only update for ServiceName that fixes several customer-reported issues
* (**sts**) Fixed typos in the descriptions.

## [1.3.109] - 01/09/2025

### Features
* (**codebuild**) AWS CodeBuild Now Supports BuildBatch in Reserved Capacity and Lambda
* (**computeoptimizer**) This release expands AWS Compute Optimizer rightsizing recommendation support for Amazon EC2 Auto Scaling groups to include those with scaling policies and multiple instance types.
* (**fms**) AWS Firewall Manager now lets you combine multiple resource tags using the logical AND operator or the logical OR operator.

## [1.3.108] - 01/08/2025

### Features
* (**route53**) Amazon Route 53 now supports the Asia Pacific (Thailand) Region (ap-southeast-7) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region
* (**sagemaker**) Adds support for IPv6 for SageMaker HyperPod cluster nodes.

### Documentation
* (**rds**) Updates Amazon RDS documentation to clarify the RestoreDBClusterToPointInTime description.

### Miscellaneous
* Remove IoT 1-click services from SDK

## [1.3.107] - 01/07/2025

### Features
* (**cloudhsmv2**) Adds support to ModifyCluster for modifying a Cluster's Hsm Type.
* (**dynamodb**) This release makes Amazon DynamoDB point-in-time-recovery (PITR) to be configurable. You can set PITR recovery period for each table individually to between 1 and 35 days.
* (**imagebuilder**) This release adds support for importing images from ISO disk files. Added new ImportDiskImage API operation.

## [1.3.106] - 01/06/2025

### Features
* (**iotsecuretunneling**) Adds dualstack endpoint support for IoT Secure Tunneling
* (**supplychain**) Allow vanity DNS domain when creating a new ASC instance

### Fixes
* [#1473](https://github.com/awslabs/aws-sdk-kotlin/issues/1473) Bump to latest **smithy-kotlin** version to pick up fixes to `InputStream` wrapping

## [1.3.105] - 01/03/2025

### Features
* (**s3**) This change is only for updating the model regexp of CopySource which is not for validation but only for documentation and user guide change.

### Fixes
* [#1481](https://github.com/awslabs/aws-sdk-kotlin/issues/1481) Add missing packages to version catalog and BOM (**http-client-engine-okhttp4**, **smithy-rpcv2-protocols**, **telemetry-provider-micrometer**)

### Documentation
* (**ecs**) Adding SDK reference examples for Amazon ECS operations.
* (**route53domains**) Doc only update for Route 53 Domains that fixes several customer-reported issues

## [1.3.104] - 01/02/2025

### Features
* (**appsync**) Modify UpdateGraphQLAPI operation and flag authenticationType as required.
* (**gamelift**) Amazon GameLift releases a new game session shutdown feature. Use the Amazon GameLift console or AWS CLI to terminate an in-progress game session that's entered a bad state or is no longer needed.
* (**mediaconnect**) AWS Elemental MediaConnect now supports Content Quality Analysis for enhanced source stream monitoring. This enables you to track specific audio and video metrics in transport stream source flows, ensuring your content meets quality standards.
* (**mediaconvert**) This release adds support for the AVC3 codec and fixes an alignment issue with Japanese vertical captions.
* (**organizations**) Added ALL_FEATURES_MIGRATION_ORGANIZATION_SIZE_LIMIT_EXCEEDED to ConstraintViolationException for the EnableAllFeatures operation.
* (**sagemaker**) Adding ETag information with Model Artifacts for Model Registry

### Documentation
* (**sqs**) In-flight message typo fix from 20k to 120k.

## [1.3.103] - 12/28/2024

### Features
* (**ecr**) Restoring custom endpoint functionality for ECR
* (**ecrpublic**) Restoring custom endpoint functionality for ECR Public

## [1.3.102] - 12/27/2024

### Documentation
* (**rds**) Updates Amazon RDS documentation to correct various descriptions.

## [1.3.101] - 12/26/2024

### Features
* (**bcmpricingcalculator**) Added ConflictException to DeleteBillEstimate.
* (**ecr**) Add support for Dualstack Endpoints

### Documentation
* (**networkfirewall**) Dual-stack endpoints are now supported.
* (**securityhub**) Documentation updates for AWS Security Hub

## [1.3.100] - 12/23/2024

### Features
* (**ecrpublic**) Add support for Dualstack endpoints
* (**eks**) This release adds support for DescribeClusterVersions API that provides important information about Kubernetes versions along with end of support dates
* (**glue**) Add IncludeRoot parameters to GetCatalogs API to return root catalog.

### Documentation
* (**ecr**) Documentation update for ECR GetAccountSetting and PutAccountSetting APIs.

## [1.3.99] - 12/20/2024

### Features
* (**bedrockagent**) Support for custom user agent and max web pages crawled for web connector. Support app only credentials for SharePoint connector. Increase agents memory duration limit to 365 days. Support to specify max number of session summaries to include in agent invocation context.
* (**bedrockagentruntime**) bedrock agents now supports long term memory and performance configs. Invokeflow supports performance configs. RetrieveAndGenerate performance configs
* (**billing**) Added new API's for defining and fetching Billing Views.
* (**connect**) This release supports adding NotAttributeCondition and Range to the RoutingCriteria object.
* (**costexplorer**) Support for retrieving cost, usage, and forecast for billing view.
* (**docdb**) Support AWS Secret Manager managed password for AWS DocumentDB instance-based cluster.
* (**eks**) This release expands the catalog of upgrade insight checks
* (**macie2**) This release adds support for identifying S3 general purpose buckets that exceed the Amazon Macie quota for preventative control monitoring.
* (**outposts**) Add CS8365C as a supported power connector for Outpost sites.
* (**sagemaker**) This release adds support for c6i, m6i and r6i instance on SageMaker Hyperpod and trn1 instances in batch

### Documentation
* (**bedrockdataautomation**) Documentation update for Amazon Bedrock Data Automation
* (**bedrockdataautomationruntime**) Documentation update for Amazon Bedrock Data Automation Runtime

## [1.3.98] - 12/19/2024

### Features
* (**appstream**) Added support for Rocky Linux 8 on Amazon AppStream 2.0
* (**mediaconvert**) This release adds support for inserting timecode tracks into MP4 container outputs.
* (**medialive**) MediaLive is releasing ListVersions api
* (**qconnect**) Amazon Q in Connect enables agents to ask Q for assistance in multiple languages and Q will provide answers and recommended step-by-step guides in those languages. Qs default language is English (United States) and you can switch this by setting the locale configuration on the AI Agent.
* (**ssmsap**) AWS Systems Manager for SAP added support for registration and discovery of distributed ABAP applications
* (**workspaces**) Added AWS Global Accelerator (AGA) support for WorkSpaces Personal.

## [1.3.97] - 12/18/2024

### Features
* (**amplify**) Added WAF Configuration to Amplify Apps
* (**budgets**) Releasing minor partition endpoint updates
* (**connect**) This release adds support for the UpdateParticipantAuthentication API used for customer authentication within Amazon Connect chats.
* (**connectparticipant**) This release adds support for the GetAuthenticationUrl and CancelParticipantAuthentication APIs used for customer authentication within Amazon Connect chats. There are also minor updates to the GetAttachment API.
* (**datasync**) AWS DataSync introduces the ability to update attributes for in-cloud locations.
* (**iot**) Release connectivity status query API which is a dedicated high throughput(TPS) API to query a specific device's most recent connectivity state and metadata.
* (**quicksight**) Add support for PerformanceConfiguration attribute to Dataset entity. Allow PerformanceConfiguration specification in CreateDataset and UpdateDataset APIs.
* (**resiliencehub**) AWS Resilience Hub now automatically detects already configured CloudWatch alarms and FIS experiments as part of the assessment process and returns the discovered resources in the corresponding list API responses. It also allows you to include or exclude test recommendations for an AppComponent.
* (**transfer**) Added AS2 agreement configurations to control filename preservation and message signing enforcement. Added AS2 connector configuration to preserve content type from S3 objects.

### Documentation
* (**mwaa**) Added support for Apache Airflow version 2.10.3 to MWAA.

## [1.3.96] - 12/18/2024

### Features
* (**account**) Update endpoint configuration.
* (**backup**) Add Support for Backup Indexing
* (**backupsearch**) Add support for searching backups
* (**batch**) This feature allows AWS Batch on Amazon EKS to support configuration of Pod Annotations, overriding Namespace on which the Batch job's Pod runs on, and allows Subpath and Persistent Volume claim to be set for AWS Batch on Amazon EKS jobs.
* (**cleanroomsml**) Add support for SQL compute configuration for StartAudienceGenerationJob API.
* (**cloudfront**) Adds support for OriginReadTimeout and OriginKeepaliveTimeout to create CloudFront Distributions with VPC Origins.
* (**codepipeline**) AWS CodePipeline V2 type pipelines now support Managed Compute Rule.
* (**ecs**) Added support for enableFaultInjection task definition parameter which can be used to enable Fault Injection feature on ECS tasks.
* (**m2**) This release adds support for AWS Mainframe Modernization(M2) Service to allow specifying network type(ipv4, dual) for the environment instances. For dual network type, m2 environment applications will serve both IPv4 and IPv6 requests, whereas for ipv4 it will serve only IPv4 requests.
* (**synthetics**) Add support to toggle outbound IPv6 traffic on canaries connected to dualstack subnets.  This behavior can be controlled via the new Ipv6AllowedForDualStack parameter of the VpcConfig input object in CreateCanary and UpdateCanary APIs.

## [1.3.95] - 12/16/2024

### Features
* (**dlm**) This release adds support for Local Zones in Amazon Data Lifecycle Manager EBS snapshot lifecycle policies.
* (**ec2**) This release adds support for EBS local snapshots in AWS Dedicated Local Zones, which allows you to store snapshots of EBS volumes locally in Dedicated Local Zones.
* (**greengrassv2**) Add support for runtime in GetCoreDevice and ListCoreDevices APIs.
* (**medialive**) AWS Elemental MediaLive adds three new features: MediaPackage v2 endpoint support for live stream delivery, KLV metadata passthrough in CMAF Ingest output groups, and Metadata Name Modifier in CMAF Ingest output groups for customizing metadata track names in output streams.
* (**rds**) This release adds support for the "MYSQL_CACHING_SHA2_PASSWORD" enum value for RDS Proxy ClientPasswordAuthType.

### Documentation
* (**cloud9**) Added information about Ubuntu 18.04 will be removed from the available imageIds for Cloud9 because Ubuntu 18.04 has ended standard support on May 31, 2023.

## [1.3.94] - 12/13/2024

### Features
* (**cloudhsmv2**) Add support for Dual-Stack hsm2m.medium clusters. The customers will now be able to create hsm2m.medium clusters having both IPv4 and IPv6 connection capabilities by specifying a new param called NetworkType=DUALSTACK during cluster creation.
* (**cloudwatchlogs**) Limit PutIntegration IntegrationName and ListIntegrations IntegrationNamePrefix parameters to 50 characters
* (**ec2**) This release adds GroupId to the response for DeleteSecurityGroup.
* (**eks**) Add NodeRepairConfig in CreateNodegroupRequest and UpdateNodegroupConfigRequest
* (**mediaconnect**) AWS Elemental MediaConnect Gateway now supports Source Specific Multicast (SSM) for ingress bridges. This enables you to specify a source IP address in addition to a multicast IP when creating or updating an ingress bridge source.
* (**servicediscovery**) AWS Cloud Map now supports service-level attributes, allowing you to associate custom metadata directly with services. These attributes can be retrieved, updated, and deleted using the new GetServiceAttributes, UpdateServiceAttributes, and DeleteServiceAttributes API calls.

### Documentation
* (**networkmanager**) There was a sentence fragment in UpdateDirectConnectGatewayAttachment that was causing customer confusion as to whether it's an incomplete sentence or if it was a typo. Removed the fragment.

## [1.3.93] - 12/12/2024

### Features
* (**connect**) Configure holidays and other overrides to hours of operation in advance. During contact handling, Amazon Connect automatically checks for overrides and provides customers with an appropriate flow path. After an override period passes call center automatically reverts to standard hours of operation.
* (**databasemigrationservice**) Add parameters to support for kerberos authentication. Add parameter for disabling the Unicode source filter with PostgreSQL settings. Add parameter to use large integer value with Kinesis/Kafka settings.
* (**glue**) To support customer-managed encryption in Data Quality to allow customers encrypt data with their own KMS key, we will add a DataQualityEncryption field to the SecurityConfiguration API where customers can provide their KMS keys.
* (**route53domains**) This release includes the following API updates: added the enumeration type RESTORE_DOMAIN to the OperationType; constrained the Price attribute to non-negative values; updated the LangCode to allow 2 or 3 alphabetical characters.

### Documentation
* (**guardduty**) Improved descriptions for certain APIs.

## [1.3.92] - 12/11/2024

### Features
* (**artifact**) Add support for listing active customer agreements for the calling AWS Account.
* (**emrserverless**) This release adds support for accessing system profile logs in Lake Formation-enabled jobs.
* (**migrationhub**) API and documentation updates for AWS MigrationHub related to adding support for listing migration task updates and associating, disassociating and listing source resources
* (**sesv2**) Introduces support for multi-region endpoint.
* (**timestreaminfluxdb**) Adds networkType parameter to CreateDbInstance API which allows IPv6 support to the InfluxDB endpoint

### Documentation
* (**cloudtrail**) Doc-only updates for CloudTrail.
* (**cognitoidentityprovider**) Updated descriptions for some API operations and parameters, corrected some errors in Cognito user pools
* (**controlcatalog**) Minor documentation updates to the content of ImplementationDetails object part of the Control Catalog GetControl API

## [1.3.91] - 12/10/2024

### Features
* (**bcmpricingcalculator**) Updated condition key inference from Workload Estimate, Bill Scenario, and Bill Estimate resources. Updated documentation links.
* (**connect**) Add support for Push Notifications for Amazon Connect chat. With Push Notifications enabled an alert could be sent to customers about new messages even when they aren't actively using the mobile application.
* (**finspace**) Update KxCommandLineArgument value parameter regex to allow for spaces and semicolons
* (**ivsrealtime**) IVS Real-Time now offers customers the ability to customize thumbnails recording mode and interval for both Individual Participant Recording (IPR) and Server-Side Compositions (SSC).
* (**sesv2**) Introduces support for creating DEED (Deterministic Easy-DKIM) identities.

### Documentation
* (**applicationautoscaling**) Doc only update for AAS Predictive Scaling policy configuration API.
* (**dsql**) Doc only update to examples for DeleteMultiRegionClusters & CreateMultiRegionClusters

## [1.3.90] - 12/09/2024

### Features
* (**cognitoidentityprovider**) Change `CustomDomainConfig` from a required to an optional parameter for the `UpdateUserPoolDomain` operation.
* (**ec2**) This release includes a new API for modifying instance network-performance-options after launch.
* (**medialive**) H265 outputs now support disabling the deblocking filter.

### Documentation
* (**appsync**) Provides description of new Amazon Bedrock runtime datasource.
* (**ecs**) This is a documentation only update to address various tickets for Amazon ECS.
* (**keyspaces**) Amazon Keyspaces: adding the list of IAM actions required by the UpdateKeyspace API.
* (**workspaces**) Added text to clarify case-sensitivity

## [1.3.89] - 12/05/2024

### Features
* (**partnercentralselling**) Introducing the preview of new partner central selling APIs designed to transform how AWS partners collaborate and co-sell with multiple partners. This enables multiple partners to seamlessly engage and jointly pursue customer opportunities, fostering a new era of collaborative selling.
* (**qbusiness**) This release removes the deprecated UserId and UserGroups fields from SearchRelevantContent api's request parameters.

## [1.3.88] - 12/04/2024

### Features
* (**bedrock**) Introduced two APIs ListPromptRouters and GetPromptRouter for Intelligent Prompt Router feature. Add support for Bedrock Guardrails image content filter. New Bedrock Marketplace feature enabling a wider range of bedrock compatible models with self-hosted capability.
* (**bedrockagent**) This release introduces the ability to generate SQL using natural language, through a new GenerateQuery API (with native integration into Knowledge Bases); ability to ingest and retrieve images through Bedrock Data Automation; and ability to create a Knowledge Base backed by Kendra GenAI Index.
* (**bedrockagentruntime**) This release introduces the ability to generate SQL using natural language, through a new GenerateQuery API (with native integration into Knowledge Bases); ability to ingest and retrieve images through Bedrock Data Automation; and ability to create a Knowledge Base backed by Kendra GenAI Index.
* (**bedrockdataautomation**) Release Bedrock Data Automation SDK
* (**bedrockdataautomationruntime**) Release Bedrock Data Automation Runtime SDK
* (**bedrockruntime**) Added support for Intelligent Prompt Router in Invoke, InvokeStream, Converse and ConverseStream. Add support for Bedrock Guardrails image content filter. New Bedrock Marketplace feature enabling a wider range of bedrock compatible models with self-hosted capability.
* (**kendra**) This release adds GenAI Index in Amazon Kendra for Retrieval Augmented Generation (RAG) and intelligent search. With the Kendra GenAI Index, customers get high retrieval accuracy powered by the latest information retrieval technologies and semantic models.
* (**sagemaker**) Amazon SageMaker HyperPod launched task governance to help customers maximize accelerator utilization for model development and flexible training plans to meet training timelines and budget while reducing weeks of training time. AI apps from AWS partner is now available in SageMaker.

## [1.3.87] - 12/03/2024

### Features
* (**athena**) Add FEDERATED type to CreateDataCatalog. This creates Athena Data Catalog, AWS Lambda connector, and AWS Glue connection. Create/DeleteDataCatalog returns DataCatalog. Add Status, ConnectionType, and Error to DataCatalog and DataCatalogSummary. Add DeleteCatalogOnly to delete Athena Catalog only.
* (**bedrock**) Tagging support for Async Invoke resources. Added support for Distillation in CreateModelCustomizationJob API. Support for videoDataDeliveryEnabled flag in invocation logging.
* (**bedrockagent**) Releasing SDK for Multi-Agent Collaboration.
* (**bedrockagentruntime**) Releasing SDK for multi agent collaboration
* (**bedrockruntime**) Added support for Async Invoke Operations Start, List and Get. Support for invocation logs with `requestMetadata` field in Converse, ConverseStream, Invoke and InvokeStream. Video content blocks in Converse/ConverseStream accept raw bytes or S3 URI.
* (**datazone**) Adds support for Connections, ProjectProfiles, and JobRuns APIs. Supports the new Lineage feature at GA. Adjusts optionality of a parameter for DataSource and SubscriptionTarget APIs which may adjust types in some clients.
* (**dsql**) Add new API operations for Amazon Aurora DSQL. Amazon Aurora DSQL is a serverless, distributed SQL database with virtually unlimited scale, highest availability, and zero infrastructure management.
* (**dynamodb**) This change adds support for global tables with multi-Region strong consistency (in preview). The UpdateTable API now supports a new attribute MultiRegionConsistency to set consistency when creating global tables. The DescribeTable output now optionally includes the MultiRegionConsistency attribute.
* (**glue**) This release includes(1)Zero-ETL integration to ingest data from 3P SaaS and DynamoDB to Redshift/Redlake (2)new properties on Connections to enable reuse; new connection APIs for retrieve/preview metadata (3)support of CRUD operations for Multi-catalog (4)support of automatic statistics collections
* (**lakeformation**) This release added two new LakeFormation Permissions (CREATE_CATALOG, SUPER_USER) and added Id field for CatalogResource. It also added new conditon and expression field.
* (**qapps**) Add support for 11 new plugins as action cards to help automate repetitive tasks and improve productivity.
* (**qbusiness**) Amazon Q Business now supports customization options for your web experience, 11 new Plugins, and QuickSight support. Amazon Q index allows software providers to enrich their native generative AI experiences with their customer's enterprise knowledge and user context spanning multiple applications.
* (**quicksight**) This release includes API needed to support for Unstructured Data in Q in QuickSight Q&A (IDC).
* (**redshift**) Adds support for Amazon Redshift RegisterNamespace and DeregisterNamespace APIs to share data to AWS Glue Data Catalog.
* (**redshiftserverless**) Adds support for the ListManagedWorkgroups API to get an overview of existing managed workgroups.
* (**s3**) Amazon S3 Metadata stores object metadata in read-only, fully managed Apache Iceberg metadata tables that you can query. You can create metadata table configurations for S3 general purpose buckets.
* (**s3tables**) Amazon S3 Tables deliver the first cloud object store with built-in open table format support, and the easiest way to store tabular data at scale.

### Documentation
* (**cloudwatch**) Support for configuring AiOps investigation as alarm action

## [1.3.86] - 12/03/2024

### Features
* (**bedrockruntime**) Add an API parameter that allows customers to set performance configuration for invoking a model.
* (**s3control**) It allows customers to pass CRC64NVME as a header in S3 Batch Operations copy requests
* (**socialmessaging**) Added support for passing role arn corresponding to the supported event destination

## [1.3.85] - 12/02/2024

### Features
* (**bedrock**) Add support for Knowledge Base Evaluations & LLM as a judge
* (**bedrockagent**) This release introduces APIs to upload documents directly into a Knowledge Base
* (**bedrockagentruntime**) This release introduces a new Rerank API to leverage reranking models (with integration into Knowledge Bases); APIs to upload documents directly into Knowledge Base; RetrieveAndGenerateStream API for streaming response; Guardrails on Retrieve API; and ability to automatically generate filters
* (**chimesdkvoice**) This release adds supports for enterprises to integrate Amazon Connect with other voice systems. It supports directly transferring voice calls and metadata without using the public telephone network. It also supports real-time and post-call analytics.
* (**cleanrooms**) This release allows customers and their partners to easily collaborate with data stored in Snowflake and Amazon Athena, without having to move or share their underlying data among collaborators.
* (**cloudwatchlogs**) Adds PutIntegration, GetIntegration, ListIntegrations and DeleteIntegration APIs. Adds QueryLanguage support to StartQuery, GetQueryResults, DescribeQueries, DescribeQueryDefinitions, and PutQueryDefinition APIs.
* (**connect**) Adds support for WhatsApp Business messaging, IVR call recording, enabling Contact Lens for existing on-premise contact centers and telephony platforms, and enabling telephony and IVR migration to Amazon Connect independent of their contact center agents.
* (**connectcampaignsv2**) Amazon Connect Outbound Campaigns V2 / Features : Adds support for Event-Triggered Campaigns.
* (**customerprofiles**) This release introduces Event Trigger APIs as part of Amazon Connect Customer Profiles service.
* (**ec2**) Adds support for declarative policies that allow you to enforce desired configuration across an AWS organization through configuring account attributes. Adds support for Allowed AMIs that allows you to limit the use of AMIs in AWS accounts. Adds support for connectivity over non-HTTP protocols.
* (**eks**) Added support for Auto Mode Clusters, Hybrid Nodes, and specifying computeTypes in the DescribeAddonVersions API.
* (**eventbridge**) Call private APIs by configuring Connections with VPC connectivity through PrivateLink and VPC Lattice
* (**fsx**) FSx API changes to support the public launch of the Amazon FSx Intelligent Tiering for OpenZFS storage class.
* (**guardduty**) Add new Multi Domain Correlation findings.
* (**imagebuilder**) Added support for EC2 Image Builder's integration with AWS Marketplace for Marketplace components.
* (**invoicing**) AWS Invoice Configuration allows you to receive separate AWS invoices based on your organizational needs. You can use the AWS SDKs to manage Invoice Units and programmatically fetch the information of the invoice receiver.
* (**memorydb**) Amazon MemoryDB SDK now supports all APIs for Multi-Region. Please refer to the updated Amazon MemoryDB public documentation for detailed information on API usage.
* (**networkflowmonitor**) This release adds documentation for a new feature in Amazon CloudWatch called Network Flow Monitor. You can use Network Flow Monitor to get near real-time metrics, including retransmissions and data transferred, for your actual workloads.
* (**opensearch**) This feature introduces support for CRUDL APIs, enabling the creation and management of Connected data sources.
* (**organizations**) Add support for policy operations on the DECLARATIVE_POLICY_EC2 policy type.
* (**qbusiness**) Amazon Q Business now supports capabilities to extract insights and answer questions from visual elements embedded within documents, a browser extension for Google Chrome, Mozilla Firefox, and Microsoft Edge, and attachments across conversations.
* (**qconnect**) This release adds following capabilities: Configuring safeguards via AIGuardrails for Q in Connect inferencing, and APIs to support Q&A self-service use cases
* (**rds**) Amazon RDS supports CloudWatch Database Insights. You can use the SDK to create, modify, and describe the DatabaseInsightsMode for your DB instances and clusters.
* (**s3**) Amazon S3 introduces support for AWS Dedicated Local Zones
* (**s3control**) Amazon S3 introduces support for AWS Dedicated Local Zones
* (**securityhub**) Add new Multi Domain Correlation findings.
* (**securityir**) AWS Security Incident Response is a purpose-built security incident solution designed to help customers prepare for, respond to, and recover from security incidents.
* (**transfer**) AWS Transfer Family now offers Web apps that enables simple and secure access to data stored in Amazon S3.
* (**vpclattice**) Lattice APIs that allow sharing and access of VPC resources across accounts.

### Documentation
* (**ecs**) This release adds support for Container Insights with Enhanced Observability for Amazon ECS.

## [1.3.84] - 11/27/2024

### Features
* (**bedrockagent**) Add support for specifying embeddingDataType, either FLOAT32 or BINARY
* (**configservice**) AWS Config adds support for service-linked recorders, a new type of Config recorder managed by AWS services to record specific subsets of resource configuration data and functioning independently from customer managed AWS Config recorders.
* (**fsx**) This release adds EFA support to increase FSx for Lustre file systems' throughput performance to a single client instance. This can be done by specifying EfaEnabled=true at the time of creation of Persistent_2 file systems.
* (**observabilityadmin**) Amazon CloudWatch Observability Admin adds the ability to audit telemetry configuration for AWS resources in customers AWS Accounts and Organizations. The release introduces new APIs to turn on/off the new experience, which supports discovering supported AWS resources and their state of telemetry.

## [1.3.83] - 11/26/2024

### Features
* (**bedrockagent**) Custom Orchestration API release for AWSBedrockAgents.
* (**bedrockagentruntime**) Custom Orchestration and Streaming configurations API release for AWSBedrockAgents.
* (**connect**) Enables access to ValueMap and ValueInteger types for SegmentAttributes and fixes deserialization bug for DescribeContactFlow in AmazonConnect Public API
* (**ec2**) Adds support for Time-based Copy for EBS Snapshots and Cross Region PrivateLink. Time-based Copy ensures that EBS Snapshots are copied within and across AWS Regions in a specified timeframe. Cross Region PrivateLink enables customers to connect to VPC endpoint services hosted in other AWS Regions.
* (**qapps**) Private sharing, file upload and data collection feature support for Q Apps

## [1.3.82] - 11/25/2024

### Features
* (**directconnect**) Update DescribeDirectConnectGatewayAssociations API to return associated core network information if a Direct Connect gateway is attached to a Cloud WAN core network.
* (**networkmanager**) This release adds native Direct Connect integration on Cloud WAN enabling customers to directly attach their Direct Connect gateways to Cloud WAN without the need for an intermediate Transit Gateway.
* (**s3**) Amazon Simple Storage Service / Features: Add support for ETag based conditional writes in PutObject and CompleteMultiPartUpload APIs to prevent unintended object modifications.

## [1.3.81] - 11/22/2024

### Features
* (**autoscaling**) Now, Amazon EC2 Auto Scaling customers can enable target tracking policies to take quicker scaling decisions, enhancing their application performance and EC2 utilization. To get started, specify target tracking to monitor a metric that is available on Amazon CloudWatch at seconds-level interval.
* (**bcmpricingcalculator**) Initial release of the AWS Billing and Cost Management Pricing Calculator API.
* (**bedrockagentruntime**) InvokeInlineAgent API release to help invoke runtime agents without any dependency on preconfigured agents.
* (**chatbot**) Adds support for programmatic management of custom actions and aliases which can be associated with channel configurations.
* (**codepipeline**) AWS CodePipeline V2 type pipelines now support ECRBuildAndPublish and InspectorScan actions.
* (**cognitoidentityprovider**) Add support for users to sign up and sign in without passwords, using email and SMS OTPs and Passkeys. Add support for Passkeys based on WebAuthn. Add support for enhanced branding customization for hosted authentication pages with Amazon Cognito Managed Login. Add feature tiers with new pricing.
* (**connect**) Amazon Connect Service Feature: Add APIs for Amazon Connect Email Channel
* (**costexplorer**) This release adds the Impact field(contains Contribution field) to the GetAnomalies API response under RootCause
* (**elasticloadbalancingv2**) This release adds support for advertising trusted CA certificate names in associated trust stores.
* (**emr**) Advanced Scaling in Amazon EMR Managed Scaling
* (**inspector2**) Extend inspector2 service model to include ServiceQuotaExceededException.
* (**lambda**) Add ProvisionedPollerConfig to Lambda event-source-mapping API.
* (**mailmanager**) Added new "DeliverToQBusiness" rule action to MailManager RulesSet for ingesting email data into Amazon Q Business customer applications
* (**neptunegraph**) Add 4 new APIs to support new Export features, allowing Parquet and CSV formats. Add new arguments in Import APIs to support Parquet import. Add a new query "neptune.read" to run algorithms without loading data into database
* (**omics**) This release adds support for resource policy based cross account S3 access to sequence store read sets.
* (**quicksight**) This release includes: Update APIs to support Image, Layer Map, font customization, and Plugin Visual. Add Identity center related information in ListNamsespace API. Update API for restrictedFolder support in topics and add API for SearchTopics, Describe/Update DashboardsQA Configration.
* (**sagemaker**) This release adds APIs for new features for SageMaker endpoint to scale down to zero instances, native support for multi-adapter inference, and endpoint scaling improvements.
* (**ses**) This release adds support for starting email contacts in your Amazon Connect instance as an email receiving action.
* (**sfn**) Add support for variables and JSONata in TestState, GetExecutionHistory, DescribeStateMachine, and DescribeStateMachineForExecution

### Documentation
* (**sns**) ArchivePolicy attribute added to Archive and Replay feature
* (**workspaces**) While integrating WSP-DCV rebrand, a few mentions were erroneously renamed from WSP to DCV. This release reverts those mentions back to WSP.

## [1.3.80] - 11/21/2024

### Features
* (**apigateway**) Added support for custom domain names for private APIs.
* (**applicationautoscaling**) Application Auto Scaling now supports Predictive Scaling to proactively increase the desired capacity ahead of predicted demand, ensuring improved availability and responsiveness for customers' applications. This feature is currently only made available for Amazon ECS Service scalable targets.
* (**appsync**) Add support for the Amazon Bedrock Runtime.
* (**cloudfront**) Adds support for Origin Selection between EMPv2 origins based on media quality score.
* (**cloudtrail**) This release introduces new APIs for creating and managing CloudTrail Lake dashboards. It also adds support for resource-based policies on CloudTrail EventDataStore and Dashboard resource.
* (**cloudwatchlogs**) Adds "Create field indexes to improve query performance and reduce scan volume" and "Transform logs during ingestion". Updates documentation for "PutLogEvents with Entity".
* (**costexplorer**) This release introduces three new APIs that enable you to estimate the cost, coverage, and utilization impact of Savings Plans you plan to purchase. The three APIs are StartCommitmentPurchaseAnalysis, GetCommitmentPurchaseAnalysis, and ListCommitmentPurchaseAnalyses.
* (**ec2**) Adds support for requesting future-dated Capacity Reservations with a minimum commitment duration, enabling IPAM for organizational units within AWS Organizations, reserving EC2 Capacity Blocks that start in 30 minutes, and extending the end date of existing Capacity Blocks.
* (**elasticache**) Added support to modify the engine type for existing ElastiCache Users and User Groups. Customers can now modify the engine type from redis to valkey.
* (**health**) Adds metadata property to an AffectedEntity.
* (**iot**) General Availability (GA) release of AWS IoT Device Management - Commands, to trigger light-weight remote actions on targeted devices
* (**iotfleetwise**) AWS IoT FleetWise now includes campaign parameters to store and forward data, configure MQTT topic as a data destination, and collect diagnostic trouble code data. It includes APIs for network agnostic data collection using custom decoding interfaces, and monitoring the last known state of vehicles.
* (**iotjobsdataplane**) General Availability (GA) release of AWS IoT Device Management - Commands, to trigger light-weight remote actions on targeted devices
* (**lambda**) Adds support for metrics for event source mappings for AWS Lambda
* (**notifications**) This release adds support for AWS User Notifications. You can now configure and view notifications from AWS services in a central location using the AWS SDK.
* (**notificationscontacts**) This release adds support for AWS User Notifications Contacts. You can now configure and view email contacts for AWS User Notifications using the AWS SDK.
* (**resiliencehub**) AWS Resilience Hub's new summary view visually represents applications' resilience through charts, enabling efficient resilience management. It provides a consolidated view of the app portfolio's resilience state and allows data export for custom stakeholder reporting.
* (**s3**) Add support for conditional deletes for the S3 DeleteObject and DeleteObjects APIs. Add support for write offset bytes option used to append to objects with the S3 PutObject API.
* (**ssm**) Added support for providing high-level overviews of managed nodes and previewing the potential impact of a runbook execution.
* (**ssmquicksetup**) Add methods that retrieve details about deployed configurations: ListConfigurations, GetConfiguration
* (**xray**) AWS X-Ray introduces Transaction Search APIs, enabling span ingestion into CloudWatch Logs for high-scale trace data indexing. These APIs support span-level queries, trace graph generation, and metric correlation for deeper application insights.

### Documentation
* (**elasticloadbalancingv2**) This feature adds support for enabling zonal shift on cross-zone enabled Application Load Balancer, as well as modifying HTTP request and response headers.

## [1.3.79] - 11/20/2024

### Features
* (**applicationdiscoveryservice**) Add support to import data from commercially available discovery tools without file manipulation.
* (**autoscaling**) With this release, customers can prioritize launching instances into ODCRs using targets from ASGs or Launch Templates. Customers can express their baseline instances' CPU-performance in attribute-based Instance Requirements configuration by referencing an instance family that meets their needs.
* (**bedrockagentruntime**) Releasing new Prompt Optimization to enhance your prompts for improved performance
* (**cloudfront**) Add support for gRPC, VPC origins, and Anycast IP Lists. Allow LoggingConfig IncludeCookies to be set regardless of whether the LoggingConfig is enabled.
* (**computeoptimizer**) This release enables AWS Compute Optimizer to analyze and generate optimization recommendations for Amazon Aurora database instances. It also enables Compute Optimizer to identify idle Amazon EC2 instances, Amazon EBS volumes, Amazon ECS services running on Fargate, and Amazon RDS databases.
* (**controltower**) Adds support for child enabled baselines which allow you to see the enabled baseline status for individual accounts.
* (**costoptimizationhub**) This release adds action type "Delete" to the GetRecommendation, ListRecommendations and ListRecommendationSummaries APIs to support new EBS and ECS recommendations with action type "Delete".
* (**datazone**) This release supports Metadata Enforcement Rule feature for Create Subscription Request action.
* (**ec2**) With this release, customers can express their desire to launch instances only in an ODCR or ODCR group rather than OnDemand capacity. Customers can express their baseline instances' CPU-performance in attribute-based Instance Requirements configuration by referencing an instance family.
* (**ecs**) This release adds support for the Availability Zone rebalancing feature on Amazon ECS.
* (**elasticloadbalancingv2**) This release adds support for configuring Load balancer Capacity Unit reservations
* (**lambda**) Add Node 22.x (node22.x) support to AWS Lambda
* (**mediaconvert**) This release adds the ability to reconfigure concurrent job settings for existing queues and create queues with custom concurrent job settings.
* (**mediapackagev2**) MediaPackage v2 now supports the Media Quality Confidence Score (MQCS) published from MediaLive. Customers can control input switching based on the MQCS and publishing HTTP Headers for the MQCS via the API.
* (**omics**) Enabling call caching feature that allows customers to reuse previously computed results from a set of completed tasks in a new workflow run.
* (**rbin**) This release adds support for exclusion tags for Recycle Bin, which allows you to identify resources that are to be excluded, or ignored, by a Region-level retention rule.
* (**rds**) This release adds support for scale storage on the DB instance using a Blue/Green Deployment.
* (**timestreamquery**) This release adds support for Provisioning Timestream Compute Units (TCUs), a new feature that allows provisioning dedicated compute resources for your queries, providing predictable and cost-effective query performance.
* (**workspaces**) Added support for Rocky Linux 8 on Amazon WorkSpaces Personal.
* (**workspacesweb**) Added data protection settings with support for inline data redaction.
* [#1440](https://github.com/awslabs/aws-sdk-kotlin/issues/1440) Include Maven Bill of Materials (BOM) in version catalog

## [1.3.78] - 11/19/2024

### Features
* (**b2bi**) Add new X12 transactions sets and versions
* (**ec2**) This release adds VPC Block Public Access (VPC BPA), a new declarative control which blocks resources in VPCs and subnets that you own in a Region from reaching or being reached from the internet through internet gateways and egress-only internet gateways.
* (**ecs**) This release introduces support for configuring the version consistency feature for individual containers defined within a task definition. The configuration allows to specify whether ECS should resolve the container image tag specified in the container definition to an image digest.
* (**efs**) Add support for the new parameters in EFS replication APIs
* (**glue**) AWS Glue Data Catalog now enhances managed table optimizations of Apache Iceberg tables that can be accessed only from a specific Amazon Virtual Private Cloud (VPC) environment.
* (**keyspaces**) Amazon Keyspaces Multi-Region Replication: Adds support to add new regions to multi and single-region keyspaces.
* (**mwaa**) Amazon MWAA now supports a new environment class, mw1.micro, ideal for workloads requiring fewer resources than mw1.small. This class supports a single instance of each Airflow component: Scheduler, Worker, and Webserver.
* (**taxsettings**) Release Tax Inheritance APIs,  Tax Exemption APIs, and functionality update for some existing Tax Registration APIs
* (**workspaces**) Releasing new ErrorCodes for Image Validation failure during CreateWorkspaceImage process

## [1.3.77] - 11/18/2024

### Features
* (**appconfig**) AWS AppConfig has added a new extension action point, AT_DEPLOYMENT_TICK, to support third-party monitors to trigger an automatic rollback during a deployment.
* (**autoscaling**) Amazon EC2 Auto Scaling now supports Amazon Application Recovery Controller (ARC) zonal shift and zonal autoshift to help you quickly recover an impaired application from failures in an Availability Zone (AZ).
* (**cloudformation**) This release adds a new API, ListHookResults, that allows retrieving CloudFormation Hooks invocation results for hooks invoked during a create change set operation or Cloud Control API operation
* (**connect**) Adds CreateContactFlowVersion and ListContactFlowVersions APIs to create and view the versions of a contact flow.
* (**customerprofiles**) This release introduces Segmentation APIs and new Calculated Attribute Event Filters as part of Amazon Connect Customer Profiles service.
* (**ec2**) Adding request and response elements for managed resources.
* (**ecs**) This release adds support for adding VPC Lattice configurations in ECS CreateService/UpdateService APIs. The configuration allows for associating VPC Lattice target groups with ECS Services.
* (**iotsitewise**) The release introduces a generative AI Assistant in AWS IoT SiteWise. It includes: 1) InvokeAssistant API - Invoke the Assistant to get alarm summaries and ask questions. 2) Dataset APIs - Manage knowledge base configuration for the Assistant. 3) Portal APIs enhancement - Manage AI-aware dashboards.
* (**qconnect**) This release introduces MessageTemplate as a resource in Amazon Q in Connect, along with APIs to create, read, search, update, and delete MessageTemplate resources.
* (**rds**) Add support for the automatic pause/resume feature of Aurora Serverless v2.
* (**rdsdata**) Add support for the automatic pause/resume feature of Aurora Serverless v2.

## [1.3.76] - 11/15/2024

### Features
* (**cloudwatch**) Adds support for adding related Entity information to metrics ingested through PutMetricData.
* (**connectcampaignsv2**) Added Amazon Connect Outbound Campaigns V2 SDK.
* (**ec2**) Remove non-functional enum variants for FleetCapacityReservationUsageStrategy
* (**iot**) This release allows AWS IoT Core users to enrich MQTT messages with propagating attributes, to associate a thing to a connection, and to enable Online Certificate Status Protocol (OCSP) stapling for TLS X.509 server certificates through private endpoints.
* (**outposts**) You can now purchase AWS Outposts rack or server capacity for a 5-year term with one of  the following payment options: All Upfront, Partial Upfront, and No Upfront.
* (**pinpointsmsvoicev2**) Use rule overrides to always allow or always block messages to specific phone numbers. Use message feedback to monitor if a customer interacts with your message.
* (**route53resolver**) Route 53 Resolver DNS Firewall Advanced Rules allows you to monitor and block suspicious DNS traffic based on anomalies detected in the queries, such as DNS tunneling and Domain Generation Algorithms (DGAs).

### Documentation
* (**datasync**) Doc-only updates and enhancements related to creating DataSync tasks and describing task executions.
* (**polly**) Fixes PutLexicon usage example.

## [1.3.75] - 11/15/2024

### Features
* (**partnercentralselling**) Announcing AWS Partner Central API for Selling: This service launch Introduces new APIs for co-selling opportunity management and related functions. Key features include notifications, a dynamic sandbox for testing, and streamlined validations.

### Fixes
* Fix concurrency issues with S3 Express credential caching

## [1.3.74] - 11/14/2024

### Features
* (**accessanalyzer**) Expand analyzer configuration capabilities for unused access analyzers. Unused access analyzer configurations now support the ability to exclude accounts and resource tags from analysis providing more granular control over the scope of analysis.
* (**cloudcontrol**) Added support for CloudFormation Hooks with Cloud Control API. The GetResourceRequestStatus API response now includes an optional HooksProgressEvent and HooksRequestToken parameter for Hooks Invocation Progress as part of resource operation with Cloud Control.
* (**deadline**) Adds support for select GPU accelerated instance types when creating new service-managed fleets.
* (**iam**) This release includes support for five new APIs and changes to existing APIs that give AWS Organizations customers the ability to use temporary root credentials, targeted to member accounts in the organization.
* (**iotwireless**) New FuotaTask resource type to enable logging for your FUOTA tasks. A ParticipatingGatewaysforMulticast parameter to choose the list of gateways to receive the multicast downlink message and the transmission interval between them. Descriptor field which will be sent to devices during FUOTA transfer.
* (**ivs**) IVS now offers customers the ability to stream multitrack video to Channels.
* (**licensemanagerusersubscriptions**) New and updated API operations to support License Included User-based Subscription of Microsoft Remote Desktop Services (RDS).
* (**partnercentralselling**) Announcing AWS Partner Central API for Selling: This service launch Introduces new APIs for co-selling opportunity management and related functions. Key features include notifications, a dynamic sandbox for testing, and streamlined validations.
* (**quicksight**) This release adds APIs for Custom Permissions management in QuickSight, and APIs to support QuickSight Branding.
* (**redshift**) Adds support for Amazon Redshift S3AccessGrants
* (**s3**) This release updates the ListBuckets API Reference documentation in support of the new 10,000 general purpose bucket default quota on all AWS accounts. To increase your bucket quota from 10,000 to up to 1 million buckets, simply request a quota increase via Service Quotas.
* (**sagemaker**) Add support for Neuron instance types [ trn1/trn1n/inf2 ] on SageMaker Notebook Instances Platform.
* (**sts**) This release introduces the new API 'AssumeRoot', which returns short-term credentials that you can use to perform privileged tasks.

## [1.3.73] - 11/13/2024

### Features
* (**accessanalyzer**) This release adds support for policy validation and external access findings for resource control policies (RCP). IAM Access Analyzer helps you author functional and secure RCPs and awareness that a RCP may restrict external access. Updated service API, documentation, and paginators.
* (**applicationsignals**) Amazon CloudWatch Application Signals now supports creating Service Level Objectives with burn rates. Users can now create or update SLOs with burn rate configurations to meet their specific business requirements.
* (**b2bi**) This release adds a GenerateMapping API to allow generation of JSONata or XSLT transformer code based on input and output samples.
* (**billing**) Today, AWS announces the general availability of ListBillingViews API in the AWS SDKs, to enable AWS Billing Conductor (ABC) users to create proforma Cost and Usage Reports (CUR) programmatically.
* (**cloudtrail**) This release adds a new API GenerateQuery that generates a query from a natural language prompt about the event data in your event data store. This operation uses generative artificial intelligence (generative AI) to produce a ready-to-use SQL query from the prompt.
* (**dynamodb**) This release includes supports the new WarmThroughput feature for DynamoDB. You can now provide an optional WarmThroughput attribute for CreateTable or UpdateTable APIs to pre-warm your table or global secondary index. You can also use DescribeTable to see the latest WarmThroughput value.
* (**ec2**) This release adds the source AMI details in DescribeImages API
* (**internetmonitor**) Add new query type Routing_Suggestions regarding querying interface
* (**mediaconvert**) This release adds support for ARN inputs in the Kantar credentials secrets name field and the MSPR field to the manifests for PlayReady DRM protected outputs.
* (**organizations**) Add support for policy operations on the Resource Control Polices.

## [1.3.72] - 11/12/2024

### Features
* (**codebuild**) AWS CodeBuild now supports non-containerized Linux and Windows builds on Reserved Capacity.
* (**controltower**) Added ResetEnabledControl API.
* (**fis**) This release adds support for generating experiment reports with the experiment report configuration
* (**gamelift**) Amazon GameLift releases container fleets support for general availability. Deploy Linux-based containerized game server software for hosting on Amazon GameLift.
* (**paymentcryptography**) Updated ListAliases API with KeyArn filter.

### Documentation
* (**rds**) Updates Amazon RDS documentation for Amazon RDS Extended Support for Amazon Aurora MySQL.

## [1.3.71] - 11/11/2024

### Features
* (**cloudfront**) No API changes from previous release. This release migrated the model to Smithy keeping all features unchanged.
* (**inspector2**) Adds support for filePath filter.
* (**lambda**) Add Python 3.13 (python3.13) support to AWS Lambda
* (**opensearch**) Adds Support for new AssociatePackages and DissociatePackages API in Amazon OpenSearch Service that allows association and dissociation operations to be carried out on multiple packages at the same time.
* (**outposts**) This release updates StartCapacityTask to allow an active Outpost to be modified. It also adds a new API to list all running EC2 instances on the Outpost.

## [1.3.70] - 11/08/2024

### Features
* (**batch**) This feature allows override LaunchTemplates to be specified in an AWS Batch Compute Environment.
* (**bedrockagentruntime**) This release adds trace functionality to Bedrock Prompt Flows
* (**chimesdkmediapipelines**) Added support for Media Capture Pipeline and Media Concatenation Pipeline for customer managed server side encryption. Now Media Capture Pipeline can use IAM sink role to get access to KMS key and encrypt/decrypt recorded artifacts. KMS key ID can also be supplied with encryption context.
* (**controlcatalog**) AWS Control Catalog GetControl public API returns additional data in output, including Implementation and Parameters
* (**eks**) Adds new error code `Ec2InstanceTypeDoesNotExist` for Amazon EKS managed node groups
* (**firehose**) Amazon Data Firehose / Features : Adds support for a new DeliveryStreamType, DatabaseAsSource. DatabaseAsSource hoses allow customers to stream CDC events from their RDS and Amazon EC2 hosted databases, running MySQL and PostgreSQL database engines, to Iceberg Table destinations.
* (**lambda**) This release adds support for using AWS KMS customer managed keys to encrypt AWS Lambda .zip deployment packages.
* (**pinpointsmsvoicev2**) Added the RequiresAuthenticationTimestamp field to the RegistrationVersionStatusHistory data type.
* (**qbusiness**) Adds S3 path option to pass group member list for PutGroup API.

## [1.3.69] - 11/07/2024

### Features
* (**autoscaling**) Auto Scaling groups now support the ability to strictly balance instances across Availability Zones by configuring the AvailabilityZoneDistribution parameter. If balanced-only is configured for a group, launches will always be attempted in the under scaled Availability Zone even if it is unhealthy.
* (**bedrockagent**) Add prompt support for chat template configuration and agent generative AI resource. Add support for configuring an optional guardrail in Prompt and Knowledge Base nodes in Prompt Flows. Add API to validate flow definition
* (**bedrockruntime**) Add Prompt management support to Bedrock runtime APIs: Converse, ConverseStream, InvokeModel, InvokeModelWithStreamingResponse
* (**cleanrooms**) This release introduces support for Custom Models in AWS Clean Rooms ML.
* (**cleanroomsml**) This release introduces support for Custom Models in AWS Clean Rooms ML.
* (**quicksight**) Add Client Credentials based OAuth support for Snowflake and Starburst
* (**resourceexplorer2**) Add GetManagedView, ListManagedViews APIs.
* (**synthetics**) Add support to toggle if a canary will automatically delete provisioned canary resources such as Lambda functions and layers when a canary is deleted.  This behavior can be controlled via the new ProvisionedResourceCleanup property exposed in the CreateCanary and UpdateCanary APIs.

## [1.3.68] - 11/06/2024

### Features
* (**codebuild**) AWS CodeBuild now adds additional compute types for reserved capacity fleet.
* (**guardduty**) GuardDuty RDS Protection expands support for Amazon Aurora PostgreSQL Limitless Databases.
* (**lakeformation**) API changes for new named tag expressions feature.
* (**qapps**) Introduces category apis in AmazonQApps. Web experience users use Categories to tag and filter library items.
* (**s3control**) Fix ListStorageLensConfigurations and ListStorageLensGroups deserialization for Smithy SDKs.
* (**verifiedpermissions**) Adding BatchGetPolicy API which supports the retrieval of multiple policies across multiple policy stores within a single request.

## [1.3.67] - 11/01/2024

### Features
* (**bedrockagent**) Amazon Bedrock Knowledge Bases now supports using application inference profiles to increase throughput and improve resilience.
* (**cloudwatchlogs**) This release introduces an improvement in PutLogEvents
* (**docdbelastic**) Amazon DocumentDB Elastic Clusters adds support for pending maintenance actions feature with APIs GetPendingMaintenanceAction, ListPendingMaintenanceActions and ApplyPendingMaintenanceAction
* (**taxsettings**) Add support for supplemental tax registrations via these new APIs: PutSupplementalTaxRegistration, ListSupplementalTaxRegistrations, and DeleteSupplementalTaxRegistration.

## [1.3.66] - 10/31/2024

### Features
* (**amp**) Added support for UpdateScraper API, to enable updating collector configuration in-place
* (**autoscaling**) Adds bake time for Auto Scaling group Instance Refresh
* (**batch**) Add `podNamespace` to `EksAttemptDetail` and `containerID` to `EksAttemptContainerDetail`.
* (**elasticloadbalancingv2**) Add UDP support for AWS PrivateLink and dual-stack Network Load Balancers
* (**glue**) Add schedule support for AWS Glue column statistics
* (**sagemaker**) SageMaker HyperPod adds scale-down at instance level via BatchDeleteClusterNodes API and group level via UpdateCluster API. SageMaker Training exposes secondary job status in TrainingJobSummary from ListTrainingJobs API. SageMaker now supports G6, G6e, P5e instances for HyperPod and Training.
* (**sesv2**) This release enables customers to provide the email template content in the SESv2 SendEmail and SendBulkEmail APIs instead of the name or the ARN of a stored email template.

### Miscellaneous
* Send x-amzn-query-mode=true for services with query-compatible trait
* Bump to smithy-kotlin 1.3.18

## [1.3.65] - 10/30/2024

### Features
* (**appsync**) This release adds support for AppSync Event APIs.
* (**connect**) Updated the public documentation for the UserIdentityInfo object to accurately reflect the character limits for the FirstName and LastName fields, which were previously listed as 1-100 characters.
* (**datasync**) AWS DataSync now supports Enhanced mode tasks. This task mode supports transfer of virtually unlimited numbers of objects with enhanced metrics, more detailed logs, and higher performance than Basic mode. This mode currently supports transfers between Amazon S3 locations.
* (**ec2**) This release adds two new capabilities to VPC Security Groups: Security Group VPC Associations and Shared Security Groups.
* (**ecs**) This release supports service deployments and service revisions which provide a comprehensive view of your Amazon ECS service history.
* (**geomaps**) Release of Amazon Location Maps API. Maps enables you to build digital maps that showcase your locations, visualize your data, and unlock insights to drive your business
* (**geoplaces**) Release of Amazon Location Places API. Places enables you to quickly search, display, and filter places, businesses, and locations based on proximity, category, and name
* (**georoutes**) Release of Amazon Location Routes API. Routes enables you to plan efficient routes and streamline deliveries by leveraging real-time traffic, vehicle restrictions, and turn-by-turn directions.
* (**keyspaces**) Adds support for interacting with user-defined types (UDTs) through the following new operations: Create-Type, Delete-Type, List-Types, Get-Type.
* (**networkfirewall**) AWS Network Firewall now supports configuring TCP idle timeout
* (**opensearch**) This release introduces the new OpenSearch user interface (Dashboards), a new web-based application that can be associated with multiple data sources across OpenSearch managed clusters, serverless collections, and Amazon S3, so that users can gain a comprehensive insights in an unified interface.
* (**opensearchserverless**) Neo Integration via IAM Identity Center (IdC)
* (**redshift**) This release launches S3 event integrations to create and manage integrations from an Amazon S3 source into an Amazon Redshift database.
* (**redshiftserverless**) Adds and updates API members for the Redshift Serverless AI-driven scaling and optimization feature using the price-performance target setting.
* (**route53**) This release adds support for TLSA, SSHFP, SVCB, and HTTPS record types.
* (**sagemaker**) Added support for Model Registry Staging construct. Users can define series of stages that models can progress through for model workflows and lifecycle. This simplifies tracking and managing models as they transition through development, testing, and production stages.
* (**workmail**) This release adds support for Multi-Factor Authentication (MFA) and Personal Access Tokens through integration with AWS IAM Identity Center.

## [1.3.64] - 10/29/2024

### Features
* (**bedrock**) Update Application Inference Profile
* (**bedrockruntime**) Update Application Inference Profile
* (**cleanrooms**) This release adds the option for customers to configure analytics engine when creating a collaboration, and introduces the new SPARK analytics engine type in addition to maintaining the legacy CLEAN_ROOMS_SQL engine type.
* (**cloudwatchlogs**) Added support for new optional baseline parameter in the UpdateAnomaly API. For UpdateAnomaly requests with baseline set to True, The anomaly behavior is then treated as baseline behavior. However, more severe occurrences of this behavior will still be reported as anomalies.
* [#472](https://github.com/awslabs/aws-sdk-kotlin/issues/472) (**dynamodb-mapper**) Initial release of Developer Preview of **DynamoDB Mapper** for Kotlin
* (**iotfleetwise**) Updated BatchCreateVehicle and BatchUpdateVehicle APIs: LimitExceededException has been added and the maximum number of vehicles in a batch has been set to 10 explicitly
* (**redshiftdata**) Adding a new API GetStatementResultV2 that supports CSV formatted results from ExecuteStatement and BatchExecuteStatement calls.
* (**sagemaker**) Adding `notebook-al2-v3` as allowed value to SageMaker NotebookInstance PlatformIdentifier attribute

### Miscellaneous
* Remove Nimble Service

## [1.3.63] - 10/28/2024

### Features
* (**mediapackagev2**) MediaPackage V2 Live to VOD Harvester is a MediaPackage V2 feature, which is used to export content from an origin endpoint to a S3 bucket.
* (**opensearch**) Adds support for provisioning dedicated coordinator nodes. Coordinator nodes can be specified using the new NodeOptions parameter in ClusterConfig.
* (**rds**) This release adds support for Enhanced Monitoring and Performance Insights when restoring Aurora Limitless Database DB clusters. It also adds support for the os-upgrade pending maintenance action.

### Documentation
* (**storagegateway**) Documentation update: Amazon FSx File Gateway will no longer be available to new customers.

## [1.3.62] - 10/25/2024

### Features
* (**bedrockagent**) Add support of new model types for Bedrock Agents, Adding inference profile support for Flows and Prompt Management, Adding new field to configure additional inference configurations for Flows and Prompt Management
* (**cloudwatchlogs**) Adding inferred token name for dynamic tokens in Anomalies.
* (**codebuild**) AWS CodeBuild now supports automatically retrying failed builds
* (**lambda**) Add TagsError field in Lambda GetFunctionResponse. The TagsError field contains details related to errors retrieving tags.
* (**supplychain**) API doc updates, and also support showing error message on a failed instance

## [1.3.61] - 10/24/2024

### Features
* (**appconfig**) This release improves deployment safety by granting customers the ability to REVERT completed deployments, to the last known good state.In the StopDeployment API revert case the status of a COMPLETE deployment will be REVERTED. AppConfig only allows a revert within 72 hours of deployment completion.
* (**ec2**) This release includes a new API to describe some details of the Amazon Machine Images (AMIs) that were used to launch EC2 instances, even if those AMIs are no longer available for use.
* (**ecs**) This release adds support for EBS volumes attached to Amazon ECS Windows tasks running on EC2 instances.
* (**qbusiness**) Add a new field in chat response. This field can be used to support nested schemas in array fields

### Documentation
* (**pcs**) Documentation update: added the default value of the Slurm configuration parameter scaleDownIdleTimeInSeconds to its description.

## [1.3.60] - 10/23/2024

### Features
* (**connect**) Amazon Connect Service Feature: Add support to start screen sharing for a web calling contact.
* (**ec2**) Amazon EC2 X8g, C8g and M8g instances are powered by AWS Graviton4 processors. X8g provide the lowest cost per GiB of memory among Graviton4 instances. C8g provide the best price performance for compute-intensive workloads. M8g provide the best price performance in for general purpose workloads.
* (**mwaa**) Introducing InvokeRestApi which allows users to invoke the Apache Airflow REST API on the webserver with the specified inputs.
* (**paymentcryptography**) Add support for ECC P-256 and P-384 Keys.
* (**paymentcryptographydata**) Add ECDH support on PIN operations.

### Fixes
* Fix backwards incompatibility with versions < 1.2.28

### Documentation
* (**bedrock**) Doc updates for supporting converse

## [1.3.59] - 10/22/2024

### Features
* (**bedrockruntime**) Updating invoke regex to support imported models for converse API
* (**imagebuilder**) Add macOS platform and instance placement options
* (**m2**) Add AuthSecretsManagerArn optional parameter to batch job APIs, expand batch parameter limits, and introduce clientToken constraints.
* (**rds**) Global clusters now expose the Endpoint attribute as one of its fields. It is a Read/Write endpoint for the global cluster which resolves to the Global Cluster writer instance.
* (**repostspace**) Adds the BatchAddRole and BatchRemoveRole APIs.
* (**timestreamquery**) This release adds support for Query Insights, a feature that provides details of query execution, enabling users to identify areas for improvement to optimize their queries, resulting in improved query performance and lower query costs.

## [1.3.58] - 10/21/2024

### Features
* (**applicationinsights**) This feature enables customers to specify SNS Topic ARN. CloudWatch Application Insights (CWAI) will utilize this ARN to send problem notifications.
* (**autoscaling**) Adds support for removing the PlacementGroup setting on an Auto Scaling Group through the UpdateAutoScalingGroup API.
* (**bedrockagentruntime**) Knowledge Bases for Amazon Bedrock now supports custom prompts and model parameters in the orchestrationConfiguration of the RetrieveAndGenerate API. The modelArn field accepts Custom Models and Imported Models ARNs.
* (**databasemigrationservice**) Added support for tagging in StartReplicationTaskAssessmentRun API and introduced IsLatestTaskAssessmentRun and ResultStatistic fields for enhanced tracking and assessment result statistics.
* (**ec2**) Amazon EC2 now allows you to create network interfaces with just the EFA driver and no ENA driver by specifying the network interface type as efa-only.
* (**eks**) This release adds support for Amazon Application Recovery Controller (ARC) zonal shift and zonal autoshift with EKS that enhances the resiliency of multi-AZ cluster environments
* (**fms**) Update AWS WAF policy - add the option to retrofit existing web ACLs instead of creating all new web ACLs.
* (**paymentcryptographydata**) Adding new API to generate authenticated scripts for EMV pin change use cases.
* (**wafv2**) Add a property to WebACL to indicate whether it's been retrofitted by Firewall Manager.

## [1.3.57] - 10/18/2024

### Features
* (**athena**) Remove `DataCatalog` from `Create`/`DeleteDataCatalog`. Remove `Status`, `ConnectionType`, and `Error` from `DataCatalog` and `DataCatalogSummary`. These were released inadvertently with no functionality. They were not populated or populated with a default value. Code related to these changes can be safely removed.
* (**bedrock**) Adding converse support to CMI API's
* (**bedrockruntime**) Added converse support for custom imported models
* (**datazone**) Adding the following project member designations: PROJECT_CATALOG_VIEWER, PROJECT_CATALOG_CONSUMER and PROJECT_CATALOG_STEWARD in the CreateProjectMembership API and PROJECT_CATALOG_STEWARD designation in the AddPolicyGrant API.
* (**ec2**) RequestSpotInstances and RequestSpotFleet feature release.

## [1.3.56] - 10/17/2024

### Features
* (**bedrockagent**) Removing support for topK property in PromptModelInferenceConfiguration object, Making PromptTemplateConfiguration property as required, Limiting the maximum PromptVariant to 1
* (**dataexchange**) This release adds Data Grant support, through which customers can programmatically create data grants to share with other AWS accounts and accept data grants from other AWS accounts.
* (**pinpointsmsvoicev2**) Added the registrations status of REQUIRES_AUTHENTICATION
* (**pipes**) This release adds validation to require specifying a SecurityGroup and Subnets in the Vpc object under PipesSourceSelfManagedKafkaParameters. It also adds support for iso-e, iso-f, and other non-commercial partitions in ARN parameters.
* (**quicksight**) Add StartDashboardSnapshotJobSchedule API. RestoreAnalysis now supports restoring analysis to folders.
* (**workspaces**) Updated the DomainName pattern for Active Directory

### Documentation
* (**ecs**) This is an Amazon ECS documentation only update to address tickets.
* (**rds**) Updates Amazon RDS documentation for TAZ IAM support

### Miscellaneous
* Upgrade to Kotlin 2.0.21

## [1.3.55] - 10/16/2024

### Features
* (**s3**) Add support for the new optional bucket-region and prefix query parameters in the ListBuckets API. For ListBuckets requests that express pagination, Amazon S3 will now return both the bucket names and associated AWS regions in the response.

## [1.3.54] - 10/15/2024

### Features
* (**amplify**) Added sourceUrlType field to StartDeployment request
* (**codebuild**) Enable proxy for reserved capacity fleet.
* (**ivs**) On a channel that you own, you can now replace an ongoing stream with a new stream by streaming up with the priority parameter appended to the stream key.
* (**qbusiness**) Amazon Q Business now supports embedding the Amazon Q Business web experience on third-party websites.
* (**redshift**) This release launches the CreateIntegration, DeleteIntegration, DescribeIntegrations and ModifyIntegration APIs to create and manage Amazon Redshift Zero-ETL Integrations.
* (**resiliencehub**) AWS Resilience Hub now integrates with the myApplications platform, enabling customers to easily assess the resilience of applications defined in myApplications. The new Resiliency widget provides visibility into application resilience and actionable recommendations for improvement.
* (**sesv2**) This release adds support for email maximum delivery seconds that allows senders to control the time within which their emails are attempted for delivery.
* (**transcribestreaming**) We are expanding support for 40 new locales in AWS Transcribe Streaming.

### Documentation
* (**cloudformation**) Documentation update for AWS CloudFormation API Reference.

## [1.3.53] - 10/14/2024

### Features
* (**codepipeline**) AWS CodePipeline V2 type pipelines now support automatically retrying failed stages and skipping stage for failed entry conditions.
* (**mailmanager**) Mail Manager support for viewing and exporting metadata of archived messages.
* (**securitylake**) This release updates request validation regex for resource ARNs.
* (**supplychain**) This release adds AWS Supply Chain instance management functionality. Specifically adding CreateInstance, DeleteInstance, GetInstance, ListInstances, and UpdateInstance APIs.
* (**transfer**) This release enables customers using SFTP connectors to query the transfer status of their files to meet their monitoring needs as well as orchestrate post transfer actions.

### Fixes
* ⚠️ **IMPORTANT**: Update to the latest S3 model, which correctly models `ReplicationRuleFilter` and `LifecycleRuleFilter` as structures (not unions). This may cause compilation failures in existing code which references these two shapes.

## [1.3.52] - 10/11/2024

### Features
* (**elasticloadbalancingv2**) Add zonal_shift.config.enabled attribute. Add new AdministrativeOverride construct in the describe-target-health API response to include information about the override status applied to a target.
* (**emr**) This release provides new parameter "Context" in instance fleet clusters.
* (**guardduty**) Added a new field for network connection details.

### Fixes
* [#1433](https://github.com/awslabs/aws-sdk-kotlin/issues/1433) Remove Route53 InvalidChangeBatch error response customization

### Documentation
* (**appflow**) Doc only updates for clarification around OAuth2GrantType for Salesforce.
* (**robomaker**) Documentation update: added support notices to each API action.

## [1.3.51] - 10/10/2024

### Features
* (**databasemigrationservice**) Introduces DescribeDataMigrations, CreateDataMigration, ModifyDataMigration, DeleteDataMigration, StartDataMigration, StopDataMigration operations to SDK. Provides FailedDependencyFault error message.
* (**ec2**) This release adds support for assigning the billing of shared Amazon EC2 On-Demand Capacity Reservations.
* (**iotfleetwise**) Refine campaign related API validations
* (**neptunegraph**) Support for 16 m-NCU graphs available through account allowlisting
* (**outposts**) Adding new "DELIVERED" enum value for Outposts Order status
* (**route53resolver**) Route 53 Resolver Forwarding Rules can now include a server name indication (SNI) in the target address for rules that use the DNS-over-HTTPS (DoH) protocol. When a DoH-enabled Outbound Resolver Endpoint forwards a request to a DoH server, it will provide the SNI in the TLS handshake.
* (**socialmessaging**) This release for AWS End User Messaging includes a public SDK, providing a suite of APIs that enable sending WhatsApp messages to end users.
* (**timestreaminfluxdb**) This release updates our regex based validation rules in regards to valid DbInstance and DbParameterGroup name.

### Documentation
* (**acmpca**) Documentation updates for AWS Private CA.
* (**ecs**) This is a documentation only release that updates to documentation to let customers know that Amazon Elastic Inference is no longer available.
* (**elasticinference**) Elastic Inference - Documentation update to add service shutdown notice.

## [1.3.50] - 10/09/2024

### Features
* (**codepipeline**) AWS CodePipeline introduces a Compute category

## [1.3.49] - 10/08/2024

### Features
* (**elasticache**) AWS ElastiCache SDK now supports using APIs with newly launched Valkey engine. Please refer to updated AWS ElastiCache public documentation for detailed information on API usage.
* (**memorydb**) Amazon MemoryDB SDK now supports all APIs for newly launched Valkey engine. Please refer to the updated Amazon MemoryDB public documentation for detailed information on API usage.

### Miscellaneous
* Upgrade to Smithy 1.51.0

## [1.3.48] - 10/07/2024

### Features
* (**deadline**) Add support for using the template from a previous job during job creation and listing parameter definitions for a job.
* (**qconnect**) This release adds support for the following capabilities: Configuration of the Gen AI system via AIAgent and AIPrompts. Integration support for Bedrock Knowledge Base.
* (**redshift**) Add validation pattern to S3KeyPrefix on the EnableLogging API

### Documentation
* (**marketplacereporting**) Documentation-only update for AWS Marketplace Reporting API.

## [1.3.47] - 10/04/2024

### Features
* (**ec2**) Documentation updates for Amazon EC2.
* (**iotdataplane**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## [1.3.46] - 10/03/2024

### Features
* (**codepipeline**) AWS CodePipeline introduces Commands action that enables you to easily run shell commands as part of your pipeline execution.
* (**connect**) Public GetMetricDataV2 Grouping increase from 3 to 4
* (**ec2**) This release includes a new API for modifying instance cpu-options after launch.
* (**iot**) This release adds support for Custom Authentication with X.509 Client Certificates, support for Custom Client Certificate validation, and support for selecting application protocol and authentication type without requiring TLS ALPN for customer's AWS IoT Domain Configurations.
* (**marketplacereporting**) The AWS Marketplace Reporting service introduces the GetBuyerDashboard API. This API returns a dashboard that provides visibility into your organization's AWS Marketplace agreements and associated spend across the AWS accounts in your organization.
* (**mediapackagev2**) Added support for ClipStartTime on the FilterConfiguration object on OriginEndpoint manifest settings objects. Added support for EXT-X-START tags on produced HLS child playlists.
* (**quicksight**) QuickSight: Add support for exporting and importing folders in AssetBundle APIs

## [1.3.45] - 10/02/2024

### Features
* (**appstream**) Added support for Automatic Time Zone Redirection on Amazon AppStream 2.0
* (**b2bi**) Added and updated APIs to support outbound EDI transformations
* (**bedrockagentruntime**) Added raw model response and usage metrics to PreProcessing and PostProcessing Trace
* (**bedrockruntime**) Added new fields to Amazon Bedrock Guardrails trace
* (**iotdeviceadvisor**) Add clientToken attribute and implement idempotency for CreateSuiteDefinition.
* (**ivsrealtime**) Adds new Stage Health EventErrorCodes applicable to RTMP(S) broadcasts. Bug Fix: Enforces that EncoderConfiguration Video height and width must be even-number values.
* (**s3**) This release introduces a header representing the minimum object size limit for Lifecycle transitions.
* (**sagemaker**) releasing builtinlcc to public

### Documentation
* (**workspaces**) WSP is being rebranded to become DCV.

## [1.3.44] - 10/01/2024

### Features
* (**bedrockagent**) This release adds support to stop an ongoing ingestion job using the StopIngestionJob API in Agents for Amazon Bedrock.
* (**codeartifact**) Add support for the dual stack endpoints.
* (**rds**) This release provides additional support for enabling Aurora Limitless Database DB clusters.

## [1.3.43] - 09/30/2024

### Features
* (**bedrock**) Add support for custom models via provisioned throughput for Bedrock Model Evaluation
* (**connect**) Amazon Connect introduces StartOutboundChatContact API allowing customers to initiate outbound chat contacts
* (**resourcegroups**) This update includes new APIs to support application groups and to allow users to manage resource tag-sync tasks in applications.
* (**supplychain**) Release DataLakeDataset, DataIntegrationFlow and ResourceTagging APIs for AWS Supply Chain
* (**timestreaminfluxdb**) Timestream for InfluxDB now supports port configuration and additional customer-modifiable InfluxDB v2 parameters. This release adds Port to the CreateDbInstance and UpdateDbInstance API, and additional InfluxDB v2 parameters to the CreateDbParameterGroup API.

### Documentation
* (**clouddirectory**) Add examples for API operations in model.
* (**pricing**) Add examples for API operations in model.
* (**verifiedpermissions**) Add examples for API operations in model.

## [1.3.42] - 09/27/2024

### Features
* (**customerprofiles**) Introduces optional RoleArn parameter for PutIntegration request and includes RoleArn in the response of PutIntegration, GetIntegration and ListIntegrations
* (**quicksight**) Adding personalization in QuickSight data stories. Admins can enable or disable personalization through QuickSight settings.
* (**sesv2**) This release adds support for engagement tracking over Https using custom domains.

### Documentation
* (**securityhub**) Documentation updates for AWS Security Hub

### Miscellaneous
* Remove WorkLink service

## [1.3.41] - 09/26/2024

### Features
* (**chatbot**) Return State and StateReason fields for Chatbot Channel Configurations.
* (**lambda**) Reverting Lambda resource-based policy and block public access APIs.
* (**organizations**) Add support for policy operations on the CHATBOT_POLICY policy type.
* (**sagemaker**) Adding `TagPropagation` attribute to Sagemaker API

### Documentation
* (**pcs**) AWS PCS API documentation - Edited the description of the iamInstanceProfileArn parameter of the CreateComputeNodeGroup and UpdateComputeNodeGroup actions; edited the description of the SlurmCustomSetting data type to list the supported parameters for clusters and compute node groups.
* (**rdsdata**) Documentation update for RDS Data API to reflect support for Aurora MySQL Serverless v2 and Provisioned DB clusters.

## [1.3.40] - 09/25/2024

### Documentation
* (**cloudtrail**) Doc-only update for CloudTrail network activity events release (in preview)
* (**ec2**) Updates to documentation for the transit gateway security group referencing feature.
* (**fsx**) Doc-only update to address Lustre S3 hard-coded names.

## [1.3.39] - 09/24/2024

### Features
* (**bedrock**) Add support for Cross Region Inference in Bedrock Model Evaluations.
* (**budgets**) Releasing minor partitional endpoint updates
* (**kinesis**) This release includes support to add tags when creating a stream
* (**pinpointsmsvoicev2**) AWS End User Messaging SMS-Voice V2 has added support for resource policies. Use the three new APIs to create, view, edit, and delete resource policies.
* (**sagemaker**) Adding `HiddenInstanceTypes` and `HiddenSageMakerImageVersionAliases` attribute to SageMaker API

## [1.3.38] - 09/23/2024

### Features
* (**athena**) List/Get/Update/Delete/CreateDataCatalog now integrate with AWS Glue connections. Users can create a Glue connection through Athena or use a Glue connection to define their Athena federated parameters.
* (**bedrockagent**) Amazon Bedrock Prompt Flows and Prompt Management now supports using inference profiles to increase throughput and improve resilience.
* (**ec2**) Amazon EC2 G6e instances powered by NVIDIA L40S Tensor Core GPUs are the most cost-efficient GPU instances for deploying generative AI models and the highest performance GPU instances for spatial computing workloads.
* (**emrserverless**) This release adds support for job concurrency and queuing configuration at Application level.
* (**glue**) Added AthenaProperties parameter to Glue Connections, allowing Athena to store service specific properties on Glue Connections.
* (**rds**) Support ComputeRedundancy parameter in ModifyDBShardGroup API. Add DBShardGroupArn in DBShardGroup API response. Remove InvalidMaxAcuFault from CreateDBShardGroup and ModifyDBShardGroup API. Both API will throw InvalidParameterValueException for invalid ACU configuration.
* (**resourceexplorer2**) AWS Resource Explorer released ListResources feature which allows customers to list all indexed AWS resources within a view.

### Documentation
* (**apigateway**) Documentation updates for Amazon API Gateway

## [1.3.37] - 09/20/2024

### Features
* (**dynamodb**) Generate account endpoint for DynamoDB requests when the account ID is available
* (**neptune**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**sagemaker**) Amazon SageMaker now supports using manifest files to specify the location of uncompressed model artifacts within Model Packages
* (**sagemakermetrics**) This release introduces support for the SageMaker Metrics BatchGetMetrics API.
* (**workspaces**) Releasing new ErrorCodes for SysPrep failures during ImageImport and CreateImage process

## [1.3.36] - 09/19/2024

### Features
* (**codeconnections**) This release adds the PullRequestComment field to CreateSyncConfiguration API input, UpdateSyncConfiguration API input, GetSyncConfiguration API output and ListSyncConfiguration API output
* (**glue**) This change is for releasing TestConnection api SDK model
* (**lambda**) Tagging support for Lambda event source mapping, and code signing configuration resources.
* (**mediaconvert**) This release provides support for additional DRM configurations per SPEKE Version 2.0.
* (**medialive**) Adds Bandwidth Reduction Filtering for HD AVC and HEVC encodes, multiplex container settings.
* (**quicksight**) QuickSight: 1. Add new API - ListFoldersForResource. 2. Commit mode adds visibility configuration of Apply button on multi-select controls for authors.
* (**sagemaker**) Introduced support for G6e instance types on SageMaker Studio for JupyterLab and CodeEditor applications.
* (**workspacesweb**) WorkSpaces Secure Browser now enables Administrators to view and manage end-user browsing sessions via Session Management APIs.

### Miscellaneous
* Upgrade to kotlinx.coroutines 1.9.0

## [1.3.35] - 09/18/2024

### Features
* (**costexplorer**) This release extends the GetReservationPurchaseRecommendation API to support recommendations for Amazon DynamoDB reservations.
* (**directoryservice**) Added new APIs for enabling, disabling, and describing access to the AWS Directory Service Data API
* (**directoryservicedata**) Added new AWS Directory Service Data API, enabling you to manage data stored in AWS Directory Service directories. This includes APIs for creating, reading, updating, and deleting directory users, groups, and group memberships.
* (**guardduty**) Add `launchType` and `sourceIPs` fields to GuardDuty findings.
* (**mailmanager**) Introduce a new RuleSet condition evaluation, where customers can set up a StringExpression with a MimeHeader condition. This condition will perform the necessary validation based on the X-header provided by customers.
* (**s3**) Added SSE-KMS support for directory buckets.

### Documentation
* (**rds**) Updates Amazon RDS documentation with information upgrading snapshots with unsupported engine versions for RDS for MySQL and RDS for PostgreSQL.

## [1.3.34] - 09/17/2024

### Features
* (**codebuild**) GitLab Enhancements - Add support for Self-Hosted GitLab runners in CodeBuild. Add group webhooks
* (**ecr**) The `DescribeImageScanning` API now includes `fixAvailable`, `exploitAvailable`, and `fixedInVersion` fields to provide more detailed information about the availability of fixes, exploits, and fixed versions for identified image vulnerabilities.
* (**lambda**) Support for JSON resource-based policies and block public access
* (**ssm**) Support for additional levels of cross-account, cross-Region organizational units in Automation. Various documentation updates.

### Documentation
* (**ecs**) This is a documentation only release to address various tickets.
* (**rds**) Updates Amazon RDS documentation with configuration information about the BYOL model for RDS for Db2.

## [1.3.33] - 09/16/2024

### Features
* (**bedrock**) This feature adds cross account s3 bucket and VPC support to ModelInvocation jobs. To use a cross account bucket, pass in the accountId of the bucket to s3BucketOwner in the ModelInvocationJobInputDataConfig or ModelInvocationJobOutputDataConfig.
* (**iot**) This release adds additional enhancements to AWS IoT Device Management Software Package Catalog and Jobs. It also adds SBOM support in Software Package Version.
* (**medialive**) Removing the ON_PREMISE enum from the input settings field.
* (**rds**) Launching Global Cluster tagging.

### Documentation
* (**organizations**) Doc only update for AWS Organizations that fixes several customer-reported issues
* (**pcaconnectorscep**) This is a general availability (GA) release of Connector for SCEP, a feature of AWS Private CA. Connector for SCEP links your SCEP-enabled and mobile device management systems to AWS Private CA for digital signature installation and certificate management.

## [1.3.32] - 09/13/2024

### Documentation
* (**amplify**) Doc only update to Amplify to explain platform setting for Next.js 14 SSG only applications
* (**ivs**) Updates to all tags descriptions.
* (**ivschat**) Updates to all tags descriptions.

## [1.3.31] - 09/12/2024

### Features
* (**cognitoidentityprovider**) Added email MFA option to user pools with advanced security features.
* (**elasticloadbalancingv2**) Correct incorrectly mapped error in ELBv2 waiters
* (**emr**) Update APIs to allow modification of ODCR options, allocation strategy, and InstanceTypeConfigs on running InstanceFleet clusters.
* (**glue**) AWS Glue is introducing two new optimizers for Apache Iceberg tables: snapshot retention and orphan file deletion. Customers can enable these optimizers and customize their configurations to perform daily maintenance tasks on their Iceberg tables based on their specific requirements.
* (**mediaconvert**) This release includes support for dynamic video overlay workflows, including picture-in-picture and squeezeback
* (**storagegateway**) The S3 File Gateway now supports DSSE-KMS encryption. A new parameter EncryptionType is added to these APIs: CreateSmbFileShare, CreateNfsFileShare, UpdateSmbFileShare, UpdateNfsFileShare, DescribeSmbFileShares, DescribeNfsFileShares. Also, in favor of EncryptionType, KmsEncrypted is deprecated.
* (**synthetics**) This release introduces two features. The first is tag replication, which allows for the propagation of canary tags onto Synthetics related resources, such as Lambda functions. The second is a limit increase in canary name length, which has now been increased from 21 to 255 characters.

### Documentation
* (**rds**) This release adds support for the os-upgrade pending maintenance action for Amazon Aurora DB clusters.

## [1.3.30] - 09/11/2024

### Features
* (**bedrockagent**) Amazon Bedrock Knowledge Bases now supports using inference profiles to increase throughput and improve resilience.
* (**bedrockagentruntime**) Amazon Bedrock Knowledge Bases now supports using inference profiles to increase throughput and improve resilience.
* (**ecr**) Added KMS_DSSE to EncryptionType
* (**guardduty**) Add support for new statistic types in GetFindingsStatistics.
* (**lexmodelsv2**) Support new Polly voice engines in VoiceSettings: long-form and generative
* (**medialive**) Adds AV1 Codec support, SRT ouputs, and MediaLive Anywhere support.

## [1.3.29] - 09/10/2024

### Features
* (**cognitoidentity**) This release adds sensitive trait to some required shapes.
* (**pipes**) This release adds support for customer managed KMS keys in Amazon EventBridge Pipe

### Documentation
* (**chimesdkvoice**) Documentation-only update that clarifies the ValidateE911Address action of the Amazon Chime SDK Voice APIs.
* (**securityhub**) Documentation update for Security Hub

## [1.3.28] - 09/09/2024

### Features
* (**elasticloadbalancingv2**) Add paginators for the ELBv2 DescribeListenerCertificates and DescribeRules APIs. Fix broken waiter for the ELBv2 DescribeLoadBalancers API.
* (**ivsrealtime**) IVS Real-Time now offers customers the ability to broadcast to Stages using RTMP(S).
* (**kafka**) Amazon MSK Replicator can now replicate data to identically named topics between MSK clusters within the same AWS Region or across different AWS Regions.
* (**sagemaker**) Amazon Sagemaker supports orchestrating SageMaker HyperPod clusters with Amazon EKS
* (**sagemakerruntime**) AWS SageMaker Runtime feature: Add sticky routing to support stateful inference models.

### Documentation
* (**dynamodb**) Doc-only update for DynamoDB. Added information about async behavior for TagResource and UntagResource APIs and updated the description of ResourceInUseException.

## [1.3.27] - 09/06/2024

### Features
* (**qapps**) Adds UpdateLibraryItemMetadata api to change status of app for admin verification feature and returns isVerified field in any api returning the app or library item.

## [1.3.26] - 09/05/2024

### Features
* (**applicationsignals**) Amazon CloudWatch Application Signals now supports creating Service Level Objectives using a new calculation type. Users can now create SLOs which are configured with request-based SLIs to help meet their specific business requirements.
* (**connect**) Amazon Connect Custom Vocabulary now supports Catalan (Spain), Danish (Denmark), Dutch (Netherlands), Finnish (Finland), Indonesian (Indonesia), Malay (Malaysia), Norwegian Bokmal (Norway), Polish (Poland), Swedish (Sweden), and Tagalog/Filipino (Philippines).
* (**gamelift**) Amazon GameLift provides additional events for tracking the fleet creation process.
* (**kinesisanalyticsv2**) Support for Flink 1.20 in Managed Service for Apache Flink
* (**sagemaker**) Amazon SageMaker now supports idle shutdown of JupyterLab and CodeEditor applications on SageMaker Studio.

### Documentation
* (**codepipeline**) Updates to add recent notes to APIs and to replace example S3 bucket names globally.

## [1.3.25] - 09/04/2024

### Features
* (**appsync**) Adds new logging levels (INFO and DEBUG) for additional log output control
* (**bedrockagent**) Add support for user metadata inside PromptVariant.
* (**cloudwatchlogs**) Update to support new APIs for delivery of logs from AWS services.
* (**fis**) This release adds safety levers, a new mechanism to stop all running experiments and prevent new experiments from starting.
* (**s3control**) Amazon Simple Storage Service /S3 Access Grants / Features : This release launches new Access Grants API - ListCallerAccessGrants.

### Documentation
* (**finspace**) Updates Finspace documentation for smaller instances.

## [1.3.24] - 09/03/2024

### Features
* (**connect**) Release ReplicaConfiguration as part of DescribeInstance
* (**datazone**) Add support to let data publisher specify a subset of the data asset that a subscriber will have access to based on the asset filters provided, when accepting a subscription request.
* (**elasticloadbalancingv2**) This release adds support for configuring TCP idle timeout on NLB and GWLB listeners.
* (**mediaconnect**) AWS Elemental MediaConnect introduces thumbnails for Flow source monitoring. Thumbnails provide still image previews of the live content feeding your MediaConnect Flow allowing you to easily verify that your source is operating as expected.
* (**medialive**) Added MinQP as a Rate Control option for H264 and H265 encodes.
* (**sagemaker**) Amazon SageMaker now supports automatic mounting of a user's home folder in the Amazon Elastic File System (EFS) associated with the SageMaker Studio domain to their Studio Spaces to enable users to share data between their own private spaces.
* (**timestreaminfluxdb**) Timestream for InfluxDB now supports compute scaling and deployment type conversion. This release adds the DbInstanceType and DeploymentType parameters to the UpdateDbInstance API.

## [1.3.23] - 08/30/2024

### Features
* (**backup**) The latest update introduces two new attributes, VaultType and VaultState, to the DescribeBackupVault and ListBackupVaults APIs. The VaultState attribute reflects the current status of the vault, while the VaultType attribute indicates the specific category of the vault.
* (**cloudwatchlogs**) This release introduces a new optional parameter: Entity, in PutLogEvents request
* (**datazone**) Amazon DataZone now adds new governance capabilities of Domain Units for organization within your Data Domains, and Authorization Policies for tighter controls.
* (**redshiftdata**) The release include the new Redshift DataAPI feature for session use, customer execute query with --session-keep-alive-seconds parameter and can submit follow-up queries to same sessions with returned`session-id`

## [1.3.22] - 08/29/2024

### Features
* (**bedrockagentruntime**) Lifting the maximum length on Bedrock KnowledgeBase RetrievalFilter array
* (**bedrockruntime**) Add support for imported-model in invokeModel and InvokeModelWithResponseStream.
* (**personalize**) This releases ability to update automatic training scheduler for customer solutions
* (**quicksight**) Increased Character Limit for Dataset Calculation Field expressions
* (**sfn**) This release adds support for static analysis to ValidateStateMachineDefinition API, which can now return optional WARNING diagnostics for semantic errors on the definition of an Amazon States Language (ASL) state machine.
* (**wafv2**) The minimum request rate for a rate-based rule is now 10. Before this, it was 100.

### Fixes
* [#1126](https://github.com/smithy-lang/smithy-kotlin/issues/1126) Upgrade to **1.3.8** version of **smithy-kotlin** to pick up fixes for code-generated deserializers

## [1.3.21] - 08/28/2024

### Features
* (**appconfig**) This release adds support for deletion protection, which is a safety guardrail to prevent the unintentional deletion of a recently used AWS AppConfig Configuration Profile or Environment. This also includes a change to increase the maximum length of the Name parameter in UpdateConfigurationProfile.
* (**datazone**) Update regex to include dot character to be consistent with IAM role creation in the authorized principal field for create and update subscription target.
* (**devicefarm**) This release removed support for Calabash, UI Automation, Built-in Explorer, remote access record, remote access replay, and web performance profile framework in ScheduleRun API.
* (**ec2**) Amazon VPC IP Address Manager (IPAM) now allows customers to provision IPv4 CIDR blocks and allocate Elastic IP Addresses directly from IPAM pools with public IPv4 space
* (**internetmonitor**) Adds new querying types to show overall traffic suggestion information for monitors
* (**pcs**) Introducing AWS Parallel Computing Service (AWS PCS), a new service makes it easy to setup and manage high performance computing (HPC) clusters, and build scientific and engineering models at virtually any scale on AWS.

### Documentation
* (**workspaces**) Documentation-only update that clarifies the StartWorkspaces and StopWorkspaces actions, and a few other minor edits.

## [1.3.20] - 08/27/2024

### Features
* (**bedrock**) Amazon Bedrock SDK updates for Inference Profile.
* (**bedrockruntime**) Amazon Bedrock SDK updates for Inference Profile.
* (**chatbot**) Update documentation to be consistent with the API docs
* (**omics**) Adds data provenance to import jobs from read sets and references
* (**polly**) Amazon Polly adds 2 new voices: Jitka (cs-CZ) and Sabrina (de-CH).

### Miscellaneous
* Upgrade to **v1.3.6** of **smithy-kotlin**

## [1.3.19] - 08/26/2024

### Features
* (**iotsitewise**) AWS IoT SiteWise now supports versioning for asset models. It enables users to retrieve active version of their asset model and perform asset model writes with optimistic lock.
* (**workspaces**) This release adds support for creating and managing directories that use AWS IAM Identity Center as user identity source. Such directories can be used to create non-Active Directory domain joined WorkSpaces Personal.Updated RegisterWorkspaceDirectory and DescribeWorkspaceDirectories APIs.

## [1.3.18] - 08/23/2024

### Features
* (**bedrockagent**) Releasing the support for Action User Confirmation.
* (**bedrockagentruntime**) Releasing the support for Action User Confirmation.
* (**organizations**) Releasing minor partitional endpoint updates.
* (**qbusiness**) Amazon QBusiness: Enable support for SAML and OIDC federation through AWS IAM Identity Provider integration.

### Documentation
* (**codebuild**) Added support for the MAC_ARM environment type for CodeBuild fleets.
* (**supplychain**) Update API documentation to clarify the event SLA as well as the data model expectations

### Miscellaneous
* Remove Codestar service

## [1.3.17] - 08/22/2024

### Features
* (**bedrock**) Amazon Bedrock Evaluation BatchDeleteEvaluationJob API allows customers to delete evaluation jobs under terminated evaluation job statuses - Stopped, Failed, or Completed. Customers can submit a batch of 25 evaluation jobs to be deleted at once.
* (**emrcontainers**) Correct endpoint for FIPS is configured for US Gov Regions.
* (**inspector2**) Add enums for Agentless scan statuses and EC2 enablement error states
* (**quicksight**) Explicit query for authors and dashboard viewing sharing for embedded users
* (**route53**) Amazon Route 53 now supports the Asia Pacific (Malaysia) Region (ap-southeast-5) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.

### Documentation
* (**autoscaling**) Amazon EC2 Auto Scaling now provides EBS health check to manage EC2 instance replacement

## [1.3.16] - 08/21/2024

### Features
* (**ec2**) DescribeInstanceStatus now returns health information on EBS volumes attached to Nitro instances
* (**entityresolution**) Increase the mapping attributes in Schema to 35.
* (**glue**) Add optional field JobRunQueuingEnabled to CreateJob and UpdateJob APIs.
* (**lambda**) Release FilterCriteria encryption for Lambda EventSourceMapping,  enabling customers to encrypt their filter criteria using a customer-owned KMS key.
* (**securityhub**) Security Hub documentation and definition updates
* (**ses**) Enable email receiving customers to provide SES with access to their S3 buckets via an IAM role for "Deliver to S3 Action"

## [1.3.15] - 08/20/2024

### Features
* (**opensearchserverless**) Added FailureCode and FailureMessage to BatchGetCollectionResponse for BatchGetVPCEResponse for non-Active Collection and VPCE.
* (**s3**) Amazon Simple Storage Service / Features : Add support for conditional writes for PutObject and CompleteMultipartUpload APIs.

### Documentation
* (**ecs**) Documentation only release to address various tickets

## [1.3.14] - 08/19/2024

### Features
* (**bedrock**) Amazon Bedrock Batch Inference/ Model Invocation is a feature which allows customers to asynchronously run inference on a large set of records/files stored in S3.
* (**codebuild**) AWS CodeBuild now supports creating fleets with macOS platform for running builds.
* (**deadline**) This release adds additional search fields and provides sorting by multiple fields.
* (**lambda**) Release Lambda FunctionRecursiveConfig, enabling customers to turn recursive loop detection on or off on individual functions. This release adds two new APIs, GetFunctionRecursionConfig and PutFunctionRecursionConfig.
* (**ssmsap**) Add new attributes to the outputs of GetApplication and GetDatabase APIs.

### Fixes
* [#1326](https://github.com/awslabs/aws-sdk-kotlin/issues/1326) Correctly terminate pagination for CloudWatch Logs `GetLogEvents` operation

## [1.3.13] - 08/16/2024

### Features
* (**batch**) Improvements of integration between AWS Batch and EC2.
* (**inspector2**) Update the correct format of key and values for resource tags
* (**quicksight**) Amazon QuickSight launches Customer Managed Key (CMK) encryption for Data Source metadata
* (**sagemaker**) Introduce Endpoint and EndpointConfig Arns in sagemaker:ListPipelineExecutionSteps API response
* (**sesv2**) Marking use case description field of account details as deprecated.

## [1.3.12] - 08/15/2024

### Features
* (**docdb**) This release adds Global Cluster Failover capability which enables you to change your global cluster's primary AWS region, the region that serves writes, during a regional outage. Performing a failover action preserves your Global Cluster setup.
* (**ecs**) This release introduces a new ContainerDefinition configuration to support the customer-managed keys for ECS container restart feature.
* (**iam**) Make the LastUsedDate field in the GetAccessKeyLastUsed response optional. This may break customers who only call the API for access keys with a valid LastUsedDate. This fixes a deserialization issue for access keys without a LastUsedDate, because the field was marked as required but could be null.
* (**s3**) Amazon Simple Storage Service / Features  : Adds support for pagination in the S3 ListBuckets API.

## [1.3.11] - 08/14/2024

### Features
* (**codebuild**) AWS CodeBuild now supports using Secrets Manager to store git credentials and using multiple source credentials in a single project.

### Fixes
* [#1368](https://github.com/awslabs/aws-sdk-kotlin/issues/1368) Correctly handle all empty HeadObject/HeadBucket responses

## [1.3.10] - 08/13/2024

### Features
* (**amplify**) Add a new field "cacheConfig" that enables users to configure the CDN cache settings for an App
* (**appstream**) This release includes following new APIs: CreateThemeForStack, DescribeThemeForStack, UpdateThemeForStack, DeleteThemeForStack to support custom branding programmatically.
* (**fis**) This release adds support for additional error information on experiment failure. It adds the error code, location, and account id on relevant failures to the GetExperiment and ListExperiment API responses.
* (**glue**) Add AttributesToGet parameter support for Glue GetTables
* (**neptunegraph**) Amazon Neptune Analytics provides a new option for customers to load data into a graph using the RDF (Resource Description Framework) NTRIPLES format. When loading NTRIPLES files, use the value `convertToIri` for the `blankNodeHandling` parameter.

### Fixes
* Stop using double slashes in `ImdsCredentialsProvider` URLs

## [1.3.9] - 08/12/2024

### Features
* (**configservice**) Documentation update for the OrganizationConfigRuleName regex pattern.
* (**ec2**) This release adds new capabilities to manage On-Demand Capacity Reservations including the ability to split your reservation, move capacity between reservations, and modify the instance eligibility of your reservation.
* (**eks**) Added support for new AL2023 GPU AMIs to the supported AMITypes.
* (**medialive**) AWS Elemental MediaLive now supports now supports editing the PID values for a Multiplex.
* (**sagemaker**) Releasing large data support as part of CreateAutoMLJobV2 in SageMaker Autopilot and CreateDomain API for SageMaker Canvas.

### Documentation
* (**computeoptimizer**) Doc only update for Compute Optimizer that fixes several customer-reported issues relating to ECS finding classifications
* (**groundstation**) Updating documentation for OEMEphemeris to link to AWS Ground Station User Guide

## [1.3.8] - 08/09/2024

### Features
* (**connect**) This release supports adding RoutingCriteria via UpdateContactRoutingData public API.

### Documentation
* (**cognitoidentityprovider**) Fixed a description of AdvancedSecurityAdditionalFlows in Amazon Cognito user pool configuration.
* (**ssm**) Systems Manager doc-only updates for August 2024.

## [1.3.7] - 08/08/2024

### Features
* (**cognitoidentityprovider**) Added support for threat protection for custom authentication in Amazon Cognito user pools.
* (**connect**) This release fixes a regression in number of access control tags that are allowed to be added to a security profile in Amazon Connect. You can now add up to four access control tags on a single security profile.
* (**ec2**) Launch of private IPv6 addressing for VPCs and Subnets. VPC IPAM supports the planning and monitoring of private IPv6 usage.
* (**glue**) This release adds support to retrieve the validation status when creating or updating Glue Data Catalog Views. Also added is support for BasicCatalogTarget partition keys.

## [1.3.6] - 08/07/2024

### Features
* (**appintegrations**) Updated CreateDataIntegration and CreateDataIntegrationAssociation API to support bulk data export from Amazon Connect Customer Profiles to the customer S3 bucket.
* (**glue**) Introducing AWS Glue Data Quality anomaly detection, a new functionality that uses ML-based solutions to detect data anomalies users have not explicitly defined rules for.

### Miscellaneous
* Upgrade to Kotlin 2.0.10

## [1.3.5] - 08/06/2024

### Features
* (**bedrockagentruntime**) Introduce model invocation output traces for orchestration traces, which contain the model's raw response and usage.
* (**cognitoidentityprovider**) Advanced security feature updates to include password history and log export for Cognito user pools.
* (**costoptimizationhub**) This release adds savings percentage support to the ListRecommendationSummaries API.
* (**workspaces**) Added support for BYOL_GRAPHICS_G4DN_WSP IngestionProcess

## [1.3.4] - 08/05/2024

### Features
* (**datazone**) This releases Data Product feature. Data Products allow grouping data assets into cohesive, self-contained units for ease of publishing for data producers, and ease of finding and accessing for data consumers.
* (**ecr**) Released two new APIs along with documentation updates. The GetAccountSetting API is used to view the current basic scan type version setting for your registry, while the PutAccountSetting API is used to update the basic scan type version for your registry.
* (**kinesisvideowebrtcstorage**) Add JoinStorageSessionAsViewer API

### Documentation
* (**pi**) Added a description for the Dimension db.sql.tokenized_id on the DimensionGroup data type page.

## [1.3.3] - 08/02/2024

### Features
* (**cloudwatch**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**ecs**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**glue**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**ivs**) updates cloudtrail event source for SDKs
* (**ivschat**) updates cloudtrail event source for SDKs
* (**ivsrealtime**) updates cloudtrail event source for SDKs
* (**kinesis**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**resiliencehub**) Customers are presented with the grouping recommendations and can determine if the recommendations are accurate and apply to their case. This feature simplifies onboarding by organizing resources into appropriate AppComponents.
* (**route53**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**ssm**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**wafregional**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## [1.3.2] - 08/01/2024

### Features
* (**bedrock**) API and Documentation for Bedrock Model Copy feature. This feature lets you share and copy a custom model from one region to another or one account to another.
* (**controlcatalog**) AWS Control Tower provides two new public APIs controlcatalog:ListControls and controlcatalog:GetControl under controlcatalog service namespace, which enable customers to programmatically retrieve control metadata of available controls.
* (**iam**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**rds**) This release adds support for specifying optional MinACU parameter in CreateDBShardGroup and ModifyDBShardGroup API. DBShardGroup response will contain MinACU if specified.
* (**sagemaker**) This release adds support for Amazon EMR Serverless applications in SageMaker Studio for running data processing jobs.
* (**ssmquicksetup**) This release adds API support for the QuickSetup feature of AWS Systems Manager

### Documentation
* (**controltower**) Updated Control Tower service documentation for controlcatalog control ARN support with existing Control Tower public APIs
* (**memorydb**) Doc only update for changes to deletion API.
* (**support**) Doc only updates to CaseDetails

## [1.3.1] - 07/30/2024

### Features
* (**appstream**) Added support for Red Hat Enterprise Linux 8 on Amazon AppStream 2.0
* (**autoscaling**) Increase the length limit for VPCZoneIdentifier from 2047 to 5000
* (**cloudwatchlogs**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**codepipeline**) AWS CodePipeline V2 type pipelines now support stage level conditions to enable development teams to safely release changes that meet quality and compliance requirements.
* (**elasticloadbalancing**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**eventbridge**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**lexmodelsv2**) This release adds new capabilities to the AMAZON.QnAIntent: Custom prompting, Guardrails integration and ExactResponse support for Bedrock Knowledge Base.
* (**rolesanywhere**) IAM RolesAnywhere now supports custom role session name on the CreateSession. This release adds the acceptRoleSessionName option to a profile to control whether a role session name will be accepted in a session request with a given profile.
* (**tnb**) This release adds Network Service Update, through which customers will be able to update their instantiated networks to a new network package. See the documentation for limitations. The release also enhances the Get network operation API to return parameter overrides used during the operation.

### Documentation
* (**elasticache**) Doc only update for changes to deletion API.
* (**workspaces**) Removing multi-session as it isn't supported for pools

## [1.3.0] - 07/29/2024

### Documentation
* (**elasticache**) Renaming full service name as it appears in developer documentation.
* (**memorydb**) Renaming full service name as it appears in developer documentation.

### Miscellaneous
* ⚠️ **IMPORTANT**: Upgrade to Kotlin 2.0.0

## [1.2.56] - 07/25/2024

### Features
* (**applicationautoscaling**) Application Auto Scaling is now more responsive to the changes in demand of your SageMaker Inference endpoints. To get started, create or update a Target Tracking policy based on High Resolution CloudWatch metrics.
* (**applicationsignals**) CloudWatch Application Signals now supports application logs correlation with traces and operational health metrics of applications running on EC2 instances. Users can view the most relevant telemetry to troubleshoot application health anomalies such as spikes in latency, errors, and availability.
* (**bedrockruntime**) Provides ServiceUnavailableException error message
* (**codecommit**) CreateRepository API now throws OperationNotAllowedException when the account has been restricted from creating a repository.
* (**datazone**) Introduces GetEnvironmentCredentials operation to SDK
* (**ec2**) EC2 Fleet now supports using custom identifiers to reference Amazon Machine Images (AMI) in launch requests that are configured to choose from a diversified list of instance types.
* (**ecr**) API and documentation updates for Amazon ECR, adding support for creating, updating, describing and deleting ECR Repository Creation Template.
* (**eks**) This release adds support for EKS cluster to manage extended support.
* (**elasticloadbalancingv2**) This release adds support for sharing trust stores across accounts and organizations through integration with AWS Resource Access Manager.
* (**networkfirewall**) You can now log events that are related to TLS inspection, in addition to the existing alert and flow logging.
* (**outposts**) Adding default vCPU information to GetOutpostSupportedInstanceTypes and GetOutpostInstanceTypes responses
* (**sfn**) This release adds support to customer managed KMS key encryption in AWS Step Functions.

## [1.2.55] - 07/24/2024

### Features
* (**cleanrooms**) Three enhancements to the AWS Clean Rooms: Disallowed Output Columns, Flexible Result Receivers, SQL as a Seed
* (**iotsitewise**) Adds support for creating SiteWise Edge gateways that run on a Siemens Industrial Edge Device.
* (**mediapackagev2**) This release adds support for Irdeto DRM encryption in DASH manifests.
* (**medicalimaging**) CopyImageSet API adds copying selected instances between image sets, and overriding inconsistent metadata with a force parameter. UpdateImageSetMetadata API enables reverting to prior versions; updates to Study, Series, and SOP Instance UIDs; and updates to private elements, with a force parameter.

### Documentation
* (**dynamodb**) DynamoDB doc only update for July
* (**pinpointsmsvoicev2**) Update for rebrand to AWS End User Messaging SMS and Voice.

## [1.2.54] - 07/23/2024

### Features
* (**appsync**) Adding support for paginators in AppSync list APIs
* (**cleanrooms**) This release adds AWS Entity Resolution integration to associate ID namespaces & ID mapping workflow resources as part of ID namespace association and  ID mapping table  in AWS Clean Rooms. It also introduces a new ID_MAPPING_TABLE analysis rule to manage the protection on ID mapping table.
* (**cleanroomsml**) Adds SQL query as the source of seed audience for audience generation job.
* (**connect**) Added PostContactSummary segment type on ListRealTimeContactAnalysisSegmentsV2 API
* (**connectcontactlens**) Added PostContactSummary segment type on ListRealTimeContactAnalysisSegments API
* (**datazone**) This release removes the deprecated dataProductItem field from Search API output.
* (**entityresolution**) Support First Party ID Mapping

## [1.2.53] - 07/22/2024

### Features
* (**datazone**) This release adds 1/ support of register S3 locations of assets in AWS Lake Formation hybrid access mode for DefaultDataLake blueprint. 2/ support of CRUD operations for Asset Filters.
* (**ivs**) Documentation update for IVS Low Latency API Reference.
* (**neptunegraph**) Amazon Neptune Analytics provides new options for customers to start with smaller graphs at a lower cost. CreateGraph, CreaateGraphImportTask, UpdateGraph and StartImportTask APIs will now allow 32 and 64 for `provisioned-memory`
* (**redshiftserverless**) Adds dualstack support for Redshift Serverless workgroup.

### Miscellaneous
* Remove deprecated Mobile service

## [1.2.52] - 07/18/2024

### Features
* (**acmpca**) Fix broken waiters for the acm-pca client.  Waiters broke in version 1.13.144 of the Boto3 SDK.
* (**connect**) Amazon Connect expands search API coverage for additional resources.  Search for hierarchy groups by name, ID, tag, or other criteria (new endpoint). Search for agent statuses by name, ID, tag, or other criteria (new endpoint). Search for users by their assigned proficiencies (enhanced endpoint)
* (**ec2**) Amazon VPC IP Address Manager (IPAM) now supports Bring-Your-Own-IP (BYOIP) for IP addresses registered with any Internet Registry. This feature uses DNS TXT records to validate ownership of a public IP address range.
* (**firehose**) This release 1) Add configurable buffering hints for Snowflake as destination. 2) Add ReadFromTimestamp for MSK As Source. Firehose will start reading data from MSK Cluster using offset associated with this timestamp. 3) Gated public beta release to add Apache Iceberg tables as destination.
* (**medialive**) AWS Elemental MediaLive now supports the SRT protocol via the new SRT Caller input type.
* (**sagemaker**) SageMaker Training supports R5, T3 and R5D instances family. And SageMaker Processing supports G5 and R5D instances family.
* (**taxsettings**) Set default endpoint for aws partition. Requests from all regions in aws partition will be forward to us-east-1 endpoint.
* [#1352](https://github.com/awslabs/aws-sdk-kotlin/issues/1352) Add new Kotlin/JVM methods for converting `InputStream` to `ByteStream` and for writing `ByteStream` to `OutputStream`

### Fixes
* Correctly redact sensitive data in lists and maps

### Documentation
* (**ivschat**) Documentation update for IVS Chat API Reference.
* (**rds**) Updates Amazon RDS documentation to specify an eventual consistency model for DescribePendingMaintenanceActions.
* (**secretsmanager**) Doc only update for Secrets Manager
* (**timestreamquery**) Doc-only update for TimestreamQuery. Added guidance about the accepted valid value for the QueryPricingModel parameter.
* (**workspacesthinclient**) Documentation update for WorkSpaces Thin Client.

## [1.2.51] - 07/12/2024

### Features
* (**acmpca**) Minor refactoring of C2J model for AWS Private CA
* (**arczonalshift**) Adds the option to subscribe to get notifications when a zonal autoshift occurs in a region.
* (**autoscaling**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**codebuild**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**dynamodb**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**globalaccelerator**) This feature adds exceptions to the Customer API to avoid throwing Internal Service errors
* (**pinpoint**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**quicksight**) Vega ally control options and Support for Reviewed Answers in Topics
* (**rds**) Update path for CreateDBCluster resource identifier, and Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**sns**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* [#1302](https://github.com/awslabs/aws-sdk-kotlin/issues/1302) Add support for `smithy.protocols#rpcv2Cbor` protocol

## [1.2.50] - 07/10/2024

### Features
* (**batch**) This feature allows AWS Batch Jobs with EKS container orchestration type to be run as Multi-Node Parallel Jobs.
* (**bedrock**) Add support for contextual grounding check for Guardrails for Amazon Bedrock.
* (**bedrockagent**) Introduces new data sources and chunking strategies for Knowledge bases, advanced parsing logic using FMs, session summary generation, and code interpretation (preview) for Claude V3 Sonnet and Haiku models. Also introduces Prompt Flows (preview) to link prompts, foundational models, and resources.
* (**bedrockagentruntime**) Introduces query decomposition, enhanced Agents integration with Knowledge bases, session summary generation, and code interpretation (preview) for Claude V3 Sonnet and Haiku models. Also introduces Prompt Flows (preview) to link prompts, foundational models, and resources for end-to-end solutions.
* (**bedrockruntime**) Add support for contextual grounding check and ApplyGuardrail API for Guardrails for Amazon Bedrock.
* (**ec2**) Add parameters to enable provisioning IPAM BYOIPv4 space at a Local Zone Network Border Group level
* (**glue**) Add recipe step support for recipe node
* (**licensemanagerlinuxsubscriptions**) Add support for third party subscription providers, starting with RHEL subscriptions through Red Hat Subscription Manager (RHSM). Additionally, add support for tagging subscription provider resources, and detect when an instance has more than one Linux subscription and notify the customer.
* (**mediaconnect**) AWS Elemental MediaConnect introduces the ability to disable outputs. Disabling an output allows you to keep the output attached to the flow, but stop streaming to the output destination. A disabled output does not incur data transfer costs.

### Documentation
* (**groundstation**) Documentation update specifying OEM ephemeris units of measurement

### Miscellaneous
* Upgrade Smithy to version **1.50.0**

## [1.2.49] - 07/09/2024

### Features
* (**datazone**) This release deprecates dataProductItem field from SearchInventoryResultItem, along with some unused DataProduct shapes
* (**fsx**) Adds support for FSx for NetApp ONTAP 2nd Generation file systems, and FSx for OpenZFS Single AZ HA file systems.
* (**opensearch**) This release adds support for enabling or disabling Natural Language Query Processing feature for Amazon OpenSearch Service domains, and provides visibility into the current state of the setup or tear-down.
* (**sagemaker**) This release 1/ enables optimization jobs that allows customers to perform Ahead-of-time compilation and quantization. 2/ allows customers to control access to Amazon Q integration in SageMaker Studio. 3/ enables AdditionalModelDataSources for CreateModel action.

## [1.2.48] - 07/08/2024

### Features
* (**codedeploy**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**databasemigrationservice**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**devicefarm**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**elasticbeanstalk**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**elasticsearchservice**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**firehose**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**gamelift**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**qapps**) This is a general availability (GA) release of Amazon Q Apps, a capability of Amazon Q Business. Q Apps leverages data sources your company has provided to enable users to build, share, and customize apps within your organization.
* (**route53resolver**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**ses**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## [1.2.47] - 07/05/2024

### Features
* (**ecr**) This release for Amazon ECR makes change to bring the SDK into sync with the API.
* (**paymentcryptographydata**) Added further restrictions on logging of potentially sensitive inputs and outputs.
* (**qbusiness**) Add personalization to Q Applications. Customers can enable or disable personalization when creating or updating a Q application with the personalization configuration.

### Documentation
* (**acm**) Documentation updates, including fixes for xml formatting, broken links, and ListCertificates description.

## [1.2.46] - 07/03/2024

### Features
* (**organizations**) Added a new reason under ConstraintViolationException in RegisterDelegatedAdministrator API to prevent registering suspended accounts as delegated administrator of a service.
* (**rekognition**) This release adds support for tagging projects and datasets with the CreateProject and CreateDataset APIs.
* (**workspaces**) Fix create workspace bundle RootStorage/UserStorage to accept non null values

### Documentation
* (**applicationautoscaling**) Doc only update for Application Auto Scaling that fixes resource name.
* (**directconnect**) This update includes documentation for support of new native 400 GBps ports for Direct Connect.

## [1.2.45] - 07/02/2024

### Features
* (**ec2**) Documentation updates for Elastic Compute Cloud (EC2).
* (**fms**) Increases Customer API's ManagedServiceData length
* (**s3**) Added response overrides to Head Object requests.

## [1.2.44] - 07/01/2024

### Features
* (**apigateway**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**cognitoidentity**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**connect**) Authentication profiles are Amazon Connect resources (in gated preview) that allow you to configure authentication settings for users in your contact center. This release adds support for new ListAuthenticationProfiles, DescribeAuthenticationProfile and UpdateAuthenticationProfile APIs.
* (**docdb**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**eks**) Updates EKS managed node groups to support EC2 Capacity Blocks for ML
* (**paymentcryptography**) Added further restrictions on logging of potentially sensitive inputs and outputs.
* (**paymentcryptographydata**) Adding support for dynamic keys for encrypt, decrypt, re-encrypt and translate pin functions.  With this change, customers can use one-time TR-31 keys directly in dataplane operations without the need to first import them into the service.
* (**sfn**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**swf**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

### Documentation
* (**wafv2**) JSON body inspection: Update documentation to clarify that JSON parsing doesn't include full validation.

### Miscellaneous
* Upgrade to ktlint v1.3.0
* Upgrade to smithy-kotlin v1.2.11

## [1.2.43] - 06/28/2024

### Features
* (**acmpca**) Added CCPC_LEVEL_1_OR_HIGHER KeyStorageSecurityStandard and SM2 KeyAlgorithm and SM3WITHSM2 SigningAlgorithm for China regions.
* (**cloudhsmv2**) Added 3 new APIs to support backup sharing: GetResourcePolicy, PutResourcePolicy, and DeleteResourcePolicy. Added BackupArn to the output of the DescribeBackups API. Added support for BackupArn in the CreateCluster API.
* (**connect**) This release supports showing PreferredAgentRouting step via DescribeContact API.
* (**emr**) This release provides the support for new allocation strategies i.e. CAPACITY_OPTIMIZED_PRIORITIZED for Spot and PRIORITIZED for On-Demand by taking input of priority value for each instance type for instance fleet clusters.
* (**glue**) Added AttributesToGet parameter to Glue GetDatabases, allowing caller to limit output to include only the database name.
* (**kinesisanalyticsv2**) Support for Flink 1.19 in Managed Service for Apache Flink
* (**opensearch**) This release removes support for enabling or disabling Natural Language Query Processing feature for Amazon OpenSearch Service domains.
* (**workspaces**) Added support for Red Hat Enterprise Linux 8 on Amazon WorkSpaces Personal.

### Documentation
* (**pi**) Noting that the filter db.sql.db_id isn't available for RDS for SQL Server DB instances.

## [1.2.42] - 06/27/2024

### Features
* (**applicationautoscaling**) Amazon WorkSpaces customers can now use Application Auto Scaling to automatically scale the number of virtual desktops in a WorkSpaces pool.
* (**chimesdkmediapipelines**) Added Amazon Transcribe multi language identification to Chime SDK call analytics. Enabling customers sending single stream audio to generate call recordings using Chime SDK call analytics
* (**datazone**) This release supports the data lineage feature of business data catalog in Amazon DataZone.
* (**elasticache**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**mq**) This release makes the EngineVersion field optional for both broker and configuration and uses the latest available version by default. The AutoMinorVersionUpgrade field is also now optional for broker creation and defaults to 'true'.
* (**qconnect**) Adds CreateContentAssociation, ListContentAssociations, GetContentAssociation, and DeleteContentAssociation APIs.
* (**quicksight**) Adding support for Repeating Sections, Nested Filters
* (**sagemaker**) Add capability for Admins to customize Studio experience for the user by showing or hiding Apps and MLTools.
* (**workspaces**) Added support for WorkSpaces Pools.

### Documentation
* (**cloudfront**) Doc only update for CloudFront that fixes customer-reported issue
* (**rds**) Updates Amazon RDS documentation for TAZ export to S3.

## [1.2.41] - 06/26/2024

### Features
* (**controltower**) Added ListLandingZoneOperations API.
* (**eks**) Added support for disabling unmanaged addons during cluster creation.
* (**ivsrealtime**) IVS Real-Time now offers customers the ability to upload public keys for customer vended participant tokens.
* (**kinesisanalyticsv2**) This release adds support for new ListApplicationOperations and DescribeApplicationOperation APIs. It adds a new configuration to enable system rollbacks, adds field ApplicationVersionCreateTimestamp for clarity and improves support for pagination for APIs.
* (**opensearch**) This release adds support for enabling or disabling Natural Language Query Processing feature for Amazon OpenSearch Service domains, and provides visibility into the current state of the setup or tear-down.

## [1.2.40] - 06/25/2024

### Features
* (**autoscaling**) Doc only update for Auto Scaling's TargetTrackingMetricDataQuery
* (**ec2**) This release is for the launch of the new u7ib-12tb.224xlarge, R8g, c7gn.metal and mac2-m1ultra.metal instance types
* (**networkmanager**) This is model changes & documentation update for the Asynchronous Error Reporting feature for AWS Cloud WAN. This feature allows customers to view errors that occur while their resources are being provisioned, enabling customers to fix their resources without needing external support.
* (**workspacesthinclient**) This release adds the deviceCreationTags field to CreateEnvironment API input, UpdateEnvironment API input and GetEnvironment API output.

## [1.2.39] - 06/24/2024

### Features
* (**bedrockruntime**) Increases Converse API's document name length
* (**customerprofiles**) This release includes changes to ProfileObjectType APIs, adds functionality top set and get capacity for profile object types.
* (**ec2**) Fix EC2 multi-protocol info in models.
* (**qbusiness**) Allow enable/disable Q Apps when creating/updating a Q application; Return the Q Apps enablement information when getting a Q application.
* (**ssm**) Add sensitive trait to SSM IPAddress property for CloudTrail redaction
* (**workspacesweb**) Added ability to enable DeepLinking functionality on a Portal via UserSettings as well as added support for IdentityProvider resource tagging.

## [1.2.38] - 06/20/2024

### Features
* (**bedrockruntime**) This release adds document support to Converse and ConverseStream APIs
* (**codeartifact**) Add support for the Cargo package format.
* (**computeoptimizer**) This release enables AWS Compute Optimizer to analyze and generate optimization recommendations for Amazon RDS MySQL and RDS PostgreSQL.
* (**costoptimizationhub**) This release enables AWS Cost Optimization Hub to show cost optimization recommendations for Amazon RDS MySQL and RDS PostgreSQL.
* (**glue**) Fix Glue paginators for Jobs, JobRuns, Triggers, Blueprints and Workflows.
* (**ivsrealtime**) IVS Real-Time now offers customers the ability to record individual stage participants to S3.
* (**sagemaker**) Adds support for model references in Hub service, and adds support for cross-account access of Hubs

### Documentation
* (**dynamodb**) Doc-only update for DynamoDB. Fixed Important note in 6 Global table APIs - CreateGlobalTable, DescribeGlobalTable, DescribeGlobalTableSettings, ListGlobalTables, UpdateGlobalTable, and UpdateGlobalTableSettings.
* (**securityhub**) Documentation updates for Security Hub

## [1.2.37] - 06/19/2024

### Features
* (**artifact**) This release adds an acceptanceType field to the ReportSummary structure (used in the ListReports API response).
* (**athena**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**costandusagereportservice**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**directconnect**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**elastictranscoder**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**opensearch**) This release enables customers to use JSON Web Tokens (JWT) for authentication on their Amazon OpenSearch Service domains.

## [1.2.36] - 06/18/2024

### Features
* (**bedrockruntime**) This release adds support for using Guardrails with the Converse and ConverseStream APIs.
* (**cloudtrail**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**configservice**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**eks**) This release adds support to surface async fargate customer errors from async path to customer through describe-fargate-profile API response.
* (**lightsail**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**polly**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**rekognition**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**sagemaker**) Launched a new feature in SageMaker to provide managed MLflow Tracking Servers for customers to track ML experiments. This release also adds a new capability of attaching additional storage to SageMaker HyperPod cluster instances.
* (**shield**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**snowball**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

## [1.2.35] - 06/17/2024

### Features
* (**batch**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**codebuild**) AWS CodeBuild now supports global and organization GitHub webhooks
* (**cognitoidentityprovider**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**directoryservice**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**efs**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**glue**) This release introduces a new feature, Usage profiles. Usage profiles allow the AWS Glue admin to create different profiles for various classes of users within the account, enforcing limits and defaults for jobs and sessions.
* (**mediaconvert**) This release includes support for creating I-frame only video segments for DASH trick play.
* (**waf**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

### Documentation
* (**acmpca**) Doc-only update that adds name constraints as an allowed extension for ImportCertificateAuthorityCertificate.
* (**kms**) Updating SDK example for KMS DeriveSharedSecret API.
* (**secretsmanager**) Doc only update for Secrets Manager

## [1.2.34] - 06/14/2024

### Features
* (**datazone**) This release introduces a new default service blueprint for custom environment creation.
* (**macie2**) This release adds support for managing the status of automated sensitive data discovery for individual accounts in an organization, and determining whether individual S3 buckets are included in the scope of the analyses.
* (**mediaconvert**) This release adds the ability to search for historical job records within the management console using a search box and/or via the SDK/CLI with partial string matching search on input file name.
* (**route53domains**) Add v2 smoke tests and smithy smokeTests trait for SDK testing.

### Documentation
* (**ec2**) Documentation updates for Amazon EC2.

### Miscellaneous
* Upgrade to Smithy 1.49.0

## [1.2.33] - 06/13/2024

### Features
* (**cloudhsmv2**) Added support for hsm type hsm2m.medium. Added supported for creating a cluster in FIPS or NON_FIPS mode.
* (**glue**) This release adds support for configuration of evaluation method for composite rules in Glue Data Quality rulesets.
* (**iotwireless**) Add RoamingDeviceSNR and RoamingDeviceRSSI to Customer Metrics.
* (**kms**) This feature allows customers to use their keys stored in KMS to derive a shared secret which can then be used to establish a secured channel for communication, provide proof of possession, or establish trust with other parties.
* (**mediapackagev2**) This release adds support for CMAF ingest (DASH-IF live media ingest protocol interface 1)

## [1.2.32] - 06/12/2024

### Features
* (**apptest**) AWS Mainframe Modernization Application Testing is an AWS Mainframe Modernization service feature that automates functional equivalence testing for mainframe application modernization and migration to AWS, and regression testing.
* (**ec2**) Tagging support for Traffic Mirroring FilterRule resource
* (**osis**) SDK changes for self-managed vpc endpoint to OpenSearch ingestion pipelines.
* (**secretsmanager**) Introducing RotationToken parameter for PutSecretValue API
* (**securitylake**) This release updates request validation regex to account for non-commercial aws partitions.
* (**sesv2**) This release adds support for Amazon EventBridge as an email sending events destination.

### Documentation
* (**redshift**) Updates to remove DC1 and DS2 node types.

### Miscellaneous
* Deprecation of AWS Backup Storage

## [1.2.31] - 06/11/2024

### Features
* (**accessanalyzer**) IAM Access Analyzer now provides policy recommendations to help resolve unused permissions for IAM roles and users. Additionally, IAM Access Analyzer now extends its custom policy checks to detect when IAM policies grant public access or access to critical resources ahead of deployments.
* (**guardduty**) Added API support for GuardDuty Malware Protection for S3.
* (**networkmanager**) This is model changes & documentation update for Service Insertion feature for AWS Cloud WAN. This feature allows insertion of AWS/3rd party security services on Cloud WAN. This allows to steer inter/intra segment traffic via security appliances and provide visibility to the route updates.
* (**pcaconnectorscep**) Connector for SCEP allows you to use a managed, cloud CA to enroll mobile devices and networking gear. SCEP is a widely-adopted protocol used by mobile device management (MDM) solutions for enrolling mobile devices. With the connector, you can use AWS Private CA with popular MDM solutions.
* (**sagemaker**) Introduced Scope and AuthenticationRequestExtraParams to SageMaker Workforce OIDC configuration; this allows customers to modify these options for their private Workforce IdP integration. Model Registry Cross-account model package groups are discoverable.

## [1.2.30] - 06/10/2024

### Features
* (**applicationsignals**) This is the initial SDK release for Amazon CloudWatch Application Signals. Amazon CloudWatch Application Signals provides curated application performance monitoring for developers to monitor and troubleshoot application health using pre-built dashboards and Service Level Objectives.
* (**ecs**) This release introduces a new cluster configuration to support the customer-managed keys for ECS managed storage encryption.
* (**imagebuilder**) This release updates the regex pattern for Image Builder ARNs.

## [1.2.29] - 06/07/2024

### Features
* (**auditmanager**) New feature: common controls. When creating custom controls, you can now use pre-grouped AWS data sources based on common compliance themes. Also, the awsServices parameter is deprecated because we now manage services in scope for you. If used, the input is ignored and an empty list is returned.
* (**b2bi**) Added exceptions to B2Bi List operations and ConflictException to B2Bi StartTransformerJob operation. Also made capabilities field explicitly required when creating a Partnership.
* (**codepipeline**) CodePipeline now supports overriding S3 Source Object Key during StartPipelineExecution, as part of Source Overrides.
* (**sagemaker**) This release introduces a new optional parameter: InferenceAmiVersion, in ProductionVariant.
* (**verifiedpermissions**) This release adds OpenIdConnect (OIDC) configuration support for IdentitySources, allowing for external IDPs to be used in authorization requests.

## [1.2.28] - 06/06/2024

### Features
* (**account**) This release adds 3 new APIs (AcceptPrimaryEmailUpdate, GetPrimaryEmail, and StartPrimaryEmailUpdate) used to centrally manage the root user email address of member accounts within an AWS organization.
* (**firehose**) Adds integration with Secrets Manager for Redshift, Splunk, HttpEndpoint, and Snowflake destinations
* (**fsx**) This release adds support to increase metadata performance on FSx for Lustre file systems beyond the default level provisioned when a file system is created. This can be done by specifying MetadataConfiguration during the creation of Persistent_2 file systems or by updating it on demand.
* (**glue**) This release adds support for creating and updating Glue Data Catalog Views.
* (**iotwireless**) Adds support for wireless device to be in Conflict FUOTA Device Status due to a FUOTA Task, so it couldn't be attached to a new one.
* (**location**) Added two new APIs, VerifyDevicePosition and ForecastGeofenceEvents. Added support for putting larger geofences up to 100,000 vertices with Geobuf fields.
* (**storagegateway**) Adds SoftwareUpdatePreferences to DescribeMaintenanceStartTime and UpdateMaintenanceStartTime, a structure which contains AutomaticUpdatePolicy.

### Documentation
* (**sns**) Doc-only update for SNS. These changes include customer-reported issues and TXC3 updates.
* (**sqs**) Doc only updates for SQS. These updates include customer-reported issues and TCX3 modifications.

## [1.2.27] - 06/05/2024

### Features
* (**globalaccelerator**) This release contains a new optional ip-addresses input field for the update accelerator and update custom routing accelerator apis. This input enables consumers to replace IPv4 addresses on existing accelerators with addresses provided in the input.
* (**glue**) AWS Glue now supports native SaaS connectivity: Salesforce connector available now
* (**s3**) Added new params copySource and key to copyObject API for supporting S3 Access Grants plugin. These changes will not change any of the existing S3 API functionality.

## [1.2.26] - 06/04/2024

### Features
* (**ec2**) U7i instances with up to 32 TiB of DDR5 memory and 896 vCPUs are now available. C7i-flex instances are launched and are lower-priced variants of the Amazon EC2 C7i instances that offer a baseline level of CPU performance with the ability to scale up to the full compute performance 95% of the time.
* (**pipes**) This release adds Timestream for LiveAnalytics as a supported target in EventBridge Pipes
* (**sagemaker**) Extend DescribeClusterNode response with private DNS hostname and IP address, and placement information about availability zone and availability zone ID.
* (**taxsettings**) Initial release of AWS Tax Settings API

## [1.2.25] - 06/03/2024

### Features
* (**batch**) This release adds support for the AWS Batch GetJobQueueSnapshot API operation.
* (**eks**) Adds support for EKS add-ons pod identity associations integration
* (**iottwinmaker**) Support RESET_VALUE UpdateType for PropertyUpdates to reset property value to default or null

### Documentation
* (**amplify**) This doc-only update identifies fields that are specific to Gen 1 and Gen 2 applications.

## [1.2.24] - 05/31/2024

### Features
* (**codegurusecurity**) This release includes minor model updates and documentation updates.
* (**launchwizard**) This release adds support for describing workload deployment specifications, deploying additional workload types, and managing tags for Launch Wizard resources with API operations.

### Fixes
* [#1315](https://github.com/awslabs/aws-sdk-kotlin/issues/1315) Disable proxying of requests made to EC2 IMDS

### Documentation
* (**codebuild**) AWS CodeBuild now supports Self-hosted GitHub Actions runners for Github Enterprise
* (**elasticache**) Update to attributes of TestFailover and minor revisions.

### Miscellaneous
* [#1303](https://github.com/awslabs/aws-sdk-kotlin/issues/1303) Add trailing slash to base IMDS endpoint

## [1.2.23] - 05/30/2024

### Features
* (**acm**) add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**bedrockagent**) With this release, Knowledge bases for Bedrock adds support for Titan Text Embedding v2.
* (**bedrockruntime**) This release adds Converse and ConverseStream APIs to Bedrock Runtime
* (**cloudtrail**) CloudTrail Lake returns PartitionKeys in the GetEventDataStore API response. Events are grouped into partitions based on these keys for better query performance. For example, the calendarday key groups events by day, while combining the calendarday key with the hour key groups them by day and hour.
* (**connect**) Adding associatedQueueIds as a SearchCriteria and response field to the SearchRoutingProfiles API
* (**emrserverless**) The release adds support for spark structured streaming.
* (**sagemaker**) Adds Model Card information as a new component to Model Package. Autopilot launches algorithm selection for TimeSeries modality to generate AutoML candidates per algorithm.

### Documentation
* (**rds**) Updates Amazon RDS documentation for Aurora Postgres DBname.

## [1.2.22] - 05/29/2024

### Features
* (**athena**) Throwing validation errors on CreateNotebook with Name containing `/`,`:`,`\`
* (**codebuild**) AWS CodeBuild now supports manually creating GitHub webhooks
* (**connect**) This release includes changes to DescribeContact API's response by including ConnectedToSystemTimestamp, RoutingCriteria, Customer, Campaign, AnsweringMachineDetectionStatus, CustomerVoiceActivity, QualityMetrics, DisconnectDetails, and SegmentAttributes information from a contact in Amazon Connect.
* (**glue**) Add optional field JobMode to CreateJob and UpdateJob APIs.
* (**securityhub**) Add ROOT type for TargetType model

## [1.2.21] - 05/28/2024

### Features
* (**ec2**) Providing support to accept BgpAsnExtended attribute
* (**kafka**) Adds ControllerNodeInfo in ListNodes response to support Raft mode for MSK
* (**swf**) This release adds new APIs for deleting activity type and workflow type resources.

### Documentation
* (**dynamodb**) Doc-only update for DynamoDB. Specified the IAM actions needed to authorize a user to create a table with a resource-based policy.

## [1.2.20] - 05/24/2024

### Features
* (**iotfleetwise**) AWS IoT FleetWise now supports listing vehicles with attributes filter, ListVehicles API is updated to support additional attributes filter.

### Documentation
* (**dynamodb**) Documentation only updates for DynamoDB.
* (**managedblockchain**) This is a minor documentation update to address the impact of the shut down of the Goerli and Polygon networks.

## [1.2.19] - 05/23/2024

### Features
* (**emrserverless**) This release adds the capability to run interactive workloads using Apache Livy Endpoint.

### Documentation
* (**opsworks**) Documentation-only update for OpsWorks Stacks.

## [1.2.18] - 05/22/2024

### Features
* (**chatbot**) This change adds support for tagging Chatbot configurations.
* (**cloudformation**) Added DeletionMode FORCE_DELETE_STACK for deleting a stack that is stuck in DELETE_FAILED state due to resource deletion failure.
* (**kms**) This release includes feature to import customer's asymmetric (RSA, ECC and SM2) and HMAC keys into KMS in China.
* (**opensearch**) This release adds support for enabling or disabling a data source configured as part of Zero-ETL integration with Amazon S3, by setting its status.
* (**wafv2**) You can now use Security Lake to collect web ACL traffic data.

## [1.2.17] - 05/21/2024

### Features
* (**cloudfront**) Model update; no change to SDK functionality.
* (**glue**) Add Maintenance window to CreateJob and UpdateJob APIs and JobRun response. Add a new Job Run State for EXPIRED.
* (**lightsail**) This release adds support for Amazon Lightsail instances to switch between dual-stack or IPv4 only and IPv6-only public IP address types.
* (**mailmanager**) This release includes a new Amazon SES feature called Mail Manager, which is a set of email gateway capabilities designed to help customers strengthen their organization's email infrastructure, simplify email workflow management, and streamline email compliance control.
* (**pi**) Performance Insights added a new input parameter called AuthorizedActions to support the fine-grained access feature. Performance Insights also restricted the acceptable input characters.
* (**storagegateway**) Added new SMBSecurityStrategy enum named MandatoryEncryptionNoAes128, new mode enforces encryption and disables AES 128-bit algorithums.

### Documentation
* (**rds**) Updates Amazon RDS documentation for Db2 license through AWS Marketplace.

## [1.2.16] - 05/20/2024

### Features
* (**bedrockagent**) This release adds support for using Guardrails with Bedrock Agents.
* (**bedrockagentruntime**) This release adds support for using Guardrails with Bedrock Agents.
* (**controltower**) Added ListControlOperations API and filtering support for ListEnabledControls API. Updates also includes added metadata for enabled controls and control operations.
* (**osis**) Add support for creating an OpenSearch Ingestion pipeline that is attached to a provided VPC. Add information about the destinations of an OpenSearch Ingestion pipeline to the GetPipeline and ListPipelines APIs.
* (**rds**) This release adds support for EngineLifecycleSupport on DBInstances, DBClusters, and GlobalClusters.
* (**secretsmanager**) add v2 smoke tests and smithy smokeTests trait for SDK testing

## [1.2.15] - 05/17/2024

### Features
* (**applicationautoscaling**) add v2 smoke tests and smithy smokeTests trait for SDK testing.
* (**codebuild**) Aws CodeBuild now supports 36 hours build timeout
* (**elasticloadbalancingv2**) This release adds dualstack-without-public-ipv4 IP address type for ALB.
* (**lakeformation**) Introduces a new API, GetDataLakePrincipal, that returns the identity of the invoking principal
* (**transfer**) Enable use of CloudFormation traits in Smithy model to improve generated CloudFormation schema from the Smithy API model.

### Miscellaneous
* [#1092](https://github.com/smithy-lang/smithy-kotlin/issues/1092) Upgrade to version [**1.2.4**](https://github.com/smithy-lang/smithy-kotlin/releases/tag/v1.2.4) of **smithy-kotlin** to pick up fix for `http.nonProxyHosts` wildcards

## [1.2.14] - 05/16/2024

### Features
* (**acmpca**) This release adds support for waiters to fail on AccessDeniedException when having insufficient permissions
* (**kafka**) AWS MSK support for Broker Removal.
* (**mwaa**) Amazon MWAA now supports Airflow web server auto scaling to automatically handle increased demand from REST APIs, Command Line Interface (CLI), or more Airflow User Interface (UI) users. Customers can specify maximum and minimum web server instances during environment creation and update workflow.
* (**quicksight**) This release adds DescribeKeyRegistration and UpdateKeyRegistration APIs to manage QuickSight Customer Managed Keys (CMK).
* (**sagemaker**) Introduced WorkerAccessConfiguration to SageMaker Workteam. This allows customers to configure resource access for workers in a workteam.

### Documentation
* (**connect**) Adding Contact Flow metrics to the GetMetricDataV2 API
* (**secretsmanager**) Documentation updates for AWS Secrets Manager

## [1.2.13] - 05/15/2024

### Features
* (**bedrockagentruntime**) Updating Bedrock Knowledge Base Metadata & Filters feature with two new filters listContains and stringContains
* (**codebuild**) CodeBuild Reserved Capacity VPC Support
* (**datasync**) Task executions now display a CANCELLING status when an execution is in the process of being cancelled.
* (**grafana**) This release adds new ServiceAccount and ServiceAccountToken APIs.
* (**medicalimaging**) Added support for importing medical imaging data from Amazon S3 buckets across accounts and regions.

### Documentation
* (**securityhub**) Documentation-only update for AWS Security Hub

## [1.2.12] - 05/14/2024

### Features
* (**connect**) Amazon Connect provides enhanced search capabilities for flows & flow modules on the Connect admin website and programmatically using APIs. You can search for flows and flow modules by name, description, type, status, and tags, to filter and identify a specific flow in your Connect instances.
* (**s3**) Updated a few x-id in the http uri traits

### Miscellaneous
* Remove Alexa for Business service
* Remove Honeycode service

## [1.2.11] - 05/13/2024

### Features
* (**eventbridge**) Amazon EventBridge introduces KMS customer-managed key (CMK) encryption support for custom and partner events published on EventBridge Event Bus (including default bus) and UpdateEventBus API.
* (**vpclattice**) This release adds TLS Passthrough support. It also increases max number of target group per rule to 10.

## [1.2.10] - 05/10/2024

### Features
* (**applicationdiscoveryservice**) add v2 smoke tests and smithy smokeTests trait for SDK testing
* (**greengrassv2**) Mark ComponentVersion in ComponentDeploymentSpecification as required.
* (**sagemaker**) Introduced support for G6 instance types on Sagemaker Notebook Instances and on SageMaker Studio for JupyterLab and CodeEditor applications.
* (**ssooidc**) Updated request parameters for PKCE support.

## [1.2.9] - 05/09/2024

### Features
* (**bedrockagentruntime**) This release adds support to provide guardrail configuration and modify inference parameters that are then used in RetrieveAndGenerate API in Agents for Amazon Bedrock.
* (**pinpoint**) This release adds support for specifying email message headers for Email Templates, Campaigns, Journeys and Send Messages.
* (**ssmsap**) Added support for application-aware start/stop of SAP applications running on EC2 instances, with SSM for SAP
* (**verifiedpermissions**) Adds policy effect and actions fields to Policy API's.

### Documentation
* (**route53resolver**) Update the DNS Firewall settings to correct a spelling issue.

## [1.2.8] - 05/08/2024

### Features
* (**cognitoidentityprovider**) Add EXTERNAL_PROVIDER enum value to UserStatusType.
* (**ec2**) Adding Precision Hardware Clock (PHC) to public API DescribeInstanceTypes
* (**ecr**) This release adds pull through cache rules support for GitLab container registry in Amazon ECR.
* (**polly**) Add new engine - generative - that builds the most expressive conversational voices.
* (**sqs**) This release adds MessageSystemAttributeNames to ReceiveMessageRequest to replace AttributeNames.

### Documentation
* (**fms**) The policy scope resource tag is always a string value, either a non-empty string or an empty string.

## [1.2.7] - 05/07/2024

### Features
* (**budgets**) This release adds tag support for budgets and budget actions.
* (**resiliencehub**) AWS Resilience Hub has expanded its drift detection capabilities by introducing a new type of drift detection - application resource drift. This new enhancement detects changes, such as the addition or deletion of resources within the application's input sources.

### Documentation
* (**b2bi**) Documentation update to clarify the MappingTemplate definition.
* (**route53profiles**) Doc only update for Route 53 profiles that fixes some link  issues

## [1.2.6] - 05/06/2024

### Features
* (**medialive**) AWS Elemental MediaLive now supports configuring how SCTE 35 passthrough triggers segment breaks in HLS and MediaPackage output groups. Previously, messages triggered breaks in all these output groups. The new option is to trigger segment breaks only in groups that have SCTE 35 passthrough enabled.

## [1.2.5] - 05/03/2024

### Features
* (**bedrockagent**) This release adds support for using Provisioned Throughput with Bedrock Agents.
* (**connect**) This release adds 5 new APIs for managing attachments: StartAttachedFileUpload, CompleteAttachedFileUpload, GetAttachedFile, BatchGetAttachedFileMetadata, DeleteAttachedFile. These APIs can be used to programmatically upload and download attachments to Connect resources, like cases.
* (**connectcases**) This feature supports the release of Files related items
* (**inspector2**) This release adds CSV format to GetCisScanReport for Inspector v2
* (**sagemaker**) Amazon SageMaker Inference now supports m6i, c6i, r6i, m7i, c7i, r7i and g5 instance types for Batch Transform Jobs
* (**sesv2**) Adds support for specifying replacement headers per BulkEmailEntry in SendBulkEmail in SESv2.

### Documentation
* (**datasync**) Updated guidance on using private or self-signed certificate authorities (CAs) with AWS DataSync object storage locations.

## [1.2.4] - 05/02/2024

### Features
* (**dynamodb**) This release adds support to specify an optional, maximum OnDemandThroughput for DynamoDB tables and global secondary indexes in the CreateTable or UpdateTable APIs. You can also override the OnDemandThroughput settings by calling the ImportTable, RestoreFromPointInTime, or RestoreFromBackup APIs.
* (**ec2**) This release includes a new API for retrieving the public endorsement key of the EC2 instance's Nitro Trusted Platform Module (NitroTPM).
* (**personalize**) This releases ability to delete users and their data, including their metadata and interactions data, from a dataset group.
* (**redshiftserverless**) Update Redshift Serverless List Scheduled Actions Output Response to include Namespace Name.

## [1.2.3] - 05/01/2024

### Features
* (**bedrockagent**) This release adds support for using MongoDB Atlas as a vector store when creating a knowledge base.
* (**personalizeruntime**) This release adds support for a Reason attribute for predicted items generated by User-Personalization-v2.
* (**securityhub**) Updated CreateMembers API request with limits.
* (**sesv2**) Fixes ListContacts and ListImportJobs APIs to use POST instead of GET.

### Documentation
* (**ec2**) Documentation updates for Amazon EC2.

## [1.2.2] - 04/30/2024

### Features
* (**chimesdkvoice**) Due to changes made by the Amazon Alexa service, GetSipMediaApplicationAlexaSkillConfiguration and PutSipMediaApplicationAlexaSkillConfiguration APIs are no longer available for use. For more information, refer to the Alexa Smart Properties page.
* (**codeartifact**) Add support for the Ruby package format.
* (**fms**) AWS Firewall Manager now supports the network firewall service stream exception policy feature for accounts within your organization.
* (**omics**) Add support for workflow sharing and dynamic run storage
* (**opensearch**) This release enables customers to create Route53 A and AAAA alias record types to point custom endpoint domain to OpenSearch domain's dualstack search endpoint.
* (**pinpointsmsvoicev2**) Amazon Pinpoint has added two new features Multimedia services (MMS) and protect configurations. Use the three new MMS APIs to send media messages to a mobile phone which includes image, audio, text, or video files. Use the ten new protect configurations APIs to block messages to specific countries.
* (**qbusiness**) This is a general availability (GA) release of Amazon Q Business. Q Business enables employees in an enterprise to get comprehensive answers to complex questions and take actions through a unified, intuitive web-based chat experience - using an enterprise's existing content, data, and systems.
* (**quicksight**) New Q embedding supporting Generative Q&A
* (**route53resolver**) Release of FirewallDomainRedirectionAction parameter on the Route 53 DNS Firewall Rule.  This allows customers to configure a DNS Firewall rule to inspect all the domains in the DNS redirection chain (default) , such as CNAME, ALIAS, DNAME, etc., or just the first domain and trust the rest.
* (**sagemaker**) Amazon SageMaker Training now supports the use of attribute-based access control (ABAC) roles for training job execution roles. Amazon SageMaker Inference now supports G6 instance types.

### Documentation
* (**signer**) Documentation updates for AWS Signer. Adds cross-account signing constraint and definitions for cross-account actions.

## [1.2.1] - 04/29/2024

### Features
* (**amplify**) Updating max results limit for listing any resources (Job, Artifacts, Branch, BackendResources, DomainAssociation) to 50 with the exception of list apps that where max results can be up to 100.
* (**connectcases**) This feature releases DeleteField, DeletedLayout, and DeleteTemplate API's
* (**inspector2**) Update Inspector2 to include new Agentless API parameters.
* (**timestreamquery**) This change allows users to update and describe account settings associated with their accounts.
* (**transcribe**) This update provides error messaging for generative call summarization in Transcribe Call Analytics
* (**trustedadvisor**) This release adds the BatchUpdateRecommendationResourceExclusion API to support batch updates of Recommendation Resource exclusion statuses and introduces a new exclusion status filter to the ListRecommendationResources and ListOrganizationRecommendationResources APIs.

## [1.2.0] - 04/27/2024

### Fixes
* [#1211](https://github.com/awslabs/aws-sdk-kotlin/issues/1211) ⚠️ **IMPORTANT**: Apply clock skew interceptor to clients created via `invoke`

### Miscellaneous
* ⚠️ **IMPORTANT**: Upgrade to latest versions of OkHttp, Okio, Kotlin

## [1.1.27] - 04/26/2024

### Features
* (**codepipeline**) Add ability to manually and automatically roll back a pipeline stage to a previously successful execution.
* (**cognitoidentityprovider**) Add LimitExceededException to SignUp errors
* (**connectcampaigns**) This release adds support for specifying if Answering Machine should wait for prompt sound.
* (**marketplaceentitlementservice**) Releasing minor endpoint updates.
* (**oam**) This release introduces support for Source Accounts to define which Metrics and Logs to share with the Monitoring Account
* (**rds**) SupportsLimitlessDatabase field added to describe-db-engine-versions to indicate whether the DB engine version supports Aurora Limitless Database.
* (**support**) Releasing minor endpoint updates.

## [1.1.26] - 04/25/2024

### Features
* (**appsync**) UpdateGraphQLAPI documentation update and datasource introspection secret arn update
* (**fms**) AWS Firewall Manager adds support for network ACL policies to manage Amazon Virtual Private Cloud (VPC) network access control lists (ACLs) for accounts in your organization.
* (**ivs**) Bug Fix: IVS does not support arns with the `svs` prefix
* (**ivsrealtime**) Bug Fix: IVS Real Time does not support ARNs using the `svs` prefix.
* (**sfn**) Add new ValidateStateMachineDefinition operation, which performs syntax checking on the definition of a Amazon States Language (ASL) state machine.

### Documentation
* (**rds**) Updates Amazon RDS documentation for setting local time zones for RDS for Db2 DB instances.

## [1.1.25] - 04/24/2024

### Features
* (**datasync**) This change allows users to disable and enable the schedules associated with their tasks.
* (**ec2**) Launching capability for customers to enable or disable automatic assignment of public IPv4 addresses to their network interface
* (**emrcontainers**) EMRonEKS Service support for SecurityConfiguration enforcement for Spark Jobs.
* (**entityresolution**) Support Batch Unique IDs Deletion.
* (**gamelift**) Amazon GameLift releases container fleets support for public preview. Deploy Linux-based containerized game server software for hosting on Amazon GameLift.
* (**ssm**) Add SSM DescribeInstanceProperties API to public AWS SDK.

## [1.1.24] - 04/23/2024

### Features
* (**bedrock**) This release introduces Model Evaluation and Guardrails for Amazon Bedrock.
* (**bedrockagent**) Introducing the ability to create multiple data sources per knowledge base, specify S3 buckets as data sources from external accounts, and exposing levers to define the deletion behavior of the underlying vector store data.
* (**bedrockagentruntime**) This release introduces zero-setup file upload support for the RetrieveAndGenerate API. This allows you to chat with your data without setting up a Knowledge Base.
* (**bedrockruntime**) This release introduces Guardrails for Amazon Bedrock.
* (**costexplorer**) Added additional metadata that might be applicable to your reservation recommendations.
* (**ec2**) This release introduces EC2 AMI Deregistration Protection, a new AMI property that can be enabled by customers to protect an AMI against an unintended deregistration. This release also enables the AMI owners to view the AMI 'LastLaunchedTime' in DescribeImages API.
* (**workspacesweb**) Added InstanceType and MaxConcurrentSessions parameters on CreatePortal and UpdatePortal Operations as well as the ability to read Customer Managed Key & Additional Encryption Context parameters on supported resources (Portal, BrowserSettings, UserSettings, IPAccessSettings)

### Documentation
* (**pi**) Clarifies how aggregation works for GetResourceMetrics in the Performance Insights API.
* (**rds**) Fix the example ARN for ModifyActivityStreamRequest

## [1.1.23] - 04/22/2024

### Features
* (**bedrockagent**) Releasing the support for simplified configuration and return of control
* (**bedrockagentruntime**) Releasing the support for simplified configuration and return of control
* (**paymentcryptography**) Adding support to TR-31/TR-34 exports for optional headers, allowing customers to add additional metadata (such as key version and KSN) when exporting keys from the service.
* (**route53profiles**) Route 53 Profiles allows you to apply a central DNS configuration across many VPCs regardless of account.
* (**sagemaker**) This release adds support for Real-Time Collaboration and Shared Space for JupyterLab App on SageMaker Studio.
* (**transfer**) Adding new API to support remote directory listing using SFTP connector

### Fixes
* [#900](https://github.com/awslabs/aws-sdk-kotlin/issues/900) Correctly generate presigners, waiters, and paginators for resource operations

### Documentation
* (**redshiftserverless**) Updates description of schedule field for scheduled actions.
* (**servicediscovery**) This release adds examples to several Cloud Map actions.

## [1.1.22] - 04/19/2024

### Features
* (**glue**) Adding RowFilter in the response for GetUnfilteredTableMetadata API
* (**internetmonitor**) This update introduces the GetInternetEvent and ListInternetEvents APIs, which provide access to internet events displayed on the Amazon CloudWatch Internet Weather Map.
* (**personalize**) This releases auto training capability while creating a solution and automatically syncing latest solution versions when creating/updating a campaign
* Customize S3's `Expires` field, including adding a new `ExpiresString` field for output types.

### Fixes
* [#1210](https://github.com/awslabs/aws-sdk-kotlin/issues/1210) Service client based identity providers now respect AWS_ENDPOINT_URL_SERVICE environment variables

## [1.1.21] - 04/18/2024

### Features
* (**drs**) Outpost ARN added to Source Server and Recovery Instance
* (**emrserverless**) This release adds the capability to publish detailed Spark engine metrics to Amazon Managed Service for Prometheus (AMP) for  enhanced monitoring for Spark jobs.
* (**guardduty**) Added IPv6Address fields for local and remote IP addresses
* (**quicksight**) This release adds support for the Cross Sheet Filter and Control features, and support for warnings in asset imports for any permitted errors encountered during execution
* (**rolesanywhere**) This release introduces the PutAttributeMapping and DeleteAttributeMapping APIs. IAM Roles Anywhere now provides the capability to define a set of mapping rules, allowing customers to specify which data is extracted from their X.509 end-entity certificates.
* (**sagemaker**) Removed deprecated enum values and updated API documentation.
* (**workspaces**) Adds new APIs for managing and sharing WorkSpaces BYOL configuration across accounts.

## [1.1.20] - 04/17/2024

### Features
* (**ec2**) Documentation updates for Elastic Compute Cloud (EC2).
* (**qbusiness**) This release adds support for IAM Identity Center (IDC) as the identity gateway for Q Business. It also allows users to provide an explicit intent for Q Business to identify how the Chat request should be handled.

## [1.1.19] - 04/16/2024

### Features
* (**bedrockagent**) For Create Agent API, the agentResourceRoleArn parameter is no longer required.
* (**emrserverless**) This release adds support for shuffle optimized disks that allow larger disk sizes and higher IOPS to efficiently run shuffle heavy workloads.
* (**entityresolution**) Cross Account Resource Support .
* (**iotwireless**) Add PublicGateways in the GetWirelessStatistics call response, indicating the LoRaWAN public network accessed by the device.
* (**lakeformation**) This release adds Lake Formation managed RAM support for the 4 APIs - "DescribeLakeFormationIdentityCenterConfiguration", "CreateLakeFormationIdentityCenterConfiguration", "DescribeLakeFormationIdentityCenterConfiguration", and "DeleteLakeFormationIdentityCenterConfiguration"
* (**m2**) Adding new ListBatchJobRestartPoints API and support for restart batch job.
* (**mediapackagev2**) Dash v2 is a MediaPackage V2 feature to support egressing on DASH manifest format.
* (**outposts**) This release adds new APIs to allow customers to configure their Outpost capacity at order-time.
* (**wellarchitected**) AWS Well-Architected now has a Connector for Jira to allow customers to efficiently track workload risks and improvement efforts and create closed-loop mechanisms.

## [1.1.18] - 04/12/2024

### Features
* (**cloudformation**) Adding support for the new parameter "IncludePropertyValues" in the CloudFormation DescribeChangeSet API. When this parameter is included, the DescribeChangeSet response will include more detailed information such as before and after values for the resource properties that will change.
* (**glue**) Modifying request for GetUnfilteredTableMetadata for view-related fields.
* (**healthlake**) Added new CREATE_FAILED status for data stores. Added new errorCause to DescribeFHIRDatastore API and ListFHIRDatastores API response for additional insights into data store creation and deletion workflows.
* (**kms**) This feature supports the ability to specify a custom rotation period for automatic key rotations, the ability to perform on-demand key rotations, and visibility into your key material rotations.
* (**mediatailor**) Added InsertionMode to PlaybackConfigurations. This setting controls whether players can use stitched or guided ad insertion. The default for players that do not specify an insertion mode is stitched.
* (**outposts**) This release adds EXPEDITORS as a valid shipment carrier.
* (**redshift**) Adds support for Amazon Redshift DescribeClusterSnapshots API to include Snapshot ARN response field.
* (**transfer**) This change releases support for importing self signed certificates to the Transfer Family for sending outbound file transfers over TLS/HTTPS.

### Documentation
* (**configservice**) Updates documentation for AWS Config
* (**iotfleethub**) Documentation updates for AWS IoT Fleet Hub to clarify that Fleet Hub supports organization instance of IAM Identity Center.
* (**neptunegraph**) Update to API documentation to resolve customer reported issues.

## [1.1.17] - 04/11/2024

### Features
* (**batch**) This release adds the task properties field to attempt details and the name field on EKS container detail.
* (**cloudfront**) CloudFront origin access control extends support to AWS Lambda function URLs and AWS Elemental MediaPackage v2 origins.
* (**cloudwatch**) This release adds support for Metric Characteristics for CloudWatch Anomaly Detection. Anomaly Detector now takes Metric Characteristics object with Periodic Spikes boolean field that tells Anomaly Detection that spikes that repeat at the same time every week are part of the expected pattern.
* (**iam**) For CreateOpenIDConnectProvider API, the ThumbprintList parameter is no longer required.
* (**medialive**) AWS Elemental MediaLive introduces workflow monitor, a new feature that enables the visualization and monitoring of your media workflows. Create signal maps of your existing workflows and monitor them by creating notification and monitoring template groups.
* (**omics**) This release adds support for retrieval of S3 direct access metadata on sequence stores and read sets, and adds support for SHA256up and SHA512up HealthOmics ETags.
* (**pipes**) LogConfiguration ARN validation fixes
* (**wafv2**) Adds an updated version of smoke tests, including smithy trait, for SDK testing.

### Documentation
* (**codebuild**) Support access tokens for Bitbucket sources
* (**rds**) Updates Amazon RDS documentation for Standard Edition 2 support in RDS Custom for Oracle.
* (**s3control**) Documentation updates for Amazon S3-control.

## [1.1.16] - 04/10/2024

### Features
* (**cleanrooms**) AWS Clean Rooms Differential Privacy is now fully available. Differential privacy protects against user-identification attempts.
* (**connect**) This release adds new Submit Auto Evaluation Action for Amazon Connect Rules.
* (**qconnect**) This release adds a new QiC public API updateSession and updates an existing QiC public API createSession
* (**rekognition**) Added support for ContentType to content moderation detections.
* (**supplychain**) This release includes API SendDataIntegrationEvent for AWS Supply Chain
* (**workspacesthinclient**) Adding tags field to SoftwareSet. Removing tags fields from Summary objects. Changing the list of exceptions in tagging APIs. Fixing an issue where the SDK returns empty tags in Get APIs.

### Documentation
* (**networkmonitor**) Examples were added to CloudWatch Network Monitor commands.

## [1.1.15] - 04/09/2024

### Features
* (**codebuild**) Add new webhook filter types for GitHub webhooks
* (**mediaconvert**) This release includes support for bringing your own fonts to use for burn-in or DVB-Sub captioning workflows.
* (**pinpoint**) The OrchestrationSendingRoleArn has been added to the email channel and is used to send emails from campaigns or journeys.
* (**rds**) This release adds support for specifying the CA certificate to use for the new db instance when restoring from db snapshot, restoring from s3, restoring to point in time, and creating a db instance read replica.

## [1.1.13] - 04/05/2024

### Features
* (**quicksight**) Adding IAMIdentityCenterInstanceArn parameter to CreateAccountSubscription
* (**resourcegroups**) Added a new QueryErrorCode RESOURCE_TYPE_NOT_SUPPORTED that is returned by the ListGroupResources operation if the group query contains unsupported resource types.
* (**verifiedpermissions**) Adding BatchIsAuthorizedWithToken API which supports multiple authorization requests against a PolicyStore given a bearer token.

## [1.1.12] - 04/04/2024

### Features
* (**b2bi**) Adding support for X12 5010 HIPAA EDI version and associated transaction sets.
* (**cleanrooms**) Feature: New schemaStatusDetails field to the existing Schema object that displays a status on Schema API responses to show whether a schema is queryable or not. New BatchGetSchemaAnalysisRule API to retrieve multiple schemaAnalysisRules using a single API call.
* (**ec2**) Amazon EC2 G6 instances powered by NVIDIA L4 Tensor Core GPUs can be used for a wide range of graphics-intensive and machine learning use cases. Gr6 instances also feature NVIDIA L4 GPUs and can be used for graphics workloads with higher memory requirements.
* (**emrcontainers**) This release adds support for integration with EKS AccessEntry APIs to enable automatic Cluster Access for EMR on EKS.
* (**ivs**) API update to include an SRT ingest endpoint and passphrase for all channels.
* (**verifiedpermissions**) Adds GroupConfiguration field to Identity Source API's

## [1.1.11] - 04/03/2024

### Features
* (**cleanroomsml**) The release includes a public SDK for AWS Clean Rooms ML APIs, making them globally available to developers worldwide.
* (**cloudformation**) This release would return a new field - PolicyAction in cloudformation's existed DescribeChangeSetResponse, showing actions we are going to apply on the physical resource (e.g., Delete, Retain) according to the user's template
* (**datazone**) This release supports the feature of dataQuality to enrich asset with dataQualityResult in Amazon DataZone.
* (**docdb**) This release adds Global Cluster Switchover capability which enables you to change your global cluster's primary AWS Region, the region that serves writes, while preserving the replication between all regions in the global cluster.
* (**groundstation**) This release adds visibilityStartTime and visibilityEndTime to DescribeContact and ListContacts responses.
* (**lambda**) Add Ruby 3.3 (ruby3.3) support to AWS Lambda
* (**medialive**) Cmaf Ingest outputs are now supported in Media Live
* (**medicalimaging**) SearchImageSets API now supports following enhancements - Additional support for searching on UpdatedAt and SeriesInstanceUID - Support for searching existing filters between dates/times - Support for sorting the search result by Ascending/Descending - Additional parameters returned in the response
* (**transfer**) Add ability to specify Security Policies for SFTP Connectors

## [1.1.10] - 04/02/2024

### Features
* (**glue**) Adding View related fields to responses of read-only Table APIs.
* (**rolesanywhere**) This release increases the limit on the roleArns request parameter for the *Profile APIs that support it. This parameter can now take up to 250 role ARNs.

### Documentation
* (**ecs**) Documentation only update for Amazon ECS.
* (**ivschat**) Doc-only update. Changed "Resources" to "Key Concepts" in docs and updated text.
* (**securityhub**) Documentation updates for AWS Security Hub

## [1.1.9] - 04/01/2024

### Features
* (**cloudwatch**) This release adds support for CloudWatch Anomaly Detection on cross-account metrics. SingleMetricAnomalyDetector and MetricDataQuery inputs to Anomaly Detection APIs now take an optional AccountId field.
* (**datazone**) This release supports the feature of AI recommendations for descriptions to enrich the business data catalog in Amazon DataZone.
* (**deadline**) AWS Deadline Cloud is a new fully managed service that helps customers set up, deploy, and scale rendering projects in minutes, so they can improve the efficiency of their rendering pipelines and take on more projects.
* (**lightsail**) This release adds support to upgrade the TLS version of the distribution.

### Documentation
* (**emr**) This release fixes a broken link in the documentation.

## [1.1.8] - 03/29/2024

### Features
* (**b2bi**) Supporting new EDI X12 transaction sets for X12 versions 4010, 4030, and 5010.
* (**codebuild**) Add new fleet status code for Reserved Capacity.
* (**codeconnections**) Duplicating the CodeStar Connections service into the new, rebranded AWS CodeConnections service.
* (**internetmonitor**) This release adds support to allow customers to track cross account monitors through ListMonitor, GetMonitor, ListHealthEvents, GetHealthEvent, StartQuery APIs.
* (**iotwireless**) Add support for retrieving key historical and live metrics for LoRaWAN devices and gateways
* (**marketplacecatalog**) This release enhances the ListEntities API to support ResaleAuthorizationId filter and sort for OfferEntity in the request and the addition of a ResaleAuthorizationId field in the response of OfferSummary.
* (**neptunegraph**) Add the new API Start-Import-Task for Amazon Neptune Analytics.
* (**sagemaker**) This release adds support for custom images for the CodeEditor App on SageMaker Studio

## [1.1.7] - 03/28/2024

### Features
* (**codecatalyst**) This release adds support for understanding pending changes to subscriptions by including two new response parameters for the GetSubscription API for Amazon CodeCatalyst.
* (**computeoptimizer**) This release enables AWS Compute Optimizer to analyze and generate recommendations with a new customization preference, Memory Utilization.
* (**ec2**) Amazon EC2 C7gd, M7gd and R7gd metal instances with up to 3.8 TB of local NVMe-based SSD block-level storage have up to 45% improved real-time NVMe storage performance than comparable Graviton2-based instances.
* (**eks**) Add multiple customer error code to handle customer caused failure when managing EKS node groups
* (**guardduty**) Add EC2 support for GuardDuty Runtime Monitoring auto management.
* (**neptunegraph**) Update ImportTaskCancelled waiter to evaluate task state correctly and minor documentation changes.
* (**oam**) This release adds support for sharing AWS::InternetMonitor::Monitor resources.
* (**quicksight**) Amazon QuickSight: Adds support for setting up VPC Endpoint restrictions for accessing QuickSight Website.

## [1.1.6] - 03/27/2024

### Features
* (**batch**) This feature allows AWS Batch to support configuration of imagePullSecrets and allowPrivilegeEscalation for jobs running on EKS
* (**bedrockagent**) This changes introduces metadata documents statistics and also updates the documentation for bedrock agent.
* (**bedrockagentruntime**) This release introduces filtering support on Retrieve and RetrieveAndGenerate APIs.
* (**elasticache**) Added minimum capacity to  Amazon ElastiCache Serverless. This feature allows customer to ensure minimum capacity even without current load

### Documentation
* (**secretsmanager**) Documentation updates for Secrets Manager

## [1.1.5] - 03/26/2024

### Features
* (**bedrockagentruntime**) This release adds support to customize prompts sent through the RetrieveAndGenerate API in Agents for Amazon Bedrock.
* (**costexplorer**) Adds support for backfill of cost allocation tags, with new StartCostAllocationTagBackfill and ListCostAllocationTagBackfillHistory API.
* (**ec2**) Documentation updates for Elastic Compute Cloud (EC2).
* (**finspace**) Add new operation delete-kx-cluster-node and add status parameter to list-kx-cluster-node operation.

### Documentation
* (**ecs**) This is a documentation update for Amazon ECS.

## [1.1.4] - 03/25/2024

### Features
* (**codebuild**) Supporting GitLab and GitLab Self Managed as source types in AWS CodeBuild.
* (**ec2**) Added support for ModifyInstanceMetadataDefaults and GetInstanceMetadataDefaults to set Instance Metadata Service account defaults
* (**emrcontainers**) This release increases the number of supported job template parameters from 20 to 100.
* (**globalaccelerator**) AWS Global Accelerator now supports cross-account sharing for bring your own IP addresses.
* (**medialive**) Exposing TileMedia H265 options
* (**sagemaker**) Introduced support for the following new instance types on SageMaker Studio for JupyterLab and CodeEditor applications: m6i, m6id, m7i, c6i, c6id, c7i, r6i, r6id, r7i, and p5

### Fixes
* Support client-configured `accountIdEndpointMode`

### Documentation
* (**ecs**) Documentation only update for Amazon ECS.

## [1.1.3] - 03/22/2024

### Features
* (**kendra**) Documentation update, March 2024. Corrects some docs for Amazon Kendra.
* (**pricing**) Add ResourceNotFoundException to ListPriceLists and GetPriceListFileUrl APIs
* (**rolesanywhere**) This release relaxes constraints on the durationSeconds request parameter for the *Profile APIs that support it. This parameter can now take on values that go up to 43200.
* (**securityhub**) Added new resource detail object to ASFF, including resource for LastKnownExploitAt

### Documentation
* (**firehose**) Updates Amazon Firehose documentation for message regarding Enforcing Tags IAM Policy.

## [1.1.2] - 03/21/2024

### Features
* (**codeartifact**) This release adds Package groups to CodeArtifact so you can more conveniently configure package origin controls for multiple packages.

## [1.1.1] - 03/20/2024

### Features
* (**accessanalyzer**) This release adds support for policy validation and external access findings for DynamoDB tables and streams. IAM Access Analyzer helps you author functional and secure resource-based policies and identify cross-account access. Updated service API, documentation, and paginators.
* (**connect**) This release updates the *InstanceStorageConfig APIs to support a new ResourceType: REAL_TIME_CONTACT_ANALYSIS_CHAT_SEGMENTS. Use this resource type to enable streaming for real-time analysis of chat contacts and to associate a Kinesis stream where real-time analysis chat segments will be published.
* (**dynamodb**) This release introduces 3 new APIs ('GetResourcePolicy', 'PutResourcePolicy' and 'DeleteResourcePolicy') and modifies the existing 'CreateTable' API for the resource-based policy support. It also modifies several APIs to accept a 'TableArn' for the 'TableName' parameter.
* (**managedblockchainquery**) AMB Query: update GetTransaction to include transactionId as input
* (**savingsplans**) Introducing the Savings Plans Return feature enabling customers to return their Savings Plans within 7 days of purchase.

### Documentation
* (**codebuild**) This release adds support for new webhook events (RELEASED and PRERELEASED) and filter types (TAG_NAME and RELEASE_NAME).

## [1.1.0] - 03/19/2024

### Features
* (**cloudwatchlogs**) Update LogSamples field in Anomaly model to be a list of LogEvent
* (**ec2**) This release adds the new DescribeMacHosts API operation for getting information about EC2 Mac Dedicated Hosts. Users can now see the latest macOS versions that their underlying Apple Mac can support without needing to be updated.
* (**finspace**) Adding new attributes readWrite and onDemand to dataview models for Database Maintenance operations.
* (**managedblockchainquery**) Introduces a new API for Amazon Managed Blockchain Query: ListFilteredTransactionEvents.

### Fixes
* [#1045](https://github.com/smithy-lang/smithy-kotlin/issues/1045) ⚠️ **IMPORTANT**: Fix codegen for map shapes which use string enums as map keys. See the [**Map key changes** breaking change announcement](https://github.com/awslabs/aws-sdk-kotlin/discussions/1258) for more details
* [#1041](https://github.com/smithy-lang/smithy-kotlin/issues/1041) ⚠️ **IMPORTANT**: Disable [OkHttp's transparent response decompression](https://square.github.io/okhttp/features/calls/#rewriting-requests) by manually specifying `Accept-Encoding: identity` in requests. See the [**Disabling automatic response decompression** breaking change announcement](https://github.com/awslabs/aws-sdk-kotlin/discussions/1259) for more details.

### Documentation
* (**cloudformation**) Documentation update, March 2024. Corrects some formatting.

## [1.0.80] - 03/18/2024

### Features
* (**cloudformation**) This release supports for a new API ListStackSetAutoDeploymentTargets, which provider auto-deployment configuration as a describable resource. Customers can now view the specific combinations of regions and OUs that are being auto-deployed.
* (**kms**) Adds the ability to use the default policy name by omitting the policyName parameter in calls to PutKeyPolicy and GetKeyPolicy
* (**mediatailor**) This release adds support to allow customers to show different content within a channel depending on metadata associated with the viewer.
* (**rds**) This release launches the ModifyIntegration API and support for data filtering for zero-ETL Integrations.
* (**s3**) Fix two issues with response root node names.

### Documentation
* (**timestreamquery**) Documentation updates, March 2024

## [1.0.79] - 03/15/2024

### Features
* (**backup**) This release introduces a boolean attribute ManagedByAWSBackupOnly as part of ListRecoveryPointsByResource api to filter the recovery points based on ownership. This attribute can be used to filter out the recovery points protected by AWSBackup.
* (**codebuild**) AWS CodeBuild now supports overflow behavior on Reserved Capacity.
* (**connect**) This release adds Hierarchy based Access Control fields to Security Profile public APIs and adds support for UserAttributeFilter to SearchUsers API.
* (**ec2**) Add media accelerator and neuron device information on the describe instance types API.
* (**kinesisanalyticsv2**) Support for Flink 1.18 in Managed Service for Apache Flink
* (**sagemaker**) Adds m6i, m6id, m7i, c6i, c6id, c7i, r6i r6id, r7i, p5 instance type support to Sagemaker Notebook Instances and miscellaneous wording fixes for previous Sagemaker documentation.
* (**workspacesthinclient**) Removed unused parameter kmsKeyArn from UpdateDeviceRequest

### Documentation
* (**s3**) Documentation updates for Amazon S3.

## [1.0.78] - 03/14/2024

### Features
* (**ec2instanceconnect**) This release includes a new exception type "SerialConsoleSessionUnsupportedException" for SendSerialConsoleSSHPublicKey API.
* (**fis**) This release adds support for previewing target resources before running a FIS experiment. It also adds resource ARNs for actions, experiments, and experiment templates to API responses.
* (**timestreaminfluxdb**) This is the initial SDK release for Amazon Timestream for InfluxDB. Amazon Timestream for InfluxDB is a new time-series database engine that makes it easy for application developers and DevOps teams to run InfluxDB databases on AWS for near real-time time-series applications using open source APIs.

### Documentation
* (**amplify**) Documentation updates for Amplify. Identifies the APIs available only to apps created using Amplify Gen 1.
* (**elasticloadbalancingv2**) This release allows you to configure HTTP client keep-alive duration for communication between clients and Application Load Balancers.
* (**rds**) Updates Amazon RDS documentation for EBCDIC collation for RDS for Db2.
* (**secretsmanager**) Doc only update for Secrets Manager

### Miscellaneous
* Remove IoT RoboRunner service

## [1.0.77] - 03/13/2024

### Features
* (**ivsrealtime**) adds support for multiple new composition layout configuration options (grid, pip)
* (**kinesisanalyticsv2**) Support new RuntimeEnvironmentUpdate parameter within UpdateApplication API allowing callers to change the Flink version upon which their application runs.
* (**s3**) This release makes the default option for S3 on Outposts request signing to use the SigV4A algorithm when using AWS Common Runtime (CRT).

## [1.0.76] - 03/12/2024

### Features
* (**connect**) This release increases MaxResults limit to 500 in request for SearchUsers, SearchQueues and SearchRoutingProfiles APIs of Amazon Connect.
* (**kafka**) Added support for specifying the starting position of topic replication in MSK-Replicator.

### Documentation
* (**cloudformation**) CloudFormation documentation update for March, 2024
* (**ec2**) Documentation updates for Amazon EC2.
* (**ssm**) March 2024 doc-only updates for Systems Manager.

## [1.0.75] - 03/11/2024

### Features
* (**codestarconnections**) Added a sync configuration enum to disable publishing of deployment status to source providers (PublishDeploymentStatus). Added a sync configuration enum (TriggerStackUpdateOn) to only trigger changes.
* (**mediapackagev2**) This release enables customers to safely update their MediaPackage v2 channel groups, channels and origin endpoints using entity tags.
* Added the `sigV4aSigningRegionSet` configuration option

### Documentation
* (**elasticache**) Revisions to API text that are now to be carried over to SDK text, changing usages of "SFO" in code examples to "us-west-1", and some other typos.

### Miscellaneous
* Bump smithy version to 1.45.0

## [1.0.74] - 03/08/2024

### Features
* (**batch**) This release adds JobStateTimeLimitActions setting to the Job Queue API. It allows you to configure an action Batch can take for a blocking job in front of the queue after the defined period of time. The new parameter applies for ECS, EKS, and FARGATE Job Queues.
* (**cloudtrail**) Added exceptions to CreateTrail, DescribeTrails, and ListImportFailures APIs.
* (**cognitoidentityprovider**) Add ConcurrentModificationException to SetUserPoolMfaConfig
* (**guardduty**) Add RDS Provisioned and Serverless Usage types
* (**transfer**) Added DES_EDE3_CBC to the list of supported encryption algorithms for messages sent with an AS2 connector.
* [#1212](https://github.com/awslabs/aws-sdk-kotlin/issues/1212) Add request IDs to exception messages where available
* [#1212](https://github.com/awslabs/aws-sdk-kotlin/issues/1212) Add error metadata to ServiceException messages when a service-provided message isn't available

### Documentation
* (**bedrockagentruntime**) Documentation update for Bedrock Runtime Agent
* (**codebuild**) This release adds support for a new webhook event: PULL_REQUEST_CLOSED.

## [1.0.73] - 03/07/2024

### Features
* (**appconfig**) AWS AppConfig now supports dynamic parameters, which enhance the functionality of AppConfig Extensions by allowing you to provide parameter values to your Extensions at the time you deploy your configuration.
* (**ec2**) This release adds an optional parameter to RegisterImage and CopyImage APIs to support tagging AMIs at the time of creation.
* (**grafana**) Adds support for the new GrafanaToken as part of the Amazon Managed Grafana Enterprise plugins upgrade to associate your AWS account with a Grafana Labs account.
* (**paymentcryptographydata**) AWS Payment Cryptography EMV Decrypt Feature  Release
* (**wafv2**) You can increase the max request body inspection size for some regional resources. The size setting is in the web ACL association config. Also, the AWSManagedRulesBotControlRuleSet EnableMachineLearning setting now takes a Boolean instead of a primitive boolean type, for languages like Java.

### Documentation
* (**lambda**) Documentation updates for AWS Lambda
* (**rds**) Updates Amazon RDS documentation for io2 storage for Multi-AZ DB clusters
* (**snowball**) Doc-only update for change to EKS-Anywhere ordering.
* (**workspaces**) Added note for user decoupling

## [1.0.72] - 03/06/2024

### Features
* (**imagebuilder**) Add PENDING status to Lifecycle Execution resource status. Add StartTime and EndTime to ListLifecycleExecutionResource API response.
* (**rds**) Updated the input of CreateDBCluster and ModifyDBCluster to support setting CA certificates. Updated the output of DescribeDBCluster to show current CA certificate setting value.
* (**verifiedpermissions**) Deprecating details in favor of configuration for GetIdentitySource and ListIdentitySources APIs.

### Documentation
* (**dynamodb**) Doc only updates for DynamoDB documentation
* (**mwaa**) Amazon MWAA adds support for Apache Airflow v2.8.1.
* (**redshift**) Update for documentation only. Covers port ranges, definition updates for data sharing, and definition updates to cluster-snapshot documentation.

## [1.0.71] - 03/05/2024

### Features
* (**organizations**) This release contains an endpoint addition
* (**sesv2**) Adds support for providing custom headers within SendEmail and SendBulkEmail for SESv2.

### Documentation
* (**apigateway**) Documentation updates for Amazon API Gateway
* (**chatbot**) Minor update to documentation.

## [1.0.70] - 03/04/2024

### Features
* (**cloudformation**) Add DetailedStatus field to DescribeStackEvents and DescribeStacks APIs
* (**fsx**) Added support for creating FSx for NetApp ONTAP file systems with up to 12 HA pairs, delivering up to 72 GB/s of read throughput and 12 GB/s of write throughput.
* (**organizations**) Documentation update for AWS Organizations

## [1.0.69] - 03/01/2024

### Documentation
* (**accessanalyzer**) Fixed a typo in description field.
* (**autoscaling**) With this release, Amazon EC2 Auto Scaling groups, EC2 Fleet, and Spot Fleet improve the default price protection behavior of attribute-based instance type selection of Spot Instances, to consistently select from a wide range of instance types.
* (**ec2**) With this release, Amazon EC2 Auto Scaling groups, EC2 Fleet, and Spot Fleet improve the default price protection behavior of attribute-based instance type selection of Spot Instances, to consistently select from a wide range of instance types.

## [1.0.68] - 02/29/2024

### Features
* (**docdbelastic**) Launched Elastic Clusters Readable Secondaries, Start/Stop, Configurable Shard Instance count, Automatic Backups and Snapshot Copying
* (**eks**) Added support for new AL2023 AMIs to the supported AMITypes.
* (**lexmodelsv2**) This release makes AMAZON.QnAIntent generally available in Amazon Lex. This generative AI feature leverages large language models available through Amazon Bedrock to automate frequently asked questions (FAQ) experience for end-users.
* (**migrationhuborchestrator**) Adds new CreateTemplate, UpdateTemplate and DeleteTemplate APIs.
* (**quicksight**) TooltipTarget for Combo chart visuals; ColumnConfiguration limit increase to 2000; Documentation Update
* (**sagemaker**) Adds support for ModelDataSource in Model Packages to support unzipped models. Adds support to specify SourceUri for models which allows registration of models without mandating a container for hosting. Using SourceUri, customers can decouple the model from hosting information during registration.
* (**securitylake**) Add capability to update the Data Lake's MetaStoreManager Role in order to perform required data lake updates to use Iceberg table format in their data lake or update the role for any other reason.
* Add support for S3 Express One Zone

### Fixes
* Fix an issue where sections were not properly divided when parsing the config file
* [#1220](https://github.com/awslabs/aws-sdk-kotlin/issues/1220) Refactor XML deserialization to handle flat collections

## [1.0.67] - 02/28/2024

### Features
* (**batch**) This release adds Batch support for configuration of multicontainer jobs in ECS, Fargate, and EKS. This support is available for all types of jobs, including both array jobs and multi-node parallel jobs.
* (**bedrockagentruntime**) This release adds support to override search strategy performed by the Retrieve and RetrieveAndGenerate APIs for Amazon Bedrock Agents
* (**costexplorer**) This release introduces the new API 'GetApproximateUsageRecords', which retrieves estimated usage records for hourly granularity or resource-level data at daily granularity.
* (**ec2**) This release increases the range of MaxResults for GetNetworkInsightsAccessScopeAnalysisFindings to 1,000.
* (**iot**) This release reduces the maximum results returned per query invocation from 500 to 100 for the SearchIndex API. This change has no implications as long as the API is invoked until the nextToken is NULL.
* (**wafv2**) AWS WAF now supports configurable time windows for request aggregation with rate-based rules. Customers can now select time windows of 1 minute, 2 minutes or 10 minutes, in addition to the previously supported 5 minutes.

## [1.0.66] - 02/27/2024

### Features
* (**amplifyuibuilder**) We have added the ability to tag resources after they are created

## [1.0.65] - 02/26/2024

### Features
* (**drs**) Added volume status to DescribeSourceServer replicated volumes.
* (**kafkaconnect**) Adds support for tagging, with new TagResource, UntagResource and ListTagsForResource APIs to manage tags and updates to existing APIs to allow tag on create. This release also adds support for the new DeleteWorkerConfiguration API.
* (**rds**) This release adds support for gp3 data volumes for Multi-AZ DB Clusters.

### Documentation
* (**apigateway**) Documentation updates for Amazon API Gateway.

## [1.0.64] - 02/23/2024

### Features
* (**rds**) Add pattern and length based validations for DBShardGroupIdentifier

### Documentation
* (**appsync**) Documentation only updates for AppSync
* (**qldb**) Clarify possible values for KmsKeyArn and EncryptionDescription.
* (**rum**) Doc-only update for new RUM metrics that were added

## [1.0.63] - 02/22/2024

### Features
* (**internetmonitor**) This release adds IPv4 prefixes to health events
* (**kinesisvideo**) Increasing NextToken parameter length restriction for List APIs from 512 to 1024.

## [1.0.62] - 02/21/2024

### Features
* (**iotevents**) Increase the maximum length of descriptions for Inputs, Detector Models, and Alarm Models
* (**lookoutequipment**) This release adds a field exposing model quality to read APIs for models. It also adds a model quality field to the API response when creating an inference scheduler.
* (**medialive**) MediaLive now supports the ability to restart pipelines in a running channel.
* (**ssm**) This release adds support for sharing Systems Manager parameters with other AWS accounts.

### Fixes
* [#1208](https://github.com/awslabs/aws-sdk-kotlin/issues/1208) Profile credentials provider is no longer marked as internal API
* [#1217](https://github.com/awslabs/aws-sdk-kotlin/issues/1217) Only enable aws-chunked content encoding for S3

## [1.0.61] - 02/20/2024

### Features
* (**firehose**) This release updates a few Firehose related APIs.
* (**lambda**) Add .NET 8 (dotnet8) Runtime support to AWS Lambda.

### Documentation
* (**dynamodb**) Publishing quick fix for doc only update.

## [1.0.60] - 02/19/2024

### Features
* (**amplify**) This release contains API changes that enable users to configure their Amplify domains with their own custom SSL/TLS certificate.
* (**chatbot**) This release adds support for AWS Chatbot. You can now monitor, operate, and troubleshoot your AWS resources with interactive ChatOps using the AWS SDK.
* (**mediatailor**) MediaTailor: marking #AdBreak.OffsetMillis as required.

### Documentation
* (**configservice**) Documentation updates for the AWS Config CLI
* (**ivs**) Changed description for latencyMode in Create/UpdateChannel and Channel/ChannelSummary.
* (**keyspaces**) Documentation updates for Amazon Keyspaces

## [1.0.59] - 02/16/2024

### Features
* (**emr**) adds fine grained control over Unhealthy Node Replacement to Amazon ElasticMapReduce
* (**firehose**) This release adds support for Data Message Extraction for decompressed CloudWatch logs, and to use a custom file extension or time zone for S3 destinations.
* (**sns**) This release marks phone numbers as sensitive inputs.

### Documentation
* (**connectparticipant**) Doc only update to GetTranscript API reference guide to inform users about presence of events in the chat transcript.
* (**lambda**) Documentation-only updates for Lambda to clarify a number of existing actions and properties.
* (**rds**) Doc only update for a valid option in DB parameter group

## [1.0.58] - 02/15/2024

### Features
* (**artifact**) This is the initial SDK release for AWS Artifact. AWS Artifact provides on-demand access to compliance and third-party compliance reports. This release includes access to List and Get reports, along with their metadata. This release also includes access to AWS Artifact notifications settings.
* (**codepipeline**) Add ability to override timeout on action level.
* (**guardduty**) Marked fields IpAddressV4, PrivateIpAddress, Email as Sensitive.
* (**healthlake**) This release adds a new response parameter, JobProgressReport, to the DescribeFHIRImportJob and ListFHIRImportJobs API operation. JobProgressReport provides details on the progress of the import job on the server.
* (**opensearch**) Adds additional supported instance types.
* (**polly**) Amazon Polly adds 1 new voice - Burcu (tr-TR)
* (**sagemaker**) This release adds a new API UpdateClusterSoftware for SageMaker HyperPod. This API allows users to patch HyperPod clusters with latest platform softwares.

### Documentation
* (**detective**) Doc only updates for content enhancement
* (**secretsmanager**) Doc only update for Secrets Manager

## [1.0.57] - 02/14/2024

### Features
* (**controltower**) Adds support for new Baseline and EnabledBaseline APIs for automating multi-account governance.
* (**lookoutequipment**) This feature allows customers to see pointwise model diagnostics results for their models.
* (**qbusiness**) This release adds the metadata-boosting feature, which allows customers to easily fine-tune the underlying ranking of retrieved RAG passages in order to optimize Q&A answer relevance. It also adds new feedback reasons for the PutFeedback API.

## [1.0.56] - 02/13/2024

### Features
* (**lightsail**) This release adds support to upgrade the major version of a database.
* (**marketplacecatalog**) AWS Marketplace Catalog API now supports setting intent on requests
* (**resourceexplorer2**) Resource Explorer now uses newly supported IPv4 'amazonaws.com' endpoints by default.

### Documentation
* (**securitylake**) Documentation updates for Security Lake

## [1.0.55] - 02/12/2024

### Features
* (**appsync**) Adds support for new options on GraphqlAPIs, Resolvers and  Data Sources for emitting Amazon CloudWatch metrics for enhanced monitoring of AppSync APIs.
* (**cloudwatch**) This release enables PutMetricData API request payload compression by default.
* (**neptunegraph**) Adding a new option "parameters" for data plane api ExecuteQuery to support running parameterized query via SDK.
* (**route53domains**) This release adds bill contact support for RegisterDomain, TransferDomain, UpdateDomainContact and GetDomainDetail API.

## [1.0.54] - 02/09/2024

### Features
* (**batch**) This feature allows Batch to support configuration of repository credentials for jobs running on ECS
* (**braket**) Creating a job will result in DeviceOfflineException when using an offline device, and DeviceRetiredException when using a retired device.
* (**costoptimizationhub**) Adding includeMemberAccounts field to the response of ListEnrollmentStatuses API.
* (**iot**) This release allows AWS IoT Core users to enable Online Certificate Status Protocol (OCSP) Stapling for TLS X.509 Server Certificates when creating and updating AWS IoT Domain Configurations with Custom Domain.
* (**pricing**) Add Throttling Exception to all APIs.

### Documentation
* (**amp**) Overall documentation updates.
* (**ecs**) Documentation only update for Amazon ECS.

## [1.0.53] - 02/08/2024

### Features
* (**codepipeline**) Add ability to execute pipelines with new parallel & queued execution modes and add support for triggers with filtering on branches and file paths.
* (**quicksight**) General Interactions for Visuals; Waterfall Chart Color Configuration; Documentation Update
* (**workspaces**) This release introduces User-Decoupling feature. This feature allows Workspaces Core customers to provision workspaces without providing users. CreateWorkspaces and DescribeWorkspaces APIs will now take a new optional parameter "WorkspaceName".

### Fixes
* [#1031](https://github.com/smithy-lang/smithy-kotlin/issues/1031) Bump **smithy-kotlin** version to consume upstream fixes for URL parsing

## [1.0.52] - 02/07/2024

### Features
* (**datasync**) AWS DataSync now supports manifests for specifying files or objects to transfer.
* (**lexmodelsv2**) This release introduces a new bot replication feature as part of Lex Global Resiliency offering. This feature leverages a new set of APIs that allow customers to create bot replicas and replicate changes to bots across regions.
* (**redshift**) LisRecommendations API to fetch Amazon Redshift Advisor recommendations.

## [1.0.51] - 02/06/2024

### Features
* (**appsync**) Support for environment variables in AppSync GraphQL APIs
* (**cloudwatchlogs**) This release adds a new field, logGroupArn, to the response of the logs:DescribeLogGroups action.
* (**elasticsearchservice**) This release adds clear visibility to the customers on the changes that they make on the domain.
* (**opensearch**) This release adds clear visibility to the customers on the changes that they make on the domain.
* (**wafv2**) You can now delete an API key that you've created for use with your CAPTCHA JavaScript integration API.

### Documentation
* (**ecs**) This release is a documentation only update to address customer issues.

## [1.0.50] - 02/05/2024

### Features
* (**glue**) Introduce Catalog Encryption Role within Glue Data Catalog Settings. Introduce SASL/PLAIN as an authentication method for Glue Kafka connections

### Documentation
* (**workspaces**) Added definitions of various WorkSpace states

## [1.0.49] - 02/02/2024

### Features
* (**sagemaker**) Amazon SageMaker Canvas adds GenerativeAiSettings support for CanvasAppSettings.

### Documentation
* (**dynamodb**) Any number of users can execute up to 50 concurrent restores (any type of restore) in a given account.

## [1.0.48] - 02/01/2024

### Features
* (**cognitoidentityprovider**) Added CreateIdentityProvider and UpdateIdentityProvider details for new SAML IdP features
* (**ivs**) This release introduces a new resource Playback Restriction Policy which can be used to geo-restrict or domain-restrict channel stream playback when associated with a channel.  New APIs to support this resource were introduced in the form of Create/Delete/Get/Update/List.
* (**managedblockchainquery**) This release adds support for transactions that have not reached finality. It also removes support for the status property from the response of the GetTransaction operation. You can use the confirmationStatus and executionStatus properties to determine the status of the transaction.
* (**mediaconvert**) This release includes support for broadcast-mixed audio description tracks.
* (**neptunegraph**) Adding new APIs in SDK for Amazon Neptune Analytics. These APIs include operations to execute, cancel, list queries and get the graph summary.
* [#476](https://github.com/awslabs/aws-sdk-kotlin/issues/476) Allow full URI path to a localhost metadata service (AwsContainerCredentialsFullUri) to be a host name

### Fixes
* Bump **smithy-kotlin** version to fix an error with serializing maps which use the `Document` type as a value

## [1.0.47] - 01/31/2024

### Features
* (**cloudformation**) CloudFormation IaC generator allows you to scan existing resources in your account and select resources to generate a template for a new or existing CloudFormation stack.
* (**elasticloadbalancingv2**) This release enables unhealthy target draining intervals for Network Load Balancers.
* (**glue**) Update page size limits for GetJobRuns and GetTriggers APIs.
* (**ssm**) This release adds an optional Duration parameter to StateManager Associations. This allows customers to specify how long an apply-only-on-cron association execution should run. Once the specified Duration is out all the ongoing cancellable commands or automations are cancelled.

## [1.0.46] - 01/30/2024

### Features
* (**datazone**) Add new skipDeletionCheck to DeleteDomain. Add new skipDeletionCheck to DeleteProject which also automatically deletes dependent objects

### Documentation
* (**route53**) Update the SDKs for text changes in the APIs.

## [1.0.45] - 01/29/2024

### Features
* (**autoscaling**) EC2 Auto Scaling customers who use attribute based instance-type selection can now intuitively define their Spot instances price protection limit as a percentage of the lowest priced On-Demand instance type.
* (**ec2**) EC2 Fleet customers who use attribute based instance-type selection can now intuitively define their Spot instances price protection limit as a percentage of the lowest priced On-Demand instance type.
* (**mwaa**) This release adds MAINTENANCE environment status for Amazon MWAA environments.
* (**rds**) Introduced support for the InsufficientDBInstanceCapacityFault error in the RDS RestoreDBClusterFromSnapshot and RestoreDBClusterToPointInTime API methods. This provides enhanced error handling, ensuring a more robust experience.

### Documentation
* (**comprehend**) Comprehend PII analysis now supports Spanish input documents.
* (**snowball**) Modified description of createaddress to include direction to add path when providing a JSON file.

## [1.0.44] - 01/26/2024

### Features
* (**connect**) Update list and string length limits for predefined attributes.
* (**inspector2**) This release adds ECR container image scanning based on their lastRecordedPullTime.
* (**sagemaker**) Amazon SageMaker Automatic Model Tuning now provides an API to programmatically delete tuning jobs.

## [1.0.43] - 01/25/2024

### Features
* (**acmpca**) AWS Private CA now supports an option to omit the CDP extension from issued certificates, when CRL revocation is enabled.
* (**lightsail**) This release adds support for IPv6-only instance plans.

## [1.0.42] - 01/24/2024

### Features
* (**ec2**) Introduced a new clientToken request parameter on CreateNetworkAcl and CreateRouteTable APIs. The clientToken parameter allows idempotent operations on the APIs.
* (**outposts**) DeviceSerialNumber parameter is now optional in StartConnection API
* (**rds**) This release adds support for Aurora Limitless Database.
* (**storagegateway**) Add DeprecationDate and SoftwareVersion to response of ListGateways.

### Fixes
* Fix application of sigv4a authentication scheme for S3, Eventbridge, and CloudFront KeyValueStore

### Documentation
* (**ecs**) Documentation updates for Amazon ECS.

### Miscellaneous
* Bump smithy-kotlin version to 1.0.11

## [1.0.41] - 01/23/2024

### Features
* (**inspector2**) This release adds support for CIS scans on EC2 instances.

### Fixes
* [#1187](https://github.com/awslabs/aws-sdk-kotlin/issues/1187) (**s3control**) Add missing `x-amz-content-sha256` header for SigV4 requests.

## [1.0.40] - 01/22/2024

### Features
* (**appconfigdata**) Fix FIPS Endpoints in aws-us-gov.
* (**cloudfrontkeyvaluestore**) This release improves upon the DescribeKeyValueStore API by returning two additional fields, Status of the KeyValueStore and the FailureReason in case of failures during creation of KeyValueStore.
* (**connectcases**) This release adds the ability to view audit history on a case and introduces a new parameter, performedBy, for CreateCase and UpdateCase API's.
* (**ecs**) This release adds support for Transport Layer Security (TLS) and Configurable Timeout to ECS Service Connect. TLS facilitates privacy and data security for inter-service communications, while Configurable Timeout allows customized per-request timeout and idle timeout for Service Connect services.
* (**finspace**) Allow customer to set zip default through command line arguments.
* (**rds**) Introduced support for the InsufficientDBInstanceCapacityFault error in the RDS CreateDBCluster API method. This provides enhanced error handling, ensuring a more robust experience when creating database clusters with insufficient instance capacity.

### Fixes
* Pass client-configured region to StsWebIdentityCredentialsProvider

### Documentation
* (**cloud9**) Doc-only update around removing AL1 from list of available AMIs for Cloud9
* (**ec2**) Documentation updates for Amazon EC2.
* (**organizations**) Doc only update for quota increase change

## [1.0.39] - 01/19/2024

### Features
* (**athena**) Introducing new NotebookS3LocationUri parameter to Athena ImportNotebook API. Payload is no longer required and either Payload or NotebookS3LocationUri needs to be provided (not both) for a successful ImportNotebook API call. If both are provided, an InvalidRequestException will be thrown.
* (**codebuild**) Release CodeBuild Reserved Capacity feature
* (**dynamodb**) This release adds support for including ApproximateCreationDateTimePrecision configurations in EnableKinesisStreamingDestination API, adds the same as an optional field in the response of DescribeKinesisStreamingDestination, and adds support for a new UpdateKinesisStreamingDestination API.
* (**qconnect**) Increased Quick Response name max length to 100

## [1.0.38] - 01/18/2024

### Features
* (**b2bi**) Increasing TestMapping inputFileContent file size limit to 5MB and adding file size limit 250KB for TestParsing input file. This release also includes exposing InternalServerException for Tag APIs.
* (**cloudtrail**) This release adds a new API ListInsightsMetricData to retrieve metric data from CloudTrail Insights.
* (**connect**) GetMetricDataV2 now supports 3 groupings
* (**drs**) Removed invalid and unnecessary default values.
* (**firehose**) Allow support for Snowflake as a Kinesis Data Firehose delivery destination.
* (**sagemakerfeaturestoreruntime**) Increase BatchGetRecord limits from 10 items to 100 items

## [1.0.37] - 01/17/2024

### Features
* (**keyspaces**) This release adds support for Multi-Region Replication with provisioned tables, and Keyspaces auto scaling APIs

### Documentation
* (**dynamodb**) Updating note for enabling streams for UpdateTable.

## [1.0.36] - 01/16/2024

### Features
* (**iot**) Revert release of LogTargetTypes
* (**iotfleetwise**) Updated APIs: SignalNodeType query parameter has been added to ListSignalCatalogNodesRequest and ListVehiclesResponse has been extended with attributes field.
* (**macie2**) This release adds support for analyzing Amazon S3 objects that are encrypted using dual-layer server-side encryption with AWS KMS keys (DSSE-KMS). It also adds support for reporting DSSE-KMS details in statistics and metadata about encryption settings for S3 buckets and objects.
* (**paymentcryptography**) Provide an additional option for key exchange using RSA wrap/unwrap in addition to tr-34/tr-31 in ImportKey and ExportKey operations. Added new key usage (type) TR31_M1_ISO_9797_1_MAC_KEY, for use with Generate/VerifyMac dataplane operations  with ISO9797 Algorithm 1 MAC calculations.
* (**rekognition**) This release adds ContentType and TaxonomyLevel attributes to DetectModerationLabels and GetMediaAnalysisJob API responses.

### Fixes
* [#1177](https://github.com/awslabs/aws-sdk-kotlin/issues/1177) Bump **smithy-kotlin** version to correct S3 presigning issues with custom endpoint ports

### Documentation
* (**personalize**) Documentation updates for Amazon Personalize.
* (**personalizeruntime**) Documentation updates for Amazon Personalize
* (**securityhub**) Documentation updates for AWS Security Hub

## [1.0.35] - 01/14/2024

### Features
* (**sagemaker**) This release will have ValidationException thrown if certain invalid app types are provided. The release will also throw ValidationException if more than 10 account ids are provided in VpcOnlyTrustedAccounts.

## [1.0.34] - 01/12/2024

### Features
* (**connect**) Supervisor Barge for Chat is now supported through the MonitorContact API.
* (**connectparticipant**) Introduce new Supervisor participant role
* (**mwaa**) This Amazon MWAA feature release includes new fields in CreateWebLoginToken response model. The new fields IamIdentity and AirflowIdentity will let you match identifications, as the Airflow identity length is currently hashed to 64 characters.
* (**s3control**) S3 On Outposts team adds dualstack endpoints support for S3Control and S3Outposts API calls.
* (**supplychain**) This release includes APIs CreateBillOfMaterialsImportJob and GetBillOfMaterialsImportJob.
* (**transfer**) AWS Transfer Family now supports static IP addresses for SFTP & AS2 connectors and for async MDNs on AS2 servers.

### Documentation
* (**location**) Location SDK documentation update. Added missing fonts to the MapConfiguration data type. Updated note for the SubMunicipality property in the place data type.

## [1.0.33] - 01/11/2024

### Features
* (**ec2**) This release adds support for adding an ElasticBlockStorage volume configurations in ECS RunTask/StartTask/CreateService/UpdateService APIs. The configuration allows for attaching EBS volumes to ECS Tasks.
* (**ecs**) This release adds support for adding an ElasticBlockStorage volume configurations in ECS RunTask/StartTask/CreateService/UpdateService APIs. The configuration allows for attaching EBS volumes to ECS Tasks.
* (**eventbridge**) Adding AppSync as an EventBridge Target
* (**iot**) Add ConflictException to Update APIs of AWS IoT Software Package Catalog
* (**iotfleetwise**) The following dataTypes have been removed: CUSTOMER_DECODED_INTERFACE in NetworkInterfaceType; CUSTOMER_DECODED_SIGNAL_INFO_IS_NULL in SignalDecoderFailureReason; CUSTOMER_DECODED_SIGNAL_NETWORK_INTERFACE_INFO_IS_NULL in NetworkInterfaceFailureReason; CUSTOMER_DECODED_SIGNAL in SignalDecoderType

### Documentation
* (**secretsmanager**) Doc only update for Secrets Manager
* (**workspaces**) Added AWS Workspaces RebootWorkspaces API - Extended Reboot documentation update

## [1.0.32] - 01/10/2024

### Features
* (**cloudwatchlogs**) Add support for account level subscription filter policies to PutAccountPolicy, DescribeAccountPolicies, and DeleteAccountPolicy APIs. Additionally, PutAccountPolicy has been modified with new optional "selectionCriteria" parameter for resource selection.
* (**connectcampaigns**) Minor pattern updates for Campaign and Dial Request API fields.
* (**location**) This release adds API support for custom layers for the maps service APIs: CreateMap, UpdateMap, DescribeMap.
* (**qconnect**) QueryAssistant and GetRecommendations will be discontinued starting June 1, 2024. To receive generative responses after March 1, 2024 you will need to create a new Assistant in the Connect console and integrate the Amazon Q in Connect JavaScript library (amazon-q-connectjs) into your applications.
* (**route53**) Route53 now supports geoproximity routing in AWS regions
* (**wisdom**) QueryAssistant and GetRecommendations will be discontinued starting June 1, 2024. To receive generative responses after March 1, 2024 you will need to create a new Assistant in the Connect console and integrate the Amazon Q in Connect JavaScript library (amazon-q-connectjs) into your applications.

### Fixes
* [#1173](https://github.com/awslabs/aws-sdk-kotlin/issues/1173) Correctly presign S3 requests when `forcePathStyle = true`

### Documentation
* (**redshiftserverless**) Updates to ConfigParameter for RSS workgroup, removal of use_fips_ssl

## [1.0.31] - 01/08/2024

### Features
* (**codebuild**) Aws CodeBuild now supports new compute type BUILD_GENERAL1_XLARGE
* (**ec2**) Amazon EC2 R7iz bare metal instances are powered by custom 4th generation Intel Xeon Scalable processors.
* (**route53resolver**) This release adds support for query type configuration on firewall rules that enables customers for granular action (ALLOW, ALERT, BLOCK) by DNS query type.

## [1.0.30] - 01/05/2024

### Features
* (**connect**) Minor trait updates for User APIs
* (**qconnect**) Marked SearchQuickResponses API as readonly.

### Documentation
* (**kms**) Documentation updates for AWS Key Management Service (KMS).
* (**redshiftserverless**) use_fips_ssl and require_ssl parameter support for Workgroup, UpdateWorkgroup, and CreateWorkgroup

## [1.0.29] - 01/04/2024

### Features
* (**configservice**) Updated ResourceType enum with new resource types onboarded by AWS Config in November and December 2023.
* (**docdb**) Adding PerformanceInsightsEnabled and PerformanceInsightsKMSKeyId fields to DescribeDBInstances Response.
* (**ecs**) This release adds support for managed instance draining which facilitates graceful termination of Amazon ECS instances.
* (**elasticsearchservice**) This release adds support for new or existing Amazon OpenSearch domains to enable TLS 1.3 or TLS 1.2 with perfect forward secrecy cipher suites for domain endpoints.
* (**lightsail**) This release adds support to set up an HTTPS endpoint on an instance.
* (**opensearch**) This release adds support for new or existing Amazon OpenSearch domains to enable TLS 1.3 or TLS 1.2 with perfect forward secrecy cipher suites for domain endpoints.
* (**sagemaker**) Adding support for provisioned throughput mode for SageMaker Feature Groups
* (**servicecatalog**) Added Idempotency token support to Service Catalog  AssociateServiceActionWithProvisioningArtifact, DisassociateServiceActionFromProvisioningArtifact, DeleteServiceAction API

## [1.0.28] - 01/03/2024

### Features
* (**connect**) Amazon Connect, Contact Lens Evaluation API increase evaluation notes max length to 3072.
* (**mediaconvert**) This release includes video engine updates including HEVC improvements, support for ingesting VP9 encoded video in MP4 containers, and support for user-specified 3D LUTs.

## [1.0.27] - 12/29/2023

### Features
* (**apprunner**) AWS App Runner adds Python 3.11 and Node.js 18 runtimes.
* (**location**) This release introduces a new parameter to bypasses an API key's expiry conditions and delete the key.
* (**quicksight**) Add LinkEntityArn support for different partitions; Add UnsupportedUserEditionException in UpdateDashboardLinks API; Add support for New Reader Experience Topics

### Fixes
* [#1165](https://github.com/awslabs/aws-sdk-kotlin/issues/1165) (**s3**) Fix default execution context attributes

### Miscellaneous
* Bump smithy version to 1.42.0

## [1.0.26] - 12/28/2023

### Features
* (**codestarconnections**) New integration with the GitLab self-managed provider type.
* (**kinesisvideoarchivedmedia**) NoDataRetentionException thrown when GetImages requested for a Stream that does not retain data (that is, has a DataRetentionInHours of 0).
* (**sagemaker**) Amazon SageMaker Studio now supports Docker access from within app container

## [1.0.25] - 12/27/2023

### Features
* (**emr**) Add support for customers to modify cluster attribute auto-terminate post cluster launch

## [1.0.24] - 12/26/2023

### Documentation
* (**iam**) Documentation updates for AWS Identity and Access Management (IAM).

## [1.0.23] - 12/22/2023

### Features
* (**bedrockagent**) Adding Claude 2.1 support to Bedrock Agents
* (**glue**) This release adds additional configurations for Query Session Context on the following APIs: GetUnfilteredTableMetadata, GetUnfilteredPartitionMetadata, GetUnfilteredPartitionsMetadata.
* (**lakeformation**) This release adds additional configurations on GetTemporaryGlueTableCredentials for Query Session Context.
* (**mediaconnect**) This release adds the DescribeSourceMetadata API. This API can be used to view the stream information of the flow's source.
* (**networkmonitor**) CloudWatch Network Monitor is a new service within CloudWatch that will help network administrators and operators continuously monitor network performance metrics such as round-trip-time and packet loss between their AWS-hosted applications and their on-premises locations.
* (**s3**) Added additional examples for some operations.
* (**secretsmanager**) Update endpoint rules and examples.

### Documentation
* (**omics**) Provides minor corrections and an updated description of APIs.

## [1.0.22] - 12/21/2023

### Features
* (**amp**) This release updates Amazon Managed Service for Prometheus APIs to support customer managed KMS keys.
* (**appintegrations**) The Amazon AppIntegrations service adds DeleteApplication API for deleting applications, and updates APIs to support third party applications reacting to workspace events and make data requests to Amazon Connect for agent and contact events.
* (**bedrockagent**) This release introduces Amazon Aurora as a vector store on Knowledge Bases for Amazon Bedrock
* (**codecommit**) AWS CodeCommit now supports customer managed keys from AWS Key Management Service. UpdateRepositoryEncryptionKey is added for updating the key configuration. CreateRepository, GetRepository, BatchGetRepositories are updated with new input or output parameters.
* (**connect**) Adds APIs to manage User Proficiencies and Predefined Attributes. Enhances StartOutboundVoiceContact API input. Introduces SearchContacts API. Enhances DescribeContact API. Adds an API to update Routing Attributes in QueuePriority and QueueTimeAdjustmentSeconds.
* (**medialive**) MediaLive now supports the ability to configure the audio that an AWS Elemental Link UHD device produces, when the device is configured as the source for a flow in AWS Elemental MediaConnect.
* (**neptunegraph**) Adds Waiters for successful creation and deletion of Graph, Graph Snapshot, Import Task and Private Endpoints for Neptune Analytics
* (**rds**) This release adds support for using RDS Data API with Aurora PostgreSQL Serverless v2 and provisioned DB clusters.
* (**rdsdata**) This release adds support for using RDS Data API with Aurora PostgreSQL Serverless v2 and provisioned DB clusters.
* (**sagemaker**) Amazon SageMaker Training now provides model training container access for debugging purposes. Amazon SageMaker Search now provides the ability to use visibility conditions to limit resource access to a single domain or multiple domains.

## [1.0.21] - 12/20/2023

### Features
* (**appstream**) This release introduces configurable clipboard, allowing admins to specify the maximum length of text that can be copied by the users from their device to the remote session and vice-versa.
* (**eks**) Add support for cluster insights, new EKS capability that surfaces potentially upgrade impacting issues.
* (**guardduty**) This release 1) introduces a new API: GetOrganizationStatistics , and 2) adds a new UsageStatisticType TOP_ACCOUNTS_BY_FEATURE for GetUsageStatistics API
* (**managedblockchainquery**) Adding Confirmation Status and Execution Status to GetTransaction Response.
* (**mediatailor**) Adds the ability to configure time shifting on MediaTailor channels using the TimeShiftConfiguration field
* (**route53**) Amazon Route 53 now supports the Canada West (Calgary) Region (ca-west-1) for latency records, geoproximity records, and private DNS for Amazon VPCs in that region.
* [#239](https://github.com/awslabs/aws-sdk-kotlin/issues/239) Generate KDoc samples from modeled examples
* Added support for [request compression](https://smithy.io/2.0/spec/behavior-traits.html#requestcompression-trait)

## [1.0.20] - 12/19/2023

### Features
* (**appsync**) This release adds additional configurations on GraphQL APIs for limits on query depth, resolver count, and introspection
* (**chimesdkmeetings**) Add meeting features to specify a maximum camera resolution, a maximum content sharing resolution, and a maximum number of attendees for a given meeting.
* (**ec2**) Provision BYOIPv4 address ranges and advertise them by specifying the network border groups option in Los Angeles, Phoenix and Dallas AWS Local Zones.
* (**fsx**) Added support for FSx for OpenZFS on-demand data replication across AWS accounts and/or regions.Added the IncludeShared attribute for DescribeSnapshots.Added the CopyStrategy attribute for OpenZFSVolumeConfiguration.
* (**marketplacecatalog**) AWS Marketplace now supports a new API, BatchDescribeEntities, which returns metadata and content for multiple entities.
* (**rds**) RDS - The release adds two new APIs: DescribeDBRecommendations and ModifyDBRecommendation

### Miscellaneous
* Disable search and address accessibility violations in documentation

## [1.0.19] - 12/18/2023

### Features
* (**cognitoidentityprovider**) Amazon Cognito now supports trigger versions that define the fields in the request sent to pre token generation Lambda triggers.
* (**eks**) Add support for EKS Cluster Access Management.
* (**route53resolver**) Add DOH protocols in resolver endpoints.

### Documentation
* (**quicksight**) A docs-only release to add missing entities to the API reference.

## [1.0.18] - 12/15/2023

### Features
* (**connect**) Adds relatedContactId field to StartOutboundVoiceContact API input. Introduces PauseContact API and ResumeContact API for Task contacts. Adds pause duration, number of pauses, timestamps for last paused and resumed events to DescribeContact API response. Adds new Rule type and new Rule action.
* (**connectcases**) Increase number of fields that can be included in CaseEventIncludedData from 50 to 200
* (**sagemaker**) This release 1) introduces a new API: DeleteCompilationJob , and 2) adds InfraCheckConfig for Create/Describe training job API

### Documentation
* (**cloud9**) Updated Cloud9 API documentation for AL2023 release
* (**kms**) Documentation updates for AWS Key Management Service
* (**rds**) Updates Amazon RDS documentation by adding code examples

## [1.0.17] - 12/14/2023

### Features
* (**appstream**) This release includes support for images of Windows Server 2022 platform.
* (**billingconductor**) Billing Conductor is releasing a new API, GetBillingGroupCostReport, which provides the ability to retrieve/view the Billing Group Cost Report broken down by attributes for a specific billing group.
* (**connect**) This release adds support for more granular billing using tags (key:value pairs)
* (**firehose**) This release, 1) adds configurable buffering hints for the Splunk destination, and 2) reduces the minimum configurable buffering interval for supported destinations
* (**gamelift**) Amazon GameLift adds the ability to add and update the game properties of active game sessions.
* (**iot**) This release adds the ability to self-manage certificate signing in AWS IoT Core fleet provisioning using the new certificate provider resource.
* (**neptunegraph**) This is the initial SDK release for Amazon Neptune Analytics
* (**opensearch**) Updating documentation for Amazon OpenSearch Service support for new zero-ETL integration with Amazon S3.
* (**quicksight**) Update Dashboard Links support; SingleAxisOptions support; Scatterplot Query limit support.

### Documentation
* (**b2bi**) Documentation updates for AWS B2B Data Interchange
* (**controltower**) Documentation updates for AWS Control Tower.
* (**workspaces**) Updated note to ensure customers understand running modes.

## [1.0.16] - 12/13/2023

### Features
* (**drs**) Adding AgentVersion to SourceServer and RecoveryInstance structures

## [1.0.15] - 12/12/2023

### Features
* (**cloudwatchlogs**) This release introduces the StartLiveTail API to tail ingested logs in near real time.
* (**imagebuilder**) This release adds the Image Workflows feature to give more flexibility and control over the image building and testing process.
* (**location**) This release 1)  adds sub-municipality field in Places API for searching and getting places information, and 2) allows optimizing route calculation based on expected arrival time.

### Fixes
* [#1008](https://github.com/smithy-lang/smithy-kotlin/issues/1008) Upgrade to [**smithy-kotlin** 1.0.3](https://github.com/smithy-lang/smithy-kotlin/releases/tag/v1.0.3) to consume fixes for URL signing special characters

## [1.0.14] - 12/11/2023

### Features
* (**neptune**) This release adds a new parameter configuration setting to the Neptune cluster related APIs that can be leveraged to switch between the underlying supported storage modes.
* (**securityhub**) Added new resource detail objects to ASFF, including resources for AwsDynamoDbTable, AwsEc2ClientVpnEndpoint, AwsMskCluster, AwsS3AccessPoint, AwsS3Bucket

### Documentation
* (**pinpoint**) This release includes Amazon Pinpoint API documentation updates pertaining to campaign message sending rate limits.

## [1.0.13] - 12/08/2023

### Features
* (**cloudwatch**) Adds support for the OpenTelemetry 1.0 output format in CloudWatch Metric Streams.
* (**ec2**) M2 Mac instances are built on Apple M2 Mac mini computers. I4i instances are powered by 3rd generation Intel Xeon Scalable processors. C7i compute optimized, M7i general purpose and R7i memory optimized instances are powered by custom 4th Generation Intel Xeon Scalable processors.
* (**finspace**) Releasing Scaling Group, Dataview, and Volume APIs

## [1.0.12] - 12/07/2023

### Features
* (**codedeploy**) This release adds support for two new CodeDeploy features: 1) zonal deployments for Amazon EC2 in-place deployments, 2) deployments triggered by Auto Scaling group termination lifecycle hook events.

## [1.0.11] - 12/06/2023

### Features
* (**backup**) AWS Backup - Features: Add VaultType to the output of DescribeRecoveryPoint, ListRecoveryPointByBackupVault API and add ResourceType to the input of ListRestoreJobs API
* (**connect**) Releasing Tagging Support for Instance Management APIS
* (**ec2**) Releasing the new cpuManufacturer attribute within the DescribeInstanceTypes API response which notifies our customers with information on who the Manufacturer is for the processor attached to the instance, for example: Intel.
* (**paymentcryptography**) AWS Payment Cryptography IPEK feature release

### Documentation
* (**comprehend**) Documentation updates for Trust and Safety features.

## [1.0.10] - 12/05/2023

### Features
* (**athena**) Adding IdentityCenter enabled request for interactive query
* (**cleanroomsml**) Updated service title from cleanroomsml to CleanRoomsML.
* (**ec2**) Adds A10G, T4G, and H100 as accelerator name options and Habana as an accelerator manufacturer option for attribute based selection

### Documentation
* (**cloudformation**) Documentation update, December 2023

## [1.0.9] - 12/04/2023

### Features
* (**billingconductor**) This release adds the ability to specify a linked account of the billing group for the custom line item resource.
* (**braket**) This release enhances service support to create quantum tasks and hybrid jobs associated with Braket Direct Reservations.
* (**cloud9**) This release adds the requirement to include the imageId parameter in the CreateEnvironmentEC2 API call.
* (**cloudformation**) Including UPDATE_* states as a success status for CreateStack waiter.
* (**finspace**) Release General Purpose type clusters
* (**medialive**) Adds support for custom color correction on channels using 3D LUT files.

### Documentation
* (**servicecatalogappregistry**) Documentation-only updates for Dawn

## [1.0.8] - 12/01/2023

### Features
* (**qconnect**) This release adds the PutFeedback API and allows providing feedback against the specified assistant for the specified target.
* (**rbin**) Added resource identifier in the output and updated error handling.
* (**verifiedpermissions**) Adds description field to PolicyStore API's and namespaces field to GetSchema.
* [#1132](https://github.com/awslabs/aws-sdk-kotlin/issues/1132) Pass client-configured LogMode through to internal credentials providers

### Miscellaneous
* Add optional `configurationSource` parameter to `ProfileCredentialsProvider`

## [1.0.7] - 11/30/2023

### Features
* (**arczonalshift**) This release adds a new capability, zonal autoshift. You can configure zonal autoshift so that AWS shifts traffic for a resource away from an Availability Zone, on your behalf, when AWS determines that there is an issue that could potentially affect customers in the Availability Zone.
* (**glue**) Adds observation and analyzer support to the GetDataQualityResult and BatchGetDataQualityResult APIs.
* (**sagemaker**) This release adds support for 1/ Code Editor, based on Code-OSS, Visual Studio Code Open Source, a new fully managed IDE option in SageMaker Studio  2/ JupyterLab, a new fully managed JupyterLab IDE experience in SageMaker Studio

## [1.0.6] - 11/30/2023

### Features
* (**marketplaceagreement**) The AWS Marketplace Agreement Service provides an API interface that helps AWS Marketplace sellers manage their agreements, including listing, filtering, and viewing details about their agreements.
* (**marketplacecatalog**) This release enhances the ListEntities API to support new entity type-specific strongly typed filters in the request and entity type-specific strongly typed summaries in the response.
* (**marketplacedeployment**) AWS Marketplace Deployment is a new service that provides essential features that facilitate the deployment of software, data, and services procured through AWS Marketplace.
* (**redshiftserverless**) This release adds the following support for Amazon Redshift Serverless: 1) cross-account cross-VPCs, 2) copying snapshots across Regions, 3) scheduling snapshot creation, and 4) restoring tables from a recovery point.

## [1.0.5] - 11/29/2023

### Features
* (**applicationautoscaling**) Amazon SageMaker customers can now use Application Auto Scaling to automatically scale the number of Inference Component copies across an endpoint to meet the varying demand of their workloads.
* (**cleanrooms**) AWS Clean Rooms now provides differential privacy to protect against user-identification attempts and machine learning modeling to allow two parties to identify similar users in their data.
* (**cleanroomsml**) Public Preview SDK release of AWS Clean Rooms ML APIs
* (**opensearch**) Launching Amazon OpenSearch Service support for new zero-ETL integration with Amazon S3. Customers can now manage their direct query data sources to Amazon S3 programatically
* (**opensearchserverless**) Amazon OpenSearch Serverless collections support an additional attribute called standby-replicas. This allows to specify whether a collection should have redundancy enabled.
* (**sagemaker**) This release adds following support 1/ Improved SDK tooling for model deployment. 2/ New Inference Component based features to lower inference costs and latency 3/ SageMaker HyperPod management. 4/ Additional parameters for FM Fine Tuning in Autopilot
* (**sagemakerruntime**) This release adds InferenceComponentName to InvokeEndpoint and InvokeEndpointWithResponseStream APIs to get inferences from the deployed InferenceComponents.

### Documentation
* (**sts**) Documentation updates for AWS Security Token Service.

## [1.0.4] - 11/28/2023

### Features
* (**accessanalyzer**) This release adds support for external access findings for S3 directory buckets to help you easily identify cross-account access. Updated service API, documentation, and paginators.
* (**bedrock**) This release adds support for customization types, model life cycle status and minor versions/aliases for model identifiers.
* (**bedrockagent**) This release introduces Agents for Amazon Bedrock
* (**bedrockagentruntime**) This release introduces Agents for Amazon Bedrock Runtime
* (**bedrockruntime**) This release adds support for minor versions/aliases for invoke model identifier.
* (**connect**) Added support for following capabilities: Amazon Connect's in-app, web, and video calling. Two-way SMS integrations. Contact Lens real-time chat analytics feature. Amazon Connect Analytics Datalake capability. Capability to configure real time chat rules.
* (**customerprofiles**) This release introduces DetectProfileObjectType API to auto generate object type mapping.
* (**qbusiness**) Amazon Q - a generative AI powered application that your employees can use to ask questions and get answers from knowledge spread across disparate content repositories, summarize reports, write articles, take actions, and much more - all within their company's connected content repositories.
* (**qconnect**) Amazon Q in Connect, an LLM-enhanced evolution of Amazon Connect Wisdom. This release adds generative AI support to Amazon Q Connect QueryAssistant and GetRecommendations APIs.
* (**s3**) Adds support for S3 Express One Zone.
* (**s3control**) Adds support for S3 Express One Zone, and InvocationSchemaVersion 2.0 for S3 Batch Operations.

## [1.0.3] - 11/28/2023

### Features
* (**elasticache**) Launching Amazon ElastiCache Serverless that enables you to create a cache in under a minute without any capacity management. ElastiCache Serverless monitors the cache's memory, CPU, and network usage and scales both vertically and horizontally to support your application's requirements.

## [1.0.2] - 11/27/2023

### Features
* (**appsync**) This update enables introspection of Aurora cluster databases using the RDS Data API
* (**b2bi**) This is the initial SDK release for AWS B2B Data Interchange.
* (**backup**) AWS Backup now supports restore testing, a new feature that allows customers to automate restore testing and validating their backups. Additionally, this release adds support for EBS Snapshots Archive tier.
* (**controltower**) This release adds the following support: 1. The EnableControl API can configure controls that are configurable.  2. The GetEnabledControl API shows the configured parameters on an enabled control. 3. The new UpdateEnabledControl API can change parameters on an enabled control.
* (**efs**) Adding support for EFS Replication to existing file system.
* (**fis**) AWS FIS adds support for multi-account experiments & empty target resolution. This release also introduces the CreateTargetAccountConfiguration API that allows experiments across multiple AWS accounts, and the ListExperimentResolvedTargets API to list target details.
* (**glue**) add observations support to DQ CodeGen config model + update document for connectiontypes supported by ConnectorData entities
* (**securityhub**) Adds and updates APIs to support central configuration. This feature allows the Security Hub delegated administrator to configure Security Hub for their entire AWS Org across multiple regions from a home Region. With this release, findings also include account name and application metadata.
* (**transcribe**) This release adds support for AWS HealthScribe APIs within Amazon Transcribe

### Documentation
* (**rds**) Updates Amazon RDS documentation for support for RDS for Db2.

## [1.0.1] - 11/27/2023

### Features
* (**accessanalyzer**) IAM Access Analyzer now continuously monitors IAM roles and users in your AWS account or organization to generate findings for unused access. Additionally, IAM Access Analyzer now provides custom policy checks to validate that IAM policies adhere to your security standards ahead of deployments.
* (**amp**) This release adds support for the Amazon Managed Service for Prometheus collector, a fully managed, agentless Prometheus metrics scraping capability.
* (**bcmdataexports**) Users can create, read, update, delete Exports of billing and cost management data.  Users can get details of Export Executions and details of Tables for exporting.  Tagging support is provided for Exports
* (**cloudtrail**) CloudTrail Lake now supports federating event data stores. giving users the ability to run queries against their event data using Amazon Athena.
* (**cloudwatchlogs**) Added APIs to Create, Update, Get, List and Delete LogAnomalyDetectors and List and Update Anomalies in Detector. Added LogGroupClass attribute for LogGroups to classify loggroup as Standard loggroup with all capabilities or InfrequentAccess loggroup with limited capabilities.
* (**codestarconnections**) This release adds support for the CloudFormation Git sync feature. Git sync enables updating a CloudFormation stack from a template stored in a Git repository.
* (**computeoptimizer**) This release enables AWS Compute Optimizer to analyze and generate recommendations with customization and discounts preferences.
* (**configservice**) Support Periodic Recording for Configuration Recorder
* (**controltower**) Add APIs to create and manage a landing zone.
* (**costoptimizationhub**) This release launches Cost Optimization Hub, a new AWS Billing and Cost Management feature that helps you consolidate and prioritize cost optimization recommendations across your AWS Organizations member accounts and AWS Regions, so that you can get the most out of your AWS spend.
* (**detective**) Added new APIs in Detective to support resource investigations
* (**ecs**) Adds a new 'type' property to the Setting structure. Adds a new AccountSetting - guardDutyActivate for ECS.
* (**efs**) Adding support for EFS Archive lifecycle configuration.
* (**eks**) This release adds support for EKS Pod Identity feature. EKS Pod Identity makes it easy for customers to obtain IAM permissions for the applications running in their EKS clusters.
* (**eksauth**) This release adds support for EKS Pod Identity feature. EKS Pod Identity makes it easy for customers to obtain IAM permissions for their applications running in the EKS clusters.
* (**elasticloadbalancingv2**) This release enables both mutual authentication (mTLS), and Automatic Target Weights (ATW) for Application Load Balancers.
* (**freetier**) This is the initial SDK release for the AWS Free Tier GetFreeTierUsage API
* (**fsx**) Added support for FSx for ONTAP scale-out file systems and FlexGroup volumes. Added the HAPairs field and ThroughputCapacityPerHAPair for filesystem. Added AggregateConfiguration (containing Aggregates and ConstituentsPerAggregate) and SizeInBytes for volume.
* (**guardduty**) Add support for Runtime Monitoring for ECS and ECS-EC2.
* (**iotfleetwise**) AWS IoT FleetWise introduces new APIs for vision system data, such as data collected from cameras, radars, and lidars. You can now model and decode complex data types.
* (**lakeformation**) This release adds four new APIs "DescribeLakeFormationIdentityCenterConfiguration", "CreateLakeFormationIdentityCenterConfiguration", "DescribeLakeFormationIdentityCenterConfiguration", and "DeleteLakeFormationIdentityCenterConfiguration", and also updates the corresponding documentation.
* (**lexmodelsv2**) This release introduces new generative AI features in AWS Lex: Assisted Slot Resolution, Descriptive Bot Building, and Sample Utterance Generation. These features leverage large language models available through Amazon Bedrock to improve the bot builder and customer experiences.
* (**lexruntimev2**) This release introduces support for interpretationSource in the runtime service response.
* (**managedblockchain**) Add optional NetworkType property to Accessor APIs
* (**personalize**) Enables metadata in recommendations, recommendations with themes, and next best action recommendations
* (**personalizeevents**) This release enables PutActions and PutActionInteractions
* (**personalizeruntime**) Enables metadata in recommendations and next best action recommendations
* (**quicksight**) This release launches new APIs for trusted identity propagation setup and supports creating datasources using trusted identity propagation as authentication method for QuickSight accounts configured with IAM Identity Center.
* (**redshift**) This release adds support for multi-data warehouse writes through data sharing.
* (**repostspace**) Initial release of AWS re:Post Private
* (**s3**) Adding new params - Key and Prefix, to S3 API operations for supporting S3 Access Grants. Note - These updates will not change any of the existing S3 API functionality.
* (**s3control**) Introduce Amazon S3 Access Grants, a new S3 access control feature that maps identities in directories such as Active Directory, or AWS Identity and Access Management (IAM) Principals, to datasets in S3.
* (**secretsmanager**) AWS Secrets Manager has released the BatchGetSecretValue API, which allows customers to fetch up to 20 Secrets with a single request using a list of secret names or filters.
* (**securityhub**) Adds and updates APIs to support customizable security controls. This feature allows Security Hub customers to provide custom parameters for security controls. With this release, findings for controls that support custom parameters will include the parameters used to generate the findings.
* (**sfn**) Adds new TestState operation which accepts the definition of a single state and executes it. You can test a state without creating a state machine or updating an existing state machine.
* (**transcribe**) This release adds support for transcriptions from audio sources in 64 new languages and introduces generative call summarization in Transcribe Call Analytics (Post call)
* (**workspaces**) The release introduces Multi-Region Resilience one-way data replication that allows you to replicate data from your primary WorkSpace to a standby WorkSpace in another AWS Region. DescribeWorkspaces now returns the status of data replication.
* (**workspacesthinclient**) Initial release of Amazon WorkSpaces Thin Client

## [1.0.0] - 11/26/2023

### Features
* [#659](https://github.com/smithy-lang/smithy-kotlin/issues/659) BREAKING: Overhaul URL APIs to clarify content encoding, when data is in which state, and to reduce the number of times data is encoded/decoded

### Fixes
* **Breaking**: Make some properties of IoT types optional. Previously they defaulted to false, which isn't what the service expects.

## [0.36.1-beta] - 11/22/2023

### Features
* (**kinesis**) This release adds support for resource based policies on streams and consumers.
* (**s3control**) Amazon S3 Batch Operations now manages buckets or prefixes in a single step.
* (**sagemaker**) This feature adds the end user license agreement status as a model access configuration parameter.

## [0.36.0-beta] - 11/21/2023

### Features
* (**cloudfront**) This release adds support for CloudFront KeyValueStore, a globally managed key value datastore associated with CloudFront Functions.
* (**cloudfrontkeyvaluestore**) This release adds support for CloudFront KeyValueStore, a globally managed key value datastore associated with CloudFront Functions.
* (**inspectorscan**) This release adds support for the new Amazon Inspector Scan API. The new Inspector Scan API can synchronously scan SBOMs adhering to the CycloneDX v1.5 format.
* (**iotsitewise**) Adds 1/ user-defined unique identifier for asset and model metadata, 2/ asset model components, and 3/ query API for asset metadata and telemetry data. Supports 4/ multi variate anomaly detection using Amazon Lookout for Equipment, 5/ warm storage tier, and 6/ buffered ingestion of time series data.
* (**iottwinmaker**) This release adds following support. 1. New APIs for metadata bulk operations. 2. Modify the component type API to support composite component types - nesting component types within one another. 3. New list APIs for components and properties. 4. Support the larger scope digital twin modeling.
* (**s3**) Add support for automatic date based partitioning in S3 Server Access Logs.
* [#1002](https://github.com/awslabs/aws-sdk-kotlin/issues/1002), [#1003](https://github.com/awslabs/aws-sdk-kotlin/issues/1003) Make region providers public and allow profile name override
* [#1055](https://github.com/awslabs/aws-sdk-kotlin/issues/1055) **BREAKING**: Make the AWS retry policy inheritable
* [#199](https://github.com/awslabs/aws-sdk-kotlin/issues/199) Handle S3 errors returned with an HTTP 200 response

### Fixes
* **Breaking**: Make properties of S3Control PublicAccessBlockConfiguration optional by removing default values. Previously they defaulted to false, which caused invalid requests

### Documentation
* (**ec2**) Documentation updates for Amazon EC2.

## [0.35.1-beta] - 11/20/2023

### Features
* (**codestarconnections**) This release updates a few CodeStar Connections related APIs.
* (**docdb**) Amazon DocumentDB updates for new cluster storage configuration: Amazon DocumentDB I/O-Optimized.
* (**ec2**) This release adds support for Security group referencing over Transit gateways, enabling you to simplify Security group management and control of instance-to-instance traffic across VPCs that are connected by Transit gateway.

## [0.35.0-beta] - 11/17/2023

### Features
* (**appmesh**) Change the default value of these fields from 0 to null: MaxConnections, MaxPendingRequests, MaxRequests, HealthCheckThreshold, PortNumber, and HealthCheckPolicy -> port. Users are not expected to perceive the change, except that badRequestException is thrown when required fields missing configured.
* (**athena**) Adding SerivicePreProcessing time metric
* (**cloudformation**) This release adds a new flag ImportExistingResources to CreateChangeSet. Specify this parameter on a CREATE- or UPDATE-type change set to import existing resources with custom names instead of recreating them.
* (**codepipeline**) CodePipeline now supports overriding source revisions to achieve manual re-deploy of a past revision
* (**codestarconnections**) This release adds support for the CloudFormation Git sync feature. Git sync enables updating a CloudFormation stack from a template stored in a Git repository.
* (**connect**) This release adds WISDOM_QUICK_RESPONSES as new IntegrationType of Connect IntegrationAssociation resource and bug fixes.
* (**ec2**) This release adds new features for Amazon VPC IP Address Manager (IPAM) Allowing a choice between Free and Advanced Tiers, viewing public IP address insights across regions and in Amazon Cloudwatch, use IPAM to plan your subnet IPs within a VPC and bring your own autonomous system number to IPAM.
* (**ecr**) Documentation and operational updates for Amazon ECR, adding support for pull through cache rules for upstream registries that require authentication.
* (**emr**) Launch support for IAM Identity Center Trusted Identity Propagation and workspace storage encryption using AWS KMS in EMR Studio
* (**eventbridge**) Introduces a new rule state ENABLED_WITH_ALL_CLOUDTRAIL_MANAGEMENT_EVENTS for matching with Get, List and Describe AWS API call events from CloudTrail.
* (**internetmonitor**) Adds new querying capabilities for running data queries on a monitor
* (**ivs**) type & defaulting refinement to various range properties
* (**ivschat**) type & defaulting refinement to various range properties
* (**location**) Remove default value and allow nullable for request parameters having minimum value larger than zero.
* (**medialive**) MediaLive has now added support for per-output static image overlay.
* (**mgn**) Removed invalid and unnecessary default values.
* (**osis**) Add support for enabling a persistent buffer when creating or updating an OpenSearch Ingestion pipeline. Add tags to Pipeline and PipelineSummary response models.
* (**pipes**) TargetParameters now properly supports BatchJobParameters.ArrayProperties.Size and BatchJobParameters.RetryStrategy.Attempts being optional, and EcsTaskParameters.Overrides.EphemeralStorage.SizeInGiB now properly required when setting EphemeralStorage
* (**rds**) This release adds support for option groups and replica enhancements to Amazon RDS Custom.
* (**redshift**) Updated SDK for Amazon Redshift, which you can use to configure a connection with IAM Identity Center to manage access to databases. With these, you can create a connection through a managed application. You can also change a managed application, delete it, or get information about an existing one.
* (**redshiftserverless**) Updated SDK for Amazon Redshift Serverless, which provides the ability to configure a connection with IAM Identity Center to manage user and group access to databases.
* (**s3**) Removes all default 0 values for numbers and false values for booleans
* (**ssoadmin**) Improves support for configuring RefreshToken and TokenExchange grants on applications.
* (**ssooidc**) Adding support for `sso-oauth:CreateTokenWithIAM`.
* (**trustedadvisor**) AWS Trusted Advisor introduces new APIs to enable you to programmatically access Trusted Advisor best practice checks, recommendations, and prioritized recommendations. Trusted Advisor APIs enable you to integrate Trusted Advisor with your operational tools to automate your workloads.
* (**verifiedpermissions**) Adding BatchIsAuthorized API which supports multiple authorization requests against a PolicyStore
* (**wisdom**) This release adds QuickResponse as a new Wisdom resource and Wisdom APIs for import, create, read, search, update and delete QuickResponse resources.
* ⚠️ **IMPORTANT**: Enable account ID based endpoint routing for services that support it

### Fixes
* **Breaking**: Make some types for various services optional by removing default values

### Documentation
* (**cloud9**) A minor doc only update related to changing the date of an API change.
* (**dlm**) Added support for SAP HANA in Amazon Data Lifecycle Manager EBS snapshot lifecycle policies with pre and post scripts.
* (**kinesisvideo**) Docs only build to bring up-to-date with public docs.
* (**sts**) API updates for the AWS Security Token Service

### Miscellaneous
* Upgrade dependencies to their latest versions, notably Kotlin 1.9.20
* Remove Macie v1 service

## [0.34.9-beta] - 11/16/2023

### Features
* (**codecatalyst**) This release includes updates to the Dev Environment APIs to include an optional vpcConnectionName parameter that supports using Dev Environments with Amazon VPC.
* (**dlm**) This release adds support for Amazon Data Lifecycle Manager default policies for EBS snapshots and EBS-backed AMIs.
* (**ec2**) Enable use of tenant-specific PublicSigningKeyUrl from device trust providers and onboard jumpcloud as a new device trust provider.
* (**fsx**) Enables customers to update their PerUnitStorageThroughput on their Lustre file systems.
* (**glue**) Introduces new column statistics APIs to support statistics generation for tables within the Glue Data Catalog.
* (**imagebuilder**) This release adds the Image Lifecycle Management feature to automate the process of deprecating, disabling and deleting outdated images and their associated resources.
* (**iot**) GA release the ability to index and search devices based on their GeoLocation data. With GeoQueries you can narrow your search to retrieve devices located in the desired geographic boundary.
* (**ivsrealtime**) This release introduces server side composition and recording for stages.
* (**kafka**) Added a new API response field which determines if there is an action required from the customer regarding their cluster.
* (**lambda**) Adds support for logging configuration in Lambda Functions. Customers will have more control how their function logs are captured and to which cloud watch log group they are delivered also.
* (**macie2**) This release adds support for configuring Macie to assume an IAM role when retrieving sample occurrences of sensitive data reported by findings.
* (**mediapackage**) DRM_TOP_LEVEL_COMPACT allows placing content protection elements at the MPD level and referenced at the AdaptationSet level
* (**pinpointsmsvoicev2**) Amazon Pinpoint now offers additional operations as part of version 2 of the SMS and voice APIs. This release includes 26 new APIs to create and manage phone number registrations, add verified destination numbers, and request sender IDs.
* (**polly**) Add new engine - long-form - dedicated for longer content, such as news articles, training materials, or marketing videos.
* (**quicksight**) Custom permission support for QuickSight roles; Three new datasources STARBURST, TRINO, BIGQUERY; Lenient mode changes the default behavior to allow for exporting and importing with certain UI allowed errors, Support for permissions and tags export and import.
* (**sagemaker**) Amazon SageMaker Studio now supports Trainium instance types - trn1.2xlarge, trn1.32xlarge, trn1n.32xlarge.
* (**ssm**) This release introduces the ability to filter automation execution steps which have parent steps. In addition, runbook variable information is returned by GetAutomationExecution and parent step information is returned by the DescribeAutomationStepExecutions API.
* (**ssmincidents**) Introduces new APIs ListIncidentFindings and BatchGetIncidentFindings to use findings related to an incident.
* (**ssoadmin**) Instances bound to a single AWS account, API operations for managing instances and applications, and assignments to applications are now supported. Trusted identity propagation is also supported, with new API operations for managing trusted token issuers and application grants and scopes.
* (**transfer**) Introduced S3StorageOptions for servers to enable directory listing optimizations and added Type fields to logical directory mappings.

## [0.34.8-beta] - 11/15/2023

### Features
* (**autoscaling**) This release introduces Instance Maintenance Policy, a new EC2 Auto Scaling capability that allows customers to define whether instances are launched before or after existing instances are terminated during instance replacement operations.
* (**cloudtrail**) The Lake Repricing feature lets customers configure a BillingMode for an event data store. The BillingMode determines the cost for ingesting and storing events and the default and maximum retention period for the event data store.
* (**codecatalyst**) This release adds functionality for retrieving information about workflows and workflow runs and starting workflow runs in Amazon CodeCatalyst.
* (**ec2**) AWS EBS now supports Snapshot Lock, giving users the ability to lock an EBS Snapshot to prohibit deletion of the snapshot. This release introduces the LockSnapshot, UnlockSnapshot & DescribeLockedSnapshots APIs to manage lock configuration for snapshots. The release also includes the dl2q_24xlarge.
* (**finspace**) Adding deprecated trait on Dataset Browser Environment APIs
* (**finspacedata**) Adding deprecated trait to APIs in this name space.
* (**lambda**) Add Java 21 (java21) support to AWS Lambda
* (**mwaa**) This Amazon MWAA release adds support for customer-managed VPC endpoints. This lets you choose whether to create, and manage your environment's VPC endpoints, or to have Amazon MWAA create, and manage them for you.
* (**redshift**) The custom domain name SDK for Amazon Redshift provisioned clusters is updated with additional required parameters for modify and delete operations. Additionally, users can provide domain names with longer top-level domains.
* (**s3control**) Add 5 APIs to create, update, get, list, delete S3 Storage Lens group(eg. CreateStorageLensGroup), 3 APIs for tagging(TagResource,UntagResource,ListTagsForResource), and update to StorageLensConfiguration to allow metrics to be aggregated on Storage Lens groups.
* (**ssmsap**) Update the default value of MaxResult to 50.

### Documentation
* (**rds**) Updates Amazon RDS documentation for support for upgrading RDS for MySQL snapshots from version 5.7 to version 8.0.

## [0.34.7-beta] - 11/14/2023

### Features
* (**backup**) AWS Backup - Features: Provide Job Summary for your backup activity.
* (**cleanrooms**) This feature provides the ability for the collaboration creator to configure either the member who can run queries or a different member in the collaboration to be billed for query compute costs.
* (**connect**) Introducing SegmentAttributes parameter for StartChatContact API
* (**glue**) Introduces new storage optimization APIs to support automatic compaction of Apache Iceberg tables.
* (**iot**) This release introduces new attributes in API CreateSecurityProfile, UpdateSecurityProfile and DescribeSecurityProfile to support management of Metrics Export for AWS IoT Device Defender Detect.
* (**lambda**) Add Python 3.12 (python3.12) support to AWS Lambda
* (**mediatailor**) Removed unnecessary default values.
* (**pipes**) Added support (via new LogConfiguration field in CreatePipe and UpdatePipe APIs) for logging to Amazon CloudWatch Logs, Amazon Simple Storage Service (Amazon S3), and Amazon Kinesis Data Firehose
* (**resourceexplorer2**) Resource Explorer supports multi-account search. You can now use Resource Explorer to search and discover resources across AWS accounts within your organization or organizational unit.
* (**sagemaker**) This release makes Model Registry Inference Specification fields as not required.
* (**sfn**) This release adds support to redrive executions in AWS Step Functions with a new RedriveExecution operation.
* Enable auth scheme resolution via endpoints for S3 and EventBridge

### Documentation
* (**signer**) Documentation updates for AWS Signer

## [0.34.6-beta] - 11/13/2023

### Features
* (**databasemigrationservice**) Added new Db2 LUW Target endpoint with related endpoint settings. New executeTimeout endpoint setting for mysql endpoint. New ReplicationDeprovisionTime field for serverless describe-replications.
* (**dataexchange**) Removed Required trait for DataSet.OriginDetails.ProductId.
* (**ec2**) Adds the new EC2 DescribeInstanceTopology API, which you can use to retrieve the network topology of your running instances on select platform types to determine their relative proximity to each other.
* (**ecs**) Adds a Client Token parameter to the ECS RunTask API. The Client Token parameter allows for idempotent RunTask requests.
* (**emr**) Updated GetClusterSessionCredentials API  to allow Amazon SageMaker Studio to connect to EMR on EC2 clusters to support IdentityCenter/PEZ integration.
* (**servicecatalogappregistry**) When the customer associates a resource collection to their application with this new feature, then a new application tag will be applied to all supported resources that are part of that collection. This allows customers to more easily find the application that is associated with those resources.
* (**transcribestreaming**) This release enables customers to call the AWS Transcribe streaming service with the capability of identifying multiple languages in the stream.

## [0.34.5-beta] - 11/10/2023

### Features
* (**controltower**) AWS Control Tower supports tagging for enabled controls. This release introduces TagResource, UntagResource and ListTagsForResource APIs to manage tags in existing enabled controls. It updates EnabledControl API to tag resources at creation time.
* (**costandusagereportservice**) This release adds support for tagging and customers can now tag report definitions. Additionally, ReportStatus is now added to report definition to show when the last delivered time stamp and if it succeeded or not.
* (**ec2**) EC2 adds API updates to enable ENA Express at instance launch time.
* (**marketplaceentitlementservice**) Add paginators to GetEntitlements.
* (**mediaconvert**) This release includes the ability to specify any input source as the primary input for corresponding follow modes, and allows users to specify fit and fill behaviors without resizing content.

### Documentation
* (**fms**) Adds optimizeUnassociatedWebACL flag to ManagedServiceData, updates third-party firewall examples, and other minor documentation updates.
* (**rds**) Updates Amazon RDS documentation for zero-ETL integrations.

## [0.34.4-beta] - 11/09/2023

### Features
* (**cloudformation**) Added new ConcurrencyMode feature for AWS CloudFormation StackSets for faster deployments to target accounts.
* (**cloudtrail**) The Insights in Lake feature lets customers enable CloudTrail Insights on a source CloudTrail Lake event data store and create a destination event data store to collect Insights events based on unusual management event activity in the source event data store.
* (**cloudwatchlogs**) Update to support new APIs for delivery of logs from AWS services.
* (**comprehend**) This release adds support for toxicity detection and prompt safety classification.
* (**connect**) This release adds the ability to integrate customer lambda functions with Connect attachments for scanning and updates the ListIntegrationAssociations API to support filtering on IntegrationArn.
* (**ec2**) AWS EBS now supports Block Public Access for EBS Snapshots. This release introduces the EnableSnapshotBlockPublicAccess, DisableSnapshotBlockPublicAccess and GetSnapshotBlockPublicAccessState APIs to manage account-level public access settings for EBS Snapshots in an AWS Region.
* (**eks**) Adding EKS Anywhere subscription related operations.
* (**lambda**) Add Custom runtime on Amazon Linux 2023 (provided.al2023) support to AWS Lambda.
* (**omics**) Support UBAM filetype for Omics Storage and make referenceArn optional

## [0.34.3-beta] - 11/09/2023

### Features
* (**sqs**) This release enables customers to call SQS using AWS JSON-1.0 protocol and bug fix.

## [0.34.2-beta] - 11/08/2023

### Features
* (**connect**) This release clarifies in our public documentation that InstanceId is a requirement for SearchUsers API requests.
* (**connectcases**) This release adds the ability to add/view comment authors through CreateRelatedItem and SearchRelatedItems API. For more information see https://docs.aws.amazon.com/cases/latest/APIReference/Welcome.html
* (**datasync**) This change allows for 0 length access keys and secret keys for object storage locations. Users can now pass in empty string credentials.
* (**guardduty**) Added API support for new GuardDuty EKS Audit Log finding types.
* (**lambda**) Add Node 20 (nodejs20.x) support to AWS Lambda.
* (**lexmodelsv2**) AWS Lex now supports selective log capture in conversation logs. When you enable this option within the conversation log settings, only the utterances that trigger intents and slots specified in session attributes will be logged.
* (**omics**) Adding Run UUID and Run Output URI: GetRun and StartRun API response has two new fields "uuid" and "runOutputUri".
* (**redshiftserverless**) Added a new parameter in the workgroup that helps you control your cost for compute resources. This feature provides a ceiling for RPUs that Amazon Redshift Serverless can scale up to. When automatic compute scaling is required, having a higher value for MaxRPU can enhance query throughput.
* (**resiliencehub**) AWS Resilience Hub enhances Resiliency Score, providing actionable recommendations to improve application resilience. Amazon Elastic Kubernetes Service (EKS) operational recommendations have been added to help improve the resilience posture of your applications.
* (**sqs**) This release enables customers to call SQS using AWS JSON-1.0 protocol.

### Documentation
* (**rds**) This Amazon RDS release adds support for patching the OS of an RDS Custom for Oracle DB instance. You can now upgrade the database or operating system using the modify-db-instance command.

## [0.34.1-beta] - 11/07/2023

### Features
* (**dataexchange**) Updated SendDataSetNotificationRequest Comment to be maximum length 4096.
* (**lifecycle**) Added support for pre and post scripts in Amazon Data Lifecycle Manager EBS snapshot lifecycle policies.
* (**rds**) This Amazon RDS release adds support for the multi-tenant configuration. In this configuration, an RDS DB instance can contain multiple tenant databases. In RDS for Oracle, a tenant database is a pluggable database (PDB).

## [0.34.0-beta] - 11/06/2023

### Features
* (**awslily**) Added new API that allows Amazon Connect Outbound Campaigns to create contacts in Amazon Connect when ingesting your dial requests.
* (**codebuild**) AWS CodeBuild now supports AWS Lambda compute.
* (**docdb**) Update the input of CreateDBInstance and ModifyDBInstance to support setting CA Certificates. Update the output of DescribeDBInstance and DescribeDBEngineVersions to show current and supported CA certificates.
* (**iam**) Add partitional endpoint for iso-e.
* (**iis**) This release extends the GetReservationPurchaseRecommendation API to support recommendations for Amazon MemoryDB reservations.
* (**mwaa**) This release adds support for Apache Airflow version 2.7.2. This version release includes support for deferrable operators and triggers.
* (**polly**) Amazon Polly adds new US English voices - Danielle and Gregory. Danielle and Gregory are available as Neural voices only.
* (**route53**) Add partitional endpoints for iso-e and iso-f.
* **BREAKING**: Remove operations/fields which were marked deprecated before 11/28/2023. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/1103) for more details.

### Fixes
* [#1101](https://github.com/awslabs/aws-sdk-kotlin/issues/1101) Fix endpoint builtin bound to wrong config parameter

## [0.33.1-beta] - 11/01/2023

### Features
* Support EKS endpoints and auth token file in container credentials provider.

### Fixes
* [#1098](https://github.com/awslabs/aws-sdk-kotlin/issues/1098) Add `forcePathStyle` and `enableAccelerate` config properties back to the S3 client.

### Miscellaneous
* Upgrade to the latest version of **smithy-kotlin**

## [0.33.0-beta] - 10/26/2023

### Features
* **BREAKING**: Update codegen to improve nullability of generated types.
* [#605](https://github.com/awslabs/aws-sdk-kotlin/issues/605), [#805](https://github.com/awslabs/aws-sdk-kotlin/issues/805) Publish a BOM and a Version Catalog
* Detect and automatically correct clock skew to prevent signing errors

### Fixes
* Ignore empty environment variable and system property strings when evaluating AWS credentials

### Miscellaneous
* Upgrade Kotlin to 1.9.10
* Sync AWS models and upgrade smithy-kotlin
* Upgrade Dokka to 1.9.0
* **Breaking** Removed `enableAccelerate` & `forcePathStyle` from S3 config. As well as `use_accelerate_endpoint` & `addressing_style` from AWS profile configuration
* **BREAKING**: Remove `smithy.client.request.size`, `smithy.client.response.size`, `smithy.client.retries` metrics. Rename all `smithy.client.*` metrics to `smithy.client.call.*`.
* Remove GameSparks service
* Add skeleton implementation of a second KMP target
* Added `s3_use_arn_region` & `s3_disable_multiregion_access_points` to AWS profile configuration

## [0.32.5-beta] - 10/12/2023

### Features
* [#945](https://github.com/awslabs/aws-sdk-kotlin/issues/945) Add new sources for User-Agent app id

### Miscellaneous
* Sync to the latest versions of **smithy-kotlin** and AWS service models

## [0.32.4-beta] - 10/06/2023

### Miscellaneous
* Track upstream changes that make `ByteArrayContent` and friends internal. Users should only be using `ByteStream.fromBytes()`, `ByteStream.fromString()`, and `HttpBody.fromBytes()`.

## [0.32.3-beta] - 09/28/2023

### Fixes
* [#1048](https://github.com/awslabs/aws-sdk-kotlin/issues/1048) Restore public constructor for `EcsCredentialsProvider`
* [#1044](https://github.com/awslabs/aws-sdk-kotlin/issues/1044) ignore `__type` when deserializing union for AWS JSON 1.0, AWS JSON 1.1, and AWS restJson 1

### Miscellaneous
* Generate internal-only clients with `internal` visibility
* sync AWS models and upgrade smithy kotlin

## [0.32.2-beta] - 09/15/2023

### Miscellaneous
* [#946](https://github.com/awslabs/aws-sdk-kotlin/issues/946) Refactor CredentialsProvider APIs
* Sync smithy-kotlin and AWS service models.

## [0.32.1-beta] - 09/08/2023

### Features
* [#1033](https://github.com/awslabs/aws-sdk-kotlin/issues/1033) Add `SystemPropertyCredentialsProvider` and make it first in default chain credentials provider
* Allow endpoint URL configuration via env and shared config.
* [#1000](https://github.com/awslabs/aws-sdk-kotlin/issues/1000) Add more parameters for fetching STS credentials

### Fixes
* [#935](https://github.com/smithy-lang/smithy-kotlin/issues/935) Fix closing an event stream causing an IllegalStateException

### Miscellaneous
* Sync AWS models and bump smithy-kotlin

## [0.32.0-beta] - 08/31/2023

### Miscellaneous
* **BREAKING**: Refactor HttpCall and HttpResponse types
* Bump **smithy-kotlin** and AWS service models to latest versions

## [0.31.0-beta] - 08/24/2023

### Features
* Support initial-request and initial-response for event streams using RPC-based protocols

### Fixes
* [#1029](https://github.com/awslabs/aws-sdk-kotlin/issues/1029) Update smithy-kotlin to 0.26.0

### Miscellaneous
* **BREAKING**: prefix generated endpoint and auth scheme providers with client name and track upstream changes
* Sync AWS models
* Refactor ClientOption to AttributeKey directly and track upstream HttpContext changes

## [0.30.1-beta] - 08/17/2023

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.30.0-beta] - 08/11/2023

### Features
* [#583](https://github.com/awslabs/aws-sdk-kotlin/issues/583) Make user-supplied region available to config resolution providers
* [#1004](https://github.com/awslabs/aws-sdk-kotlin/issues/1004) Make RegionProviderChain accept a list of RegionProvider

### Fixes
* [#194](https://github.com/awslabs/aws-sdk-kotlin/issues/194) Correctly parse and handle `GetBucketLocation` responses

### Miscellaneous
* Upgrade Kotlin to 1.8.22
* [#968](https://github.com/awslabs/aws-sdk-kotlin/issues/968) Add service-level benchmarks
* Upgrade kotlinx.coroutines to 1.7.3
* Sync AWS service models and **smithy-kotlin** to latest versions

## [0.29.1-beta] - 07/27/2023

### Features
* [#745](https://github.com/awslabs/aws-sdk-kotlin/issues/745) Validate returned content length on S3 `GetObject` responses.

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.29.0-beta] - 07/20/2023

### Features
* [#146](https://github.com/smithy-lang/smithy-kotlin/issues/146), [#800](https://github.com/awslabs/aws-sdk-kotlin/issues/800) Enable **Timestream Query** and **Timestream Write** service clients
* [#969](https://github.com/awslabs/aws-sdk-kotlin/issues/969) Make `region` an optional client config parameter to support multi-region use cases

### Miscellaneous
* **BREAKING**: Refactor observability API and configuration. See the [discussion](https://github.com/awslabs/aws-sdk-kotlin/discussions/981) for more information.
* Sync AWS service models.

## [0.28.2-beta] - 07/13/2023

### Fixes
* [#242](https://github.com/awslabs/aws-sdk-kotlin/issues/242) Correctly handle and throw `InvalidChangeBatch` responses from Route53

### Miscellaneous
* Sync AWS service models

## [0.28.1-beta] - 07/06/2023

### Miscellaneous
* Upgrade smithy-kotlin and sync service models.

## [0.28.0-beta] - 06/29/2023

### Features
* [#701](https://github.com/awslabs/aws-sdk-kotlin/issues/701) **Breaking**: Simplify mechanisms for setting/updating retry strategies in client config. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/964) for more details.
* [#701](https://github.com/awslabs/aws-sdk-kotlin/issues/701) Add adaptive retry mode

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.27.2-beta] - 06/22/2023

### Miscellaneous
* Sync AWS service models

## [0.27.1-beta] - 06/19/2023

### Fixes
* [#815](https://github.com/awslabs/aws-sdk-kotlin/issues/815) Fix a bug in forming S3 WriteGetObjectResponse's host path

### Miscellaneous
* Sync AWS service models.
* Update user agent header to new cross-SDK format

## [0.27.0-beta] - 06/09/2023

### Miscellaneous
* Upgrade smithy-kotlin and sync service models.
* [#824](https://github.com/awslabs/aws-sdk-kotlin/issues/824) **BREAKING:** Update closeability of various types of CredentialsProvider

## [0.26.1-beta] - 06/01/2023

### Fixes
* Fix infinite pagination in S3 `ListParts`

### Miscellaneous
* Sync AWS models and bump smithy-kotlin

## [0.26.0-beta] - 05/25/2023

### Features
* [#755](https://github.com/smithy-lang/smithy-kotlin/issues/755) **Breaking**: Refresh presigning APIs to simplify usage and add new capabilities. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/925) for more information.

### Miscellaneous
* Sync the latest versions of AWS service models and **smithy-kotlin**

## [0.25.0-beta] - 05/19/2023

### Features
* **Breaking**: Make HTTP engines configurable in client config during initialization and during `withCopy`. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/new?category=announcements) for more information.

### Miscellaneous
* Sync smithy-kotlin and AWS service models.

## [0.24.0-beta] - 05/12/2023

### Features
* [#806](https://github.com/awslabs/aws-sdk-kotlin/issues/806) Add support for IAM Identity Center authentication and sso-session support in shared config

### Miscellaneous
* **BREAKING**: Refactor `SsoCredentialsProvider` to take an `SsoSession` parameter.
* Sync smithy-kotlin and service models.

## [0.23.0-beta] - 05/04/2023

### Features
* [#432](https://github.com/awslabs/aws-sdk-kotlin/issues/432) Enable resolving `LogMode` from environment

### Miscellaneous
* Sync AWS models and **smithy-kotlin** to the latest versions
* Refactor environment settings and retry modes into smithy-kotlin
* Sync AWS models and bump smithy-kotlin version

## [0.22.1-beta] - 04/21/2023

### Features
* BREAKING: Add support for retrying certain transient HTTP errors. `RetryErrorType.Timeout` was renamed to `RetryErrorType.Transient`.

### Miscellaneous
* Refactor internal endpoint resolver execution to track upstream changes.
* Sync AWS models and bump smithy-kotlin version

## [0.22.0-beta] - 04/14/2023

### Miscellaneous
* Refactor identity and authentication APIs
* Upgrade smithy-kotlin and sync AWS service models.

## [0.21.5-beta] - 04/06/2023

### Fixes
* [#492](https://github.com/awslabs/aws-sdk-kotlin/issues/492) Don't use potentially stale profile when retrieving credentials via IMDS.

### Miscellaneous
* Upgrade **smithy-kotlin** to 0.16.6
* Sync AWS models to latest versions
* Upgrade smithy to pull in protocol tests for intEnum support.

## [0.21.4-beta] - 03/30/2023

### Features
* Add support for awsQuery-compatible error responses.

### Miscellaneous
* add clarifying docs for endpointUrl
* Sync latest AWS models

## [0.21.3-beta] - 03/16/2023

### Features
* [#206](https://github.com/awslabs/aws-sdk-kotlin/issues/206) Add support for loading FIPS and dual-stack endpoint settings from env, system properties, and shared config.
* [#206](https://github.com/awslabs/aws-sdk-kotlin/issues/206) Add support for loading S3 accelerate and addressing settings from shared config.

### Fixes
* [#874](https://github.com/awslabs/aws-sdk-kotlin/issues/874) Ensure all unsigned operations are accessible without credentials in CognitoIdentityProvider.

### Miscellaneous
* Update smithy-kotlin version and sync service models.

## [0.21.2-beta] - 03/09/2023

### Features
* Add sub-property support for AWS config.

### Miscellaneous
* Sync AWS models and bump smithy-kotlin version

## [0.21.1-beta] - 03/02/2023

### Miscellaneous
* Sync AWS service models and **smithy-kotlin** version

## [0.21.0-beta] - 02/24/2023

### Features
* Sync AWS service models and **smithy-kotlin** version

### Miscellaneous
* Refactor: move CachedCredentialsProvider and CredentialsProviderChain to smithy-kotlin under aws.smithy.kotlin.runtime.auth.awscredentials package

## [0.20.3-beta] - 02/16/2023

### Features
* [#839](https://github.com/awslabs/aws-sdk-kotlin/issues/839) Add `Expect: 100-continue` header to S3 PUT requests over 2MB

### Miscellaneous
* Upgrade smithy-kotlin and sync latest service models.

## [0.20.2-beta] - 02/09/2023

### Features
* Add configuration for retry policy on clients

### Fixes
* [#836](https://github.com/awslabs/aws-sdk-kotlin/issues/836) Fix bug caused by reading too few bytes when parsing header values in event streams

### Miscellaneous
* Sync AWS service models
* Refactor: track upstream module changes
* Refactor: track upstream HTTP module changes

## [0.20.1-beta] - 02/06/2023

### Features
* Sync to latest AWS service models
* [#446](https://github.com/smithy-lang/smithy-kotlin/issues/446) Implement flexible checksums customization

### Miscellaneous
* Update to latest **smithy-kotlin** version
* Upgrade to Kotlin 1.8.10
* Refactor: track upstream module refactoring

## [0.20.0-beta] - 01/31/2023

### Features
* add ProcessCredentialsProvider which invokes a user-specified command to retrieve credentials
* Allow config override for one or more operations with an existing service client.

### Miscellaneous
* **Breaking** Remove `Closeable` supertype from `HttpClientEngine` interface. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/818) for more information.
* Refactor the way service client configuration is generated
* Upgrade Kotlin version to 1.8.0
* Update to latest AWS service models.
* Upgrade dependencies

## [0.19.5-beta] - 01/19/2023

### Miscellaneous
* Sync AWS service models.

## [0.19.4-beta] - 01/13/2023

### Miscellaneous
* Sync AWS models

## [0.19.3-beta] - 01/12/2023
**NOTE**: Do not use. Prefer 0.19.4-beta or later.

### Features
* [#122](https://github.com/smithy-lang/smithy-kotlin/issues/122) Add capability to intercept SDK operations

### Miscellaneous
* Sync AWS service models

## [0.19.2-beta] - 12/22/2022

### Fixes
* Correct validation of empty segments in ARN parser

### Miscellaneous
* Upgrade smithy-kotlin and sync AWS models.

## [0.19.1-beta] - 12/15/2022

### Miscellaneous
* Sync AWS service models

## [0.19.0-beta] - 12/01/2022

### Miscellaneous
* Upgrade smithy-kotlin and sync service models and partitions.
* **BREAKING** Refactor SDK I/O types. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/768) for more information

## [0.18.0-beta] - 11/23/2022

### Features
* Add support for dual-stack endpoints in client config.
* [#399](https://github.com/awslabs/aws-sdk-kotlin/issues/399) Add support for [S3 Virtual Host Addressing](https://docs.aws.amazon.com/AmazonS3/latest/userguide/VirtualHosting.html) (enabled by default).
* [#231](https://github.com/awslabs/aws-sdk-kotlin/issues/231) Add support for [S3 Access Points](https://aws.amazon.com/s3/features/access-points/).
* Add support for [S3 Object Lambda](https://aws.amazon.com/s3/features/object-lambda/).
* [#677](https://github.com/smithy-lang/smithy-kotlin/issues/677) Add a new tracing framework for centralized handling of log messages and metric events and providing easy integration points for connecting to downstream tracing systems (e.g., kotlin-logging)
* **BREAKING** Add smithy-modeled endpoint resolvers for AWS services. See [this discussion post](https://github.com/awslabs/aws-sdk-kotlin/discussions/761) for more information.
* Add support for [S3 PrivateLink](https://docs.aws.amazon.com/AmazonS3/latest/userguide/privatelink-interface-endpoints.html).
* Add support for [FIPS](https://aws.amazon.com/compliance/fips/) endpoints in client config.
* Add support for [S3 Transfer Acceleration](https://docs.aws.amazon.com/AmazonS3/latest/userguide/transfer-acceleration.html).
* Add support for [S3 Outposts](https://aws.amazon.com/s3/outposts/).

### Miscellaneous
* Sync AWS service models

## [0.17.12-beta] - 11/15/2022

### Fixes
* [#753](https://github.com/awslabs/aws-sdk-kotlin/issues/753) Upgrade smithy-kotlin to fix Android crash when OkHttp response body coroutine throws an exception

### Miscellaneous
* Sync AWS models to latest

## [0.17.11-beta] - 11/10/2022

### Miscellaneous
* Sync AWS service models

## [0.17.10-beta] - 11/03/2022

### Miscellaneous
* Upgrade smithy to 1.26.1.
* Sync models and bump smithy-kotlin version for release.

## [0.17.9-beta] - 10/27/2022

### Fixes
* #711 Pass client configuration's httpClientEngine to the CredentialsProvider and region to ProfileCredentialsProvider
* [#733](https://github.com/awslabs/aws-sdk-kotlin/issues/733) Fix OkHttp engine crashing on Android when coroutine is cancelled while uploading request body

## [0.17.8-beta] - 10/14/2022

### Features
* #707 Support static stability for IMDS credentials

### Fixes
* [#715](https://github.com/awslabs/aws-sdk-kotlin/issues/715) Enable intra-repo links in API ref docs

### Miscellaneous
* Sync AWS service models

## [0.17.7-beta] - 10/03/2022

### Features
* #486 Enable configurability of the retry strategy through environment variables, system properties, and AWS profiles.

### Fixes
* [#697](https://github.com/awslabs/aws-sdk-kotlin/issues/697) Correct handling of non-success responses when retrieving credentials on ECS.

### Miscellaneous
* Upgrade smithy-kotlin version.
* Update/clarify changelog and add commit instructions in the Contributing Guidelines
* Upgrade Kotlin version and dependencies in ECS credentials integration test.
* Sync AWS models and upgrade smithy-kotlin.

## [0.17.6-beta] - 09/19/2022

### Features
* [#543](https://github.com/awslabs/aws-sdk-kotlin/issues/543) Add support for event streams
* Mark event stream HTTP body as duplex stream

### Fixes
* [#694](https://github.com/awslabs/aws-sdk-kotlin/issues/694) Merge per-op custom metadata to avoid clobbering per-client metadata

### Miscellaneous
* Sync AWS service models
* Update smithy-kotlin version
* Add unbound event stream payload deserialization path
* Use explict CoroutineScope for consuming event stream flow

## [0.17.5-beta] - 08/18/2022

### Fixes
* [#55](https://github.com/awslabs/aws-crt-kotlin/issues/55) Upgrade smithy-kotlin dependency to fix Mac dlopen issue
* [#601](https://github.com/awslabs/aws-sdk-kotlin/issues/601) Remove incorrect `.` at end of license identifier header in source files.

### Documentation
* [#683](https://github.com/awslabs/aws-sdk-kotlin/issues/683) Enhance **CONTRIBUTING.md** with additional details about required PR checks and how to run them locally

### Miscellaneous
* Upgrade smithy-kotlin to latest released version, 0.12.5
* Upgrade ktlint to 0.46.1.
* Sync AWS service models
* Upgrade Smithy to 1.23.0, upgrade Smithy Gradle to 0.6.0

## [0.17.4-beta] - 08/11/2022

### Fixes
* Update event stream model test template

### Miscellaneous
* Upgrade Kotlin version to 1.7.10
* Upgrade smithy-kotlin to 0.12.4.

## [0.17.3-beta] - 08/04/2022

### Miscellaneous
* Sync AWS service models

## [0.17.2-beta] - 07/28/2022

### Miscellaneous
* Sync AWS service models.
* [#216](https://github.com/smithy-lang/smithy-kotlin/issues/216) Enable [Explicit API mode](https://github.com/Kotlin/KEEP/blob/master/proposals/explicit-api-mode.md)

## [0.17.1-beta] - 07/21/2022

### Features
* [#509](https://github.com/awslabs/aws-sdk-kotlin/issues/509) Implement ID prefix trimming for route53 resources.

### Miscellaneous
* Sync AWS service models.

## [0.17.0-beta] - 07/14/2022

### Fixes
* **Breaking**: Move DSL overloads on generated clients to extension methods

### Miscellaneous
* Sync AWS service models.
* **Breaking**: Upgrade **smithy-kotlin** version which will replace all instances of `Set<T>` with `List<T>` in service shapes

## [0.16.6-beta] - 07/08/2022

### Features
* [#123](https://github.com/smithy-lang/smithy-kotlin/issues/123) Add support for smithy Document type.

### Miscellaneous
* Update AWS models to latest versions
* Upgrade smithy-kotlin version to 0.11.2
* [#599](https://github.com/smithy-lang/smithy-kotlin/issues/599) Upgrade Smithy version to 1.22

## [0.16.5-beta] - 07/01/2022

### Miscellaneous
* [#622](https://github.com/awslabs/aws-sdk-kotlin/issues/622) Upgrade Kotlin to 1.7

## [0.16.4-beta] - 06/23/2022

### Fixes
* [#139](https://github.com/smithy-lang/smithy-kotlin/issues/139) Validate that members bound to URI paths are non-null at object construction

### Miscellaneous
* Upgrade smithy kotlin to [0.11.0](https://github.com/smithy-lang/smithy-kotlin/releases/tag/v0.11.0)

## [0.16.3-beta] - 06/16/2022

### Features
* Support bootstrapping services by package name (in addition to by model filename)

### Documentation
* Update the debugging guide to demonstrate how to use Log4j2 for logging

### Miscellaneous
* Sync AWS models to latest

## [0.16.2-beta] - 06/10/2022

### Fixes
* [#619](https://github.com/awslabs/aws-sdk-kotlin/issues/619), [#657](https://github.com/smithy-lang/smithy-kotlin/issues/657) Upgrade smithy-kotlin to pull in fixes for signing bugs.

### Documentation
* [#620](https://github.com/awslabs/aws-sdk-kotlin/issues/620) Update outdated howto docs to correctly describe client instantiation and client engine configuration

### Miscellaneous
* Sync AWS models to latest

## [0.16.1-beta] - 06/02/2022

### Features
* [#617](https://github.com/smithy-lang/smithy-kotlin/issues/617) Add a new non-CRT SigV4 signer and use it as the default. This removes the CRT as a hard dependency for using the SDK (although the CRT signer can still be used via explicit configuration on client creation).

### Miscellaneous
* Sync AWS models to latest

## [0.16.0] - 05/26/2022

### Features
* [#460](https://github.com/awslabs/aws-sdk-kotlin/issues/460) Enhance generic codegen to be more KMP-friendly. This is a **breaking change** which means service client artifacts will now include their platform name (e.g., `s3-jvm-<version>.jar` vs `s3-<version>.jar`). Users consuming dependencies through the Gradle Kotlin plugin will have this handled automatically for them.

### Fixes
* [#480](https://github.com/awslabs/aws-sdk-kotlin/issues/480) Upgrade smithy-kotlin to 0.10.0 which upgrades to ktor-2.x. This is considered a **breaking change** as it may reverse the issue described in #480 and break ktor-1.x users.

### Miscellaneous
* Upgrade smithy-kotlin to 0.9.2 which includes codegen updates to generate operations with all optional inputs to include a default parameter. See [smithy-kotlin#129](https://github.com/smithy-lang/smithy-kotlin/issues/129)
* upgrade kotlin to 1.6.21 and other deps to latest

## [0.15.2-beta] - 05/13/2022

### Features
* Implement recursion detection middleware.
* [#575](https://github.com/awslabs/aws-sdk-kotlin/issues/575) Add support for detecting custom metadata in system properties (starting with `aws.customMetadata.`) and environment variables (starting with `AWS_CUSTOM_METADATA_`)

## [0.15.1-beta] - 04/29/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Documentation
* update API reference doc styling

### Miscellaneous
* Sync latest AWS service models
* Refactor hashing functions into new subproject

## [0.15.0] - 04/29/2022

**NOTE**: Do not use. Prefer 0.15.1-beta or later.

## [0.14.4-beta] - 04/21/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* CreateMultipartUpload doesn't get signed correctly [#588](https://github.com/awslabs/aws-sdk-kotlin/issues/588)
* Possible memory leak in new default HTTP engine [#587](https://github.com/awslabs/aws-sdk-kotlin/issues/587)

### Miscellaneous
* sync AWS models [#590](https://github.com/awslabs/aws-sdk-kotlin/pull/590)

## [0.14.3-beta] - 04/14/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* resolve region only when profile credentials require it [#582](https://github.com/awslabs/aws-sdk-kotlin/pull/582)
* only set Content-Type when appropriate [#570](https://github.com/awslabs/aws-sdk-kotlin/pull/570)

### Miscellaneous
* sync AWS models [#585](https://github.com/awslabs/aws-sdk-kotlin/pull/585)

## [0.14.2-beta] - 04/07/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes

* fix timeout on large requests [#572](https://github.com/awslabs/aws-sdk-kotlin/issues/572)

## [0.14.1-beta] - 03/31/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* implement KMP XML serde and remove XmlPull dependency [#563](https://github.com/awslabs/aws-sdk-kotlin/pull/563)

### Miscellaneous
* sync AWS service models [#564](https://github.com/awslabs/aws-sdk-kotlin/pull/564)

## [0.14.0-beta] - 03/24/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* replace default HTTP client engine [#554](https://github.com/awslabs/aws-sdk-kotlin/pull/554)

### New features
* bootstrap event streams [#545](https://github.com/awslabs/aws-sdk-kotlin/pull/545)

### Fixes
* temporarily bypass httpchecksum traits until full flexible checksum support is available [#558](https://github.com/awslabs/aws-sdk-kotlin/pull/558)
* include headers in presigning requests [#556](https://github.com/awslabs/aws-sdk-kotlin/pull/556)
* backfill optional auth trait for cognito and cognito-idp [#555](https://github.com/awslabs/aws-sdk-kotlin/pull/555)

### Miscellaneous
* update AWS models to latest versions [#559](https://github.com/awslabs/aws-sdk-kotlin/pull/559)
* cleanup presign tests [#546](https://github.com/awslabs/aws-sdk-kotlin/pull/546)

## [0.13.1-beta] - 02/25/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* improve detection of available read bytes to avoid hang [#535](https://github.com/awslabs/aws-sdk-kotlin/pull/535)

## [0.13.0-beta] - 02/17/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* add sso credential provider; make all providers kmp compatible [#469](https://github.com/awslabs/aws-sdk-kotlin/pull/469)

### New features
* update AWS models to latest versions [#532](https://github.com/awslabs/aws-sdk-kotlin/pull/532)

### Fixes
* bump crt-kotlin to latest to fix leaked connections [#529](https://github.com/awslabs/aws-sdk-kotlin/pull/529)
* isClosedForRead implies availableForRead is zero
* fix CRT read channel buffer management

### Miscellaneous
* coroutine version bump to 1.6.0 and Duration stabilization [#514](https://github.com/awslabs/aws-sdk-kotlin/pull/514)
* dokka upgrade [#523](https://github.com/awslabs/aws-sdk-kotlin/pull/523)
* upgrade smithy to 1.17.0 [#521](https://github.com/awslabs/aws-sdk-kotlin/pull/521)

## [0.12.0-beta] - 02/04/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* enable waiters

### Fixes
* propagate crt stream errors to response body consumer [#510](https://github.com/awslabs/aws-sdk-kotlin/pull/510)

## [0.11.0-beta] - 01/20/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* Generate presigner into nested package for consistency [#502](https://github.com/awslabs/aws-sdk-kotlin/pull/502)

### New features
* update AWS models to latest versions [#505](https://github.com/awslabs/aws-sdk-kotlin/pull/505)

## [0.10.1-beta] - 01/13/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* update AWS models to latest versions [#499](https://github.com/awslabs/aws-sdk-kotlin/pull/499)
* Paginators! [smithy-kotlin#557](https://github.com/smithy-lang/smithy-kotlin/pull/557)

### Fixes
* enforce only once shutdown logic for crt engine connections [#497](https://github.com/awslabs/aws-sdk-kotlin/pull/497)

## [0.10.0-beta] - 01/06/2022

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* Update codegen to generate base exception rather than UnknownServiceErrorException [#484](https://github.com/awslabs/aws-sdk-kotlin/pull/484)

### New features
* update AWS models to latest versions
* upgrade to Kotlin 1.6.10 [#474](https://github.com/awslabs/aws-sdk-kotlin/pull/474)

### Fixes
* Fix usage of unicode in bucket names of s3 presigner [#487](https://github.com/awslabs/aws-sdk-kotlin/pull/487)
* Add new services in published release [#468](https://github.com/awslabs/aws-sdk-kotlin/pull/468)

### Miscellaneous
* Add design tenets [#466](https://github.com/awslabs/aws-sdk-kotlin/pull/466)
* updated the Readme doc to include API reference guide [#471](https://github.com/awslabs/aws-sdk-kotlin/pull/471)

## [0.9.5-beta] - 12/09/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* amplifyuibuilder
* appconfigdata
* backupgateway
* chimesdkmeetings
* drs
* evidently
* inspector2
* iottwinmaker
* migrationhubrefactorspaces
* migrationhubstrategy
* rbin
* resiliencehub
* rum
* workspacesweb

### Fixes
* move endpoint resolution log messages from DEBUG to TRACE [#443](https://github.com/awslabs/aws-sdk-kotlin/pull/443)
* presigner cleanup [#452](https://github.com/awslabs/aws-sdk-kotlin/pull/452)
* import signing test suite [#451](https://github.com/awslabs/aws-sdk-kotlin/pull/451)

## [0.9.4-beta] - 12/01/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## [0.9.3-alpha] - 11/19/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Fixes
* render default endpoint resolver for machinelearning [#424](https://github.com/awslabs/aws-sdk-kotlin/pull/424)

## [0.9.2-alpha] - 11/11/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* SDK generation and build docs [#377](https://github.com/awslabs/aws-sdk-kotlin/pull/377)

### Fixes
* disable signing for sts operations AssumeRoleWithSaml and AssumeRoleWithWebIdentity [#407](https://github.com/awslabs/aws-sdk-kotlin/pull/407)

### Miscellaneous
* Add howto to override default http client. [#412](https://github.com/awslabs/aws-sdk-kotlin/pull/412)

## [0.9.1-alpha] - 11/04/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### New features
* implement retries for imds [#404](https://github.com/awslabs/aws-sdk-kotlin/pull/404)
* enable machinelearning endpoint customization [#378](https://github.com/awslabs/aws-sdk-kotlin/pull/378)
* add glacier request body checksum [#379](https://github.com/awslabs/aws-sdk-kotlin/pull/379)

### Fixes
* restJson1 empty httpPayload body serialization [#402](https://github.com/awslabs/aws-sdk-kotlin/pull/402)

## [0.9.0-alpha] - 10/28/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes

* overhaul endpoint resolver types [#361](https://github.com/awslabs/aws-sdk-kotlin/pull/361)

### New features

* extend user agent metadata with framework, feature, and config [#372](https://github.com/awslabs/aws-sdk-kotlin/pull/372)

### Misc

* add sync models task and sync latest models [#374](https://github.com/awslabs/aws-sdk-kotlin/pull/374)

## [0.8.0-alpha] - 10/21/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes
* require a resolved configuration [#351](https://github.com/awslabs/aws-sdk-kotlin/pull/351)

### New features
* detect region from active AWS profile [#344](https://github.com/awslabs/aws-sdk-kotlin/pull/344)
* ec2 imds region provider [#341](https://github.com/awslabs/aws-sdk-kotlin/pull/341)
* Add STS assume role and web identity credential providers [#352](https://github.com/awslabs/aws-sdk-kotlin/pull/352)
* ECS credential provider [#353](https://github.com/awslabs/aws-sdk-kotlin/pull/353)
* ec2 credentials provider [#348](https://github.com/awslabs/aws-sdk-kotlin/pull/348)

### Fixes
* use wrapped response when deserializing modeled exceptions [#358](https://github.com/awslabs/aws-sdk-kotlin/pull/358)
* switch from ULong to Long in public presigner API for better java interop [#359](https://github.com/awslabs/aws-sdk-kotlin/pull/359)

### Misc
* Bump Kotlin and Dokka versions to latest release [#360](https://github.com/awslabs/aws-sdk-kotlin/pull/360)
* update aws models [#347](https://github.com/awslabs/aws-sdk-kotlin/pull/347)
* add docs for enabling logging in unit tests [#339](https://github.com/awslabs/aws-sdk-kotlin/pull/339)


## [0.7.0-alpha] - 10/14/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## New features

* http engine config [#336](https://github.com/awslabs/aws-sdk-kotlin/pull/336)
* add codegen wrappers for retries [#331](https://github.com/awslabs/aws-sdk-kotlin/pull/331)

## [0.6.0-alpha] - 10/07/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

## New features

* implement basic retry support in runtime [#328](https://github.com/awslabs/aws-sdk-kotlin/pull/328)
* event stream framing support [#320](https://github.com/awslabs/aws-sdk-kotlin/pull/320)
* replace GSON based JSON serde with KMP compatible impl [#477](https://github.com/smithy-lang/smithy-kotlin/pull/477)
* Add IMDSv2 client in runtime [#330](https://github.com/awslabs/aws-sdk-kotlin/pull/330)


## [0.5.0-alpha] - 09/30/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Breaking changes

* split auth and signing packages [#318](https://github.com/awslabs/aws-sdk-kotlin/issues/318)
    * Import paths changed to `aws.sdk.kotlin.runtime.auth.credentials` and `aws.sdk.kotlin.runtime.auth.signing`

### New features

* autofill Glacier accountId [#246](https://github.com/awslabs/aws-sdk-kotlin/issues/246)
* support JVM system property and environment variables for profiles [#297](https://github.com/awslabs/aws-sdk-kotlin/issues/297)
* expose method to sign standalone requests [#318](https://github.com/awslabs/aws-sdk-kotlin/issues/318)
* AWS configuration loader and parser [#216](https://github.com/awslabs/aws-sdk-kotlin/issues/216)

### Fixes

* utilize custom endpoint ports [#310](https://github.com/awslabs/aws-sdk-kotlin/issues/310)
* Replace junit imports with kotlin.test imports where possible [#321](https://github.com/awslabs/aws-sdk-kotlin/issues/321)
* update readme to include latest version [#319](https://github.com/awslabs/aws-sdk-kotlin/issues/319)
* sync models and endpoints [#317](https://github.com/awslabs/aws-sdk-kotlin/issues/317)
* Favor kotlin-test-juint5 over kotlin-test to resolve intermittent build failures [#316](https://github.com/awslabs/aws-sdk-kotlin/issues/316)
* kotlin 1.5.30, coroutine, kotest version bumps [#307](https://github.com/awslabs/aws-sdk-kotlin/issues/307)


## [0.4.0-alpha] - 08/26/2021

**WARNING: Alpha releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* ec2
* location
* marketplacecommerceanalytics

### New features

* Support for presigning requests [#435](https://github.com/smithy-lang/smithy-kotlin/issues/435)
* Detect aws region from system properties [#202](https://github.com/awslabs/aws-sdk-kotlin/issues/202)
* EC2 Query Protocol [#230](https://github.com/awslabs/aws-sdk-kotlin/issues/230)
* Provide opt-in wire logging [#425](https://github.com/smithy-lang/smithy-kotlin/issues/425)
* Support profile credentials provider [#302](https://github.com/smithy-lang/smithy-kotlin/issues/302)

### Fixes

* s3.deleteObjects causes an exception [#125](https://github.com/awslabs/aws-sdk-kotlin/issues/125)
* Streaming request BodyStream never read [#282](https://github.com/awslabs/aws-sdk-kotlin/issues/282)
* location service references traits not in sdk classpath [#286](https://github.com/awslabs/aws-sdk-kotlin/issues/286)
* Ignore unboxed types for subset of services [#261](https://github.com/awslabs/aws-sdk-kotlin/issues/261)
* Service operations specifying no auth should not sign requests with sigv4 [#263](https://github.com/awslabs/aws-sdk-kotlin/issues/263)
* Create S3 object with Unicode name fails with signature mismatch [#200](https://github.com/awslabs/aws-sdk-kotlin/issues/200)
* Codegen errors in marketplacecommerceanalytics [#214](https://github.com/awslabs/aws-sdk-kotlin/issues/214)
* Escape model-extra files for Windows [#191](https://github.com/awslabs/aws-sdk-kotlin/issues/191)
* Support Glacier APIVersion Header [#165](https://github.com/smithy-lang/smithy-kotlin/issues/165)
* Support APIGateway Accept Header [#157](https://github.com/smithy-lang/smithy-kotlin/issues/157)
* Add support for awsQueryError trait [#375](https://github.com/smithy-lang/smithy-kotlin/issues/375)
* S3 HeadObject errors require customization [#152](https://github.com/awslabs/aws-sdk-kotlin/issues/152)
* S3 custom treatment of GetBucketLocation response [#194](https://github.com/awslabs/aws-sdk-kotlin/issues/194)

## [0.3.0-M2] - 06/18/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Services new in this release

* applicationcostprofiler
* apprunner
* autoscaling
* cloudformation
* cloudsearch
* cloudwatch
* docdb
* elasticache
* elasticbeanstalk
* elasticloadbalancing
* elasticloadbalancingv2
* finspace
* finspacedata
* iam
* neptune
* nimble
* rds
* redshift
* ses
* sns
* sqs
* ssmcontacts
* ssmincidents
* sts

## Changes

### New Features

* `awsQuery` protocol support (https://github.com/smithy-lang/smithy-kotlin/issues/127)
* detect region from environment variables (https://github.com/smithy-lang/smithy-kotlin/issues/356)
* custom S3 error metadata support (https://github.com/smithy-lang/smithy-kotlin/issues/323)
* environment credentials provider (https://github.com/smithy-lang/smithy-kotlin/issues/301)
* bind default HTTP client engine to CRT (https://github.com/smithy-lang/smithy-kotlin/issues/199)
* operation DSL overloads (https://github.com/smithy-lang/smithy-kotlin/issues/184)
* Kinesis read timeouts (https://github.com/smithy-lang/smithy-kotlin/issues/162)
* XML EOL encoding support (https://github.com/smithy-lang/smithy-kotlin/issues/142)

### Fixes

* path literal not escaped correctly (https://github.com/smithy-lang/smithy-kotlin/issues/395)
* idempotency tokens are not detected on resources (https://github.com/smithy-lang/smithy-kotlin/issues/376)
* httpPayload bound members need dedicated serde (https://github.com/smithy-lang/smithy-kotlin/issues/353)
* Route53 error messages (and maybe other restXml messages) are not deserialized and present in stacktrace
  (https://github.com/smithy-lang/smithy-kotlin/issues/352)
* fix url-encoding behavior of httpLabel and httpQuery members (https://github.com/smithy-lang/smithy-kotlin/issues/328)
* runtime error when using Kotlin 1.5.0 (https://github.com/smithy-lang/smithy-kotlin/issues/319)
* SES fails to build due to invalid docs (https://github.com/awslabs/aws-sdk-kotlin/issues/153)
* exception is thrown for SQS delete message (https://github.com/awslabs/aws-sdk-kotlin/issues/147)
* SNS getTopicAttributes throws an exception (https://github.com/awslabs/aws-sdk-kotlin/issues/142)
* elasticbeanstalk model generates invalid enum (https://github.com/smithy-lang/smithy-kotlin/issues/403)

### Other

* Kotlin 1.5.0 support
* design docs added to [docs/design](docs/design) directory

## [0.2.0-M1] - 05/10/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

### Known Issues

* The Kotlin SDK is not compatible with Kotlin 1.5.0.  Please use Kotlin 1.4.x.

### Services new in this release

* accessanalyzer
* acm
* acmpca
* alexaforbusiness
* amp
* amplify
* amplifybackend
* appconfig
* appflow
* appintegrations
* applicationautoscaling
* applicationdiscoveryservice
* applicationinsights
* appmesh
* appstream
* appsync
* athena
* auditmanager
* autoscalingplans
* backup
* batch
* braket
* budgets
* build
* chime
* cloud9
* clouddirectory
* cloudfront
* cloudhsm
* cloudhsmv2
* cloudsearchdomain
* cloudtrail
* cloudwatchevents
* cloudwatchlogs
* codeartifact
* codebuild
* codecommit
* codedeploy
* codeguruprofiler
* codegurureviewer
* codepipeline
* codestar
* codestarconnections
* codestarnotifications
* cognitoidentity
* cognitosync
* comprehend
* comprehendmedical
* computeoptimizer
* configservice
* connect
* connectcontactlens
* connectparticipant
* costandusagereportservice
* costexplorer
* customerprofiles
* databasemigrationservice
* databrew
* dataexchange
* datapipeline
* datasync
* dax
* detective
* devicefarm
* devopsguru
* directconnect
* directoryservice
* dlm
* dynamodbstreams
* ebs
* ec2instanceconnect
* ecr
* ecrpublic
* ecs
* efs
* eks
* elasticinference
* elasticsearchservice
* elastictranscoder
* emr
* emrcontainers
* eventbridge
* fis
* fms
* forecast
* forecastquery
* frauddetector
* fsx
* glacier
* globalaccelerator
* greengrass
* greengrassv2
* groundstation
* guardduty
* health
* healthlake
* honeycode
* identitystore
* imagebuilder
* inspector
* ivs
* kafka
* kendra
* kinesis
* kinesisanalytics
* kinesisanalyticsv2
* kinesisvideo
* kinesisvideoarchivedmedia
* kinesisvideomedia
* kinesisvideosignaling
* lakeformation
* lexmodelbuildingservice
* lexmodelsv2
* lexruntimeservice
* lexruntimev2
* licensemanager
* lookoutequipment
* lookoutmetrics
* lookoutvision
* machinelearning
* macie
* macie2
* managedblockchain
* marketplacecatalog
* marketplaceentitlementservice
* marketplacemetering
* mediaconnect
* mediapackage
* mediapackagevod
* mediastore
* mediastoredata
* mediatailor
* mgn
* migrationhub
* migrationhubconfig
* mobile
* mq
* mturk
* mwaa
* networkfirewall
* networkmanager
* opsworks
* opsworkscm
* organizations
* outposts
* personalize
* personalizeevents
* personalizeruntime
* pi
* pinpointemail
* pinpointsmsvoice
* pricing
* qldb
* qldbsession
* quicksight
* ram
* rdsdata
* redshiftdata
* rekognition
* repocache
* resourcegroups
* resourcegroupstaggingapi
* robomaker
* route53
* route53domains
* route53resolver
* s3
  * NOTE: S3 is a complicated service, this initial release **DOES NOT** have complete support for all S3 features.
* s3control
* s3outposts
* sagemaker
* sagemakera2iruntime
* sagemakeredge
* sagemakerfeaturestoreruntime
* sagemakerruntime
* savingsplans
* schemas
* serverlessapplicationrepository
* servicecatalog
* servicecatalogappregistry
* servicediscovery
* servicequotas
* sesv2
* sfn
* shield
* signer
* sms
* snowball
* sso
* ssoadmin
* ssooidc
* storagegateway
* support
* swf
* synthetics
* textract
* timestreamquery
* timestreamwrite
* transcribe
* transfer
* waf
* wafregional
* wafv2
* wellarchitected
* workdocs
* worklink
* workmail
* workmailmessageflow
* workspaces
* xray

## Changes

### New Features

* `restXml` protocol support
* add conversions to/from `java.time.Instant` and SDK `Instant` (https://github.com/smithy-lang/smithy-kotlin/issues/271)
* implement missing IO runtime primitives (https://github.com/smithy-lang/smithy-kotlin/issues/264)
* API documentation (https://github.com/awslabs/aws-sdk-kotlin/issues/119)

### Fixes

* generate per/service base exception types (https://github.com/smithy-lang/smithy-kotlin/issues/233)
* use sdkId if available for service client generation (https://github.com/smithy-lang/smithy-kotlin/issues/276)
* explicitly set jvm target compatibility (https://github.com/awslabs/aws-sdk-kotlin/issues/103)
* http error registration (https://github.com/awslabs/aws-sdk-kotlin/issues/118)

### Other

* generate per/service base exception types (https://github.com/smithy-lang/smithy-kotlin/issues/270)

## [0.1.0-M0] - 03/19/2021

**WARNING: Beta releases may contain bugs and no guarantee is made about API stability. They are not recommended for production use!**

This is the initial beta release of AWS SDK Kotlin. It represents an early look at the overall API surface.


See the [Getting Started Guide](docs/GettingStarted.md) for how to work with beta releases and examples.


### Services in this release

* DynamoDB
* Polly
* Translate
* Cognito Identity Provider
* Secrets Manager
  * NOTE: Default idempotency token provider will not currently work, you'll need to override the config to create or update secrets until [#180](https://github.com/smithy-lang/smithy-kotlin/issues/180) is implemented
* KMS
* Lambda

NOTES:
* We currently can (theoretically) support any JSON based AWS protocol. If there is a service you would like to see added in a future release (before developer preview) please reach out and let us know.
* No customizations are currently implemented, some SDK's may not behave 100% correctly without such support.
* Retries, waiters, paginators, and other features are not yet implemented

### Features
* Coroutine API
* DSL Builders
* Default (environment or config) or static credential providers only. Additional providers will be added in later releases.
* JVM only support (multiplatform support is on the roadmap)
