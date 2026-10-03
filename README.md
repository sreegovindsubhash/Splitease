# SplitEase

An offline-first Android application for splitting shared expenses among
groups of people. Built as a final-year university project using Kotlin,
Jetpack Compose, and Material 3.

------------------------------------------------------------------------

## Table of Contents

-   [Project Overview](#project-overview)
-   [Key Features](#key-features)
-   [Technology Stack](#technology-stack)
-   [Architecture](#architecture)
-   [Offline-First / Local Data](#offline-first--local-data)
-   [Expense Splitting Methods](#expense-splitting-methods)
-   [Expense Categories](#expense-categories)
-   [Group Budgets](#group-budgets)
-   [Expense Reminders](#expense-reminders)
-   [Settlements and Balance
    Calculation](#settlements-and-balance-calculation)
-   [Summary and CSV Export](#summary-and-csv-export)
-   [Themes and Accessibility](#themes-and-accessibility)
-   [Testing](#testing)
-   [Project Structure](#project-structure)
-   [Building and Running](#building-and-running)
-   [Scope and Limitations](#scope-and-limitations)

------------------------------------------------------------------------

## Project Overview

SplitEase is targeted at university students who share living costs in
hostels, shared flats, or while organising group trips and events. The
app allows users to create groups, add members, record expenses with
flexible split methods, track individual balances, and generate
simplified settlement instructions --- all without requiring an internet
connection or user account.

All core application data is stored locally on the device using Room.
SplitEase uses no backend server or cloud synchronization, and it does
not automatically transmit user data over the network.

------------------------------------------------------------------------

## Key Features

-   Create and manage multiple expense groups (e.g. Goa Trip, Hostel
    Room 204, Apartment)
-   Add named members to each group
-   Record expenses with a description, amount, payer, category, date,
    and optional note
-   Four expense splitting methods: Equal, Exact Amounts, Percentage,
    and Shares
-   Live split preview while entering an expense
-   Full expense editing and deletion (with undo via Snackbar)
-   Search, filter by category, filter by date range, and sort the
    expense list
-   Per-member balance view showing who owes whom and by how much
-   Debt simplification algorithm to produce a deterministic, simplified
    set of settlement transfers
-   Settlement payment recording to track completed payments
-   Group summary screen with spending by category, spending over time,
    and member balances
-   CSV export of group expenses, shared via the Android document/file
    sharing mechanism
-   Optional group-level budget with progress tracking
-   One-time expense reminders backed by `AlarmManager`; reminders
    survive device reboot
-   Onboarding flow on first launch
-   Light, Dark, System, and High Contrast themes persisted across
    sessions
-   Accessibility-focused UI with TalkBack support, semantic
    information, 48 dp touch targets, scalable typography, and WCAG 2.1
    AA contrast targets

------------------------------------------------------------------------

## Technology Stack

  Layer                  Library / Tool                              Version
  ---------------------- ------------------------------------------- ---------------
  Language               Kotlin                                      2.1.20
  UI toolkit             Jetpack Compose (BOM)                       2025.05.01
  Design system          Material 3                                  via BOM
  Navigation             Navigation Compose                          2.9.0
  Local database         Room                                        2.7.1
  Preferences            DataStore Preferences                       1.1.4
  Async                  Kotlin Coroutines / Flow                    1.10.2
  Build tooling          Android Gradle Plugin                       8.10.1
  Code generation        KSP                                         2.1.20-1.0.32
  Unit testing           JUnit 4, Kotlin Test, Coroutines Test       ---
  Instrumented testing   AndroidX JUnit, Espresso, Compose UI Test   ---

-   **Minimum SDK:** 26 (Android 8.0)
-   **Target SDK:** 37
-   **Java compatibility:** 17

------------------------------------------------------------------------

## Architecture

The project follows a three-layer architecture with unidirectional data
flow.

``` text
app/src/main/java/com/splitease/
├── data/
│   ├── local/
│   │   ├── dao/            # Room DAOs
│   │   ├── database/       # AppDatabase, migrations
│   │   ├── entity/         # Room entities
│   │   └── mapper/         # Entity ↔ domain model mappers
│   └── repository/         # Repository implementations
├── domain/
│   ├── model/              # Pure Kotlin domain models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Business logic (use cases, split engine, debt simplifier)
├── notifications/          # AlarmManager receivers, notification scheduler
├── presentation/
│   ├── components/         # Shared composables
│   ├── navigation/         # NavGraph, Screen sealed class
│   ├── screens/            # One package per screen: Screen, ViewModel, UiState, Factory
│   └── theme/              # AppTheme enum, Color, Type, Theme composable
└── util/                   # ExpenseCsvExporter, MoneyFormatter
```

### Key design decisions

-   `ViewModel`s expose UI state through `StateFlow<UiState>`.
-   Composables consume state and emit events; they do not access Room
    directly.
-   Repositories mediate between use cases / ViewModels and Room DAOs.
-   Business logic such as splitting, balance calculation, and debt
    simplification lives in pure Kotlin code under `domain/usecase/`,
    making it independently testable without Android dependencies.
-   Navigation passes stable IDs (`Long`) between destinations rather
    than full domain objects.

------------------------------------------------------------------------

## Offline-First / Local Data

SplitEase requires no internet connection for core functionality.
Creating groups, recording expenses, calculating balances, generating
settlement transfers, producing summaries, and exporting CSV all run
on-device against the local Room database.

The database is named `splitease.db` and is currently at schema version
5. All schema upgrades are handled by explicit `Migration` objects
rather than destructive resets.

  Migration   Change
  ----------- --------------------------------------------------------
  1 → 2       Added `settlement_payments` table
  2 → 3       Normalised `category` column in `expenses`
  3 → 4       Added optional `budget_minor_units` column to `groups`
  4 → 5       Added `reminders` table

### Financial precision

Monetary values are stored and calculated as `Long` integer minor units
(paise, cents, etc.) throughout. `Double` and `Float` are not used for
monetary values.

For example:

``` text
₹123.45 → 12345 minor units
```

This avoids floating-point representation and rounding problems in
financial calculations.

------------------------------------------------------------------------

## Expense Splitting Methods

All splitting is handled by `MoneySplitEngine`, a pure Kotlin object
with no Android dependencies.

  -----------------------------------------------------------------------
  Method                              How it works
  ----------------------------------- -----------------------------------
  **Equal**                           Total divided equally among
                                      selected participants.

  **Exact Amounts**                   User enters the exact amount for
                                      each participant. Validation: sum
                                      must equal the expense total.

  **Percentage**                      User assigns a percentage (as basis
                                      points; 10,000 = 100%) to each
                                      participant. Validation: total must
                                      equal exactly 100%.

  **Shares**                          User assigns a positive integer
                                      share count to each participant;
                                      amounts are proportional to the
                                      ratio of shares.
  -----------------------------------------------------------------------

For the proportional methods (Equal, Percentage, Shares), the engine
uses a largest-remainder approach: each participant first receives the
floor of their proportional share, then remaining minor units are
distributed one at a time according to the fractional remainders. This
guarantees that:

``` text
sum(allocations) == expense.amountMinorUnits
```

with no money created or lost.

------------------------------------------------------------------------

## Expense Categories

Each expense is assigned one of the following categories:

-   Food
-   Transport
-   Accommodation
-   Shopping
-   Entertainment
-   Bills
-   Other

Categories are used for filtering on the expense list screen,
spending-by-category information on the summary screen, and as a column
in the CSV export.

------------------------------------------------------------------------

## Group Budgets

An optional spending budget can be set for any group. The budget is
stored as `Long` minor units in the `groups` table (`budget_minor_units`
column, nullable).

When a budget is configured, the group details screen shows a progress
indicator comparing total group spending against the budget. Setting a
budget to `null` removes it.

------------------------------------------------------------------------

## Expense Reminders

Users can create one-time reminders with a title, scheduled date/time,
and an optional note. Reminders are stored in the local `reminders`
table and scheduled using `AlarmManager` for an exact alarm.

A `BroadcastReceiver` fires when the alarm is due and posts a
notification. A separate `BootReceiver` re-schedules pending reminders
after a device reboot, so reminders can survive device restarts.

### Permissions used

-   `SCHEDULE_EXACT_ALARM` --- required for exact alarms on Android 12+
-   `POST_NOTIFICATIONS` --- required for notifications on Android 13+
-   `RECEIVE_BOOT_COMPLETED` --- required to re-schedule reminders after
    reboot

------------------------------------------------------------------------

## Settlements and Balance Calculation

### Balance Calculation

For every group the app computes a `MemberBalance` for each member:

``` text
netBalance = amountPaid − amountOwed
```

-   **Positive** --- the member is owed money.
-   **Negative** --- the member owes money.
-   **Zero** --- the member is fully settled.

Balance state is communicated using text and other visual indicators so
that it is not conveyed through colour alone.

### Debt Simplification

`DebtSimplifier` converts net balances into a deterministic, simplified
set of settlement transfers.

The algorithm:

1.  Separates members into **debtors** (negative balance) and
    **creditors** (positive balance), sorted by member ID for
    determinism.
2.  Matches the current debtor against the current creditor and
    transfers `min(debt, credit)`.
3.  Advances the pointer of whichever side reaches zero, or updates the
    remaining amount.
4.  Repeats until all balances are resolved.

This produces at most `N − 1` transfers for `N` members with non-zero
balances. The algorithm operates on derived balance data and does not
modify historical expense records.

Recorded settlement payments are stored in the `settlement_payments`
table and applied via `ApplySettlementPaymentsUseCase` when
recalculating the current state.

------------------------------------------------------------------------

## Summary and CSV Export

### Summary Screen

The group summary screen displays:

-   Total group spending
-   Spending broken down by category
-   Spending over time
-   Largest individual expenses
-   Member balance summary

Summary information includes text-based representations so important
information remains accessible to TalkBack users and is not conveyed
through colour alone.

### CSV Export

`ExpenseCsvExporter` generates a UTF-8 CSV string for a group's
expenses. The export is triggered from the summary screen and shared
using the Android document/file sharing mechanism. The export works
entirely offline.

CSV columns:

``` text
Date,Expense,Paid By,Amount,Currency,Category
```

-   Amounts are formatted using integer arithmetic only.
-   Fields containing commas, double-quotes, or newlines are quoted
    according to RFC 4180-style CSV escaping.
-   Expenses are ordered newest-first, consistent with the expense list.

------------------------------------------------------------------------

## Themes and Accessibility

### Themes

Four themes are available, selectable from the Settings screen and
persisted via DataStore:

  -----------------------------------------------------------------------
  Theme                               Description
  ----------------------------------- -----------------------------------
  **System**                          Follows the device dark-mode
                                      setting

  **Light**                           Custom Material 3 light colour
                                      scheme

  **Dark**                            Custom Material 3 dark colour
                                      scheme

  **High Contrast**                   High-contrast colour scheme
                                      designed around WCAG 2.1 AA
                                      contrast targets
  -----------------------------------------------------------------------

The theme is applied immediately on change without requiring an app
restart.

### Accessibility

The UI includes accessibility-focused design such as:

-   Appropriate content descriptions and semantic information for
    TalkBack.
-   Logical focus traversal on screens.
-   Touch targets of at least 48 dp × 48 dp.
-   Scalable typography using `sp` units.
-   Redundant text, icons, or shapes for important states so information
    is not conveyed through colour alone.
-   WCAG 2.1 AA contrast targets: ≥ 4.5:1 for normal text and ≥ 3:1 for
    large text.

------------------------------------------------------------------------

## Testing

Tests are located in two source sets:

-   `app/src/test/` --- JVM unit tests that do not require an Android
    runtime.
-   `app/src/androidTest/` --- instrumented tests that run on a device
    or emulator.

### Unit Tests

  ----------------------------------------------------------------------------------
  Test file                              Coverage area
  -------------------------------------- -------------------------------------------
  `MoneySplitEngineTest`                 All four split methods, rounding, edge
                                         cases

  `DebtSimplifierTest`                   Two-person, three-person chain, multiple
                                         creditors/debtors, equal/zero balances,
                                         rounding, complex groups

  `CalculateMemberBalancesUseCaseTest`   Balance aggregation

  `ApplySettlementPaymentsUseCaseTest`   Settlement payment application

  `SplitReconciliationTest`              `sum(shares) == expense.amountMinorUnits`
                                         invariant

  `ExpenseUseCasesTest`                  Expense use case behaviour

  `MemberUseCasesTest`                   Member use case behaviour

  `ReminderUseCasesTest`                 Reminder use case behaviour

  `ExpenseCsvExporterTest`               CSV generation, escaping, amount formatting

  `MoneyFormatterTest`                   Currency formatting

  `ExpenseMapperTest`,                   Entity ↔ domain model mapping
  `GroupMapperTest`, `MemberMapperTest`, 
  `ReminderMapperTest`                   

  `ThemePreferenceRepositoryTest`        Theme persistence

  `NavigationGuardTest`                  Navigation destination guard logic

  `GroupBudgetViewModelTest`             Budget ViewModel state

  `GroupDetailsViewModelTest`            Group details ViewModel state

  `BalancesViewModelTest`                Balances ViewModel state

  `SettlementViewModelTest`              Settlement ViewModel state

  `SummaryViewModelTest`                 Summary ViewModel state

  `CreateGroupViewModelTest`             Group creation ViewModel state

  `EditDeleteGroupTest`                  Group editing and deletion ViewModel state

  `AddEditExpenseViewModelTest`          Expense add/edit ViewModel state

  `AmountParserTest`                     Text-to-minor-units parsing

  `MembersViewModelTest`                 Members ViewModel state

  `OnboardingViewModelTest`              Onboarding ViewModel state

  `ExpenseCategoryFeatureTest`           Category feature integration
  ----------------------------------------------------------------------------------

### Instrumented Tests

  Test file                 Coverage area
  ------------------------- --------------------------------------
  `GroupsScreenTest`        Groups screen Compose UI flow
  `DatabaseMigrationTest`   Room migration correctness (v1 → v5)

Run unit tests:

``` bash
./gradlew test
```

Run instrumented tests:

``` bash
./gradlew connectedAndroidTest
```

Instrumented tests require a connected Android device or emulator.

------------------------------------------------------------------------

## Project Structure

``` text
SplitEase/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml
│   │   │   └── java/com/splitease/
│   │   │       ├── data/
│   │   │       │   ├── local/
│   │   │       │   │   ├── dao/            # Room DAOs
│   │   │       │   │   ├── database/       # AppDatabase and migrations
│   │   │       │   │   ├── entity/         # Room entities
│   │   │       │   │   └── mapper/         # Entity ↔ domain mappers
│   │   │       │   └── repository/         # Repository implementations
│   │   │       ├── domain/
│   │   │       │   ├── model/              # Domain models
│   │   │       │   ├── repository/         # Repository interfaces
│   │   │       │   └── usecase/            # Business logic and use cases
│   │   │       ├── notifications/          # Reminder receivers and scheduler
│   │   │       ├── presentation/
│   │   │       │   ├── components/         # Shared composables
│   │   │       │   ├── navigation/         # Navigation and guards
│   │   │       │   ├── screens/            # Screen/ViewModel/UiState packages
│   │   │       │   └── theme/              # App themes and styling
│   │   │       ├── util/                   # CSV exporter and formatting
│   │   │       ├── MainActivity.kt
│   │   │       └── SplitEaseApplication.kt
│   │   ├── test/                           # JVM unit tests
│   │   └── androidTest/                    # Instrumented tests
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                  # Version catalogue
├── build.gradle.kts
├── settings.gradle.kts
└── PROJECT_SPEC.md
```

------------------------------------------------------------------------

## Building and Running

### Prerequisites

-   Android Studio Meerkat (2024.3) or later, or a standalone JDK 17
    with the Android SDK.
-   Android SDK with Build Tools for `compileSdk = 37`.
-   A connected Android device (API 26+) or an AVD.

### Clone and open

``` bash
git clone https://github.com/sreegovindsubhash/Splitease.git
cd Splitease
```

Open the project root in Android Studio. Gradle will sync automatically.

### Build

``` bash
# Debug APK
./gradlew assembleDebug

# Release APK
./gradlew assembleRelease
```

The debug APK is output to:

``` text
app/build/outputs/apk/debug/app-debug.apk
```

### Install on a connected device

``` bash
./gradlew installDebug
```

### Run checks

``` bash
# Unit tests
./gradlew test

# Instrumented tests (requires device / emulator)
./gradlew connectedAndroidTest

# Build
./gradlew assembleDebug
```

No API keys, configuration files, or environment variables are required.
The app has no external service dependencies.

------------------------------------------------------------------------

## Scope and Limitations

-   **Single device only.** There is no synchronization mechanism; data
    exists solely on the device where it was entered. Sharing expenses
    between users requires manual coordination outside the app.
-   **No currency conversion.** Each group operates in one currency.
    Multi-currency groups are not supported.
-   **No account or authentication.** Anyone with access to the device
    can see the locally stored groups and expenses.
-   **Settlement payments are local records.** Marking a settlement as
    paid records it in the local database but does not notify other
    parties.
-   **Reminders are device-local.** Reminder notifications fire on the
    device where they were created.
-   **No cloud backup service.** The project does not provide its own
    cloud backup or synchronization mechanism. Android's built-in app
    backup setting is enabled but is not explicitly configured by
    SplitEase.
