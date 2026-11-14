# MedSafe: Android App Spec

Audience: coding agents building the app. This document is the source of truth for scope, architecture, data model, and design. Where something is unspecified, choose the simplest option consistent with this spec and note the decision.

## 1. Product summary

MedSafe is a native Android app for tracking medications and reminding users to take them. One device owner (the main profile) can add up to 2 extra profiles (e.g. family members), for 3 profiles total. Each profile has its own medications, schedules, and history. The app pushes a notification at each scheduled dose time.

**v1 is local-first.** "Main account" means the owner profile on the device. There is no login and no cloud sync. Cloud backup or caregiver sharing is a possible later phase and is out of scope now.

## 2. Out of scope for v1

- Medication strength (e.g. "500 mg"). Do not include this field in the schema, UI, or notifications. Dose is expressed only as a quantity (e.g. 2 tablets, 5 ml).
- Cloud sync, accounts, authentication
- Drug interaction checking, pharmacy integrations, barcode scanning
- Wear OS, tablet-specific layouts

## 3. Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 with a custom color scheme and shapes |
| Persistence | Room (data), DataStore (settings) |
| DI | Hilt |
| Reminders | `AlarmManager` exact alarms (`setExactAndAllowWhileIdle`). Do **not** use WorkManager for dose times; it is too imprecise. |
| Navigation | Navigation Compose |
| Min SDK | 26 (Android 8.0), target latest stable |

## 4. Features

### 4.1 Profiles
- Exactly one main profile (`isMain = true`, cannot be deleted) plus up to 2 additional profiles. Enforce the cap of 3 in the data layer and the UI.
- Each profile has: name, color, avatar.
- Switch profiles with a chip row in the top bar.
- Medications, schedules, and history are fully isolated per profile.
- Deleting an extra profile cascades to its meds, schedules, and logs, and requires confirmation.

### 4.2 Medications
Fields:
- Name (required)
- Form: tablet, capsule, liquid, injection, drops, inhaler, other
- Quantity per dose (number plus unit, e.g. 2 tablets, 5 ml)
- Pill icon: shape and colors (see 4.3)
- Frequency: 1 to 6 times per day, each with its own time of day. Optional repeat rule: every day (default), specific weekdays, or every X days.
- Duration, a required toggle:
  - **Fixed:** start date plus either number of days or an end date
  - **Ongoing:** no end date
- Optional notes (e.g. "take with food")
- Optional stock count with a low-stock alert threshold

Rules:
- Fixed-duration meds auto-archive after their end date. Archived meds stay in history but no longer schedule alarms.
- Editing a med cancels and reschedules its alarms.
- Deleting a med cancels its alarms. Offer archive as the softer alternative.

### 4.3 Pill icons
- Drawn with **Compose Canvas**. No raster or vector image assets.
- Icon = shape x color(s). Shapes: capsule, round tablet, oval tablet, half-and-half capsule (two colors), drop (liquid), plus injection and inhaler glyphs.
- User picks the shape and one or two colors from a palette plus a custom color picker.
- Build as a reusable `PillIcon(shape, color1, color2, size)` composable used in lists, cards, notifications (render to bitmap for the large icon), and the add/edit flow.

### 4.4 Reminders and notifications
- A notification fires at each scheduled time.
- Title includes the profile name so it is clear whose dose it is (e.g. "Aqif: time for Vitamin D"). Body shows quantity and any note.
- Actions: **Taken**, **Snooze** (default 10 min, configurable), **Skip**.
- Unacknowledged doses re-nudge once (default 15 min), then are logged as **missed**.
- Reschedule all alarms on: device reboot (`BOOT_COMPLETED`), timezone change, time change, and app update (`MY_PACKAGE_REPLACED`).
- Notification channels are per urgency, not per medication: `dose_reminder` (high importance) and `low_stock` (default importance).
- Notification actions must work without opening the app (use a `BroadcastReceiver`) and must write to `DoseLog`.

### 4.5 Screens
1. **Today:** timeline of doses grouped by time, with checkable cards, for the active profile
2. **My Meds:** list with pill icons and a duration badge (Ongoing / "N days left")
3. **Add/Edit Med:** stepper form (basics, pill icon, schedule, duration, extras)
4. **History:** adherence calendar showing taken / missed / skipped
5. **Profiles:** manage, switch, add (cap of 3)
6. **Settings:** theme (system / light / dark), snooze length, notification sound, battery-optimization help
7. **Onboarding:** notification permission, exact alarm permission, battery-optimization exemption guide

