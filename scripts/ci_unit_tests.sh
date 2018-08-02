#!/usr/bin/env bash

./gradlew coveralls :testutils:test :testutils-android:testDebugUnitTest -Dpre-dex=false -Pkotlin.incremental=false --stacktrace
