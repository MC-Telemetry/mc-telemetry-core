architectury {
    common(rootProject.property("enabled_platforms").toString().split(","))
}

loom {
    accessWidenerPath.set(project.layout.projectDirectory.file("src/main/resources/mcotelcore.accesswidener"))
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
    compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi:${rootProject.property("otel_version")}")
}
