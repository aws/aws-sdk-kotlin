<#-- Copied from Dokka, customized for SEO: https://github.com/Kotlin/dokka/blob/3cc0b61498877f8e9025c1260f967f7e4236b301/dokka-subprojects/plugin-base/src/main/resources/dokka/templates/includes/page_metadata.ftl -->
<#macro display>
    <title>${pageName} - AWS SDK for Kotlin</title>
    <meta name="description" content="Learn how to use ${pageName} in the AWS SDK for Kotlin">
    <@template_cmd name="pathToRoot">
        <link href="${pathToRoot}images/logo-icon.svg" rel="icon" type="image/svg">
    </@template_cmd>
</#macro>