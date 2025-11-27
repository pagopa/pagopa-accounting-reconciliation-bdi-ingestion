import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm") version "2.1.0"
  kotlin("plugin.spring") version "2.1.0"
  id("org.springframework.boot") version "3.5.7"
  id("io.spring.dependency-management") version "1.1.7"
  id("com.diffplug.spotless") version "8.0.0"
  id("org.sonarqube") version "7.0.1.6134"
  id("com.dipien.semantic-version") version "2.0.0" apply false
  id("org.openapi.generator") version "7.17.0"
  jacoco
  application
}

group = "it.pagopa.accounting.reconciliation"

version = "0.4.0-SNAPSHOT"

description = "pagopa-accounting-reconciliation-bdi-ingestion"

sourceSets {
  main { java.srcDirs("src/main/java", layout.buildDirectory.dir("generated/src/main/java")) }
}

java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }

repositories { mavenCentral() }

object Dependencies {
  const val ecsLoggingVersion = "1.5.0"
  const val openTelemetryVersion = "1.37.0"
  const val mockitoVersion = "6.1.0"
  const val JsonNullableJacksonVersion = "0.2.8"
  const val bouncyCastleVersion = "1.82"
}

dependencyLocking { lockAllConfigurations() }

dependencyManagement {
  imports { mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.7") }
  // Kotlin BOM
  imports { mavenBom("org.jetbrains.kotlin:kotlin-bom:2.2.21") }
  imports { mavenBom("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.10.2") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
  implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
  implementation("co.elastic.logging:logback-ecs-encoder:${Dependencies.ecsLoggingVersion}")
  implementation("io.opentelemetry:opentelemetry-api:${Dependencies.openTelemetryVersion}")
  implementation(
    "org.openapitools:jackson-databind-nullable:${Dependencies.JsonNullableJacksonVersion}"
  )
  implementation("org.bouncycastle:bcpkix-jdk18on:${Dependencies.bouncyCastleVersion}")

  // tests
  testImplementation("org.mockito.kotlin:mockito-kotlin:${Dependencies.mockitoVersion}")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("io.projectreactor:reactor-test")
  testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations {
  implementation.configure {
    exclude(module = "spring-boot-starter-web")
    exclude("org.apache.tomcat")
    exclude(group = "org.slf4j", module = "slf4j-simple")
  }
}

kotlin { compilerOptions { freeCompilerArgs.addAll("-Xjsr305=strict") } }

springBoot {
  mainClass.set(
    "it.pagopa.accounting.reconciliation.bdi.ingestion.PagopaAccountingReconciliationBdiIngestionApplicationKt"
  )
  buildInfo {
    properties {
      additional.set(mapOf("description" to (project.description ?: "Default description")))
    }
  }
}

tasks.withType<Test> { useJUnitPlatform() }

tasks.named<Jar>("jar") { enabled = false }

tasks
  .register("applySemanticVersionPlugin") {
    group = "semantic-versioning"
    description = "Semantic versioning plugin"
    dependsOn("prepareKotlinBuildScriptModel")
  }
  .apply { apply(plugin = "com.dipien.semantic-version") }

configure<com.diffplug.gradle.spotless.SpotlessExtension> {
  kotlin {
    toggleOffOn()
    targetExclude("build/**/*")
    ktfmt().kotlinlangStyle()
  }
  kotlinGradle {
    toggleOffOn()
    targetExclude("build/**/*.kts")
    ktfmt().googleStyle()
  }
  java {
    target("**/*.java")
    targetExclude("build/**/*")
    eclipse().configFile("eclipse-style.xml")
    toggleOffOn()
    removeUnusedImports()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("bdi") {
  description = "Generates the BDI classes for this project."
  group = "openapi-generation"
  generatorName.set("java")
  inputSpec.set("$rootDir/api-spec/bdi/openapi.yaml")
  outputDir.set(layout.buildDirectory.get().dir("generated").asFile.toString())
  apiPackage.set("it.pagopa.generated.bdi.api")
  modelPackage.set("it.pagopa.generated.bdi.model")
  generateApiDocumentation.set(false)
  generateApiTests.set(false)
  generateModelTests.set(false)
  library.set("webclient")
  modelNameSuffix.set("Dto")
  configOptions.set(
    mapOf(
      "swaggerAnnotations" to "false",
      "openApiNullable" to "true",
      "interfaceOnly" to "false",
      "hideGenerationTimestamp" to "true",
      "skipDefaultInterface" to "true",
      "useSwaggerUI" to "false",
      "reactive" to "true",
      "useSpringBoot3" to "true",
      "oas3" to "true",
      "generateSupportingFiles" to "false",
      "useJakartaEe" to "true",
    )
  )
  typeMappings.set(mapOf("File" to "org.springframework.core.io.Resource"))
  importMappings.set(mapOf("Resource" to "org.springframework.core.io.Resource"))
}

tasks.withType<KotlinCompile> {
  dependsOn("bdi")
  compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.jacocoTestReport {
  dependsOn(tasks.test) // tests are required to run before generating the report

  classDirectories.setFrom(
    files(
      classDirectories.files.map {
        fileTree(it).matching {
          exclude(
            "it/pagopa/accounting/reconciliation/bdi/ingestion/PagopaAccountingReconciliationBdiIngestionApplicationKt.class"
          )
        }
      }
    )
  )

  reports { xml.required.set(true) }
}

/**
 * Task used to expand application properties with build specific properties such as artifact name
 * and version
 */
tasks.processResources { filesMatching("application.properties") { expand(project.properties) } }
