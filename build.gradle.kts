plugins {
  id("com.android.application") version "8.7.3" apply false
  id("org.jetbrains.kotlin.android") version "2.0.21" apply false
  id("com.android.library") version "8.7.3" apply false
  id("com.mux.gradle.android.mux-android-distribution") version "1.3.0" apply false
}

allprojects {
  ext {
    set("muxDataVersion", "1.6.2")
  }
}