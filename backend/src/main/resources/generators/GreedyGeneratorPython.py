import json, sys, random as rmd
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

shfitTypes = ["MORNING", "AFTERNOON", "NIGHT"]

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


nightShiftPrevDay = set()
for fixed in fixedAssignments:
    if fixed["shiftType"] == "NIGHT" and fixed["date"] == (start - timedelta(days=1)).isoformat():
        nightShiftPrevDay.add(str(fixed["userId"]))


def is_valid(emp, dateStr, assignedToday, shiftType, nightPrev):
    empId = str(emp["id"])
    if empId in assignedToday:
        return False
    if dateStr in leaveDays.get(empId, []):
        return False
    if shiftType == "MORNING" and empId in nightPrev:
        return False
    return True


def get_hours(e):
    return hoursWorked[str(e["id"])]


result = []
current = start

while current <= end:
    dateStr = current.isoformat()
    assignedToday = set(fixedUsers.get(dateStr, set()))
    nightShiftToday = set()
    for fixed in fixedAssignments:
        if fixed["date"] == dateStr and fixed["shiftType"] == "NIGHT":
            nightShiftToday.add(str(fixed["userId"]))

    for shiftType in shfitTypes:
        slotKey = dateStr + "_" + shiftType
        alreadyFilled = fixedCounts.get(slotKey, 0)
        remaining = staffPerShift - alreadyFilled
        for staff in range(remaining):
            candidates = []
            for emp in employees:
                if is_valid(emp, dateStr, assignedToday, shiftType, nightShiftPrevDay):
                    candidates.append(emp)

            if not candidates:
                print("Nem generalhato beosztas erre a honapra!", file=sys.stderr)
                sys.exit(1)

            dayRequests = workRequests.get(dateStr, {})
            prioritized = []
            others = []
            for e in candidates:
                if dayRequests.get(str(e["id"])) == shiftType:
                    prioritized.append(e)
                else:
                    others.append(e)

            pool = prioritized if prioritized else candidates

            minHours = min(get_hours(e) for e in pool)
            leastHours = []
            for e in pool:
                if get_hours(e) == minHours:
                    leastHours.append(e)

            selected = rmd.choice(leastHours)
            selId = str(selected["id"])

            result.append({
                "userId": selected["id"],
                "date": dateStr,
                "shiftType": shiftType
            })

            assignedToday.add(selId)
            hoursWorked[selId] += 8
            if shiftType == "NIGHT":
                nightShiftToday.add(selId)

    nightShiftPrevDay = nightShiftToday
    current += timedelta(days=1)

json.dump(result, sys.stdout)