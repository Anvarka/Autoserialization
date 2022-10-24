import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.7.10" apply false
    application
}

group = "org.itmo.java"
version = "1.0-SNAPSHOT"

subprojects {
    apply {
        plugin("java")
        plugin("application")
        plugin("kotlin")
    }

    repositories {
        mavenCentral()
    }

    dependencies {
        implementation("org.jetbrains:annotations:23.0.0")
        testImplementation(platform("org.junit:junit-bom:5.8.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    sourceSets {
        main {
            java.setSrcDirs(listOf("src"))
            resources.setSrcDirs(listOf("resources"))
        }
        test {
            java.setSrcDirs(listOf("test"))
            resources.setSrcDirs(listOf("testResources"))
        }
    }

    tasks.test {
        useJUnitPlatform()
    }

    java.toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    val compileKotlin: KotlinCompile by tasks

    compileKotlin.kotlinOptions {
        freeCompilerArgs += "-Xuse-k2"
    }

}
