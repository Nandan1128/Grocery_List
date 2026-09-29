# Master Implementation Plan: Native Android Grocery List App

A comprehensive, turnkey implementation specification to recreate the 100% offline, highly resilient, zero-friction Grocery List Android application from scratch in a new project.

---

## 1. Executive Summary & Design Principles

### 1.1 Problem Statement & Solution
Shoppers frequently walk supermarket aisles with spotty or nonexistent cellular signal. They need a dead-simple, blazing-fast app to jot down groceries before leaving home, tick them off sequentially in the store, and retain their data forever across app launches without relying on network sync or cloud backends.

### 1.2 Core Architectural Principles
* **100% Offline by Design**: Zero internet permissions declared in `AndroidManifest.xml`. No analytics, no remote dependencies, no external network requests.
* **Backward Compatibility**: `minSdk = 24` (Android 7.0 Nougat, covers ~10-year-old devices and >98% of active Android devices globally) up to `targetSdk = 37`.
* **Zero Third-Party UI Bloat**: Pure native Android ViewBinding, XML layouts, Material Components, and AndroidX libraries.
* **Predictable Reactive Dataflow (MVVM + UDF)**: Unidirectional Data Flow via Room SQLite Database -> Kotlin Coroutines Flow -> ViewModel `StateFlow` -> UI `repeatOnLifecycle`.
* **Ergonomic Shopping UX**:
  * **Bottom-Docked Input Bar**: Designed for one-handed thumb interaction while pushing a grocery cart.
  * **Dynamic Sequential Numbering (`1.`, `2.`, `3.`)**: Automatically renumbers visible items as items are completed or deleted.
  * **Zero Jitter / Scroll Jump Prevention**: Custom `LinearLayoutManager` suppresses child focus scroll-jumping when items change state, and item animators are tuned for instant responsiveness.
  * **Defensive Edge-Case Handling**: Blank input rejection, 200-character length capping, case-insensitive duplicate handling (reactivating previously bought items), and rapid click debouncing (`throttleClick`).

---

## 2. Project Directory Tree

When complete, the target project structure will look as follows:

```
GroceryList/
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/example/grocerylist/
        │   │   ├── data/
        │   │   │   ├── local/
        │   │   │   │   ├── GroceryItem.kt
        │   │   │   │   ├── GroceryDao.kt
        │   │   │   │   └── GroceryDatabase.kt
        │   │   │   └── repository/
        │   │   │       └── GroceryRepository.kt
        │   │   ├── ui/
        │   │   │   ├── GroceryUiState.kt
        │   │   │   ├── GroceryViewModel.kt
        │   │   │   ├── GroceryListAdapter.kt
        │   │   │   └── MainActivity.kt
        │   │   └── util/
        │   │       └── ViewExtensions.kt
        │   └── res/
        │       ├── drawable/
        │       │   ├── bg_add_button.xml
        │       │   ├── bg_input_bar.xml
        │       │   ├── bg_input_field.xml
        │       │   ├── bg_item_card.xml
        │       │   ├── bg_item_card_bought.xml
        │       │   ├── bg_top_bar.xml
        │       │   ├── ic_add.xml
        │       │   ├── ic_delete.xml
        │       │   └── ic_shopping_basket.xml
        │       ├── layout/
        │       │   ├── activity_main.xml
        │       │   └── item_grocery.xml
        │       ├── values/
        │       │   ├── colors.xml
        │       │   ├── strings.xml
        │       │   └── themes.xml
        │       ├── values-night/
        │       │   └── themes.xml
        │       └── xml/
        │           ├── backup_rules.xml
        │           └── data_extraction_rules.xml
        └── test/
            └── java/com/example/grocerylist/
                └── ExampleUnitTest.kt
```

---

## 3. Step 1: Build Configuration & Dependencies

### 3.1 Version Catalog: `gradle/libs.versions.toml`
Create `gradle/libs.versions.toml` to manage all plugins and library coordinates in a central place:

```toml
[versions]
agp = "9.2.1"
coreKtx = "1.19.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
appcompat = "1.7.1"
kotlinxCoroutinesAndroid = "1.8.0"
lifecycleRuntimeKtx = "2.7.0"
lifecycleViewmodelKtx = "2.11.0"
material = "1.14.0"
activityKtx = "1.13.0"
constraintlayout = "2.2.1"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"
room = "2.8.5"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-ktx = { module = "androidx.lifecycle:lifecycle-viewmodel-ktx", version.ref = "lifecycleViewmodelKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinxCoroutinesAndroid" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-activity-ktx = { group = "androidx.activity", name = "activity-ktx", version.ref = "activityKtx" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

### 3.2 Root `build.gradle.kts`
```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
}
```

### 3.3 Root `settings.gradle.kts`
```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Grocery List"
include(":app")
```

### 3.4 Root `gradle.properties`
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
kotlin.code.style=official
android.disallowKotlinSourceSets=false
```

### 3.5 Module `app/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.grocerylist"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.grocerylist"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
```

---

## 4. Step 2: Manifest & System Configuration

### 4.1 `app/src/main/AndroidManifest.xml`
> [!IMPORTANT]
> Note that `android.permission.INTERNET` is strictly omitted. The activity uses `android:windowSoftInputMode="adjustResize"` to ensure the bottom input dock rises gracefully above the virtual keyboard without clipping or layout breaking.

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.GroceryList">
        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### 4.2 Backup Rules: `app/src/main/res/xml/backup_rules.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
