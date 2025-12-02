import java.nio.file.Paths
import java.util.Properties

plugins {
    id("com.gradleup.shadow")
}

val MOD_ID: String = rootProject.property("mod_id").toString()
val otelVersion: String = rootProject.property("otel_version") as String
val relocatePrefix = rootProject.property("relocate_prefix") as String

repositories {
    maven {
        url = uri("https://maven.quiltmc.org/repository/release/")
    }
}

architectury {
    platformSetupLoomIde()
    fabric()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
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
            runDir = "serverRun"
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
        }
        create("clientWithDocker") {
            client()
            inherit(this@runs["client"])
            configName = "Minecraft Client + Docker"
            environmentVariable(
                "OTEL_JAVAAGENT_CONFIGURATION_FILE",
                rootProject.layout.projectDirectory.file("docker.otel.properties")
            )
        }
        create("gameTestServer") {
            server()

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

            this.property("fabric-api.gametest")
            runDir = "gameTestRun"
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
val developmentFabric: Configuration by configurations.getting

configurations {
    compileClasspath.configure { extendsFrom(common) }
    runtimeClasspath.configure { extendsFrom(common) }
    developmentFabric.extendsFrom(common)
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-fabric:${rootProject.property("architectury_version")}")

    common(project(":common", "namedElements")) {
        isTransitive = false
    }
    shadowBundle(project(":common", "transformProductionFabric")) {
        isTransitive = false
    }

    // Fabric Kotlin
    modImplementation("net.fabricmc:fabric-language-kotlin:${rootProject.property("fabric_kotlin_version")}")

    // owo-lib/oωo-lib
    modImplementation("io.wispforest:owo-lib:${rootProject.property("owo_fabric_version")}")
    include("io.wispforest:owo-sentinel:${rootProject.property("owo_fabric_version")}")

    // opentelemetry
    api("io.opentelemetry:opentelemetry-api:$otelVersion")
    common("io.opentelemetry:opentelemetry-api:$otelVersion")
    shadowBundle("io.opentelemetry:opentelemetry-api:$otelVersion")

    // KObserve
    api("io.github.pixix4:KObserve:1.0.0-beta")
    common("io.github.pixix4:KObserve:1.0.0-beta")
    shadowBundle("io.github.pixix4:KObserve:1.0.0-beta")
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
        "architectury_version" to rootProject.property("architectury_version"),
        "fabric_kotlin_version" to rootProject.property("fabric_kotlin_version"),
        "owo_fabric_version" to rootProject.property("owo_fabric_version"),
    )
    inputs.properties(expansionMap)

    filesMatching("fabric.mod.json") {
        expand(expansionMap)
    }
}

tasks.shadowJar {
    exclude("architectury.common.json")
    dependencies {
        this.exclude {
            it.moduleGroup == "org.jetbrains.kotlin"
                    && it.moduleName.startsWith("kotlin-stdlib")
        }
    }
    for (pattern in listOf(
        "com",
        "de",
        "io",
        "okhttp3",
        "okio",
        "org",
        "zipkin2"
    )) {
        relocate("$pattern.", "$relocatePrefix.$pattern.") {
            this.exclude("org.jetbrains.annotations.**")
            this.exclude("dev.**")
            this.exclude("io.wispforest.**")
            this.exclude("org.sinytra.**")
            this.exclude("com.mojang.**")
            this.exclude("io.netty.**")
            this.exclude("com.google.errorprone.annotations.**")
            this.exclude("com.google.auto.value.**")
            this.exclude("org/codehaus/mojo/animal_sniffer/IgnoreJRERequirement")
            this.exclude("kotlin.**")
            this.exclude("de.mctelemetry.core.**")
            this.exclude("io.opentelemetry.**")
            this.exclude("io.prometheus.**")
            this.exclude("org.apache.**")
        }
    }
    configurations = listOf(shadowBundle)
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    injectAccessWidener.set(true)
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
    filesMatching("mcotelcore.accesswidener") {
        this.duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
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
