# Implementation decisions

Where the spec left room, these are the choices that were made. They are intentionally
simple and consistent with the v1 scope.

## Open items resolved

1. **Dark scheme** — finalized with the primary `#E08A6E`, surface `#2C211B`, cream text
   `#F3E6DA`, and derived container/outline roles. All text pairs pass WCAG AA; values and
   ratios are recorded in `docs/DESIGN.md`.
2. **Avatar** — initials rendered on a profile colour disc (`ProfileAvatar`). No raster or
   vector avatar assets; the `avatar` column stores `"initials"` so presets can be added
   later without a migration.
3. **Exact-alarm eligibility** — the app uses the user-granted `SCHEDULE_EXACT_ALARM` path,
   not Play's restricted `USE_EXACT_ALARM`. When denied it falls back to
   `setAndAllowWhileIdle`, shows a warning on Today and Settings, and rebuilds alarms when
   the permission changes.

## Data model

- **One repeat rule per medication.** The schema stores `daysOfWeek`/`intervalDays` per
  `ScheduleTime`, but the Add/Edit UI applies one repeat rule to all daily times (which is
  how the product describes frequency). The data layer remains per-time for future use.
- **"Every X days"** is anchored to `Medication.startDate`. Editing the start date shifts
  the interval phase.
- **Fixed duration** stores an inclusive `endDate`; the UI accepts either a number of days
  or an explicit end date and converts days to `startDate + days - 1`.
- **Auto-archive** runs whenever alarms are rebuilt (app launch, boot, time change, app
  update) and archives any non-indefinite medication whose `endDate` is before today.
  Archived medication stays visible under My Meds → Archived and in History.
- **Stock** is optional. It is decremented by `ceil(doseQty)` on Taken (so "2 tablets"
  removes 2), clamped at zero. Low-stock notifications require both `stock` and a
  `lowStockThreshold`.
- **Dose logs** are created for the exact scheduled instant. Pending logs are created when
  an alarm fires or when Today records an action; Today derives upcoming doses directly
  from schedules, so the timeline is complete even before an alarm fires.
- `schedule_times` has no unique constraint on `(medId, timeOfDay)` because the UI prevents
  duplicates and the repository replaces the whole set on save.

## Reminder behaviour

- **Re-nudge:** at alarm time a notification is posted and a one-shot re-nudge is scheduled
  (`renudgeMinutes`, default 15). When the re-nudge fires and the log is still `PENDING`,
  the dose becomes `MISSED` and the notification is re-posted with "Missed dose — tap Taken
  to log it late." Taken/Skip still work from that notification.
- **Snooze:** records `SNOOZED`, cancels the re-nudge, and schedules a one-shot alarm after
  `snoozeMinutes` (default 10). When it fires the log returns to `PENDING`, the notification
  is re-posted and a fresh re-nudge is armed, so a snoozed dose cannot loop forever.
- **Alarm identity:** each alarm is keyed by `(medId, action, scheduledAt)` and cancelled via
  a recorded key set in SharedPreferences. Rebuilding cancels everything this app scheduled,
  including snoozes and re-nudges.
- **Notification channels:** exactly two urgency channels, `dose_reminder` (high) and
  `low_stock` (default). The sound setting recreates the dose channel because Android cannot
  modify a channel's sound in place; this is the only way to honour the toggle.
- **Exact-alarm fallback:** `setAndAllowWhileIdle` when `canScheduleExactAlarms()` is false,
  with warning banners. Alarms are also rebuilt on
  `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
- **Battery-optimization guide** deep-links to the system exemption list
  (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`) rather than declaring
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, which requires a Play policy declaration. Users
  reach the same exemption toggle from the list.
- **Time storage:** times are stored as local `LocalTime`, dates as `LocalDate`, and resolved
  to instants at scheduling time using the current timezone, as required by the spec.

## Architecture

- **Repository layer enforces rules**: profile cap of 3, main profile cannot be deleted,
  cascade deletes via Room foreign keys, and alarm rebuild after every medication mutation.
- **Hilt** provides the database, DAOs and an application-scoped coroutine scope.
- **Destructive migration fallback** is used at version 1 so a schema mistake during early
  development cannot brick a test install. Replace with real migrations before release.
- **Typography** uses the system Roboto at larger sizes rather than bundling Nunito; this
  keeps the APK small and matches the spec's "e.g. Nunito or default Roboto" allowance.
- **Navigation** is Navigation Compose with four bottom-bar destinations; Profiles, Add/Edit
  and Onboarding are full-screen routes. Onboarding is the start destination until the
  DataStore flag is set.
- **No WorkManager** is used anywhere; all dose timing is `AlarmManager`.

## Known gaps / next steps

- `ScheduleCalculator` has JVM unit tests (`./gradlew :app:testDebugUnitTest`, 8 cases)
  covering daily, weekday, interval, fixed-duration and timezone-DST edge cases.
- Notification permission denial is surfaced in-app, but there is no periodic re-prompt.
- Adherence percentages and richer history filters are not implemented; the calendar shows
  taken/missed/skipped counts per day plus a selected-day list.
- Release signing, Play listing assets, and real-device OEM testing remain (build order 7).
