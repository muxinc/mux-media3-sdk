pluginManagement {
  repositories {
    maven { url = uri("https://muxinc.jfrog.io/artifactory/default-maven-release-local") }
    maven { url = uri("https://muxinc.jfrog.io/artifactory/default-maven-local") }
    google()
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
  }
}
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
  repositories {
    maven { url = uri("https://muxinc.jfrog.io/artifactory/default-maven-release-local") }
    maven { url = uri("https://muxinc.jfrog.io/artifactory/default-maven-local") }
    google()
    mavenCentral()
    mavenLocal()
  }
}

rootProject.name = "Mux Player for Android"
include(":app")
include(":library")
