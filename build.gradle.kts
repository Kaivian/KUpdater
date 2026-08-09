plugins {
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.run.paper) apply false
}

allprojects {
    group = property("group").toString()
    version = property("version").toString()
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }


    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    val sourceSets = the<SourceSetContainer>()

    tasks.named<Test>("test") {
        useJUnitPlatform {
            includeTags("unit")
            excludeTags("integration", "minecraft", "compatibility")
        }
    }

    tasks.register<Test>("integrationTest") {
        group = "verification"
        description = "Runs integration tests for this module."
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform {
            includeTags("integration")
        }
    }

    tasks.register<Test>("minecraftTest") {
        group = "verification"
        description = "Runs real Minecraft server smoke test."
        dependsOn(":bootstrap:shadowJar")
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform {
            includeTags("minecraft")
        }
    }

    tasks.register<Test>("compatibilityTest") {
        group = "verification"
        description = "Runs version matrix compatibility test."
        dependsOn(":bootstrap:shadowJar")
        testClassesDirs = sourceSets["test"].output.classesDirs
        classpath = sourceSets["test"].runtimeClasspath
        useJUnitPlatform {
            includeTags("compatibility")
        }
    }
}
