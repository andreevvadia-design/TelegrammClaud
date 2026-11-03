# claud mode for Android

[Telegram: новости claud mode](https://t.me/telegaClaudMod)


Personal Telegram Android fork with plugin settings, profile customization and the Claude companion. Based on Telegram for Android; upstream license and copyright notices are retained.

## Build

Use JDK 17 and an Android SDK configured through an untracked local.properties. Build ARM64 with `./gradlew :TMessagesProj_App:assembleAfatDebug -PclaudAbi=arm64-v8a`.

Signing keystores, Firebase configuration, APKs, build directories, logs and local design/diagnostic files are deliberately not published. Supply your own configuration and signing credentials locally. Keep the existing private signing key for updates of installed builds; generating a different key prevents an in-place update.

APK downloads belong in GitHub Releases, not in Git history. In-app update delivery is still under development; publishing this source does not enable automatic updates by itself.
