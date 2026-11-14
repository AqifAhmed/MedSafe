# Terracotta Clay — final design tokens

These values are implemented in `ui/theme/Color.kt` and `ui/theme/Theme.kt`. They were
finalized by checking WCAG contrast for the text pairs that appear in the UI. All checks
below are at least 4.5:1 (AA for body text); most are far higher.

## Light scheme

| Role | Hex | Notes |
|---|---|---|
| background | `#FFF8F1` | spec |
| onBackground | `#3B2A22` | spec |
| surface | `#FBEBDD` | spec |
| onSurface | `#3B2A22` | spec |
| surfaceVariant | `#F1DECB` | derived |
| onSurfaceVariant | `#6B5448` | derived, secondary text |
| primary | `#B5543A` | spec |
| onPrimary | `#FFF8F1` | spec |
| primaryContainer | `#F5D9CE` | derived |
| onPrimaryContainer | `#4A2118` | derived |
| secondary | `#D9A066` | spec |
| onSecondary | `#3A2614` | derived |
| secondaryContainer | `#F3E2C8` | derived |
| onSecondaryContainer | `#4A3418` | derived |
| tertiary | `#6F7F55` | muted sage (derived) |
| onTertiary | `#FFF8F1` | derived |
| tertiaryContainer | `#DDE5C6` | derived |
| onTertiaryContainer | `#2B3417` | derived |
| error | `#A53F32` | warm error, derived |
| onError | `#FFFFFF` | |
| errorContainer | `#FFDAD4` | |
| onErrorContainer | `#410002` | |
| outline | `#A08A7B` | derived |
| outlineVariant | `#DDCBBE` | derived |
| surfaceContainerLowest | `#FFFCF8` | |
| surfaceContainerLow | `#FDF1E7` | |
| surfaceContainer | `#F7E7D9` | |
| surfaceContainerHigh | `#F2E0D0` | |
| surfaceContainerHighest | `#ECD9C7` | |
| inverseSurface | `#3B2A22` | |
| inverseOnSurface | `#FFF8F1` | |
| inversePrimary | `#E08A6E` | |

### Contrast checks (light)

| Pair | Ratio |
|---|---|
| onBackground / background (`#3B2A22` / `#FFF8F1`) | 12.95 |
| onSurface / surface (`#3B2A22` / `#FBEBDD`) | 11.71 |
| primary text / background (`#B5543A` / `#FFF8F1`) | 4.65 |
| onPrimary / primary (`#FFF8F1` / `#B5543A`) | 4.65 |
| onPrimaryContainer / primaryContainer | 10.32 |
| onSurfaceVariant / surface | 6.04 |
| onSurfaceVariant / background | 6.68 |
| onSecondaryContainer / secondaryContainer | 9.20 |
| error / background | 5.94 |
| onError / error | 6.26 |
| onTertiaryContainer / tertiaryContainer | 10.02 |
| onErrorContainer / errorContainer | 13.24 |

## Dark scheme

| Role | Hex | Notes |
|---|---|---|
| background | `#201713` | spec |
| onBackground | `#F3E6DA` | spec (warm cream) |
| surface | `#2C211B` | spec proposal |
| onSurface | `#F3E6DA` | spec |
| surfaceVariant | `#4A392F` | derived |
| onSurfaceVariant | `#D8C3B4` | derived |
| primary | `#E08A6E` | spec, lightened terracotta |
| onPrimary | `#45231B` | derived |
| primaryContainer | `#6B3527` | derived |
| onPrimaryContainer | `#FFDBCF` | derived |
| secondary | `#E8C08A` | derived |
| onSecondary | `#3F2D16` | derived |
| secondaryContainer | `#553D25` | derived |
| onSecondaryContainer | `#FFE1BC` | derived |
| tertiary | `#B9C99A` | sage, dark variant |
| onTertiary | `#263016` | |
| tertiaryContainer | `#3C4A29` | |
| onTertiaryContainer | `#D5E5B7` | |
| error | `#FFB4A8` | |
| onError | `#690005` | |
| errorContainer | `#93000A` | |
| onErrorContainer | `#FFDAD6` | |
| outline | `#A08A7B` | |
| outlineVariant | `#55443A` | |
| surfaceContainerLowest | `#1A120E` | |
| surfaceContainerLow | `#271C17` | |
| surfaceContainer | `#33261F` | |
| surfaceContainerHigh | `#3E3028` | |
| surfaceContainerHighest | `#4A392F` | |
| inverseSurface | `#F3E6DA` | |
| inverseOnSurface | `#33261F` | |
| inversePrimary | `#B5543A` | |

### Contrast checks (dark)

| Pair | Ratio |
|---|---|
| onBackground / background (`#F3E6DA` / `#201713`) | 14.36 |
| onSurface / surface (`#F3E6DA` / `#2C211B`) | 12.79 |
| primary text / background (`#E08A6E` / `#201713`) | 6.73 |
| onPrimary / primary (`#45231B` / `#E08A6E`) | 5.32 |
| onPrimaryContainer / primaryContainer | 7.53 |
| onSurfaceVariant / surface | 9.24 |
| onSecondaryContainer / secondaryContainer | 8.05 |
| error / background | 10.35 |
| onTertiaryContainer / tertiaryContainer | 7.13 |
| onErrorContainer / errorContainer | 7.24 |

## Shape and type

- Cards: 20dp radius (`shapes.large`), 1dp soft elevation, surface background.
- Buttons and chips: 16dp+ radius; all actionable rows have a 48dp minimum touch target.
- Type: system Roboto (no bundled font) with body sizes raised to 16sp, title 17–21sp,
  headings 22–32sp, and a 24sp line height on body copy. This keeps the app legible for
  older users without adding font assets.
- Status is never conveyed by colour alone: every dose state pairs a container colour with
  an icon and a text label (`DoseStatusBadge`).

## Pill palette

`PillPalette` in `Color.kt`: terracotta, sand, cream, sage, blue-grey, lavender, rose,
teal, amber, berry, cocoa, chalk. Glyphs receive a dark translucent outline so light
colours remain visible on the light background.
