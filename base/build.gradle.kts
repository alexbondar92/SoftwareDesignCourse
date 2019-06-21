import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Duration

plugins {
    kotlin("jvm") version "1.3.31"
}

allprojects {
    repositories {
        jcenter()
    }

    extra.apply {
        set("junitVersion", "5.4.2")
        set("hamkrestVersion", "1.7.0.0")
        set("guiceVersion", "4.2.2")
        set("kotlinGuiceVersion", "1.3.0")
        set("mockkVersion", "1.9.3")
    }
}

subprojects {
    apply(plugin = "kotlin")
    dependencies {
        val junitVersion: String? by extra
        implementation(kotlin("stdlib-jdk8"))
        compile(kotlin("reflect"))

        testRuntime("org.junit.jupiter", "junit-jupiter-engine", junitVersion)
    }
    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
    tasks.withType<Test> {
        useJUnitPlatform()

        // Make sure tests don't take over 10 minutes
        timeout.set(Duration.ofMinutes(10))
    }
}

task<Zip>("submission") {
    val taskname = "submission"
    val base = project.rootDir.name
    archiveBaseName.set(taskname)
    from(project.rootDir.parentFile) {
        include("$base/**")
        exclude("$base/**/*.iml", "$base/*/build", "$base/**/.gradle", "$base/**/.idea", "$base/*/out",
                "$base/**/.git", "$base/**/.DS_Store")
        exclude("$base/$taskname.zip")
    }
    destinationDirectory.set(project.rootDir)
}