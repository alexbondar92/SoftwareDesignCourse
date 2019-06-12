plugins {
    application
    id("org.jetbrains.dokka") version "0.9.18"
}

application {
    tasks.dokka {
        outputFormat = "html"
        outputDirectory = " build/javadoc"
    }
}

val hamkrestVersion: String? by extra
val junitVersion: String? by extra

val guiceVersion: String? by extra
val kotlinGuiceVersion: String? by extra

dependencies {
    compile("il.ac.technion.cs.softwaredesign", "primitive-storage-layer", "1.2")

    compile("com.google.inject", "guice", guiceVersion)
    compile("com.authzee.kotlinguice4", "kotlin-guice", kotlinGuiceVersion)

    testCompile("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testCompile("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testCompile("com.natpryce", "hamkrest", hamkrestVersion)

    runtime("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
    testImplementation("io.mockk:mockk:1.9.3")
}