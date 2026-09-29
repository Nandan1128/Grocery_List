# 🛒 Grocery List App

A lightweight, dead-simple, 100% offline native Android shopping list application built with Kotlin, Room SQLite, and MVVM architecture. Designed specifically for reliable, zero-friction usage in supermarket aisles with patchy or nonexistent cellular signal.

---

## 📌 Project Overview

When grocery shopping, users often deal with poor connectivity, crowded aisles, and the inconvenience of using both hands while steering a shopping cart. **Grocery List** solves this by providing:

* **100% Offline Capability**: Zero network permissions declared in [`AndroidManifest.xml`](file:///D:/GroceryList/app/src/main/AndroidManifest.xml). Operates completely independently of cellular data or Wi-Fi.
* **Rock-Solid Persistence**: Local SQLite persistence powered by AndroidX Room guarantees list data survives app restarts, device reboots, and process death.
* **Ergonomic One-Handed UI**: Bottom-docked input field and action controls allow effortless one-handed thumb entry while on the move.
* **High Device Compatibility**: Configured with `minSdk = 24` (Android 7.0 Nougat) through `targetSdk = 37`, covering >98.5% of active Android devices globally without third-party UI dependencies.

---

## ✨ Features

### 🛒 Core Shopping Actions
* **Quick Item Addition**: Enter item names via the bottom input bar or press **Done / Enter** on the soft keyboard.
* **One-Tap Mark as Bought**: Tap any item card or checkbox to toggle its completed state.
  * **Unbought Items**: Pinned to the top of the list with bold text and clean styling.
  * **Bought Items**: Automatically move to the lower section with strikethrough typography and subtle muted tinting.
* **Instant Removal with Undo**: Tap the delete icon on any item for immediate removal, accompanied by a non-blocking [`Snackbar`](file:///D:/GroceryList/app/src/main/java/com/example/grocerylist/ui/MainActivity.kt#L213-L227) featuring an **Undo** button.
* **Bulk "Delete All"**: Clear the entire grocery list with one tap from the top bar, guarded by a [`MaterialAlertDialogBuilder`](file:///D:/GroceryList/app/src/main/java/com/example/grocerylist/ui/MainActivity.kt#L146-L159) confirmation dialog to prevent accidental data loss.

### 🧠 Smart UX & Defensiveness
* **Continuous Serial Numbering**: Automatically displays sequential numbering (`1.`, `2.`, `3.`) that recalculates dynamically as items are checked, deleted, or restored.
* **Intelligent Duplicate Handling & Reactivation**:
  * If an unbought item already exists: Notifies the user that the item is already on the list.
  * If the item was previously checked off: Automatically reactivates it, unchecks it, refreshes its timestamp, and brings it back to the top of active items.
* **Jump-Free List Interactions**: Custom [`LinearLayoutManager`](file:///D:/GroceryList/app/src/main/java/com/example/grocerylist/ui/MainActivity.kt#L98-L109) and suppressed item animations prevent sudden auto-scrolling or visual jerking when items move between sections.
* **Input Sanitization**: Rejects blank / whitespace-only entries and safely caps long text at 200 characters to prevent layout distortion.
* **Rapid Tap Debouncing**: [`throttleClick`](file:///D:/GroceryList/app/src/main/java/com/example/grocerylist/util/ViewExtensions.kt) prevents multi-tap races on buttons and checkboxes.
* **Edge-to-Edge & Keyboard Handling**: Supports `enableEdgeToEdge()` and WindowInsets to ensure the bottom dock cleanly elevates above the virtual keyboard without clipping.

---

## 🏗️ Architecture & Tech Stack

The project adheres to Google's recommended Modern Android Architecture (MVVM + Repository Pattern + Unidirectional Data Flow):

```
┌────────────────────────────────────────────────────────┐
│             UI Layer (Activity / ViewBinding)          │
│   MainActivity ──> GroceryListAdapter (DiffUtil)       │
└───────────────────────────▲────────────────────────────┘
                            │ observes StateFlow
┌───────────────────────────┴────────────────────────────┐
│                       ViewModel                        │
│    GroceryViewModel (Input validation, StateFlow)      │
└───────────────────────────▲────────────────────────────┘
                            │ coroutines / flows
┌───────────────────────────┴────────────────────────────┐
│                   Repository Layer                     │
│                 GroceryRepositoryImpl                  │
└───────────────────────────▲────────────────────────────┘
                            │ Dispatchers.IO
┌───────────────────────────┴────────────────────────────┐
│                    Data / Room Layer                   │
│       GroceryDao ──> GroceryDatabase ──> SQLite        │
└────────────────────────────────────────────────────────┘
```

| Layer | Technologies / Components |
| :--- | :--- |
| **Language** | Kotlin 2.0.21 |
| **Architecture** | MVVM + Repository Pattern + Unidirectional Data Flow (UDF) |
| **Reactive Streams** | Kotlin Coroutines (`viewModelScope`, `Dispatchers.IO`) & Kotlin Flow (`StateFlow`, `combine`) |
| **Local Persistence** | AndroidX Room 2.8.5 with SQLite & Google KSP (Kotlin Symbol Processing) |
| **View Layer** | Native XML Views, AndroidX ViewBinding, Material Components (`Theme.MaterialComponents.DayNight.NoActionBar`) |
| **Build System** | Gradle 9.4.1 + Android Gradle Plugin 9.2.1 + Version Catalog (`libs.versions.toml`) |

---

## 📁 Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/grocerylist/
│   ├── data/
│   │   ├── local/
│   │   │   ├── GroceryItem.kt          # Room SQLite Entity
│   │   │   ├── GroceryDao.kt           # Room Data Access Object (Queries, Flows)
│   │   │   └── GroceryDatabase.kt      # Room Database Singleton
│   │   └── repository/
│   │       └── GroceryRepository.kt    # Repository interface & implementation
│   ├── ui/
│   │   ├── GroceryUiState.kt           # Immutable UI state model
│   │   ├── GroceryViewModel.kt         # Business logic, validation, StateFlow
│   │   ├── GroceryListAdapter.kt       # RecyclerView ListAdapter with DiffUtil
│   │   └── MainActivity.kt             # View orchestration, WindowInsets, Dialogs
│   └── util/
│       └── ViewExtensions.kt           # Click debouncing helper
└── res/
    ├── drawable/                       # Vector icons and shape drawables
    ├── layout/
    │   ├── activity_main.xml           # Header, list, empty state, bottom dock
    │   └── item_grocery.xml            # Grocery card item row
    └── values/                         # Colors, strings, and app themes
```

---

## 🚀 Building & Running

### Prerequisites
* **Android Studio**: Koala, Ladybug, Meerkat, or newer.
* **JDK**: Version 17 or 21 configured in your Gradle toolchains.
* **Android SDK**: Build tools and platform for SDK 37 (API 24 minimum).

### Command Line Build
To compile the debug APK:
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## 📋 Complete Turnkey Replication Plan

For step-by-step instructions on reproducing this exact application in a new project from scratch, refer to the [implementation-plan.md](file:///D:/GroceryList/implementation-plan.md) file.