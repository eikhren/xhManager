plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    val coroutinesVersion: String by rootProject.extra
    val junitVersion: String by rootProject.extra

    implementation(project(":core"))
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.ui)
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}