</full-backup-content>
```

### 4.3 Data Extraction Rules: `app/src/main/res/xml/data_extraction_rules.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
    </cloud-backup>
</data-extraction-rules>
```

---

## 5. Step 3: Design Tokens & Resources

### 5.1 Color Palette: `app/src/main/res/values/colors.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Brand / Primary (Clean Grocery Market Green) -->
    <color name="primary">#1B5E20</color>
    <color name="primary_variant">#003300</color>
    <color name="primary_light">#E8F5E9</color>
    <color name="on_primary">#FFFFFF</color>

    <!-- Backgrounds & Surfaces -->
    <color name="background">#F8F9FA</color>
    <color name="surface">#FFFFFF</color>
    <color name="surface_variant">#F1F3F4</color>
    <color name="divider">#E0E0E0</color>

    <!-- Typography -->
    <color name="text_primary">#212121</color>
    <color name="text_secondary">#757575</color>
    <color name="text_strikethrough">#9E9E9E</color>

    <!-- Actions & States -->
    <color name="action_delete">#E53935</color>
    <color name="action_check">#2E7D32</color>
</resources>
```

### 5.2 String Catalog: `app/src/main/res/values/strings.xml`
```xml
<resources>
    <string name="app_name">Grocery List</string>
    <string name="add_item_hint">Add an item (e.g. Milk, Apples)…</string>
    <string name="add_button_content_description">Add item to list</string>
    <string name="delete_button_content_description">Remove item</string>
    <string name="item_checkbox_content_description">Mark as bought or not bought</string>
    <string name="empty_list_title">Your grocery list is empty</string>
    <string name="empty_list_subtitle">Add items before leaving for the store!</string>
    <string name="items_remaining">%1$d items remaining</string>
    <string name="item_remaining_single">1 item remaining</string>
    <string name="all_items_bought">All items bought! 🎉</string>
    <string name="error_empty_item">Please enter an item name</string>
    <string name="item_deleted">Item removed</string>
    <string name="undo">Undo</string>
    <string name="delete_all">Delete all items</string>
    <string name="delete_all_confirmation_title">Delete all items?</string>
    <string name="delete_all_confirmation_message">Are you sure you want to delete all items from your grocery list? This action cannot be undone.</string>
    <string name="delete_all_confirm">Delete All</string>
    <string name="cancel">Cancel</string>
    <string name="all_items_deleted">All items deleted</string>
</resources>
```

### 5.3 Themes
#### `app/src/main/res/values/themes.xml`
```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base application theme. -->
    <style name="Theme.GroceryList" parent="Theme.MaterialComponents.DayNight.NoActionBar">
        <!-- Primary brand color. -->
        <item name="colorPrimary">@color/primary</item>
        <item name="colorPrimaryVariant">@color/primary_variant</item>
        <item name="colorOnPrimary">@color/on_primary</item>
        <!-- Status bar color. -->
        <item name="android:statusBarColor">@android:color/white</item>
        <item name="android:windowLightStatusBar" tools:targetApi="m">true</item>
        <!-- Background -->
        <item name="android:windowBackground">@color/background</item>
    </style>
</resources>
```

#### `app/src/main/res/values-night/themes.xml`
```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <style name="Base.Theme.GroceryList" parent="Theme.Material3.DayNight.NoActionBar">
    </style>
</resources>
```

---

## 6. Step 4: Vector Assets & Custom Drawables

### 6.1 Vector Drawables
#### `app/src/main/res/drawable/ic_add.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z" />
</vector>
```

#### `app/src/main/res/drawable/ic_delete.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/action_delete"
        android:pathData="M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z" />
</vector>
```

#### `app/src/main/res/drawable/ic_shopping_basket.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="64dp"
    android:height="64dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/text_secondary"
        android:pathData="M17.21,9l-4.38,-6.56c-0.19,-0.28,-0.51,-0.42,-0.83,-0.42c-0.32,0,-0.64,0.14,-0.83,0.43L6.79,9H2c-0.55,0,-1,0.45,-1,1c0,0.09,0.01,0.18,0.04,0.27l2.54,9.27c0.23,0.84,1,1.46,1.92,1.46h12.9c0.92,0,1.69,-0.62,1.93,-1.46l2.54,-9.27L23,10c0,-0.55,-0.45,-1,-1,-1h-4.79zM9,9l3,-4.5L15,9H9zM12,17c-1.1,0,-2,-0.9,-2,-2s0.9,-2,2,-2s2,0.9,2,2s-0.9,2,-2,2z" />
</vector>
```

### 6.2 Backgrounds & State Drawables
#### `app/src/main/res/drawable/bg_add_button.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="@color/primary_light">
    <item android:id="@android:id/mask">
        <shape android:shape="oval">
            <solid android:color="@android:color/white" />
        </shape>
    </item>
    <item>
        <shape android:shape="oval">
            <solid android:color="@color/primary" />
        </shape>
    </item>
</ripple>
```

#### `app/src/main/res/drawable/bg_input_bar.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/surface" />
        </shape>
    </item>
    <item android:bottom="-2dp" android:left="-2dp" android:right="-2dp">
        <shape android:shape="rectangle">
            <stroke
                android:width="1dp"
                android:color="@color/divider" />
        </shape>
    </item>
</layer-list>
```

#### `app/src/main/res/drawable/bg_input_field.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="@color/surface_variant" />
    <corners android:radius="24dp" />
    <stroke
        android:width="1dp"
        android:color="@color/divider" />
