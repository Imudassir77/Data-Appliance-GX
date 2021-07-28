/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
    id("application")
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("com.bmuschko.docker-remote-api") version "6.7.0"
}


dependencies {
    implementation(project(":implementations:minimal:runtime"))
    implementation(project(":core:protocol:web"))
    implementation(project(":implementations:minimal:control-http"))

    implementation(project(":core:transfer"))
    implementation(project(":extensions:azure:transfer-process-store-cosmos"))
    implementation(project(":extensions:aws:s3:provision"))
    implementation(project(":extensions:azure:blob:provision"))
    implementation(project(":samples:copy-with-nifi"))

    implementation(project(":extensions:azure:events"))

    implementation(project(":implementations:minimal:ids"))

    implementation(project(":extensions:catalog-atlas"))
    implementation(project(":samples:dataseed"))

    implementation(project(":extensions:azure:vault"))
    implementation(project(":implementations:minimal:policy:policy-registry-memory"))
    implementation(project(":core:iam:iam-mock"))
    implementation(project(":implementations:minimal:ids:ids-policy-mock"))
    implementation(project(":implementations:minimal:configuration:configuration-fs"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.5.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.5.2")

}

application {
    @Suppress("DEPRECATION")
    mainClassName = "com.microsoft.dagx.runtime.DagxRuntime"
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    exclude("**/pom.properties", "**/pom.xm")
    mergeServiceFiles()
    archiveFileName.set("dagx-demo-e2e.jar")
}
