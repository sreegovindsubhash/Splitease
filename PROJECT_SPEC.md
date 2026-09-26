# SplitEase — Product and Technical Specification

## 1. Product Overview

SplitEase is an offline-first Android application for university students who share expenses while living in hostels, shared flats, or organizing group trips and events.

The application allows users to create groups, add members, record expenses, calculate individual balances, and settle debts using a debt-simplification algorithm.

The application must work completely offline and store all application data locally using Room.

Primary goals:

* Simple and focused student-oriented UX
* Offline-first operation
* Accurate financial calculations
* Accessible interface
* Light, dark, and high-contrast themes
* Jetpack Compose and Material 3
* Maintainable Kotlin architecture
* Strong automated test coverage

---

# 2. Technology Stack

Use:

* Kotlin
* Android
* Jetpack Compose
* Material 3
* AndroidX Navigation Compose
* Room
* Kotlin Coroutines
* Kotlin Flow / StateFlow
* ViewModel
* Repository pattern
* MVVM or clean-ish layered architecture
* Kotlin serialization where useful
* JUnit
* AndroidX testing
* Compose UI testing

Do not introduce unnecessary frameworks.

The application must not require a backend server.

The application must not require an internet connection for core functionality.

---

# 3. Architecture

Use a clear layered architecture:

data/
local/
database/
dao/
entity/
repository/

domain/
model/
repository/
usecase/

presentation/
navigation/
theme/
components/
screens/
viewmodel/

Use unidirectional data flow where practical.

UI composables should not directly access Room.

ViewModels should expose UI state using StateFlow.

Repositories should mediate between ViewModels/use cases and Room.

Business logic should be separated from Compose UI.

---

# 4. Core Domain Model

The main entities are:

## Group

Fields:

* id
* name
* description or optional note
* currencyCode
* createdAt
* updatedAt

Examples:

* Goa Trip
* Hostel Room 204
* Apartment
* College Event

## Member

Fields:

* id
* groupId
* name
* optional email or identifier
* createdAt

A member belongs to exactly one group.

Deleting a group should delete its associated members and expenses.

## Expense

Fields:

* id
* groupId
* description
* amount
* currencyCode
* paidByMemberId
* category
* date
* note
* createdAt
* updatedAt

Categories should include at minimum:

* Food
* Transport
* Accommodation
* Shopping
* Entertainment
* Utilities
* Education
* Other

The user must be able to edit and delete expenses.

---

# 5. Expense Splitting

Support these split methods:

## Equal

The expense is divided equally between selected members.

Example:

₹900 / 3 people = ₹300 each.

Handle rounding deterministically.

The sum of all individual shares must exactly equal the original expense amount.

Never allow floating-point financial errors.

Prefer storing monetary values as integer minor units, such as paise/cents, rather than Double.

Example:

₹100.50 should be stored as 10050 paise.

## Exact Amounts

The user enters the exact amount owed by each participant.

Validation:

sum of participant amounts must equal the expense total.

Do not allow saving if the amounts do not reconcile.

## Percentage

The user assigns a percentage to each participant.

Validation:

sum of percentages must equal exactly 100%.

Calculate monetary shares deterministically and ensure rounded shares sum exactly to the expense amount.

## Shares

The user assigns integer or positive share counts.

Example:

Alice = 1 share
Bob = 2 shares
Charlie = 1 share

Total = 4 shares.

The expense is divided according to the ratio.

All rounding must reconcile exactly with the original amount.

---

# 6. Balance Calculation

For every group, calculate:

* Total paid by each member
* Total owed by each member
* Net balance

Formula:

netBalance = amountPaid - amountOwed

Positive balance:
member is owed money.

Negative balance:
member owes money.

Zero:
member is settled.

The UI should clearly distinguish:

* "You owe ₹X"
* "You are owed ₹X"
* "Settled"

Do not rely exclusively on color to communicate these states.

---

# 7. Debt Simplification Algorithm

Implement a deterministic debt simplification algorithm.

Input:

Each member's net balance.

Positive values represent creditors.

Negative values represent debtors.

Algorithm:

1. Separate creditors and debtors.
2. Sort both deterministically.
3. Match the largest outstanding debtor with the largest outstanding creditor.
4. Transfer the minimum of the debtor's debt and creditor's credit.
5. Continue until all balances reach zero.

Generate transactions:

SettlementTransaction:

* fromMemberId
* toMemberId
* amount

The resulting transaction list should settle the entire group.

