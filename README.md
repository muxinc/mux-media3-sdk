# Mux Player SDK for Android

The Mux Video Media3 SDK is a thin wrapper on top of Google's media3 player SDK with convenient tools for Mux Video users. This SDK is not required to use Mux Video, but it can help you do things like controlling your data and delivery usage, playing Mux assets by ID, automatically leveraging advanced player features like CMCD, and transparently tracking performance and engagement with [Mux Data](https://www.mux.com/data)

## Adding the SDK

### Add our repository to your Gradle project

Add Mux's maven repository to your gradle files. Newer projects require declaring this in `settings.gradle`, and older projects require it to be set in the project-level `build.gradle`.

```kotlin
// in a repositories {} block
maven {
  url = uri("https://muxinc.jfrog.io/artifactory/default-maven-release-local")
}
```

### Add the dependency to your app

Add our library to the `dependencies` block for your app. Replace the string `[Current Version]` with the current version of the SDK

```kotlin
implementation("com.mux.player:android:[Current Version]")
```

## Play a Mux Video Asset

### Create a MuxPlayer

To use the SDK, you must create a `MuxPlayer` object using its `Builder`. The basic configuration will enable all of Mux's extra features, and you can make additional config changes using our `Builder`. Almost all of our defaults config options are the same as ExoPlayer's. We only change things about the default configuration when we need to in order to support a Mux Player feature.

```kotlin
val out: MuxPlayer = MuxPlayer.Builder(context = this)
  .enableLogcat() // Only applies to Mux. Media3 logging is not touched
  .applyExoConfig {
    // Call ExoPlayer.Builder methods here (but not build()!)
    setHandleAudioBecomingNoisy(true)
  }
  .build()
```
### Play a Mux Video asset

To play a Mux Video asset using this SDK, you can use our `MediaItems` API to create new instances of media3's `MediaItem` or `MediaItem.Builder`. For the basic example, we'll leave everything default and play an asset you've already uploaded to Mux Video

```kotlin
// Use the MediaItems class instead of MediaItem.Builder()
val mediaItem = MediaItems.builderFromMuxPlaybackId("YOUR PLAYBACK ID")
  // It's just a MediaItem from here, so you can configure it however you like
  .setMediaMetadata(
    MediaMetadata.Builder()
      .setTitle("Hello from Mux Player on Android!")
      .build()
  )
  .build()

// From here, everything is exactly the same as ExoPlayer
player.setMediaItem(mediaItem)
player.prepare()
player.playWhenReady = true
```
