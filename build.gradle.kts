import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.2.40"
}

repositories {
    jcenter()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    testCompile(kotlin("test-junit"))
}
