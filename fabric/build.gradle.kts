plugins {
    alias(libs.plugins.multimod)
}

dependencies {
    compileOnly(libs.geyser.base.api)
}

multimod.fabric(project(":common")) {
    loom {
        accessWidenerPath = file("../common/src/main/resources/capecommand.accesswidener")
    }
}
