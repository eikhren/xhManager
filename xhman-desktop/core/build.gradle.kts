plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

dependencies {
    val coroutinesVersion: String by rootProject.extra
    val junitVersion: String by rootProject.extra
    val serializationVersion: String by rootProject.extra

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}
