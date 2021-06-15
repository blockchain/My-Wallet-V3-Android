# My-Wallet-V3-Android

[![CircleCI](https://circleci.com/gh/blockchain/My-Wallet-V3-Android/tree/master.svg?style=svg)](https://circleci.com/gh/blockchain/My-Wallet-V3-Android/tree/master)

[![Coverage Status](https://coveralls.io/repos/github/blockchain/My-Wallet-V3-Android/badge.svg?branch=master)](https://coveralls.io/github/blockchain/My-Wallet-V3-Android?branch=master)

Next-generation HD (BIP32, BIP39, BIP44) bitcoin, ethereum and bitcoin cash wallet. 

# Getting started

## Install Android Studio

Download from [Android Studio](https://developer.android.com/studio). Make sure to install the command line tools as well.
After installing AS, open it and install API 28 (current `compileSdkVersion`) and 29 (current `targetSdkVersion`)
from `Preferences -> Appearance & Behavior -> System Settings -> Android SDK`.
Install [Oracle JDK](https://www.oracle.com/java/technologies/javase-downloads.html) for Gradle command line tools.

**Required: Run the quickstart script from a bash terminal at the base of the project; `./scripts/quick_start.sh` this will install the necessary
dependencies for the project to compile successfully.**

Optional: Run the bootstrap script from terminal via `scripts/bootstrap.sh`. This will install the Google Java code style as well
as the official Android Kotlin code style and remove any file header templates. The script may indicate that you need
to restart Android Studio for it's changes to take effect.

## Building

**Required: Create a signing key in the `app` folder with the filename `blockchainDebugKey.keystore` with same the key password and store password. Then update the `keyPassword` and `debugKeyAlias` entries in the `secretDefaults.properties` file.**

Clone the [Android Repository](https://github.com/blockchain/wallet-android-private). Make sure your repository
is on `master`. Import it as an Android Studio project (`File -> Open`).

### Contributions and Code Style

All new code must be in Kotlin. We are using the official Kotlin style guide, which can be applied in Android Studio via 
`Preferences -> Editor -> Code Style -> Kotlin -> Set from -> Predefined style -> Kotlin Style Guide`. It should be 
noted that this is not currently the default in Android Studio, so please configure this if you have recently 
reinstalled AS. Alternatively, simply run the bootstrap script and ktlint will configure your IDE for you.

All code must be tested if possible, and must pass CI. Therefore it must introduce no new Lint errors, and must pass 
Ktlint. Before committing any new Kotlin code it is recommended formatting your files in Android Studio with 
`CMD + ALT + L` and running `./gradlew ktlint` locally. You can if you so wish run `./gradlew ktlintFormat` which 
will fix any style violations. Be aware that this may need to be run twice to apply all fixes as of 0.20.

## Commit message style

Use git change log style.

Where you have access to Jira, you should apply the git hooks with `./gradlew installGitHooks`. This enforces the
git change log style with Jira references.

### Security

Security issues can be reported to us in the following venues:
* Email: security@blockchain.info
* Bug Bounty: https://hackerone.com/blockchain
