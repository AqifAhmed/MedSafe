# MedSafe

MedSafe is a local-first Android app for tracking medications and reminding you to take
them. One device owner can add up to three profiles — each with isolated medications,
schedules, history and reminders.

This repository implements the v1 scope in [`MEDSAFE_SPEC.md`](MEDSAFE_SPEC.md).

## Status

The complete v1 flow is implemented and the project builds (`assembleDebug`):

- **Profiles** — main profile plus up to two more, enforced in the repository layer; switch
  with the chip row on Today; cascade delete with confirmation; main profile cannot be deleted.
- **Medications** — required name, form, quantity + unit (never strength), per-dose notes,
  optional stock and low-stock threshold.
- **Pill icons** — drawn with `Canvas` (`PillPainter`) with seven shapes, one or two colours,
  palette + RGB custom picker, and no image assets. The same painter renders the notification
  large icon.
- **Scheduling** — 1–6 daily times (default 09:00), every day / chosen weekdays / every N
  days, ongoing or fixed duration with automatic archival. Times are summarised by the two
  daily digests rather than notifying individually.
- **Reminders** — exactly two consolidated digest notifications per day, at 09:00 and 21:00
  local time. The morning digest covers 09:00–21:00; the evening digest covers 21:00–09:00.
  Doses from every profile are grouped into one notification, with one notification id so the
  evening digest replaces the morning one. Taken / Snooze / Skip live on Today; Snooze is the
  only per-dose notification. Unanswered doses are marked missed at the next digest. Exact
  `AlarmManager` alarms (`setExactAndAllowWhileIdle` with an inexact fallback and visible
  warning) rebuild on boot, time/timezone change, app update and exact-alarm permission
  changes, and snoozes and doses missed while the phone was off are repaired from the dose log
  (`DoseReconciler`).
- **Screens** — Today timeline, My Meds, Add/Edit stepper, adherence History calendar,
  Profiles, Settings and permission onboarding.
- **Design** — Terracotta Clay Material 3 theme with light/dark schemes and WCAG AA contrast.

## Build

Requirements: JDK 17, Android SDK platform 35 and build-tools 35.0.0.

```bash
# Point at your SDK (or create local.properties with sdk.dir=...)
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :app:assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

```bash
./gradlew :app:testDebugUnitTest   # JVM tests for schedule maths
./gradlew :app:lintDebug           # Android lint (0 errors)
./gradlew :app:installDebug        # install on a connected device/emulator
```

## Tests to run on a device

The acceptance criteria in the spec are hardware flows; verify them on a real phone,
ideally a low-end OEM device:

1. Create a medication with 3 daily times; confirm it appears as three entries in the
   correct digest and that only one morning and one evening notification are posted (also
   after rebooting).
2. Use Taken / Snooze / Skip from Today, then confirm a snoozed dose re-notifies once after
   the snooze delay and its actions work with the app closed.
3. Create a fixed-duration medication ending today; confirm it stops appearing in digests and
   archives the next day.
4. Try to create a fourth profile; the button is disabled and the repository rejects it.
5. Switch profiles and confirm meds/history are isolated.
6. Deny notification and exact-alarm permissions; confirm the in-app warning banners and
   inexact fallback.

## Structure

```
app/src/main/java/com/medsafe/app/
├── alarm/          Alarm scheduling, receivers, notification helper, dose actions
├── data/
│   ├── local/      Room database, entities, DAOs, type converters
│   ├── repository/ Profile/Medication repositories (business rules live here)
│   ├── settings/   DataStore-backed app settings and active profile
│   └── model/      Enums shared by data and UI
├── di/             Hilt modules and qualifiers
├── ui/
│   ├── components/ PillIcon/PillPainter and shared clay components
│   ├── navigation/ Bottom-bar routes and NavHost
│   ├── screens/    Today, My Meds, Add/Edit, History, Profiles, Settings, Onboarding
│   └── theme/      Terracotta Clay colour scheme, type, shapes
└── util/           Formatting, date helpers, permission helpers
```

See [`docs/DESIGN.md`](docs/DESIGN.md) for the final colour values and contrast checks, and
[`docs/DECISIONS.md`](docs/DECISIONS.md) for the choices made where the spec left room.
