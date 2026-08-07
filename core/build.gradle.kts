dependencies {
    api(project(":api"))
    compileOnly(libs.paper.api)
    testImplementation(libs.junit.jupiter)
}
