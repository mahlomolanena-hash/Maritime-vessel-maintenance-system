#!/usr/bin/env bash
# Walks the whole maintenance workflow against a running backend (dev profile, seeded data).
#   cd backend && mvn spring-boot:run      # in another terminal
#   bash docs/curl-examples.sh
set -euo pipefail
BASE=${BASE:-http://localhost:8080/api/v1}
json() { python3 -c "import sys,json; print(json.load(sys.stdin)$1)"; }

login() { curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
            -d "{\"email\":\"$1\",\"password\":\"$2\"}" | json "['accessToken']"; }

SUPER=$(login superintendent@example.com 'Super123!')
ENG=$(login engineer@example.com 'Engineer123!')
auth() { echo "Authorization: Bearer $1"; }

echo "== Dashboard"
curl -s "$BASE/dashboard" -H "$(auth "$SUPER")" | python3 -m json.tool

echo "== What is overdue or due soon?"
curl -s "$BASE/schedules/due" -H "$(auth "$SUPER")" | python3 -c "
import sys,json
for s in json.load(sys.stdin): print(f\"{s['dueStatus']:9} {s['equipmentName']:28} {s['taskTitle']}\")"

echo "== Turn the first due schedule into a work order and assign it"
SCHEDULE_ID=$(curl -s "$BASE/schedules/due" -H "$(auth "$SUPER")" | json "[0]['id']")
ENGINEER_ID=$(curl -s "$BASE/users" -H "$(auth "$SUPER")" | python3 -c "
import sys,json
print(next(u['id'] for u in json.load(sys.stdin) if u['email']=='engineer@example.com'))")
WO=$(curl -s -X POST "$BASE/schedules/$SCHEDULE_ID/work-orders" -H "$(auth "$SUPER")" \
      -H 'Content-Type: application/json' -d "{\"priority\":\"HIGH\",\"assignedToId\":$ENGINEER_ID}" | json "['id']")
echo "Work order id: $WO"

echo "== Engineer starts, uses a part, completes"
curl -s -X POST "$BASE/work-orders/$WO/start" -H "$(auth "$ENG")" > /dev/null
curl -s -X POST "$BASE/work-orders/$WO/parts" -H "$(auth "$ENG")" -H 'Content-Type: application/json' \
     -d '{"sparePartId":1,"quantity":1}' > /dev/null
curl -s -X POST "$BASE/work-orders/$WO/complete" -H "$(auth "$ENG")" -H 'Content-Type: application/json' \
     -d '{"completionRemarks":"Filter changed, pressures normal.","actualHours":2.5}' | python3 -m json.tool

echo "== Audit trail"
curl -s "$BASE/work-orders/$WO/history" -H "$(auth "$ENG")" | python3 -m json.tool

echo "== The schedule restarted: it is no longer overdue"
curl -s "$BASE/schedules/$SCHEDULE_ID" -H "$(auth "$SUPER")" | python3 -m json.tool

echo "== Maintenance history now contains the job"
curl -s "$BASE/maintenance-records?size=3" -H "$(auth "$SUPER")" | python3 -m json.tool
