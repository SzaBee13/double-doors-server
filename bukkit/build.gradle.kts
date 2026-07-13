plugins {
  id("com.gradleup.shadow") version "9.5.1"
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

base {
  archivesName.set("doubledoors-bukkit")
}

val minecraftVersionId = (findProperty("minecraftVersionId") ?: "v26_2_x").toString()

sourceSets {
  main {
    java {
      srcDirs(
        "src/main/java-common",
        "src/main/java-$minecraftVersionId"
      )
    }
    resources {
      setSrcDirs(listOf("src/main/resources"))
    }
  }
  test {
    java {
      srcDir("src/test/java")
    }
  }
}

dependencies {
  implementation(project(":core"))

  paperweight.paperDevBundle(findProperty("paperDevBundleVersion") as String)

  implementation("org.lushplugins.pluginupdater:PluginUpdater-API:${findProperty("pluginUpdaterVersion")}") {
    exclude(group = "org.lushplugins", module = "ChatColorHandler")
  }

  implementation("dev.faststats.metrics:bukkit:${findProperty("faststatsVersion")}")
  implementation("com.google.code.gson:gson:${findProperty("gsonVersion")}") {
    exclude(group = "com.google.errorprone", module = "error_prone_annotations")
  }
  implementation("org.xerial:sqlite-jdbc:${findProperty("sqliteVersion")}")
  implementation("com.mysql:mysql-connector-j:${findProperty("mysqlConnectorVersion")}")

  testImplementation(platform("org.junit:junit-bom:${findProperty("junitVersion")}"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.mockito:mockito-core:${findProperty("mockitoVersion")}")
  testImplementation("org.mockito:mockito-junit-jupiter:${findProperty("mockitoVersion")}")
  testRuntimeOnly(platform("org.junit:junit-bom:${findProperty("junitVersion")}"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
  filesMatching("plugin.yml") {
    expand(mapOf("project" to mapOf("version" to project.version)))
  }
}

tasks.shadowJar {
  archiveClassifier.set("")
  duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.INCLUDE
  mergeServiceFiles()
  exclude("module-info.class")
  exclude("META-INF/MANIFEST.MF")
  exclude("META-INF/versions/**/module-info.class")
  exclude("META-INF/faststats.properties")
}

tasks.jar {
  enabled = false
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["shadow"])
      artifactId = "doubledoors-bukkit"
    }
  }
}