## 5. Android platform requirements (handle early)

- **Android 13+:** request the runtime `POST_NOTIFICATIONS` permission with a clear explainer first.
- **Android 12+:** `SCHEDULE_EXACT_ALARM` must be granted by the user via settings. `USE_EXACT_ALARM` is restricted by Play to alarm/clock-type apps, so verify eligibility before relying on it. Always provide a fallback (inexact alarm plus a visible warning) and a permission explainer screen.
- **Doze and OEM battery killers** (Xiaomi, Oppo, Vivo, Samsung, Huawei): include an onboarding step that deep-links to battery-optimization settings and explains why. This is the most common failure cause for reminder apps.
- Use a foreground-safe approach: alarms trigger a receiver that posts the notification quickly and defers any heavy work.
- Declare receivers for boot, package replaced, timezone changed, and time changed.

## 6. Data model (Room)

```
Profile(
  id, name, color, avatar, isMain
)

Medication(
  id, profileId, name, form,
  doseQty, doseUnit,
  pillShape, pillColor1, pillColor2?,
  startDate, endDate?, isIndefinite,
  stock?, lowStockThreshold?, notes?,
  archived
)

ScheduleTime(
  id, medId, timeOfDay, daysOfWeek, intervalDays?
)

DoseLog(
  id, medId, scheduledAt, status, actedAt?
)
// status: PENDING | TAKEN | SKIPPED | MISSED | SNOOZED
```

Notes:
- There is deliberately **no `strength` field**.
- Foreign keys with cascade delete: Profile -> Medication -> ScheduleTime, DoseLog.
- Store times as local `LocalTime` plus resolve to an instant at scheduling time so timezone changes behave correctly.
- Enforce "max 3 profiles" in the repository layer, not only in the UI.

## 7. Design system: Terracotta Clay

Mood: earthy, calm, soft light, easy on the eyes. Avoid pure white and pure black everywhere.

### Light scheme
| Role | Hex |
|---|---|
| Background | `#FFF8F1` |
| Surface | `#FBEBDD` |
| Primary | `#B5543A` |
| Secondary | `#D9A066` |
| On-background / on-surface (text) | `#3B2A22` |
| On-primary | `#FFF8F1` |

### Dark scheme (follows system setting by default)
| Role | Hex |
|---|---|
| Background | `#201713` |
| Surface | derive a slightly lighter warm brown (e.g. `#2C211B`) |
| Primary | lighten the terracotta for contrast (e.g. `#E08A6E`) |
| Text | a warm cream (e.g. `#F3E6DA`) |

Values marked "derive" or "e.g." are proposals. Finalize the remaining Material 3 roles (container, outline, error, tertiary, etc.) in a single `Theme.kt`, keeping text contrast at WCAG AA or better (4.5:1 for body text) and recording the final values in the repo.

### Shape and type
- Cards: 20dp corner radius, soft low-elevation shadows
- Buttons and chips: fully rounded or 16dp+
- Generous spacing and touch targets of at least 48dp (the audience includes older users)
- Typography: a friendly, highly legible sans (e.g. Nunito or the default Roboto with larger body size). Body text at least 16sp.
- Do not rely on color alone to convey dose status; pair it with an icon or label.

## 8. Build order

1. Project setup, theme, navigation, Room schema
2. Profiles plus the `PillIcon` composable
3. Add/Edit med flow with duration logic
4. Alarm scheduling, notifications, and actions, with the reboot and time-change handlers
5. Today screen and dose logging
6. History, low-stock alerts, onboarding permissions flow, polish
7. Testing on real low-end and OEM devices, then Play Store release prep

## 9. Acceptance criteria (v1)

- A med with 3 daily times fires 3 notifications at the correct times, including after a reboot.
- Taken / Snooze / Skip from the notification update `DoseLog` without opening the app.
- A fixed-duration med stops reminding and auto-archives after its end date; an ongoing med never does.
- The 4th profile cannot be created.
- Each profile sees only its own meds and history.
- No medication strength appears anywhere in the schema or UI.
- Reminders still fire on Android 12, 13, and 14+ with permissions granted, and the app degrades clearly when they are denied.

## 10. Open items for the owner

- Confirm the final dark-scheme values after contrast checking
- Decide the avatar approach (initials vs preset illustrations)
- Decide whether to apply for exact-alarm eligibility or use the user-granted permission path
