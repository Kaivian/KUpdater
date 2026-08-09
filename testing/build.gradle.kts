dependencies {
    implementation(project(":api"))
    implementation(project(":core"))
    implementation(project(":platform:common"))
    implementation(project(":bootstrap"))

    testImplementation(project(":features:tools"))
    testImplementation(project(":features:progression"))
    testImplementation(project(":features:combat"))
    testImplementation(project(":features:farming"))
    testImplementation(project(":features:mining"))
    testImplementation(project(":features:economy"))
    testImplementation(project(":features:exploration"))

    implementation(libs.paper.api)
    implementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