</shape>
```

#### `app/src/main/res/drawable/bg_top_bar.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/surface" />
        </shape>
    </item>
    <item android:top="-2dp" android:left="-2dp" android:right="-2dp">
        <shape android:shape="rectangle">
            <stroke
                android:width="1dp"
                android:color="@color/divider" />
        </shape>
    </item>
</layer-list>
```

#### `app/src/main/res/drawable/bg_item_card.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="@color/primary_light">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/surface" />
            <corners android:radius="12dp" />
            <stroke
                android:width="1dp"
                android:color="@color/divider" />
        </shape>
    </item>
</ripple>
```

#### `app/src/main/res/drawable/bg_item_card_bought.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<ripple xmlns:android="http://schemas.android.com/apk/res/android"
    android:color="@color/primary_light">
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/surface_variant" />
            <corners android:radius="12dp" />
            <stroke
                android:width="1dp"
                android:color="@color/divider" />
        </shape>
    </item>
</ripple>
```

---

## 7. Step 5: Screen & Item Layouts

### 7.1 Main Activity Layout: `app/src/main/res/layout/activity_main.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/rootLayout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/background">

    <!-- Top App Bar / Header -->
    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/headerContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_top_bar"
        android:elevation="2dp"
        android:paddingStart="20dp"
        android:paddingTop="16dp"
        android:paddingEnd="12dp"
        android:paddingBottom="14dp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent">

        <TextView
            android:id="@+id/tvAppTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginEnd="8dp"
            android:text="@string/app_name"
            android:textColor="@color/primary"
            android:textSize="22sp"
            android:textStyle="bold"
            app:layout_constrainedWidth="true"
            app:layout_constraintEnd_toStartOf="@+id/btnDeleteAll"
            app:layout_constraintHorizontal_bias="0"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/tvItemsCount"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:layout_marginEnd="8dp"
            android:textColor="@color/text_secondary"
            android:textSize="14sp"
            app:layout_constrainedWidth="true"
            app:layout_constraintEnd_toStartOf="@+id/btnDeleteAll"
            app:layout_constraintHorizontal_bias="0"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toBottomOf="@+id/tvAppTitle"
            tools:text="3 items remaining" />

        <ImageButton
            android:id="@+id/btnDeleteAll"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/delete_all"
            android:src="@drawable/ic_delete"
            android:visibility="gone"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:tint="@color/action_delete"
            tools:visibility="visible" />

    </androidx.constraintlayout.widget.ConstraintLayout>

    <!-- Grocery Items RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerViewItems"
        android:layout_width="0dp"
        android:layout_height="0dp"
        android:clipToPadding="false"
        android:paddingTop="8dp"
        android:paddingBottom="8dp"
        android:scrollbars="vertical"
        app:layout_constraintBottom_toTopOf="@+id/bottomInputContainer"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/headerContainer"
        tools:listitem="@layout/item_grocery" />

    <!-- Empty State View -->
    <LinearLayout
        android:id="@+id/layoutEmptyState"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:orientation="vertical"
        android:padding="24dp"
        android:visibility="gone"
        app:layout_constraintBottom_toTopOf="@+id/bottomInputContainer"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toBottomOf="@+id/headerContainer"
        tools:visibility="visible">

        <ImageView
            android:id="@+id/ivEmptyIcon"
            android:layout_width="72dp"
            android:layout_height="72dp"
            android:contentDescription="@string/empty_list_title"
            android:src="@drawable/ic_shopping_basket" />

        <TextView
            android:id="@+id/tvEmptyTitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="16dp"
            android:text="@string/empty_list_title"
            android:textColor="@color/text_primary"
            android:textSize="18sp"
            android:textStyle="bold" />

        <TextView
            android:id="@+id/tvEmptySubtitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="6dp"
            android:text="@string/empty_list_subtitle"
            android:textColor="@color/text_secondary"
            android:textSize="14sp" />

    </LinearLayout>

    <!-- Bottom Fixed Input Bar (Ergonomic shopping thumb reach) -->
    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/bottomInputContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@drawable/bg_input_bar"
        android:elevation="8dp"
        android:paddingStart="12dp"
        android:paddingTop="10dp"
        android:paddingEnd="12dp"
        android:paddingBottom="12dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent">

        <EditText
            android:id="@+id/etItemInput"
            android:layout_width="0dp"
            android:layout_height="48dp"
            android:layout_marginEnd="10dp"
            android:background="@drawable/bg_input_field"
            android:hint="@string/add_item_hint"
            android:imeOptions="actionDone"
            android:importantForAutofill="no"
            android:inputType="textCapSentences"
            android:maxLength="200"
            android:maxLines="1"
            android:paddingStart="16dp"
            android:paddingTop="10dp"
            android:paddingEnd="16dp"
            android:paddingBottom="10dp"
            android:singleLine="true"
            android:textColor="@color/text_primary"
            android:textColorHint="@color/text_secondary"
            android:textSize="15sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toStartOf="@+id/btnAddItem"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <ImageButton
            android:id="@+id/btnAddItem"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="@drawable/bg_add_button"
            android:contentDescription="@string/add_button_content_description"
            android:src="@drawable/ic_add"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</androidx.constraintlayout.widget.ConstraintLayout>
