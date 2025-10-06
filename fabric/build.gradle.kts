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
    runs {
        create("gameTestServer") {
            server()
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

    api("io.opentelemetry:opentelemetry-api:$otelVersion")
}

tasks.processResources {
    val expansionMap = mapOf(
        "group" to rootProject.property("maven_group"),
        "version" to project.version,

        "mod_id" to rootProject.property("mod_id"),
        "minecraft_version" to rootProject.property("minecraft_version"),
        "architectury_version" to rootProject.property("architectury_version"),
        "fabric_kotlin_version" to rootProject.property("fabric_kotlin_version")
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
        "com.fasterxml",
        "okhttp3",
        "okio",
        "org.intellij",
        "org.jetbrains",
        "org.snakeyaml",
        "org.yaml",
        "zipkin2"
    )) {
        relocate(pattern, "$relocatePrefix.$pattern")
    }
    configurations = listOf(shadowBundle)
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