The algorithm should attempt to minimize the number of transactions required.

Do not alter the original expenses.

Settlement transactions are calculated views derived from balances.

Add unit tests for:

* Two-person debt
* Three-person chain
* Multiple creditors
* Multiple debtors
* Equal balances
* Zero balances
* Rounding
* Complex groups

---

# 8. Main Screens

Implement the following screens.

## Onboarding

First launch should explain:

1. Create a group
2. Add expenses
3. See who owes whom

Allow the user to skip onboarding.

Store onboarding completion locally.

---

## Home / Groups

Display:

* App title
* List of groups
* Group name
* Number of members
* Total group spending
* User's current balance

Actions:

* Create group
* Open group

Provide a useful empty state when there are no groups.

---

## Create Group

Fields:

* Group name
* Currency

Optional:

* Description

Validate required fields.

After creation, navigate to the group details screen.

---

# 9. Group Details

Show:

* Group name
* User's balance
* Total expenses
* Members
* Recent expenses

Actions:

* Add expense
* Add member
* View all expenses
* Settle up
* Group settings

---

# 10. Members

Allow:

* Add member
* Edit member
* Remove member

Do not allow deleting a member if doing so would leave existing expenses invalid unless the application explicitly handles that situation.

Prefer warning the user and requiring confirmation.

---

# 11. Add Expense

Fields:

* Description
* Amount
* Paid by
* Category
* Date
* Optional note
* Split method
* Participants

Split methods:

* Equal
* Exact
* Percentage
* Shares

Show a live summary of how the expense is being divided.

Prevent saving invalid splits.

Examples:

Exact split:
Alice ₹200
Bob ₹300
Total ₹500

Percentage:
Alice 40%
Bob 60%

Shares:
Alice 1
Bob 2

---

# 12. Edit Expense

Reuse the expense form where practical.

Allow changing:

* Description
* Amount
* Payer
* Category
* Date
* Note
* Split method
* Participants

Recalculate balances immediately after editing.

---

# 13. Delete Expense

Require confirmation.

After deletion:

* Recalculate balances
* Show an undo snackbar where practical

Undo should restore the deleted expense.

---

# 14. Expense List

Display:

* Description
* Category
* Date
* Amount
* Paid by
* Optional participant summary

Provide:

* Search
* Category filter
* Date filter
* Sort options

Useful sort options:

* Newest
* Oldest
* Highest amount
* Lowest amount

---

# 15. Expense Details

Display:

* Description
* Total amount
* Payer
* Date
* Category
* Note
* Complete participant split

Actions:

* Edit
* Delete

---

# 16. Settle Up

Create a dedicated settlement screen.

Display simplified transactions such as:

"Alice pays Bob ₹350"

Each transaction must show:

* Payer
* Receiver
* Amount

Explain that these transactions are calculated from the group's balances.

Do not modify the original expenses.

Provide an action to mark a settlement as completed if settlement history is implemented.

---

# 17. Settlement History

If implemented, store:

* id
* groupId
* fromMemberId
* toMemberId
* amount
* date

Settlement history must not alter historical expense records.

---

# 18. Dashboard / Summary

Provide a group summary including:

* Total spending
* Spending by category
* Spending over time
* Largest expenses
* Member balances

Use accessible charts.

Do not communicate information through color alone.

Charts should have text summaries for TalkBack users.

---

# 19. CSV Export

Allow the user to export a group's expenses as CSV.

CSV should contain:

* Date
* Description
* Category
* Amount
* Currency
* Paid by
* Split method
* Participant shares

Use Android's document/file sharing mechanisms.

The export must work offline.

---

# 20. Multi-Currency

Support currency selection per group.

At minimum support display of common currencies such as:

* INR
* USD
* EUR
* GBP

Do not implement live currency conversion.

Each group should use one primary currency.

Currency should be treated primarily as a display and accounting unit.

---

# 21. Theme System

Implement:

1. Light
2. Dark
3. System default
4. High contrast

Persist the user's theme preference locally.

The UI must respond immediately when the theme changes.

Use Material 3 color schemes.

---

# 22. Accessibility

Accessibility is a major project requirement.

Support:

* TalkBack
* Content descriptions
* Semantic labels
* Logical traversal order
* Accessible buttons
* Accessible form controls
* Minimum touch target size
* Large text
* 200% system font scaling
* High contrast mode
* No color-only information

Interactive controls should have at least 48dp touch targets.

Avoid hardcoded text sizes that break when font scaling increases.

Use scalable typography.

