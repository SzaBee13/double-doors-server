plugins {
  id("com.gradleup.shadow") version "9.5.1"
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

base {
  archivesName.set("doubledoors-paper")
}

val minecraftVersionId = property("minecraftVersionId").toString()

sourceSets {
  main {
    java {
      srcDirs(
        "../bukkit/src/main/java-common",
        "../bukkit/src/main/java-$minecraftVersionId"
      )
    }
    resources {
      setSrcDirs(listOf("src/main/resources", "../bukkit/src/main/resources"))
    }
  }
  test {
    java {
      srcDir("../bukkit/src/test/java")
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

  compileOnly("com.google.code.gson:gson:${property("gsonVersion")}")
  compileOnly("org.xerial:sqlite-jdbc:${property("sqliteVersion")}")
  compileOnly("com.mysql:mysql-connector-j:${property("mysqlConnectorVersion")}")

  testImplementation(platform("org.junit:junit-bom:${property("junitVersion")}"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testImplementation("org.mockito:mockito-core:${property("mockitoVersion")}")
  testImplementation("org.mockito:mockito-junit-jupiter:${property("mockitoVersion")}")
  testRuntimeOnly(platform("org.junit:junit-bom:${property("junitVersion")}"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.processResources {
  inputs.property("projectVersion", project.version)
  exclude("plugin.yml")
  filesMatching("paper-plugin.yml") {
    expand(
      mapOf(
        "project" to mapOf("version" to project.version),
        "gsonVersion" to project.property("gsonVersion"),
        "sqliteVersion" to project.property("sqliteVersion"),
        "mysqlConnectorVersion" to project.property("mysqlConnectorVersion")
      )
    )
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
      artifactId = "doubledoors-paper"
    }
  }
}
