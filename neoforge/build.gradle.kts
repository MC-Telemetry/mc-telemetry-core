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
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    neoForge.apply {
        this.accessTransformer(project.layout.projectDirectory.file("src/main/resources/META-INF/accesstransformer.cfg"))
    }
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
    maven {
        name = "CurseMaven"
        setUrl("https://cursemaven.com")
        content {
            includeGroup("curse.maven")
        }
    }
}

dependencies {
    neoForge("net.neoforged:neoforge:${rootProject.property("neoforge_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-neoforge:${rootProject.property("architectury_version")}")

    modRuntimeOnly("curse.maven:jei-238222:7057366")
    modRuntimeOnly("curse.maven:nbtedit-reborn-678133:6125444")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowBundle(project(":common", "transformProductionNeoForge")) { isTransitive = false }

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge-neoforge:${rootProject.property("kotlin_for_forge_version")}")

    // owo-lib/oωo-lib
    modImplementation("io.wispforest:owo-lib-neoforge:${rootProject.property("owo_neoforge_version")}")
    forgeRuntimeLibrary("blue.endless:jankson:1.2.2")
    forgeRuntimeLibrary("io.wispforest:endec:0.1.5.1")
    forgeRuntimeLibrary("io.wispforest.endec:netty:0.1.2")
    forgeRuntimeLibrary("io.wispforest.endec:gson:0.1.3.1")
    forgeRuntimeLibrary("io.wispforest.endec:jankson:0.1.3.1")

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
        "neoforge_version" to rootProject.property("neoforge_version"),
        "architectury_version" to rootProject.property("architectury_version"),
        "kotlin_for_forge_version" to rootProject.property("kotlin_for_forge_version"),
        "owo_neoforge_version" to rootProject.property("owo_neoforge_version"),
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
