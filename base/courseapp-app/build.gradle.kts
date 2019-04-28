plugins {
    application
}

application {
    mainClassName = "il.ac.technion.cs.softwaredesign.MainKt"
}

val junitVersion = "5.5.0-M1"
val hamkrestVersion = "1.7.0.0"
val mockkVersion = "1.9"

dependencies {
    compile(project(":library"))


    testCompile("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testCompile("com.natpryce:hamkrest:$hamkrestVersion")

    testImplementation("io.mockk:mockk:$mockkVersion")

    runtime("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
