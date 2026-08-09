dependencies {
    api(project(":api"))
    implementation(libs.hikaricp)
    compileOnly(libs.sqlite.jdbc)
    compileOnly(libs.mysql.connector)
    compileOnly(libs.postgresql.driver)
    compileOnly(libs.mariadb.driver)
    compileOnly(libs.mssql.driver)

    testImplementation(libs.hikaricp)
    testImplementation(libs.sqlite.jdbc)
    testImplementation(libs.mysql.connector)
    testImplementation(libs.postgresql.driver)
    testImplementation(libs.mariadb.driver)
    testImplementation(libs.mssql.driver)

    compileOnly(libs.paper.api)
    testImplementation(libs.paper.api)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.launcher)
}



