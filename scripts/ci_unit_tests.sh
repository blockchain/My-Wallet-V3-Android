#!/usr/bin/env bash

./gradlew coveralls :testutils:testDebugUnitTest -Dpre-dex=false -Pkotlin.incremental=false --stacktrace
