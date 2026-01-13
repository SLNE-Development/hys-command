plugins {
    kotlin("jvm") version "2.3.0"
}

allprojects {
    group = "dev.slne.hys.command"
    version = findProperty("version") as String

    repositories {
        mavenCentral()
    }
}