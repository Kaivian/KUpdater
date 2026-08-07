plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":platform:common"))
    implementation(project(":platform:v1_21"))
    implementation(project(":features:tools"))
    implementation(project(":features:progression"))
    implementation(project(":features:combat"))
    implementation(project(":features:farming"))
    implementation(project(":features:mining"))
    implementation(project(":features:economy"))
    implementation(project(":features:exploration"))

    compileOnly(libs.paper.api)
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveFileName.set("KUpdater-${project.version}.jar")
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to project.version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
