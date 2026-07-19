plugins {
  id("com.gradleup.shadow") version "9.5.1"
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

base {
  archivesName.set("doubledoors-bukkit")
}

val minecraftVersionId = property("minecraftVersionId").toString()

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

  paperweight.paperDevBundle(property("paperDevBundleVersion") as String)

  implementation("org.lushplugins.pluginupdater:PluginUpdater-API:${property("pluginUpdaterVersion")}") {
    exclude(group = "org.lushplugins", module = "ChatColorHandler")
  }

  implementation("dev.faststats.metrics:bukkit:${property("faststatsVersion")}")
  implementation("com.google.code.gson:gson:${property("gsonVersion")}") {
    exclude(group = "com.google.errorprone", module = "error_prone_annotations")
  }
  implementation("org.xerial:sqlite-jdbc:${property("sqliteVersion")}")
  implementation("com.mysql:mysql-connector-j:${property("mysqlConnectorVersion")}")

  testImplementation(platform("org.junit:junit-bom:${property("junitVersion")}"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.mockito:mockito-core:${property("mockitoVersion")}")
  testImplementation("org.mockito:mockito-junit-jupiter:${property("mockitoVersion")}")
  testRuntimeOnly(platform("org.junit:junit-bom:${property("junitVersion")}"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
  inputs.property("projectVersion", project.version)
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
      artifactId = "doubledoors-bukkit"
    }
  }
}
