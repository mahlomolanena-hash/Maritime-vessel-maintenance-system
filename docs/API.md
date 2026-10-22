# REST API

Base URL `http://localhost:8080`, prefix `/api/v1`. Interactive docs: `/swagger-ui.html`.

All endpoints except login and refresh need `Authorization: Bearer <accessToken>`.
List endpoints are paginated: `?page=0&size=20` (max size 100) and return
`{content, pageNumber, pageSize, totalElements, totalPages, last}`.

Errors share one shape: `{timestamp, status, error, message, fieldErrors?}`.

| Status | Meaning |
|---|---|
| 400 | Validation failed or malformed request |
| 401 | Not logged in, or bad credentials |
| 403 | Logged in but the role (or assignment) does not allow it |
| 404 | Not found |
| 409 | Conflicts with current state: duplicate, invalid workflow step, insufficient stock, still in use, or changed by someone else |

**Roles:** `ADMIN`, `SUPERINTENDENT` (together "manager"), `ENGINEER`.
"Any" means any logged-in user.

## Authentication

| Method | Path | Who | Notes |
|---|---|---|---|
| POST | `/auth/login` | public | `{email, password}` returns access and refresh tokens |
| POST | `/auth/refresh` | public | `{refreshToken}`; the old refresh token is revoked (rotation) |
| POST | `/auth/logout` | any | Revokes all refresh tokens for the user |
| GET | `/auth/me` | any | Current user |

There is no public registration; an admin creates accounts.

## Users

| Method | Path | Who |
|---|---|---|
| GET | `/users` | manager (used to pick an assignee) |
| POST | `/users` | admin |
| PATCH | `/users/{id}` | admin (roles, enabled; you cannot demote or disable yourself) |

## Vessels

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/vessels?q=&status=` | any | Search by name or IMO |
| GET | `/vessels/{id}` | any | |
| GET | `/vessels/{id}/equipment` | any | Flat list; build the tree from `parentId` |
| POST / PUT | `/vessels`, `/vessels/{id}` | manager | IMO must be 7 digits and unique |
| DELETE | `/vessels/{id}` | manager | 409 while it still has equipment |

## Equipment

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/equipment?vesselId=&q=&status=&criticality=` | any | |
| GET | `/equipment/{id}` | any | |
| POST / PUT / DELETE | `/equipment`, `/equipment/{id}` | manager | |
| GET | `/equipment/{id}/hours` | any | Hour-meter reading history |
| POST | `/equipment/{id}/hours` | any | `{meterType: RUNNING\|OPERATING, hours, readingDate?, notes?}`; meters cannot go backwards |
| GET | `/equipment/{id}/tasks` | any | |
| GET | `/equipment/{id}/schedules?dueStatus=` | any | |
| GET | `/equipment/{id}/history` | any | Completed maintenance records |

## Maintenance tasks and schedules

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/tasks?equipmentId=` | any | |
| POST / PUT / DELETE | `/tasks`, `/tasks/{id}` | manager | |
| GET | `/schedules?equipmentId=&vesselId=&dueStatus=` | any | Most urgent first |
| GET | `/schedules/due?vesselId=` | any | Only `OVERDUE` and `DUE_SOON` |
| POST / PUT / DELETE | `/schedules`, `/schedules/{id}` | manager | `intervalType`: `CALENDAR_DAYS`, `RUNNING_HOURS`, `OPERATING_HOURS` |
| POST | `/schedules/{id}/work-orders` | manager | Generates a PREVENTIVE work order; 409 if one is already open |

Schedule responses include `dueStatus` (`OK`, `DUE_SOON`, `OVERDUE`) and `daysRemaining` or
`hoursRemaining` (negative when overdue). The "due soon" window is configured by
`app.maintenance.due-soon-days` and `app.maintenance.due-soon-hours`.

## Work orders

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/work-orders?status=&priority=&type=&vesselId=&equipmentId=&assignedToId=&overdue=&q=` | any | |
| GET | `/work-orders/mine?status=` | any | The caller's assigned work |
| GET | `/work-orders/{id}` | any | Includes parts and total parts cost |
| GET | `/work-orders/{id}/history` | any | Status audit trail |
| POST | `/work-orders` | any | Only managers may set `assignedToId` |
| PUT | `/work-orders/{id}` | manager | Not allowed once completed or cancelled |
| POST | `/work-orders/{id}/assign` | manager | `{userId}`; OPEN becomes ASSIGNED, otherwise reassigns |
| POST | `/work-orders/{id}/unassign` | manager | ASSIGNED returns to OPEN |
| POST | `/work-orders/{id}/start` | assignee or manager | ASSIGNED becomes IN_PROGRESS |
| POST | `/work-orders/{id}/hold` | assignee or manager | `{notes}`; IN_PROGRESS becomes ON_HOLD |
| POST | `/work-orders/{id}/resume` | assignee or manager | ON_HOLD becomes IN_PROGRESS |
| POST | `/work-orders/{id}/complete` | assignee or manager | `{completionRemarks, actualHours?}`; writes the maintenance record |
| POST | `/work-orders/{id}/cancel` | manager | `{notes}` |
| POST | `/work-orders/{id}/parts` | assignee or manager | `{sparePartId, quantity}`; only while IN_PROGRESS; draws stock |
| DELETE | `/work-orders/{id}/parts/{lineId}` | assignee or manager | Returns the quantity to stock |

## Spare parts

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/spare-parts?q=&lowStock=` | any | `lowStock=true` returns parts at or below reorder level |
| GET | `/spare-parts/{id}` | any | |
| POST / PUT / DELETE | `/spare-parts`, `/spare-parts/{id}` | manager | Cannot delete a part used on a work order |
| PATCH | `/spare-parts/{id}/stock` | manager | `{delta, reason?}`; positive receives, negative writes off |

## Maintenance history and dashboard

| Method | Path | Who | Notes |
|---|---|---|---|
| GET | `/maintenance-records?vesselId=&equipmentId=&type=&from=&to=&q=` | any | Dates are `YYYY-MM-DD` |
| GET | `/maintenance-records/{id}` | any | |
| GET | `/dashboard` | any | Counts: vessels, equipment, work orders by status, overdue, due-soon, low stock |
