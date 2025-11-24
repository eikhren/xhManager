import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

dependencies {
    val coroutinesVersion: String by rootProject.extra
    val junitVersion: String by rootProject.extra

    implementation(project(":ui"))
    implementation(project(":core"))
    implementation(project(":render"))
    implementation(project(":storage"))

    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
}

compose.desktop {
    application {
        mainClass = "xhman.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Rpm, TargetFormat.AppImage)
            packageName = "xhman"
            packageVersion = "0.1.0"
            linux {
                iconFile.set(project.file("src/main/resources/icons/xhman.png"))
            }
            jvmArgs += listOf(
                "-Dsun.java2d.uiScale.enabled=true",
                "-Dsun.java2d.uiScale=1.0"
            )
        }
    }
}
