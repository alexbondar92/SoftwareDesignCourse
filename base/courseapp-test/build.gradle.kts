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
val junitVersion: String? by extra
val hamkrestVersion: String? by extra
val guiceVersion: String? by extra
val kotlinGuiceVersion: String? by extra
val mockkVersion: String? by extra

dependencies {
    compile(project(":library"))
    compile(project(":courseapp-app"))

    testCompile("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testCompile("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testCompile("com.natpryce", "hamkrest", hamkrestVersion)
    testCompile("com.google.inject", "guice", guiceVersion)
    testCompile("com.authzee.kotlinguice4", "kotlin-guice", kotlinGuiceVersion)

    testImplementation("io.mockk:mockk:1.9.3")
}




