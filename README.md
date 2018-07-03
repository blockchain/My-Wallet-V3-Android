# My-Wallet-V3-Android

[![CircleCI](https://circleci.com/gh/blockchain/My-Wallet-V3-Android/tree/master.svg?style=svg)](https://circleci.com/gh/blockchain/My-Wallet-V3-Android/tree/master)

[![Coverage Status](https://coveralls.io/repos/github/blockchain/My-Wallet-V3-Android/badge.svg?branch=master)](https://coveralls.io/github/blockchain/My-Wallet-V3-Android?branch=master)

Next-generation HD (BIP32, BIP39, BIP44) bitcoin, ethereum and bitcoin cash wallet. 

## Getting started

Install Android Studio: https://developer.android.com/sdk/index.html

Import as Android Studio project.

Run the bootstrap script from terminal via `scripts/bootstrap.sh`. This will install the Google Java code style as well 
as the official Android Kotlin code style and remove any file header templates. The script may indicate that you need 
to restart Android Studio for it's changes to take effect.

Build -> Make Project

If there are build errors, in Android Studio go to Tools -> Android -> SDK Manager and install any available updates.

### Contributions and Code Style

All new code must be in Kotlin. We are using the official Kotlin style guide, which can be applied in Android Studio via 
`Preferences -> Editor -> Code Style -> Kotlin -> Set from -> Predefined style -> Kotlin Style Guide`. It should be 
noted that this is not currently the default in Android Studio, so please configure this if you have recently 
reinstalled AS. Alternatively, simply run the bootstrap script and ktlint will configure your IDE for you.

All code must be tested if possible, and must pass CI. Therefore it must introduce no new Lint errors, and must pass 
Ktlint. Before committing any new Kotlin code I could recommend formatting your files in Android Studio with 
`CMD + ALT + L` and running `./gradlew ktlint` locally. You can if you so wish run `./gradlew ktlintFormat` which 
will fix any style violations. Be aware that this may need to be run twice to apply all fixes as of 0.20.

## Tests

Unit tests for the project can be run via `scripts/ci_unit_tests.sh`. This also generates coverage reports.

### Security

Security issues can be reported to us in the following venues:
* Email: security@blockchain.info
* Bug Bounty: https://hackerone.com/blockchain