```

### 7.2 Grocery Row Item Layout: `app/src/main/res/layout/item_grocery.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/itemContainer"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginStart="12dp"
    android:layout_marginTop="4dp"
    android:layout_marginEnd="12dp"
    android:layout_marginBottom="4dp"
    android:background="@drawable/bg_item_card"
    android:clickable="true"
    android:focusable="true"
    android:minHeight="56dp"
    android:paddingStart="8dp"
    android:paddingTop="6dp"
    android:paddingEnd="8dp"
    android:paddingBottom="6dp">

    <!-- 48dp touch area CheckBox, driven directly by card clicks -->
    <CheckBox
        android:id="@+id/cbBought"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:buttonTint="@color/action_check"
        android:clickable="false"
        android:focusable="false"
        android:contentDescription="@string/item_checkbox_content_description"
        android:gravity="center"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <!-- Item Serial Number (e.g. 1., 2., 3.) -->
    <TextView
        android:id="@+id/tvSerialNumber"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="2dp"
        android:layout_marginEnd="6dp"
        android:textColor="@color/text_secondary"
        android:textSize="15sp"
        android:textStyle="bold"
        app:layout_constraintBaseline_toBaselineOf="@+id/tvItemName"
        app:layout_constraintStart_toEndOf="@+id/cbBought"
        tools:text="1." />

    <!-- Item Name with defensive multi-line wrap and ellipsize -->
    <TextView
        android:id="@+id/tvItemName"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="6dp"
        android:layout_marginEnd="8dp"
        android:ellipsize="end"
        android:maxLines="3"
        android:textColor="@color/text_primary"
        android:textSize="16sp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toStartOf="@+id/btnDelete"
        app:layout_constraintStart_toEndOf="@+id/tvSerialNumber"
        app:layout_constraintTop_toTopOf="parent"
        tools:text="Fresh Organic Apples" />

    <!-- 48dp minimum touch target Delete Button -->
    <ImageButton
        android:id="@+id/btnDelete"
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:background="?attr/selectableItemBackgroundBorderless"
        android:contentDescription="@string/delete_button_content_description"
        android:src="@drawable/ic_delete"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

</androidx.constraintlayout.widget.ConstraintLayout>
```

---

## 8. Step 6: Data & Persistence Layer (Room Database)

### 8.1 Room Entity: `app/src/main/java/com/example/grocerylist/data/local/GroceryItem.kt`
```kotlin
package com.example.grocerylist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room database entity representing an item in the grocery list.
 *
 * @property id Unique auto-generated identifier.
 * @property name Cleaned display name of the grocery item.
 * @property isBought Whether the item has been ticked off / purchased.
 * @property createdAt Epoch timestamp in milliseconds when the item was added.
 */
@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isBought: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

### 8.2 Room DAO: `app/src/main/java/com/example/grocerylist/data/local/GroceryDao.kt`
```kotlin
package com.example.grocerylist.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for grocery items.
 * Provides SQLite operations backed by Kotlin Coroutines and Flows.
 */
@Dao
interface GroceryDao {

    /**
     * Observes all items in the grocery list.
     * Items are ordered with unbought items first (isBought = 0),
     * followed by bought items (isBought = 1).
     * Within each group, items are sorted by creation date descending (newest first).
     */
    @Query("SELECT * FROM grocery_items ORDER BY isBought ASC, createdAt DESC, id DESC")
    fun getAllItems(): Flow<List<GroceryItem>>

    /**
     * Inserts or replaces a grocery item in the database.
     *
     * @param item Item to insert.
     * @return The row ID of the inserted item.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: GroceryItem): Long

    /**
     * Inserts multiple grocery items in a single transaction.
     *
     * @param items Items to insert.
     * @return List of row IDs of the inserted items.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg items: GroceryItem): List<Long>

    /**
     * Updates an existing item (e.g. toggling isBought status or updating name).
     *
     * @param item Item with updated fields.
     */
    @Update
    suspend fun update(item: GroceryItem)

    /**
     * Deletes a specific grocery item.
     *
     * @param item Item to delete.
     */
    @Delete
    suspend fun delete(item: GroceryItem)

    /**
     * Fetches a single grocery item matching the specified name case-insensitively.
     *
     * @param name Name of the item to query.
     * @return The item if found, null otherwise.
     */
    @Query("SELECT * FROM grocery_items WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getItemByName(name: String): GroceryItem?

    /**
     * Observes the count of remaining unbought items.
     */
    @Query("SELECT COUNT(*) FROM grocery_items WHERE isBought = 0")
    fun getUnboughtCount(): Flow<Int>

    /**
     * Returns total count of all grocery items.
     */
    @Query("SELECT COUNT(*) FROM grocery_items")
    suspend fun getCount(): Int

    /**
     * Deletes all grocery items from the table.
     *
     * @return Number of rows deleted.
     */
    @Query("DELETE FROM grocery_items")
    suspend fun deleteAll(): Int
}
```

### 8.3 Room Database Singleton: `app/src/main/java/com/example/grocerylist/data/local/GroceryDatabase.kt`
```kotlin
package com.example.grocerylist.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room Database for this app.
 * Provides the main access point to the underlying persisted SQLite connection.
 */
@Database(
    entities = [GroceryItem::class],
    version = 1,
    exportSchema = false
)
abstract class GroceryDatabase : RoomDatabase() {

    abstract fun groceryDao(): GroceryDao

    companion object {
        private const val DATABASE_NAME = "grocery_database"

        @Volatile
        private var INSTANCE: GroceryDatabase? = null

        /**
         * Gets the singleton instance of [GroceryDatabase].
         * Uses double-checked locking for thread-safe lazy initialization.
         */
        fun getDatabase(context: Context): GroceryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GroceryDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### 8.4 Repository: `app/src/main/java/com/example/grocerylist/data/repository/GroceryRepository.kt`
```kotlin
package com.example.grocerylist.data.repository

