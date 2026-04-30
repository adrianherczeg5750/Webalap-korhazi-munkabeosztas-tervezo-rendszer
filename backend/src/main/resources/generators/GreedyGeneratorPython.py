import json, sys, random as rmd
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

shfitTypes = ["MORNING", "AFTERNOON", "NIGHT"]

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


result = []
current = start

while current <= end:
    dateStr = current.isoformat()
    assignedToday = set()
    for shiftType in shfitTypes:
        for staff in range(staffPerShift):
            candidates = []
            for emp in employees:
                if is_valid(emp, dateStr, assignedToday):
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

    current += timedelta(days=1)

json.dump(result, sys.stdout)