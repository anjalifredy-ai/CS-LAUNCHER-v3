#!/bin/bash
# Insert EditionMode.bindHeader into LauncherActivity after bindViews()
set -e
cd "$(dirname "$0")/.."
f="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/LauncherActivity.java"
if grep -q 'EditionMode.bindHeader' "$f"; then
  echo "Already wired"
  exit 0
fi
sed -i '/bindViews();/a\        net.kdt.pojavlaunch.edition.EditionMode.bindHeader(this);' "$f"
echo "Wired EditionMode.bindHeader into LauncherActivity"
