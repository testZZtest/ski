#!/usr/bin/env bash
set -euo pipefail
export PYTHONPATH=/tmp/jl
FAIL=0
REPORT_JAVA=$(mktemp)
REPORT_XML=$(mktemp)

count=0
for p in $(git ls-files 'app/src/main/java/ski/agent/*.java'); do
  count=$((count + 1))
  if ! python3 -c "import javalang; javalang.parse.parse(open('$p').read())" 2>>"$REPORT_JAVA"; then
    echo "PARSE FAIL: $p" >> "$REPORT_JAVA"
  fi
done
echo "Java files checked: $count"

for p in $(git ls-files 'app/src/main/res/**/*.xml' | grep -v -E 'values-\w+/strings\.xml$' | grep -v -E 'res/\w+/raw/'); do
  if [ -f "$p" ]; then
    if ! python3 -c "import xml.dom.minidom as m; m.parse('$p')" 2>>"$REPORT_XML"; then
      echo "XML FAIL: $p" >> "$REPORT_XML"
    fi
  fi
done

echo "=== PARSE FAILURES ==="
[ -s "$REPORT_JAVA" ] && cat "$REPORT_JAVA" || echo "(none)"
echo "=== XML FAILURES ==="
[ -s "$REPORT_XML" ] && cat "$REPORT_XML" || echo "(none)"

echo "=== REMAINING app.buddy (should be internal only) ==="
grep -rn "app\.buddy" --exclude-dir=.git . | grep -v fixtures | grep -v '/values-[^/]*/strings.xml:' | grep -v '/linux/' | grep -v '/mcp-bridge/' | grep -v '.git/' || echo "(none)"
echo "=== REMAINING buddy-release/buddy-keys/buddy.apk ==="
grep -rnE 'buddy-release|buddy-keys|buddy\.apk|app\.buddy' --exclude-dir=.git . | grep -v fixtures | grep -v 'values-[^/]*/strings' || echo "(none)"
echo "=== 'SkiSki' / doubled-word artifacts ==="
grep -rnE '\bSkiSki|\bSki{3,}|\bBuddyBuddy|\bBuddy{3,}' --exclude-dir=.git . | grep -v fixtures || echo "(none)"
echo "=== git diff whitespace/blank lines ==="
git diff --check --word-diff=porcelain 2>&1 | grep -iE 'trailing|newline|adding' | head -30 || echo "(check clean)"

rm -f "$REPORT_JAVA" "$REPORT_XML"
