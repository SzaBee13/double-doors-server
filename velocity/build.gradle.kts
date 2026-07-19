plugins {
  id("com.gradleup.shadow") version "9.5.1"
}

base {
  archivesName.set("doubledoors-velocity")
}

val filteredJavaDir = layout.buildDirectory.dir("generated/sources/filtered-java")

tasks.register<Copy>("filterVelocityJava") {
  from("src/main/java")
  into(filteredJavaDir)
  include("**/*.java")
  filteringCharset = "UTF-8"
  expand(mapOf(
    "project" to mapOf("version" to project.version),
    "sqliteVersion" to findProperty("sqliteVersion"),
    "sqliteSha256" to findProperty("sqliteSha256"),
    "mysqlConnectorVersion" to findProperty("mysqlConnectorVersion"),
    "mysqlConnectorSha256" to findProperty("mysqlConnectorSha256"),
    "hikariVersion" to findProperty("hikariVersion"),
    "hikariSha256" to findProperty("hikariSha256"),
  ))
}

sourceSets {
  main {
    java {
      setSrcDirs(listOf(filteredJavaDir))
    }
    resources {
      setSrcDirs(listOf("src/main/resources"))
    }
  }
  test {
    java {
      setSrcDirs(listOf("src/test/java"))
    }
  }
}

dependencies {
  implementation(project(":core"))

  compileOnly("com.velocitypowered:velocity-api:${findProperty("velocityApiVersion")}")
  annotationProcessor("com.velocitypowered:velocity-api:${findProperty("velocityApiVersion")}")

  implementation("dev.faststats.metrics:velocity:${findProperty("faststatsVersion")}")
  compileOnly("org.xerial:sqlite-jdbc:${findProperty("sqliteVersion")}")
  compileOnly("com.mysql:mysql-connector-j:${findProperty("mysqlConnectorVersion")}")
  compileOnly("com.zaxxer:HikariCP:${findProperty("hikariVersion")}")

  testImplementation(platform("org.junit:junit-bom:${findProperty("junitVersion")}"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("com.velocitypowered:velocity-api:${findProperty("velocityApiVersion")}")
  testRuntimeOnly(platform("org.junit:junit-bom:${findProperty("junitVersion")}"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testRuntimeOnly("org.xerial:sqlite-jdbc:${findProperty("sqliteVersion")}")
  testRuntimeOnly("com.mysql:mysql-connector-j:${findProperty("mysqlConnectorVersion")}")
  testRuntimeOnly("com.zaxxer:HikariCP:${findProperty("hikariVersion")}")
}

tasks.named<JavaCompile>("compileJava") {
  dependsOn(tasks.named("filterVelocityJava"))
}

tasks.processResources {
  filesMatching("velocity-plugin.json") {
    expand(mapOf("project" to mapOf("version" to project.version)))
  }
}

tasks.shadowJar {
  archiveClassifier.set("")
  duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
  mergeServiceFiles()
  exclude("module-info.class")
  exclude("META-INF/MANIFEST.MF")
  exclude("META-INF/versions/**/module-info.class")
  exclude("META-INF/maven/**")
  exclude("META-INF/proguard/**")
  exclude("META-INF/native-image/**")
  exclude("META-INF/versions/9/org/sqlite/nativeimage/**")
}

tasks.jar {
  enabled = false
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["shadow"])
      artifactId = "doubledoors-velocity"
    }
  }
}
