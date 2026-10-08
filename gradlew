#!/bin/sh
# Phone-friendly Gradle launcher for Android IDE environments.
# AndroidIDE normally provides a Gradle installation in its environment.
set -e
if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi
echo "Gradle executable was not found in PATH."
echo "In AndroidIDE, install/enable the Gradle/JDK build tools and retry."
exit 127
