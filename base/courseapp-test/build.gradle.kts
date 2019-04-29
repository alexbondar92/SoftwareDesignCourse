plugins {
}

val junitVersion = "5.5.0-M1"
val hamkrestVersion = "1.7.0.0"

dependencies {
    compile(project(":library"))
    compile(project(":courseapp-app"))

    testCompile("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testCompile("com.natpryce:hamkrest:$hamkrestVersion")

    runtime("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

