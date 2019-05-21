
val hamkrestVersion: String? by extra

dependencies {
    compile("il.ac.technion.cs.softwaredesign", "primitive-storage-layer", "1.1")

    testCompile("com.natpryce", "hamkrest", hamkrestVersion)
}