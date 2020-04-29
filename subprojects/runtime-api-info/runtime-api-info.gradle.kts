/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.gradle.api.internal.runtimeshaded.PackageListGenerator

plugins {
    id("gradlebuild.distribution.core-implementation-java")
}

val runtimeShadedPath = "$buildDir/runtime-api-info"

configurations {
    create("testKitPackages") {
        isVisible = false
        isCanBeResolved = true
        isCanBeConsumed = false
        attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
        }
    }
}

dependencies {
    "testKitPackages"(project(":testKit"))
}

val generateGradleApiPackageList by tasks.registering(PackageListGenerator::class) {
    classpath = configurations["gradleApi"]
    outputFile = file("$runtimeShadedPath/api-relocated.txt")
}

val generateTestKitPackageList by tasks.registering(PackageListGenerator::class) {
    classpath = configurations["testKitPackages"]
    outputFile = file("$runtimeShadedPath/test-kit-relocated.txt")
}

tasks.jar {
    into("org/gradle/api/internal/runtimeshaded") {
        from(generateGradleApiPackageList)
        from(generateTestKitPackageList)
    }
}
