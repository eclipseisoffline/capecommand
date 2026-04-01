plugins {
    alias(libs.plugins.multimod)
}

dependencies {
    compileOnly(libs.geyser.base.api)
}

multimod.neoForge(project(":common"))
