import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Properties

version = "0.1"

plugins {
    application
    kotlin("jvm") version "1.2.50"
}

application {
    mainClassName = "ktorrent.ui.Main"
}

repositories {
    jcenter()
}

tasks {
    "createVersionFile" {
        dependsOn("processResources")
        doLast {
            File("$buildDir/resources/main/version.txt").writer().use {
                it.write(version as String)
            }
        }
    }

    "classes" {
        dependsOn("createVersionFile")
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    testCompile(kotlin("test-junit"))
}
