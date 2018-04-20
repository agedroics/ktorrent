plugins {
    kotlin("jvm")
}

dependencies {
	compile(project(":utils"))
    compile(project(":bencoding"))
    compile(kotlin("stdlib-jdk8"))
    testCompile(kotlin("test-junit"))
}
