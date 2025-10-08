import java.nio.file.Paths
import java.util.Properties

plugins {
    id("com.gradleup.shadow")
}

val MOD_ID: String = rootProject.property("mod_id").toString()
val otelVersion: String = rootProject.property("otel_version") as String
val relocatePrefix = (rootProject.property("relocate_prefix") as String)

architectury {
    platformSetupLoomIde()
    neoForge()
}

loom {
    runs {
        named("server") {
            vmArg(
                "-javaagent:${
                    rootProject.layout.buildDirectory.file("downloadOTelAgent/opentelemetry-javaagent.jar")
                        .get().asFile.absolutePath
                }"
            )
            environmentVariable(
                "OTEL_JAVAAGENT_CONFIGURATION_FILE",
                rootProject.layout.projectDirectory.file("dev.otel.properties")
            )
            environmentVariable(
                "OTEL_JAVAAGENT_EXTENSIONS",
                rootProject.project("common").tasks.named("jar").get()
                    .outputs.files.singleFile.absolutePath
            )
        }
        named("client") {
            vmArg(
                "-javaagent:${
                    rootProject.layout.buildDirectory.file("downloadOTelAgent/opentelemetry-javaagent.jar")
                        .get().asFile.absolutePath
                }"
            )
            environmentVariable(
                "OTEL_JAVAAGENT_CONFIGURATION_FILE",
                rootProject.layout.projectDirectory.file("dev.otel.properties")
            )
            environmentVariable(
                "OTEL_JAVAAGENT_EXTENSIONS",
                rootProject.project("common").tasks.named("jar").get()
                    .outputs.files.singleFile.absolutePath
            )
        }
        create("gameTestServer") {
            server()
            runDir = "gameTestRun"

            vmArg(
                "-javaagent:${
                    rootProject.layout.buildDirectory.file("downloadOTelAgent/opentelemetry-javaagent.jar")
                        .get().asFile.absolutePath
                }"
            )
            environmentVariable(
                "OTEL_JAVAAGENT_CONFIGURATION_FILE",
                rootProject.layout.projectDirectory.file("gameTest.otel.properties")
            )
            environmentVariable(
                "OTEL_JAVAAGENT_EXTENSIONS",
                rootProject.project("common").tasks.named("jar").get()
                    .outputs.files.singleFile.absolutePath
            )
            property("neoforge.logging.console.level", "debug")
            property("neoforge.logging.markers", "REGISTRIES")
            property("neoforge.enabledGameTestNamespaces", MOD_ID)
            property("neoforge.enableGameTest", "true")
            property("neoforge.gameTestServer", "true")
        }
    }
}

tasks["check"].dependsOn("runGameTestServer")

val common: Configuration by configurations.creating {
    this.isCanBeResolved = true
    this.isCanBeConsumed = false
}
val shadowBundle: Configuration by configurations.creating {
    this.isCanBeResolved = true
    this.isCanBeConsumed = false
}
val developmentNeoForge: Configuration by configurations.getting

configurations {
    compileClasspath.configure { extendsFrom(common) }
    runtimeClasspath.configure { extendsFrom(common) }
    developmentNeoForge.extendsFrom(common)
}

repositories {
    // KFF
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
    maven {
        name = "NeoForged"
        setUrl("https://maven.neoforged.net/releases")
    }
}

dependencies {
    neoForge("net.neoforged:neoforge:${rootProject.property("neoforge_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-neoforge:${rootProject.property("architectury_version")}")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowBundle(project(":common", "transformProductionNeoForge")) { isTransitive = false }

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge-neoforge:${rootProject.property("kotlin_for_forge_version")}")

    api("io.opentelemetry:opentelemetry-api:$otelVersion")
    common("io.opentelemetry:opentelemetry-sdk-metrics:$otelVersion")
    shadowBundle("io.opentelemetry:opentelemetry-sdk-metrics:$otelVersion")
}

tasks.named("configureLaunch") {
    dependsOn(rootProject.tasks.named("verifyOTelAgent"))
}

tasks.processResources {
    val expansionMap = mapOf(
        "group" to rootProject.property("maven_group"),
        "version" to project.version,

        "mod_id" to rootProject.property("mod_id"),
        "minecraft_version" to rootProject.property("minecraft_version"),
        "neoforge_version" to rootProject.property("neoforge_version"),
        "architectury_version" to rootProject.property("architectury_version"),
        "kotlin_for_forge_version" to rootProject.property("kotlin_for_forge_version")
    )
    inputs.properties(expansionMap)

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expansionMap)
    }
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")
    configurations = listOf(shadowBundle)
    dependencies {
        this.exclude {
            it.moduleGroup == "org.jetbrains.kotlin"
                    && it.moduleName.startsWith("kotlin-stdlib")
        }
    }
    for (pattern in listOf("com", "de", "io", "okhttp3", "okio", "org", "zipkin2")) {
        relocate(pattern, "$relocatePrefix.$pattern") {
            this.exclude("kotlin.**")
            this.exclude("de.mctelemetry.core.**")
            this.exclude("io.opentelemetry.**")
            this.exclude("io.prometheus.**")
            this.exclude("org.apache.**")
        }
    }
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set(null as String?)
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.sourcesJar {
    val commonSources = project(":common").tasks.getByName<Jar>("sourcesJar")
    dependsOn(commonSources)
    from(commonSources.archiveFile.map { zipTree(it) })
}

components.getByName("java") {
    this as AdhocComponentWithVariants
    this.withVariantsFromConfiguration(project.configurations["shadowRuntimeElements"]) {
        skip()
    }
}

tasks.register("configureGameTestServer") {
    val serverProperties = mapOf(
        "enable-command-block" to "true",
        "level-type" to "minecraft:flat",
    )
    inputs.properties(serverProperties)
    val gameTestServerRun = loom.runs["gameTestServer"]
    val serverPropertiesFile =
        file(Paths.get(gameTestServerRun.runDir, "server.properties"), validation = PathValidation.NONE)
    onlyIf { !serverPropertiesFile.exists() }
    doLast {
        gameTestServerRun.makeRunDir()
    }
    doLast {
        val properties = Properties(serverProperties.size).apply {
            serverProperties.forEach(::setProperty)
        }
        serverPropertiesFile.writer(Charsets.UTF_8).use {
            properties.store(it, null)
        }
    }
    tasks["runGameTestServer"].dependsOn(this)
}
