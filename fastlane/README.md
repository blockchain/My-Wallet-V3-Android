fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew cask install fastlane`

# Available Actions
## Android
### android build_tag_and_upload_release
```
fastlane android build_tag_and_upload_release
```
Runs our entire release process from start to finish. Tests the app, increments version code and updates version name, commits changes to build.gradle, and then tags + signs + generates changelog, uploads to Drive, uploads to Alpha, posts to Slack.
### android test
```
fastlane android test
```
Runs all tests
### android beta
```
fastlane android beta
```
Submit a Dogfood Beta build to Crashlytics Beta
### android generate_apks
```
fastlane android generate_apks
```
Generate release builds for both staging and production
### android archive
```
fastlane android archive
```
Upload APK to Google Drive
### android alpha
```
fastlane android alpha
```
Submit a release Alpha build to the Play Store. This won't publish, just upload.

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
