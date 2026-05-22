import json, sys
from datetime import date, timedelta

data = json.load(sys.stdin)

month = data["month"]
staffPerShift = data["staffPerShift"]
employees = data["employees"]
leaveDays = data["leaveDays"]
workRequests = data["workRequests"]
fixedAssignments = data.get("fixedAssignments", [])

start = date.fromisoformat(data["startDate"])
end = date.fromisoformat(data["endDate"])

shiftTypes = ["MORNING", "AFTERNOON", "NIGHT"]

preHours = data.get("hoursWorked", {})
hoursWorked = {}
for emp in employees:
    hoursWorked[str(emp["id"])] = preHours.get(str(emp["id"]), 0)

fixedCounts = {}
fixedUsers = {}
for fixed in fixedAssignments:
    key = fixed["date"] + "_" + fixed["shiftType"]
    fixedCounts[key] = fixedCounts.get(key, 0) + 1
    fixedUsers.setdefault(fixed["date"], set()).add(str(fixed["userId"]))
    hoursWorked[str(fixed["userId"])] = hoursWorked.get(str(fixed["userId"]), 0) + 8

nightShiftDays = {}
for fixed in fixedAssignments:
    if fixed["shiftType"] == "NIGHT":
        nightShiftDays.setdefault(fixed["date"], set()).add(str(fixed["userId"]))

slots = []
current = start
while current <= end:
    dateStr = current.isoformat()
    for shiftType in shiftTypes:
        slotKey = dateStr + "_" + shiftType
        alreadyFilled = fixedCounts.get(slotKey, 0)
        remaining = staffPerShift - alreadyFilled
        for i in range(remaining):
            slots.append((dateStr, shiftType))
    current += timedelta(days=1)


def is_valid(emp, dateStr, assignedToday, shiftType):
    empId = str(emp["id"])
    if empId in assignedToday:
        return False
    if dateStr in leaveDays.get(empId, []):
        return False
    if shiftType == "MORNING":
        prevDay = (date.fromisoformat(dateStr) - timedelta(days=1)).isoformat()
        if empId in nightShiftDays.get(prevDay, set()):
            return False
    return True

def get_hours(e):
    return hoursWorked[str(e["id"])]

def get_candidates(dateStr, shiftType, assignedToday):
    valid = []
    for e in employees:
        if is_valid(e, dateStr, assignedToday, shiftType):
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
        if shiftType == "NIGHT":
            nightShiftDays.setdefault(dateStr, set()).add(empId)

        if solve(slotIndex + 1, result, assignedPerDay):
            return True

        result.pop()
        assignedToday.remove(empId)
        hoursWorked[empId] -= 8
        if shiftType == "NIGHT":
            nightShiftDays.get(dateStr, set()).discard(empId)

    return False


result = []
assignedPerDay = {}
for dateKey, userIds in fixedUsers.items():
    assignedPerDay[dateKey] = set(userIds)

sys.setrecursionlimit(10000)

if not solve(0, result, assignedPerDay):
    print("Nem generalhato beosztas erre a honapra!", file=sys.stderr)
    sys.exit(1)

json.dump(result, sys.stdout)