plugins {
    alias(libs.plugins.multimod)
}

multimod.common {
    accessWidenerPath = file("src/main/resources/capecommand.accesswidener")
}
