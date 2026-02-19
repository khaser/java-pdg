plugins {
    java
    application
}

group = "ru.itmo.khaser.java_pdg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.25.8")
    // testImplementation(kotlin("test"))
}

application {
    mainClass.set("ru.itmo.khaser.java_pdg.Main")
}
