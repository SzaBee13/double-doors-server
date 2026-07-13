plugins {
  base
}

val javaRelease = (findProperty("javaRelease") ?: "25").toString().toInt()

allprojects {
  group = findProperty("group") ?: "me.szabee.doubledoors"
  version = findProperty("version") ?: "1.4.6"
}

subprojects {
  apply(plugin = "java-library")
  apply(plugin = "maven-publish")
  apply(plugin = "jacoco")

  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.toVersion(javaRelease)
    targetCompatibility = JavaVersion.toVersion(javaRelease)
  }

  tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaRelease)
  }

  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy(tasks.named("jacocoTestReport"))
  }

  configure<JacocoPluginExtension> {
    toolVersion = "0.8.14"
  }

  tasks.named<JacocoReport>("jacocoTestReport") {
    reports {
      xml.required.set(true)
      html.required.set(false)
    }
  }

  configure<PublishingExtension> {
    repositories {
      maven {
        name = "githubPackages"
        url = uri("https://maven.pkg.github.com/SzaBee13/double-doors-server")
        credentials {
          username = System.getenv("GITHUB_ACTOR") ?: findProperty("gpr.user")?.toString() ?: "x-access-token"
          password = System.getenv("GITHUB_TOKEN") ?: findProperty("gpr.key")?.toString()
        }
      }
    }
  }
}
