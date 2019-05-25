
val hamkrestVersion: String? by extra


val guiceVersion: String? by extra
val kotlinGuiceVersion: String? by extra

dependencies {
    compile("il.ac.technion.cs.softwaredesign", "primitive-storage-layer", "1.1")

    compile("com.google.inject", "guice", guiceVersion)
    compile("com.authzee.kotlinguice4", "kotlin-guice", kotlinGuiceVersion)

    testCompile("com.natpryce", "hamkrest", hamkrestVersion)
}