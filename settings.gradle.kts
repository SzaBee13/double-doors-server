pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven {
      url = uri("https://repo.papermc.io/repository/maven-public/")
    }
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
  repositories {
    mavenCentral()
    maven {
      url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
      url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
      url = uri("https://repo.faststats.dev/releases")
    }
    maven {
      url = uri("https://repo.lushplugins.org/releases/")
    }
  }
}

rootProject.name = "doubledoors-parent"
include("core", "bukkit", "velocity")
