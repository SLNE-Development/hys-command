plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "shy-command"

//include("shy-command-api")
include("shy-command-server")
