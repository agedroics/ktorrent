plugins {
    kotlin("jvm")
}

dependencies {
    compile(project(":bencoding"))
    compile(kotlin("stdlib-jdk8"))
    testCompile(kotlin("test-junit"))
}
