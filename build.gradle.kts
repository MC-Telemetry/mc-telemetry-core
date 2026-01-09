import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "2.2.20"
    id("architectury-plugin") version "3.4.162"
    id("dev.architectury.loom") version "1.11.445" apply false
    id("com.gradleup.shadow") version "8.3.8" apply false
    id("de.undercouch.download") version "5.6.0"
}

architectury {
    minecraft = rootProject.property("minecraft_version").toString()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.contracts.ExperimentalContracts")
    }
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "kotlin")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")

    kotlin {
        compilerOptions {
            optIn.add("kotlin.contracts.ExperimentalContracts")
        }
    }

    dependencies {
        "minecraft"("com.mojang:minecraft:${project.property("minecraft_version")}")
        // The following line declares the mojmap mappings, you may use other mappings as well
        "mappings"(
            loom.officialMojangMappings()
        )
        // The following line declares the yarn mappings you may select this one as well.
        // "mappings"("net.fabricmc:yarn:1.18.2+build.3:v2")
    }

    base.archivesName.set("${rootProject.property("archives_base_name")}-${project.name}")
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    version = rootProject.property("mod_version").toString()
    group = rootProject.property("maven_group").toString()

    repositories {
        // Add repositories to retrieve artifacts from in here.
        // You should only use this when depending on other mods because
        // Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
        // See https://docs.gradle.org/current/userguide/declaring_repositories.html
        // for more information about repositories.
        maven { url = uri("https://maven.wispforest.io/releases/") }
        maven { url = uri("https://maven.su5ed.dev/releases") }
    }

    dependencies {
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:${rootProject.property("kotlin_stdlib_version")}")
        compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:${rootProject.property("kotlin_coroutines_version")}")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    java {
        withSourcesJar()
    }
}

val downloadOTelAgentTask = tasks.register<Download>("downloadOTelAgent") {
    src(rootProject.property("otel_javaagent_url"))
    dest(rootProject.layout.buildDirectory.dir("downloadOTelAgent"))
    overwrite(false)
}

tasks.register<Verify>("verifyOTelAgent") {
    dependsOn(downloadOTelAgentTask)
    src(rootProject.layout.buildDirectory.file("downloadOTelAgent/opentelemetry-javaagent.jar"))
    val (algorithm, checksum) = (rootProject.property("otel_javaagent_checksum") as String).split(':', limit = 2)
    algorithm(algorithm)
    checksum(checksum)
}
