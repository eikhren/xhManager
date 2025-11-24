plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    val coroutinesVersion: String by rootProject.extra
    val junitVersion: String by rootProject.extra

    implementation(project(":core"))
    implementation(project(":render"))
    implementation(project(":storage"))

    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.desktop.common)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:$coroutinesVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}
