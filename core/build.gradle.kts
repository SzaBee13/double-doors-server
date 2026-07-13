base {
  archivesName.set("doubledoors-core")
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:${findProperty("junitVersion")}"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly(platform("org.junit:junit-bom:${findProperty("junitVersion")}"))
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      artifactId = "doubledoors-core"
    }
  }
}
