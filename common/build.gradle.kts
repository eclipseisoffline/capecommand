plugins {
    alias(libs.plugins.multimod)
}

dependencies {
    compileOnly(libs.geyser.base.api)
}

multimod.common {
    accessWidenerPath = file("src/main/resources/capecommand.accesswidener")
}