Test with large font settings.

---

# 23. WCAG-Oriented Contrast

Aim for WCAG 2.1 AA contrast requirements.

Normal text:

minimum contrast ratio 4.5:1.

Large text:

minimum contrast ratio 3:1.

Do not rely on color alone for:

* debt/credit status
* selected states
* errors
* categories
* charts

Use text, icons, labels, shapes, or other redundant indicators.

Document any platform-specific accessibility limitations.

---

# 24. Offline-First Requirements

Core functionality must work without internet.

The app must:

* Create groups offline
* Add members offline
* Add expenses offline
* Edit expenses offline
* Delete expenses offline
* Calculate balances offline
* Calculate settlements offline
* View summaries offline
* Export CSV offline

Do not introduce Firebase or a backend unless explicitly requested later.

---

# 25. Error Handling

Handle:

* Empty states
* Invalid amounts
* Invalid split totals
* Missing payer
* No participants
* Deleted members
* Database errors
* Export failures

Errors should be user-friendly.

Do not expose stack traces to users.

---

# 26. UX Requirements

Use:

* Material 3
* Consistent spacing
* Clear hierarchy
* Simple navigation
* Confirmation dialogs for destructive actions
* Snackbars for reversible actions
* Loading states where required
* Empty states
* Helpful validation messages

Avoid unnecessary animations.

The application should feel lightweight and student-focused.

---

# 27. Navigation

Use Navigation Compose.

Suggested destinations:

* Onboarding
* Groups
* CreateGroup
* GroupDetails
* Members
* AddExpense
* EditExpense
* ExpenseDetails
* Expenses
* SettleUp
* Summary
* Settings

Navigation arguments should use stable IDs rather than passing complete domain objects between screens.

---

# 28. Database

Use Room.

Create:

* AppDatabase
* GroupEntity
* MemberEntity
* ExpenseEntity
* ExpenseSplitEntity
* SettlementEntity if settlement history is implemented

Use foreign keys where appropriate.

Use cascading deletion carefully.

Add Room migrations rather than destructive database resets.

---

# 29. Testing

Create comprehensive unit tests.

Test:

* Equal splitting
* Exact splitting
* Percentage splitting
* Shares splitting
* Rounding
* Balance calculation
* Debt simplification
* Repository behavior
* ViewModel state
* Input validation

Create Compose UI tests for important flows:

1. Create group
2. Add members
3. Add expense
4. View balances
5. Settle up
6. Edit expense
7. Delete expense
8. Change theme

Test accessibility semantics where practical.

---

# 30. Financial Precision

This is critical.

Never use Double for monetary calculations.

Represent money as Long integer minor units.

For example:

₹123.45 = 12345 paise.

All split algorithms must preserve:

sum(shares) == expense.amountMinorUnits

No money may appear or disappear due to rounding.

---

# 31. Security and Privacy

No account should be required.

No cloud storage should be required.

No personal financial information should leave the device.

Avoid collecting analytics unless explicitly requested.

Do not add unnecessary permissions.

---

# 32. Documentation

Create:

README.md

containing:

* Project overview
* Features
* Architecture
* Technology stack
* Database design
* Debt simplification algorithm
* Accessibility approach
* Testing strategy
* Build instructions

Also create:

docs/
architecture.md
database.md
accessibility-audit.md
debt-simplification.md
usability-testing.md

---

# 33. Code Quality

Follow Kotlin conventions.

Prefer:

* Immutable state
* Small composables
* Single responsibility
* Dependency injection only where justified
* Repository abstraction
* Testable business logic
* Meaningful names
* No unnecessary comments
* No duplicated business logic

Avoid:

* Giant composables
* God ViewModels
* Business logic inside UI
* Global mutable state
* Hardcoded financial calculations
* Floating-point money calculations
* Network dependencies

---

# 34. Implementation Strategy

Build incrementally.

Phase 1:
Project architecture and theme.

Phase 2:
Room database and domain models.

Phase 3:
Groups and members.

Phase 4:
Expenses and splitting.

Phase 5:
Balance calculation.

Phase 6:
Debt simplification.

Phase 7:
Search/filter and summaries.

Phase 8:
CSV export.

Phase 9:
Accessibility and high contrast.

Phase 10:
Testing and documentation.

Do not attempt to implement all phases in one operation.

After each phase:

1. Compile the project.
2. Run relevant tests.
3. Fix errors.
4. Review the diff.
5. Update documentation.
6. Continue to the next phase.
