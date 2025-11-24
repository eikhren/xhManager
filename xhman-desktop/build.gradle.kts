import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
    id("org.jetbrains.compose") version "1.6.10" apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.6" apply false
}

extra.apply {
    set("coroutinesVersion", "1.8.0")
    set("junitVersion", "5.10.2")
    set("serializationVersion", "1.6.3")
}

allprojects {
    group = "xhman.desktop"
    version = "0.1.0-SNAPSHOT"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure(KotlinJvmProjectExtension::class) {
            // Kotlin 1.9/Compose toolchain does not parse JDK 25 properly; use JDK 21 installed on host.
            jvmToolchain(21)
        }
    }
}
