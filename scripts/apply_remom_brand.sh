#!/bin/bash
# Replace CS/Nexus visible branding with RemomLauncher in strings.xml
set -e
cd "$(dirname "$0")/.."
f="app_pojavlauncher/src/main/res/values/strings.xml"
[ -f "$f" ] || { echo "strings.xml not found"; exit 1; }
sed -i \
  -e 's/Nexus Launcher CS/RemomLauncher/g' \
  -e 's/Nexus Launcher/RemomLauncher/g' \
  -e 's/CS Launcher V3\.1/RemomLauncher/g' \
  -e 's/CS Launcher V3/RemomLauncher/g' \
  -e 's/CS Launcher/RemomLauncher/g' \
  "$f"
echo "OK: Remom branding applied to $f"
echo "Accounts / play / mods engine code was NOT removed."
