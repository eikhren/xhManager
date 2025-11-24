plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    val coroutinesVersion: String by rootProject.extra
    val serializationVersion: String by rootProject.extra
    val junitVersion: String by rootProject.extra

    implementation(project(":core"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}
