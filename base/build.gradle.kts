import java.time.Duration
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.21"
}

allprojects {
    repositories {
        jcenter()
    }
}

subprojects {
    apply(plugin = "kotlin")
    dependencies {
        implementation(kotlin("stdlib-jdk8"))
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
                "$base/**/.git")
        exclude("$base/$taskname.zip")
    }
    destinationDirectory.set(project.rootDir)
}