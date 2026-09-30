#!/usr/bin/env bash
# Pre-migration scan: what will break on JDK 25 / Gradle 9 / Boot 3+, before anything is changed.
# Usage: scripts/scan.sh [app-dir]     (JDK17_HOME / JDK25_HOME override the SDKMAN defaults)
set -uo pipefail

APP_DIR="${1:-legacy-orders-service}"
JDK17="${JDK17_HOME:-$HOME/.sdkman/candidates/java/17.0.20-tem}"
JDK25="${JDK25_HOME:-$HOME/.sdkman/candidates/java/25.0.4-tem}"

cd "$APP_DIR" || { echo "no such dir: $APP_DIR"; exit 1; }

echo "############ 1. compile on 17 ############"
JAVA_HOME="$JDK17" ./gradlew classes -q || exit 1
CLASSES=build/classes/java/main
CP=$(JAVA_HOME="$JDK17" ./gradlew -q printRuntimeClasspath 2>/dev/null | tail -1)

echo
echo "############ 2. JDK-internal API usage (jdeps) ############"
"$JDK25/bin/jdeps" --jdk-internals --multi-release 25 --ignore-missing-deps "$CLASSES" 2>&1 | head -30

echo
echo "############ 3. deprecated-for-removal API usage (jdeprscan) ############"
"$JDK25/bin/jdeprscan" --release 25 --for-removal --class-path "$CP" "$CLASSES" 2>&1 | grep -v '^error:' | head -30

echo
echo "############ 4. charset-unsafe I/O (JEP 400) ############"
grep -rnE 'new (FileReader|FileWriter|InputStreamReader|OutputStreamWriter)\([^,)]*\)|\.getBytes\(\)|readFileToString\([^,]*\)' src/main \
  --include='*.java' || echo "  none found"

echo
echo "############ 5. locale-sensitive calls ############"
grep -rnE '\.to(Upper|Lower)Case\(\)|Locale\.getDefault\(\)|new SimpleDateFormat' src/main \
  --include='*.java' || echo "  none found"

echo
echo "############ 6. javax.* -> jakarta.* candidates ############"
grep -rhoE '^import javax\.(persistence|validation|servlet|annotation|transaction)[a-z.]*' src --include='*.java' \
  | sed -E 's/^import (javax\.[a-z]+).*/\1/' | sort | uniq -c || echo "  none found"

echo
echo "############ 7. Gradle deprecations blocking Gradle 9 ############"
JAVA_HOME="$JDK17" ./gradlew help --warning-mode all 2>&1 | grep -iE 'has been deprecated|removed in Gradle 9' | sort -u | head -20

echo
echo "############ 8. configuration-cache violations ############"
JAVA_HOME="$JDK17" ./gradlew assemble stampBuild --configuration-cache 2>&1 | grep -E '^- |problems? (was|were) found' | sort -u | head -20

echo
echo "done."
