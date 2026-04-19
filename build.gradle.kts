import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    java
}

val paperApiVersion = providers.gradleProperty("paperApiVersion").orElse("1.21.11-R0.1-SNAPSHOT")
val javaRelease = providers.gradleProperty("javaRelease").map(String::toInt).orElse(21)
val versionPropertiesFile = layout.projectDirectory.file("version.properties").asFile
val versionProperties = Properties().apply {
    versionPropertiesFile.inputStream().use(::load)
}
val majorVersion = versionProperties.getProperty("majorVersion", "1")
val minorVersion = versionProperties.getProperty("minorVersion", "0")
val patchVersion = versionProperties.getProperty("patchVersion", "0")
val releaseStage = versionProperties.getProperty("releaseStage", "beta").trim()
val releaseStageNumber = versionProperties.getProperty("releaseStageNumber", "1").trim()
val lastBuildNumber = versionProperties.getProperty("lastBuildNumber", "0").toIntOrNull() ?: 0
val currentBuildNumber = lastBuildNumber + 1
val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z")
    .format(ZonedDateTime.now(ZoneId.of("Europe/Moscow")))

fun composePluginVersion(): String {
    val baseVersion = "$majorVersion.$minorVersion.$patchVersion"
    val stageSuffix = when {
        releaseStage.isBlank() -> ""
        releaseStageNumber.isBlank() -> "-$releaseStage"
        else -> "-$releaseStage.$releaseStageNumber"
    }
    return "$baseVersion$stageSuffix+$currentBuildNumber"
}

group = "dev.macuser"
version = composePluginVersion()

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Compile against the oldest supported Paper API so one jar can run on both
    // Paper 1.21.11 and newer compatible server versions such as 26.1.1 and 26.1.2.
    compileOnly("io.papermc.paper:paper-api:${paperApiVersion.get()}")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaRelease.get()))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaRelease.get())
}

tasks.jar {
    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }
}

val persistBuildNumber = tasks.register("persistBuildNumber") {
    doLast {
        versionProperties.setProperty("lastBuildNumber", currentBuildNumber.toString())
        versionPropertiesFile.writer().use { writer ->
            versionProperties.store(writer, "SkyRandom build version")
        }
    }
}

tasks.jar {
    finalizedBy(persistBuildNumber)
}

tasks.processResources {
    inputs.file(versionPropertiesFile)
    inputs.property("pluginVersion", project.version.toString())
    inputs.property("buildNumber", currentBuildNumber)
    inputs.property("buildDate", buildDate)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(
            "version" to project.version,
            "buildNumber" to currentBuildNumber,
            "buildDate" to buildDate
        )
    }
    filesMatching("build-info.properties") {
        expand(
            "version" to project.version,
            "buildNumber" to currentBuildNumber,
            "buildDate" to buildDate
        )
    }
}
