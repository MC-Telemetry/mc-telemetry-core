import com.github.jengelman.gradle.plugins.shadow.relocation.SimpleRelocator
import org.jetbrains.kotlin.gradle.utils.extendsFrom
import java.nio.file.Paths
import java.util.Properties
import java.util.jar.JarFile

plugins {
    id("com.gradleup.shadow")
}

val MOD_ID: String = rootProject.property("mod_id").toString()
val otelVersion: String = rootProject.property("otel_version") as String
val relocatePrefix = (rootProject.property("relocate_prefix") as String)

architectury {
    platformSetupLoomIde()
    forge()
}

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    forge.apply {
        convertAccessWideners.set(true)
        extraAccessWideners.add(loom.accessWidenerPath.get().asFile.name)
    }

    runs {
        create("gameTestServer") {
            server()
            runDir = "gameTestRun"

            property("forge.logging.console.level", "debug")
            property("forge.logging.markers", "REGISTRIES")
            property("forge.enabledGameTestNamespaces", MOD_ID)
            property("forge.enableGameTest", "true")
            property("forge.gameTestServer", "true")
        }
    }
}

tasks["check"].dependsOn("runGameTestServer")

val common: Configuration by configurations.creating
val shadowCommon: Configuration by configurations.creating
val developmentForge: Configuration by configurations.getting

configurations {
    compileOnly.configure { extendsFrom(common) }
    runtimeOnly.configure { extendsFrom(common) }
    developmentForge.extendsFrom(common)
}

repositories {
    // KFF
    maven {
        name = "Kotlin for Forge"
        setUrl("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

dependencies {
    forge("net.minecraftforge:forge:${rootProject.property("minecraft_version")}-${rootProject.property("forge_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury-forge:${rootProject.property("architectury_version")}")

    common(project(":common", "namedElements")) { isTransitive = false }
    shadowCommon(project(":common", "transformProductionForge")) { isTransitive = false }

    // Kotlin For Forge
    implementation("thedarkcolour:kotlinforforge:${rootProject.property("kotlin_for_forge_version")}")

    api("io.opentelemetry:opentelemetry-api:$otelVersion")
}

tasks.processResources {
    val expansionMap = mapOf(
        "group" to rootProject.property("maven_group"),
        "version" to project.version,

        "mod_id" to rootProject.property("mod_id"),
        "minecraft_version" to rootProject.property("minecraft_version"),
        "forge_version" to rootProject.property("forge_version"),
        "architectury_version" to rootProject.property("architectury_version"),
        "kotlin_for_forge_version" to rootProject.property("kotlin_for_forge_version")
    )
    inputs.properties(expansionMap)

    filesMatching("META-INF/mods.toml") {
        expand(expansionMap)
    }
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")
    configurations = listOf(shadowCommon)
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
    mergeServiceFiles()
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

tasks.create("configureGameTestServer") {
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
