# Player Tags (RuneLite plugin starter)

A RuneLite plugin starter for custom player labels.

## What is implemented

- A hover tooltip appears when the mouse is over a tagged player model.
- The tooltip displays a custom tag and pronouns (either field can be blank).
- Your own tag and pronouns are configurable.
- Local per-player overrides, stored in RuneLite configuration.
- Per-player hide entries.
- Text colour and size settings.

## Shared publishing status

The shared publishing/lookup feature is **not enabled in this starter**. It needs a hosted registry service so one player's published tag can be retrieved by other clients. A RuneLite plugin cannot make a local setting visible to other players by itself.

The intended shared design is documented in `docs/shared-registry.md`. The client should only publish after an explicit opt-in, should use the player's RuneScape display name as the lookup key, and should provide an unpublish action. Do not put account credentials, tokens, or private information in the registry.

## Build

This project is a source starter, not a verified Plugin Hub release. Import it into IntelliJ IDEA as a Gradle project and align the Gradle/RuneLite versions with the current RuneLite example-plugin template:

https://github.com/runelite/example-plugin

Then run the Gradle `run` task (Windows: `gradlew.bat run`; macOS/Linux: `./gradlew run`). This launches a separate development RuneLite client with this plugin loaded. RuneLite's plugin development guide:
https://github.com/runelite/runelite/wiki/Developer-Guide

## Local override format

In the plugin configuration, enter one mapping per line:

```text
PlayerName=Fox|she/her
AnotherPlayer=Community helper|they/them
ThirdPlayer=Just a custom tag
```

Use the exact in-game display name. The first value is the tag; the optional value after `|` is pronouns. Existing `PlayerName=Custom text` entries continue to work as tag-only overrides.
