dependencies {
    api(project(":api"))
    implementation(libs.hikaricp)
    implementation(libs.sqlite.jdbc)
    implementation(libs.mysql.connector)
    implementation(libs.postgresql.driver)
    implementation(libs.mariadb.driver)
    implementation(libs.mssql.driver)
    compileOnly(libs.paper.api)
    testImplementation(libs.paper.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}



