plugins {
    id("com.vanniktech.maven.publish") version "0.35.0"
}

architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
}

loom {
    accessWidenerPath.set(project.layout.projectDirectory.file("src/main/resources/mcotelcore.accesswidener"))
}

sourceSets {
    val main by getting

    val gametest by creating {
        compileClasspath += main.output + main.compileClasspath
        runtimeClasspath += main.output + main.runtimeClasspath
    }
}

dependencies {
    // We depend on fabric loader here to use the fabric @Environment annotations and get the mixin dependencies
    // Do NOT use other classes from fabric loader
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    // Remove the next line if you don't want to depend on the API
    modApi("dev.architectury:architectury:${rootProject.property("architectury_version")}")

    // owo-lib/oωo-lib
    // Docs specify modImplementation, but that leads to class-access problems during compilation.
    // Should be fine to be specified as compileOnly due to implementation in subprojects?
    modCompileOnly("io.wispforest:owo-lib:${rootProject.property("owo_fabric_version")}")

    // opentelemetry
    compileOnly("io.opentelemetry:opentelemetry-api:${rootProject.property("otel_version")}")

    // KObserve
    compileOnly("io.github.pixix4:KObserve:${rootProject.property("kobserve_version")}")
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "mc-telemetry-core", version.toString())

    pom {
        name = "MC Telemetry Core"
        description = "Monitor Minecraft with OpenTelemetry"
        inceptionYear = "2025"
        url = "https://github.com/MC-Telemetry/mc-telemetry-core"
        licenses {
            license {
                name = "MIT"
                url = "https://github.com/MC-Telemetry/mc-telemetry-core/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = "pixix4"
                name = "Lars Westermann"
                email = "maven@lars-westermann.de"
                organization = ""
                organizationUrl = "https://github.com/pixix4"
            }
            developer {
                id = "ZincFox"
                name = "Alexander Schwerin"
                email = "Alexander_C._Schwerin@web.de"
                organization = ""
                organizationUrl = "https://github.com/Zincfox"
            }
        }
        scm {
            url = "github.com/MC-Telemetry/mc-telemetry-core/"
            connection = "scm:git:git://github.com/MC-Telemetry/mc-telemetry-core.git"
            developerConnection = "scm:git:ssh://git@github.com/MC-Telemetry/mc-telemetry-core.git"
        }
    }
}
