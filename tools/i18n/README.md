# Localization (i18n) — OpenWrt Manager

The app UI is localized into **English (default)** and **Arabic (RTL)** only.

## Architecture

| Piece | Location | Purpose |
| --- | --- | --- |
| Default strings (English) | `app/src/main/res/values/strings.xml` | Fallback language + English UI |
| Arabic strings | `app/src/main/res/values-ar/strings.xml` | Full Arabic translation |
| Runtime helper | `app/src/main/java/.../util/Loc.kt` | Localized strings for ViewModels / repositories / services / widgets, and locale wrapping for the Activity |
| Per-app locales | `app/src/main/res/xml/locales_config.xml` | Declares `en` + `ar` (Android 13+ language picker) |

- UI strings are resolved through `Loc.s(R.string.xxx)` (works both inside
  `@Composable` functions and in non-UI code) or `stringResource(...)`.
- Format placeholders are **positional**: `%1$s`, `%2$s` …
- The in-app language switcher lives in **Profile → Settings → Language**
  (Follow system / English / العربية). Changing it persists to
  `settings_prefs` (`language` key) and recreates the activity.
- RTL is enabled globally (`android:supportsRtl="true"`); the Arabic locale
  drives `layoutDirection` automatically.

## String key namespaces

| Prefix | Origin |
| --- | --- |
| *(semantic names)* e.g. `nav_home`, `common_confirm` | Original resource keys |
| `s0001` … `s0955` | Strings extracted from Kotlin sources during localization |
| `enum_001` … `enum_095` | Enum display names |
| `lang_*`, `widget_*`, `version_*` | Feature strings (language picker, widgets, OpenWrt version support) |

## Translation reference

`tools/i18n/mapping.json` maps every generated key to its original Chinese
source (`zh`) plus the English (`en`) and Arabic (`ar`) translations. Use it
to review or re-translate a key, then update both `strings.xml` files.

## Adding a new string

1. Add `<string name="my_key">English text</string>` to `values/strings.xml`.
2. Add the Arabic translation to `values-ar/strings.xml`.
3. Reference it with `Loc.s(R.string.my_key)` (or `stringResource(R.string.my_key)` in composables).

## Supported OpenWrt firmware

`util/OpenWrtVersions.kt` defines the supported release line
**23.05.0 – 23.05.6** (the short notation `23.5.0 – 23.5.6` is accepted and
normalized automatically). The support status is surfaced in:

- System → firmware info (support badge)
- Firmware upgrade screen (support status row)
- Smart diagnostics (firmware version check against the supported range)
- Repository auto-configuration (normalized version in feed URLs)
