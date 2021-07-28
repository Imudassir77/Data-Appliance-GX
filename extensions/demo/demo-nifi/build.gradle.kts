/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

plugins {
    `java-library`
}


dependencies {
    api(project(":core:spi"))
    implementation(project(":extensions:aws:s3:s3-schema"))
    implementation(project(":extensions:azure:blob:blob-schema"))
    implementation(project(":implementations:minimal:metadata:metadata-memory"))
    implementation(project(":implementations:minimal:policy:policy-registry-memory"))
}


