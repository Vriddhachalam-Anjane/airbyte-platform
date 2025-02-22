plugins {
    id("io.airbyte.gradle.jvm.lib")
    id("io.airbyte.gradle.publish")
}

dependencies {
    annotationProcessor(libs.bundles.micronaut.annotation.processor)
    api(libs.bundles.micronaut.annotation)

    implementation(libs.bundles.apache)
    implementation(libs.bundles.jackson)
    implementation(libs.guava)
    implementation(project(":airbyte-commons"))
    implementation(project(":airbyte-commons-protocol"))
    implementation(project(":airbyte-config:config-models"))
    implementation(project(":airbyte-config:config-secrets"))
    implementation(project(":airbyte-db:db-lib"))
    implementation(project(":airbyte-db:jooq"))
    implementation(project(":airbyte-json-validation"))
    implementation(project(":airbyte-featureflag"))
    implementation(libs.airbyte.protocol)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}
