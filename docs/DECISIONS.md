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
- **Stock units:** stock is counted in the dose unit (the form field shows "Stock (tablet)" or
  "Stock (ml)"), so a 5 ml dose removes 5 from a stock counted in ml.
- **Stock** is optional. It is decremented by `ceil(doseQty)` on Taken (so "2 tablets"
  removes 2), clamped at zero. Low-stock notifications require both `stock` and a
  `lowStockThreshold`.
- **Dose logs** are created for the exact scheduled instant. Each digest creates PENDING logs
  for every dose in its twelve-hour window; Today records actions against the same rows and
  derives the timeline directly from schedules, so the timeline is complete even before a
  digest fires. A dose still PENDING 15 minutes after its scheduled time is marked MISSED at
  the next digest, app launch, reboot or clock change.
- **Default dose time** is 09:00. Adding another daily time offsets it by two hours from the
  last entry; the digest window determines whether a given time is announced in the morning
  or the evening summary.
- `schedule_times` has no unique constraint on `(medId, timeOfDay)` because the UI prevents
  duplicates and the repository replaces the whole set on save.

## Reminder behaviour

- **Two daily digests, nothing else:** exactly one morning digest at 09:00 and one evening
  digest at 21:00 local time. Each digest covers the twelve hours that follow it
  (`[09:00, 21:00)` and `[21:00, 09:00)`), so every scheduled dose belongs to exactly one
  digest. There is no per-dose notification frequency and no re-nudge.
- **Digest contents:** every active medication across every profile is scanned at fire time
  with `ScheduleCalculator.occurrencesBetween`. The notification uses one id, so the evening
  digest replaces the morning one, and lists the profile, medication, quantity, and time for
  each dose. Tapping opens Today.
- **Missed doses:** when a digest fires it first marks any PENDING dose from an earlier
  window as MISSED. On app launch, reboot and clock changes the reconciler does the same for
  doses more than 15 minutes past their scheduled time (`MISSED_GRACE_MS`), so opening the
  app does not prematurely close a dose that just came due. Repair never posts a reminder.
- **Snooze:** the only per-dose notification. Records `SNOOZED` and schedules a one-shot alarm
  after `snoozeMinutes` (default 10). When it fires the log returns to `PENDING`, and a
  single-dose notification is re-posted with Taken / Snooze / Skip actions that work with the
  app closed. Snoozed doses are excluded from digests so they are not announced twice.
- **Alarm identity:** each alarm is keyed by `(medId, action, scheduledAt)` through a unique
  intent data URI, with a recorded key set in SharedPreferences for bulk cancellation. Digest
  alarms use `medId = 0` and the `DIGEST` action. `scheduleAll()` cancels and rebuilds only the
  `DIGEST` alarm. Snooze alarms are never touched by it, so opening the app or editing a
  medication does not wipe them.
- **Reconciliation (`DoseReconciler`):** alarms vanish on reboot, app update and force-stop, so
  on app launch and after boot/update/time/timezone/exact-alarm changes the dose log is used to
  repair state. `SNOOZED` doses get their snooze re-armed at `actedAt + snooze` (or fire
  immediately if overdue). Doses that came due after the last known-healthy moment
  (`ReminderScheduler.lastAliveAt`, at most 48 h back) and have no log are logged `MISSED`;
  they are not announced, because only the two digests notify. Doses before the medication's
  `createdAt` are never counted. Existing alarms are detected with
  `PendingIntent.FLAG_NO_CREATE`, so repair is idempotent.
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
- **No destructive migration fallback.** `MedSafeMigrations.ALL` (empty at version 1) is the
  place for every future `Migration`. Opening a database with an unhandled version change fails
  loudly rather than erasing medication history. During early development, uninstall the app
  after a schema change.
- **Backup:** `allowBackup="false"`, so medication data is not copied to Google Backup.
- **Typography** uses the system Roboto at larger sizes rather than bundling Nunito; this
  keeps the APK small and matches the spec's "e.g. Nunito or default Roboto" allowance.
- **Navigation** is Navigation Compose with four bottom-bar destinations; Profiles, Add/Edit
  and Onboarding are full-screen routes. Onboarding is the start destination until the
  DataStore flag is set.
- **No WorkManager** is used anywhere; all dose timing is `AlarmManager`.

## Known gaps / next steps

- `ScheduleCalculator` and `ReconcilePlanner` have JVM unit tests
  (`./gradlew :app:testDebugUnitTest`). Repositories, the scheduler and the receivers have no
  automated tests yet; they need instrumented tests on a device or emulator.
- Force-stopping the app cancels every alarm until the user opens MedSafe again. Android does
  not allow an app to prevent this; onboarding and Settings warn about it and the reconciler
  repairs state on the next launch.
- Notification permission denial is surfaced in-app, but there is no periodic re-prompt.
- Adherence percentages and richer history filters are not implemented; the calendar shows
  taken/missed/skipped counts per day plus a selected-day list.
- Release builds enable R8 minify and resource shrinking and sign with `keystore.properties`
  (git-ignored; keys `storeFile`, `storePassword`, `keyAlias`, `keyPassword`) when present.
  Run a release build on a device before publishing, since minified builds are untested here.
- Play listing assets and real-device OEM testing remain (build order 7).
