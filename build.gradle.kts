import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    java
    kotlin("jvm") version "2.2.20"
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("dev.architectury.loom") version "1.11-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "8.3.8" apply false
}

val otelVersion = rootProject.property("otel_version") as String

architectury {
    minecraft = rootProject.property("minecraft_version").toString()
}

kotlin {
    jvmToolchain(21)
}

subprojects {
    apply(plugin = "dev.architectury.loom")

    val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")


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
    }

    dependencies {
        compileOnly("org.jetbrains.kotlin:kotlin-stdlib:${rootProject.property("kotlin_stdlib_version")}")
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    java {
        withSourcesJar()
    }
}
