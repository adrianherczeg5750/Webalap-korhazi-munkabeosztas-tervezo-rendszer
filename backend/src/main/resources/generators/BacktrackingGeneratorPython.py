import json, sys
from datetime import date, timedelta

data = json.load(sys.stdin)

month = data["month"]
staffPerShift = data["staffPerShift"]
employees = data["employees"]
leaveDays = data["leaveDays"]
workRequests = data["workRequests"]

year, mon = map(int, month.split("-"))
start = date(year, mon, 1)

if mon == 12:
    end = date(year + 1, 1, 1) - timedelta(days=1)
else:
    end = date(year, mon + 1, 1) - timedelta(days=1)

shiftTypes = ["MORNING", "AFTERNOON", "NIGHT"]

slots = []
current = start
while current <= end:
    for shiftType in shiftTypes:
        for i in range(staffPerShift):
            slots.append((current.isoformat(), shiftType))
    current += timedelta(days=1)

hoursWorked = {}
for emp in employees:
    hoursWorked[str(emp["id"])] = 0


def is_valid(emp, dateStr, assignedToday):
    empId = str(emp["id"])
    if empId in assignedToday:
        return False
    if dateStr in leaveDays.get(empId, []):
        return False
    return True

def get_hours(e):
    return hoursWorked[str(e["id"])]

def get_candidates(dateStr, shiftType, assignedToday):
    valid = []
    for e in employees:
        if is_valid(e, dateStr, assignedToday):
            valid.append(e)

    valid.sort(key=get_hours)

    dayRequests = workRequests.get(dateStr, {})
    prioritized = []
    others = []
    for e in valid:
        if dayRequests.get(str(e["id"])) == shiftType:
            prioritized.append(e)
        else:
            others.append(e)

    return prioritized + others


def solve(slotIndex, result, assignedPerDay):
    if slotIndex == len(slots):
        return True

    dateStr, shiftType = slots[slotIndex]
    assignedToday = assignedPerDay.get(dateStr, set())

    candidates = get_candidates(dateStr, shiftType, assignedToday)

    for emp in candidates:
        empId = str(emp["id"])

        result.append({"userId": emp["id"], "date": dateStr, "shiftType": shiftType})
        assignedToday.add(empId)
        assignedPerDay[dateStr] = assignedToday
        hoursWorked[empId] += 8

        if solve(slotIndex + 1, result, assignedPerDay):
            return True

        result.pop()
        assignedToday.remove(empId)
        hoursWorked[empId] -= 8

    return False


result = []
assignedPerDay = {}

sys.setrecursionlimit(10000)

if not solve(0, result, assignedPerDay):
    print("Nem generalhato beosztas erre a honapra!", file=sys.stderr)
    sys.exit(1)

json.dump(result, sys.stdout)