import com.example.grocerylist.data.local.GroceryDao
import com.example.grocerylist.data.local.GroceryItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Interface defining domain operations for grocery items.
 */
interface GroceryRepository {

    /**
     * Observes all grocery items sorted with unbought first and newest first.
     */
    val allItems: Flow<List<GroceryItem>>

    /**
     * Observes the count of remaining unbought items.
     */
    val unboughtCount: Flow<Int>

    /**
     * Adds a new grocery item with sanitized name.
     *
     * @param name Name of the grocery item.
     * @return Row ID of the inserted item.
     */
    suspend fun addItem(name: String): Long

    /**
     * Restores a complete [GroceryItem] (used for Undo functionality and reactivation).
     *
     * @param item The grocery item to restore.
     * @return Row ID of the inserted item.
     */
    suspend fun restoreItem(item: GroceryItem): Long

    /**
     * Toggles the bought status of an item.
     *
     * @param item Item to toggle.
     */
    suspend fun toggleItemStatus(item: GroceryItem)

    /**
     * Deletes a grocery item.
     *
     * @param item Item to delete.
     */
    suspend fun deleteItem(item: GroceryItem)

    /**
     * Fetches an item by name (case-insensitive).
     *
     * @param name Name of the item.
     * @return The item if found, null otherwise.
     */
    suspend fun getItemByName(name: String): GroceryItem?

    /**
     * Deletes all grocery items from the list.
     *
     * @return Number of rows deleted.
     */
    suspend fun deleteAll(): Int
}

/**
 * Default implementation of [GroceryRepository] backed by Room [GroceryDao].
 */
