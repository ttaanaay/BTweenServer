# BTween

A personal quote-collecting Android app, built natively in Kotlin with Jetpack Compose.

## Tech Stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean layering (data / domain / ui) |
| DI | Hilt |
| Local storage | Room (quotes, categories) + DataStore Preferences (settings) |
| Navigation | Navigation-Compose, single-Activity |
| Async | Kotlin Coroutines + Flow |
| Image loading | Coil |
| Image export ("share as image") | Compose `graphicsLayer`/`ImageBitmap` capture → FileProvider |
| Min SDK | 24 (Android 7.0) |
| Target/Compile SDK | 35 |

## Module layout

```
app/src/main/java/com/btween/app/
├── data/
│   ├── local/
│   │   ├── entity/       Room @Entity classes
│   │   ├── dao/          Room @Dao interfaces
│   │   ├── database/      AppDatabase, seed data
│   │   └── converter/      Room TypeConverters (tags list, dates)
│   ├── preferences/       DataStore-backed settings source
│   ├── backup/            JSON export/import (backup & restore)
│   └── repository/        Repository implementations
├── domain/
│   ├── model/             Plain domain models used by UI/ViewModels
│   ├── repository/        Repository interfaces (contracts)
│   └── usecase/           Single-purpose use cases
├── di/                     Hilt modules
├── ui/
│   ├── theme/              Color, Type, Shape, Theme
│   ├── navigation/         NavHost + destinations
│   ├── components/         Shared composables (QuoteCard, EmptyState, etc.)
│   ├── home/
│   ├── library/
│   ├── addedit/
│   ├── detail/
│   ├── search/
│   ├── favorites/
│   ├── categories/
│   └── settings/
└── util/                   Small shared helpers (share, image export, formatting)
```

## Build

```
./gradlew assembleDebug
```

Requires JDK 17 and Android SDK 35. No API keys or secrets are required — the app is
100% offline/local-storage.

## Development phases

This project is being generated in reviewable phases:

1. **Project foundation** *(this phase)* — Gradle setup, manifest, theming, launcher icon.
2. Data layer — Room entities/DAOs/database, DataStore preferences, repositories.
3. Domain layer — models, repository interfaces, use cases, Hilt modules.
4. Navigation shell — bottom navigation, NavHost, MainActivity wiring.
5. Home screen.
6. Quote Library (list/grid, sort, filter).
7. Add/Edit Quote screen.
8. Quote Detail (copy/share text/share image/edit/delete).
9. Search.
10. Categories management.
11. Favorites.
12. Settings (theme, backup/restore, about).
13. Final polish, README updates, verification pass.

## License

Personal project — all rights reserved by the app owner.
