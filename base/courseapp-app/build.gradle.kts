plugins {
    application
}

application {
    mainClassName = "il.ac.technion.cs.softwaredesign.MainKt"
}

val junitVersion: String? by extra
val hamkrestVersion: String? by extra
val guiceVersion: String? by extra
val kotlinGuiceVersion: String? by extra

dependencies {
    compile(project(":library"))
    compile("com.google.inject", "guice", guiceVersion)
    compile("com.authzee.kotlinguice4", "kotlin-guice", kotlinGuiceVersion)

    testCompile("org.junit.jupiter", "junit-jupiter-api", junitVersion)
    testCompile("org.junit.jupiter", "junit-jupiter-params", junitVersion)
    testCompile("com.natpryce", "hamkrest", hamkrestVersion)
}