class GroceryRepositoryImpl(
    private val groceryDao: GroceryDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GroceryRepository {

    override val allItems: Flow<List<GroceryItem>> = groceryDao.getAllItems()

    override val unboughtCount: Flow<Int> = groceryDao.getUnboughtCount()

    override suspend fun addItem(name: String): Long = withContext(ioDispatcher) {
        val item = GroceryItem(
            name = name.trim(),
            isBought = false,
            createdAt = System.currentTimeMillis()
        )
        groceryDao.insert(item)
    }

    override suspend fun restoreItem(item: GroceryItem): Long = withContext(ioDispatcher) {
        groceryDao.insert(item)
    }

    override suspend fun toggleItemStatus(item: GroceryItem): Unit = withContext(ioDispatcher) {
        val updated = item.copy(isBought = !item.isBought)
        groceryDao.update(updated)
    }

    override suspend fun getItemByName(name: String): GroceryItem? = withContext(ioDispatcher) {
        groceryDao.getItemByName(name.trim())
    }

    override suspend fun deleteItem(item: GroceryItem): Unit = withContext(ioDispatcher) {
        groceryDao.delete(item)
    }

    override suspend fun deleteAll(): Int = withContext(ioDispatcher) {
        groceryDao.deleteAll()
    }
}
```

---

## 9. Step 7: Utilities

### 9.1 Throttle Extension: `app/src/main/java/com/example/grocerylist/util/ViewExtensions.kt`
> [!TIP]
> Prevents rapid button clicking from queuing duplicate database transactions or triggering multiple dialogs.

```kotlin
package com.example.grocerylist.util

import android.os.SystemClock
import android.view.View

/**
 * Extension on [View] to debounce click events.
 * Prevents multiple rapid clicks from triggering redundant transactions or crashes.
 *
 * @param debounceTime Milliseconds window to ignore subsequent clicks. Default: 350ms.
 * @param action Lambda invoked when a non-throttled click is registered.
 */
fun View.throttleClick(debounceTime: Long = 350L, action: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { v ->
        val currentTime = SystemClock.uptimeMillis()
        if (currentTime - lastClickTime >= debounceTime) {
            lastClickTime = currentTime
            action(v)
        }
    }
}
```

---

## 10. Step 8: Presentation Layer (UI & MVVM)

### 10.1 UI State: `app/src/main/java/com/example/grocerylist/ui/GroceryUiState.kt`
```kotlin
package com.example.grocerylist.ui

import com.example.grocerylist.data.local.GroceryItem

/**
 * Immutable state representation for the grocery list screen.
 *
 * @property items Current list of grocery items.
 * @property unboughtCount Number of items left to buy.
 * @property isLoading Whether initial state loading is in progress.
 * @property userMessage Transient message for user feedback (errors, notifications).
 * @property lastDeletedItem Cached copy of the last deleted item to support Undo.
 */
data class GroceryUiState(
    val items: List<GroceryItem> = emptyList(),
    val unboughtCount: Int = 0,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val lastDeletedItem: GroceryItem? = null
)
```

### 10.2 ViewModel & Factory: `app/src/main/java/com/example/grocerylist/ui/GroceryViewModel.kt`
```kotlin
package com.example.grocerylist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.grocerylist.data.local.GroceryItem
import com.example.grocerylist.data.repository.GroceryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel managing business logic, input validation, and UI state for the grocery list.
 * Exposes a unidirectional [uiState] stream observed by the UI layer.
 */
class GroceryViewModel(
    private val repository: GroceryRepository,
    started: SharingStarted = SharingStarted.WhileSubscribed(5000),
) : ViewModel() {

    companion object {
        const val MAX_ITEM_NAME_LENGTH = 200
    }

    private val _userMessage = MutableStateFlow<String?>(null)
    private val _lastDeletedItem = MutableStateFlow<GroceryItem?>(null)

    val uiState: StateFlow<GroceryUiState> = combine(
        repository.allItems,
        _userMessage,
        _lastDeletedItem,
    ) { items, userMessage, lastDeletedItem ->
        val unbought = items.count { !it.isBought }
        GroceryUiState(
            items = items,
            unboughtCount = unbought,
            isLoading = false,
            userMessage = userMessage,
            lastDeletedItem = lastDeletedItem
        )
    }.stateIn(
        scope = viewModelScope,
        started = started,
        initialValue = GroceryUiState(isLoading = true)
    )

    /**
     * Validates and sanitizes item name, then inserts it into the database.
     * Prevents empty or blank entries and caps max length defensively.
     *
     * @param rawName Raw input text entered by the user.
     * @return `true` if item was valid and queued for insertion; `false` otherwise.
     */
    fun addItem(rawName: String): Boolean {
        val trimmed = rawName.trim()
        if (trimmed.isEmpty()) {
            _userMessage.value = "Item name cannot be empty"
            return false
        }

        val sanitized = if (trimmed.length > MAX_ITEM_NAME_LENGTH) {
            trimmed.substring(0, MAX_ITEM_NAME_LENGTH)
        } else {
            trimmed
        }

        viewModelScope.launch {
            val existingItem = repository.getItemByName(sanitized)
            if (existingItem != null) {
                if (existingItem.isBought) {
                    val reactivated = existingItem.copy(
                        isBought = false,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.restoreItem(reactivated)
                    _userMessage.value = "\"${existingItem.name}\" restored to list"
                } else {
                    _userMessage.value = "\"${existingItem.name}\" is already on your list"
                }
            } else {
                repository.addItem(sanitized)
            }
        }
        return true
    }

    private var lastToggledItemId = -1L
    private var lastToggleTime = 0L

    /**
     * Toggles an item between bought and unbought status.
     * Prevents accidental double-taps on the same item, while allowing
     * instant fast clicks across different items.
     *
     * @param item Item to toggle.
     */
    fun toggleItemBought(item: GroceryItem) {
        val now = System.currentTimeMillis()
        if (item.id == lastToggledItemId && (now - lastToggleTime < 250L)) {
            return
        }
        lastToggledItemId = item.id
        lastToggleTime = now

        viewModelScope.launch {
            repository.toggleItemStatus(item)
        }
    }

    /**
     * Deletes an item from the grocery list and stages it for potential Undo restoration.
     *
     * @param item Item to remove.
     */
    fun removeItem(item: GroceryItem) {
        _lastDeletedItem.value = item
        _userMessage.value = "\"${item.name}\" removed"
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    /**
     * Restores a deleted item back to the database.
     * If no item is passed, restores the last deleted item staged in [_lastDeletedItem].
     *
     * @param item Optional specific item to restore.
     */
    fun undoRemove(item: GroceryItem? = null) {
        val itemToRestore = item ?: _lastDeletedItem.value ?: return
        viewModelScope.launch {
            repository.restoreItem(itemToRestore)
            _lastDeletedItem.value = null
            _userMessage.value = "\"${itemToRestore.name}\" restored"
        }
    }

    /**
     * Deletes all items from the grocery list.
     */
    fun deleteAllItems() {
        viewModelScope.launch {
            val count = repository.deleteAll()
            if (count > 0) {
                _lastDeletedItem.value = null
                _userMessage.value = "All items deleted"
            }
        }
    }

    /**
     * Clears transient user messages after they have been shown.
     */
    fun clearUserMessage() {
        _userMessage.value = null
    }
}

/**
 * Factory for creating [GroceryViewModel] with its repository dependency.
 */
class GroceryViewModelFactory(
    private val repository: GroceryRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroceryViewModel::class.java)) {
            return GroceryViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
```

### 10.3 RecyclerView ListAdapter: `app/src/main/java/com/example/grocerylist/ui/GroceryListAdapter.kt`
> [!IMPORTANT]
> In `GroceryDiffCallback`, `areContentsTheSame` returns `false`. This intentional choice guarantees that whenever items reorder (e.g. an item is checked off and moves down), all shifting items re-bind so their serial number (`tvSerialNumber.text = "${position + 1}."`) accurately updates without stale or out-of-order numbering.

```kotlin
package com.example.grocerylist.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerylist.R
import com.example.grocerylist.data.local.GroceryItem
import com.example.grocerylist.databinding.ItemGroceryBinding

/**
 * RecyclerView ListAdapter for displaying grocery items with DiffUtil animations.
 *
 * @param onItemToggle Callback invoked when an item's check status is toggled.
 * @param onItemDelete Callback invoked when an item is deleted.
 */
class GroceryListAdapter(
    private val onItemToggle: (GroceryItem) -> Unit,
    private val onItemDelete: (GroceryItem) -> Unit
) : ListAdapter<GroceryItem, GroceryListAdapter.GroceryViewHolder>(GroceryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroceryViewHolder {
        val binding = ItemGroceryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GroceryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroceryViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    inner class GroceryViewHolder(
        private val binding: ItemGroceryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            // Tapping the card (including the checkbox area) toggles bought status
            binding.itemContainer.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemToggle(getItem(position))
                }
            }

            // Tapping delete button removes the item
            binding.btnDelete.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemDelete(getItem(position))
                }
            }
        }

        fun bind(item: GroceryItem, position: Int) {
            val context = binding.root.context
            binding.tvSerialNumber.text = "${position + 1}."
            binding.tvItemName.text = item.name

            // Consistent checkbox checked state
            binding.cbBought.isChecked = item.isBought

            // Strikethrough, colors, and background styling
            if (item.isBought) {
                binding.itemContainer.setBackgroundResource(R.drawable.bg_item_card_bought)
                binding.tvItemName.paint.isStrikeThruText = true
                binding.tvItemName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_strikethrough)
                )
                binding.tvSerialNumber.setTextColor(
                    ContextCompat.getColor(context, R.color.text_strikethrough)
                )
            } else {
                binding.itemContainer.setBackgroundResource(R.drawable.bg_item_card)
                binding.tvItemName.paint.isStrikeThruText = false
                binding.tvItemName.setTextColor(
                    ContextCompat.getColor(context, R.color.text_primary)
                )
                binding.tvSerialNumber.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )
            }

            // Ensure alpha is always 1.0f so recycled views never get stuck with dimming/transparency
            binding.itemContainer.alpha = 1.0f
        }
    }

    companion object GroceryDiffCallback : DiffUtil.ItemCallback<GroceryItem>() {
        override fun areItemsTheSame(oldItem: GroceryItem, newItem: GroceryItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: GroceryItem, newItem: GroceryItem): Boolean {
            // Return false so DiffUtil triggers a rebind whenever items reorder or change positions.
            // This ensures onBindViewHolder is called for shifted views so tvSerialNumber always
            // displays the correct adapter position (${position + 1}.).
            return false
        }
    }
}
```

### 10.4 Main Activity: `app/src/main/java/com/example/grocerylist/ui/MainActivity.kt`
> [!NOTE]
> Key MainActivity refinements:
> 1. Edge-to-edge window insets listener accounts for both `systemBars` and virtual keyboard `imeInsets`.
> 2. `itemAnimator = null` and custom `requestChildRectangleOnScreen` return `false` on the layout manager. This eliminates jumping/flying animation artifacts when items move to the bottom upon being checked.
> 3. Soft keyboard "Done" action triggers addition seamlessly.
> 4. Dialog confirmation on bulk removal protects against accidental deletions.

```kotlin
package com.example.grocerylist.ui

