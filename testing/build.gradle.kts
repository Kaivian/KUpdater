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
    testImplementation(libs.hikaricp)
    testImplementation(libs.sqlite.jdbc)
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.2")
    testRuntimeOnly(libs.junit.launcher)
}

tasks.withType<Test>().configureEach {
    dependsOn(":bootstrap:shadowJar")
    useJUnitPlatform()
    jvmArgs("-Dnet.bytebuddy.experimental=true")
}
