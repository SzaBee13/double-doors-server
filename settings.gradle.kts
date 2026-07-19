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
      content {
        includeGroupByRegex("io\\.papermc(\\..*)?")
        includeGroup("ca.spottedleaf")
        includeGroup("com.mojang")
        includeGroup("com.velocitypowered")
        includeGroup("me.lucko")
        includeGroup("net.md-5")
      }
    }
    maven {
      url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
      content {
        includeGroup("org.spigotmc")
      }
    }
    maven {
      url = uri("https://repo.faststats.dev/releases/")
      content {
        includeGroup("dev.faststats.metrics")
      }
    }
    maven {
      url = uri("https://repo.lushplugins.org/releases/")
      content {
        includeGroup("org.lushplugins.pluginupdater")
      }
    }
  }
}

rootProject.name = "doubledoors-parent"
include("core", "bukkit", "paper", "velocity")
