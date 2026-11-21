#!/usr/bin/env bash
# Fails if any Google artifact would end up in the shipped APK, i.e. appears in the kidsProdRelease
# runtime classpath without being substituted by one of our kidsmdm-stubs projects.
# Usage: kidsmdm/scripts/check-no-google.sh   (from the repo root)
set -euo pipefail

report=$(./gradlew -q :Signal-Android:dependencies --configuration kidsProdReleaseRuntimeClasspath)

pattern='(com\.google\.android\.gms|com\.google\.firebase|com\.google\.android\.play|com\.google\.android\.libraries|com\.google\.android\.datatransport|com\.android\.billingclient|androidx\.credentials:credentials-play-services-auth)'

# Lines like "com.google.firebase:firebase-messaging:25.0.1 -> project ':kidsmdm-stubs:...'" are fine.
leaks=$(grep -E "$pattern" <<<"$report" | grep -v -E -- "-> project '?:kidsmdm-stubs:" || true)

if [ -n "$leaks" ]; then
  echo "Google artifacts would ship in the release APK:" >&2
  echo "$leaks" | sed -E 's/^[ |+\\-]*//' | sort -u >&2
  echo "Add a substitution to kidsmdm/settings.gradle.kts (and a stub under kidsmdm/stubs/)." >&2
  exit 1
fi

if grep -v -E -- "-> project '?:kidsmdm-stubs:billing" <<<"$report" | grep -q -E -- "project '?:lib:billing"; then
  echo "Signal's :lib:billing (Google Play Billing) is not substituted" >&2
  exit 1
fi

echo "OK: no Google artifacts in kidsProdRelease"