import android.graphics.Rect
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.grocerylist.R
import com.example.grocerylist.data.local.GroceryDatabase
import com.example.grocerylist.data.repository.GroceryRepositoryImpl
import com.example.grocerylist.databinding.ActivityMainBinding
import com.example.grocerylist.util.throttleClick
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * Main activity displaying the grocery shopping list.
 * Designed for 100% offline, zero-friction usage during shopping trips.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: GroceryListAdapter

    private val viewModel: GroceryViewModel by viewModels {
        val database = GroceryDatabase.getDatabase(applicationContext)
        val repository = GroceryRepositoryImpl(database.groceryDao())
        GroceryViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.getInsetsController(window, binding.rootLayout).isAppearanceLightStatusBars = true

        setupWindowInsets()
        setupRecyclerView()
        setupInputListeners()
        observeUiState()
    }

    private fun setupWindowInsets() {
        val initialHeaderPaddingTop = binding.headerContainer.paddingTop
        val initialBottomPaddingBottom = binding.bottomInputContainer.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

            binding.rootLayout.updatePadding(
                left = systemBars.left,
                right = systemBars.right
            )

            binding.headerContainer.updatePadding(
                top = initialHeaderPaddingTop + systemBars.top
            )

            binding.bottomInputContainer.updatePadding(
                bottom = initialBottomPaddingBottom + maxOf(systemBars.bottom, imeInsets.bottom)
            )

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupRecyclerView() {
        adapter = GroceryListAdapter(
            onItemToggle = { item ->
                viewModel.toggleItemBought(item)
            },
            onItemDelete = { item ->
                viewModel.removeItem(item)
            }
        )

        val customLayoutManager = object : LinearLayoutManager(this@MainActivity) {
            override fun requestChildRectangleOnScreen(
                parent: RecyclerView,
                child: View,
                rect: Rect,
                immediate: Boolean,
                focusedChildVisible: Boolean
            ): Boolean {
                // Prevent RecyclerView from auto-scrolling to follow moved/animating children to the end
                return false
            }
        }

        binding.recyclerViewItems.apply {
            layoutManager = customLayoutManager
            adapter = this@MainActivity.adapter
            itemAnimator = null // Instantaneous, jitter-free updates without flying items or scroll jumps
        }
    }

    private fun setupInputListeners() {
        TooltipCompat.setTooltipText(binding.btnDeleteAll, getString(R.string.delete_all))

        // Delete all items button tap
        binding.btnDeleteAll.throttleClick {
            showDeleteAllConfirmationDialog()
        }

        // Add button tap
        binding.btnAddItem.throttleClick {
            submitItemInput()
        }

        // Soft keyboard 'Done' action
        binding.etItemInput.setOnEditorActionListener { _, actionId, event ->
            val isEnterKey = event != null &&
                    event.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN

            if (actionId == EditorInfo.IME_ACTION_DONE || isEnterKey) {
                submitItemInput()
                true
            } else {
                false
            }
        }
    }

    private fun showDeleteAllConfirmationDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_all_confirmation_title)
            .setMessage(R.string.delete_all_confirmation_message)
            .setPositiveButton(R.string.delete_all_confirm) { _, _ ->
                viewModel.deleteAllItems()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
            ContextCompat.getColor(this, R.color.action_delete)
        )
    }

    private fun submitItemInput() {
        val rawInput = binding.etItemInput.text?.toString().orEmpty()
        val wasAdded = viewModel.addItem(rawInput)
        if (wasAdded) {
            binding.etItemInput.text?.clear()
            binding.recyclerViewItems.post {
                binding.recyclerViewItems.scrollToPosition(0)
            }
        }
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUi(state)
                }
            }
        }
    }

    private fun renderUi(state: GroceryUiState) {
        val layoutManager = binding.recyclerViewItems.layoutManager as? LinearLayoutManager
        val wasNearTop = (layoutManager?.findFirstVisibleItemPosition() ?: 0) <= 1

        // Update items list and guarantee the viewport stays at the top if the user was near the top
        adapter.submitList(state.items) {
            if (wasNearTop) {
                binding.recyclerViewItems.scrollToPosition(0)
            }
        }

        // Handle empty vs populated state
        if (state.items.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.recyclerViewItems.visibility = View.GONE
            binding.tvItemsCount.visibility = View.GONE
            binding.btnDeleteAll.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.recyclerViewItems.visibility = View.VISIBLE
            binding.tvItemsCount.visibility = View.VISIBLE
            binding.btnDeleteAll.visibility = View.VISIBLE

            binding.tvItemsCount.text = when (state.unboughtCount) {
                0 -> getString(R.string.all_items_bought)
                1 -> getString(R.string.item_remaining_single)
                else -> getString(R.string.items_remaining, state.unboughtCount)
            }
        }

        // Handle user notifications and Undo actions
        state.userMessage?.let { message ->
            val snackbar = Snackbar.make(binding.rootLayout, message, Snackbar.LENGTH_LONG).apply {
                setBackgroundTint(ContextCompat.getColor(this@MainActivity, R.color.surface))
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_primary))
                setActionTextColor(ContextCompat.getColor(this@MainActivity, R.color.primary))
            }
            val itemToRestore = state.lastDeletedItem
            if (itemToRestore != null) {
                snackbar.setAction(R.string.undo) {
                    viewModel.undoRemove(itemToRestore)
                }
            }
            snackbar.show()
            viewModel.clearUserMessage()
        }
    }
}
```

---

## 11. Verification & Quality Assurance Checklist

After implementing all files in your new project, perform the following verification steps:

| Test Case | Procedure | Expected Outcome |
| :--- | :--- | :--- |
| **1. Clean Build** | Run `./gradlew assembleDebug` or `./gradlew testDebugUnitTest` | Build succeeds with zero compilation errors and zero unresolved symbols. |
| **2. Cold Start Empty State** | Launch the app with an empty database | Full empty state illustration is displayed with title "Your grocery list is empty", header shows no count, and Delete All icon is hidden. |
| **3. Adding Items** | Type "Organic Milk" and press the green `+` button or keyboard `Done` | Item is inserted immediately at position #1, input field clears, and header updates to "1 item remaining". |
| **4. Edge Cases: Empty & Spaces** | Submit an empty string or multiple spaces `"   "` | Input is rejected without crash, and a Snackbar notifies: "Item name cannot be empty". |
| **5. Edge Cases: Long Strings** | Paste a 300-character grocery description | String is safely capped to 200 characters, renders on up to 3 lines without clipping buttons or breaking layout. |
| **6. Duplicate Detection & Reactivation** | 1. Add "Eggs".<br>2. Add "Eggs" again.<br>3. Tick off "Eggs" (bought).<br>4. Type "Eggs" and submit. | On duplicate unbought item: notifies "Eggs is already on your list".<br>On duplicate bought item: reactivates "Eggs", unchecks it, and brings it back to the active section. |
| **7. Marking as Bought** | Tap an active item card | Item strikes through immediately, text changes to muted gray, card gets subtle tint, and it moves below unbought items without any jarring scroll jumps. |
| **8. Numbering Integrity** | Add 5 items, tick item #2 | Remaining items automatically renumber continuously (`1.`, `2.`, `3.`, `4.`, `5.`) without duplicates or skips. |
| **9. Deletion & Undo** | Tap trash icon on an item | Item is removed immediately. Snackbar appears: `"Item removed"` with an `UNDO` action. Tapping `UNDO` restores the item in place. |
| **10. Bulk Deletion** | Tap the header trash icon | Material dialog prompts confirmation. Tapping Cancel preserves list; confirming deletes all items and shows empty state. |
| **11. Offline Persistence** | Turn on Airplane Mode, add items, force-close app from Recents, and reopen | All items remain fully intact, perfectly matching prior state with zero network activity. |
| **12. Keyboard Insets & Edge-to-Edge** | Tap the text field to raise the soft keyboard | Bottom bar floats seamlessly above the keyboard; no elements are covered. |

---

## 12. Troubleshooting & Build Hints

1. **KSP & Kotlin Version Pairing**:
   * Ensure `ksp = "2.0.21-1.0.28"` matches `kotlin = "2.0.21"`. If you upgrade Kotlin in `libs.versions.toml`, update the KSP version accordingly.
2. **Package Name Changes**:
   * If creating under a package different than `com.example.grocerylist`, update:
     * `namespace` and `applicationId` in `app/build.gradle.kts`.
     * Package declarations across all 9 Kotlin files.
     * `package` attribute and activity path in `AndroidManifest.xml`.
3. **ViewBinding Access**:
   * Make sure `buildFeatures { viewBinding = true }` is enabled in `app/build.gradle.kts`, and sync Gradle so `ActivityMainBinding` and `ItemGroceryBinding` generate cleanly.
