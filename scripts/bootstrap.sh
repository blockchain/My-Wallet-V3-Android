#!/usr/bin/env bash
printf 'Starting bootstrap process\n'

ANDROID_STUDIO_PREFERENCES_PATH=`find ${HOME}/Library/Preferences -name 'AndroidStudio*' -depth 1 | sort -r | head -1`

if [ ! -d ${ANDROID_STUDIO_PREFERENCES_PATH} ]; then
  printf "Android Studio expected in ${HOME}/Library/Preferences"
  exit 1
fi

# Copy Google Styles across if necessary
mkdir -p "${ANDROID_STUDIO_PREFERENCES_PATH}/codestyles"
ANDROID_STYLE_PATH="${ANDROID_STUDIO_PREFERENCES_PATH}/codestyles/GoogleStyle.xml"
REPOSITORY_STYLE_PATH="scripts/style/GoogleStyle.xml"

printf "Checking for GoogleStyle.xml\n"
cmp -s "${ANDROID_STYLE_PATH}" "${REPOSITORY_STYLE_PATH}"
if [ $? -ne 0 ]; then
  if [ ! -f ${ANDROID_STYLE_PATH} ]; then
    printf "You need to switch to the Google code style in Android Studio. Update this setting in Preferences > Code Style. If Android Studio is currently open you may need to restart first to see the code style.\n"
  else
    printf "Updated Google code style, restart Android Studio to apply changes\n"
  fi
  cp ${REPOSITORY_STYLE_PATH} ${ANDROID_STYLE_PATH}
fi

# Install ktlint - this is only really used for style setup as we generally run ktlint from the Gradle wrapper
printf "Installing ktlint - this may require a password\n"
curl -sSLO https://github.com/shyiko/ktlint/releases/download/0.27.0/ktlint &&
  chmod a+x ktlint &&
  sudo mv ktlint /usr/local/bin/

# Use ktlint to configure Kotlin styles for Android Studio
printf "Configuring Kotlin styles for Android Studio\n"
ktlint --apply-to-idea-project --android

# Remove author header from new files
FILE_HEADER_PATH=${ANDROID_STUDIO_PREFERENCES_PATH}/fileTemplates/includes/File\ Header.java
if [ -f "$FILE_HEADER_PATH" ]; then
  printf "Removing file header template\n"
  > "${FILE_HEADER_PATH}"
fi

printf "Bootstrap complete!\n"