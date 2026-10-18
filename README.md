# 💰 ExpenseSum

**ExpenseSum** is a modern, privacy-focused, 100% offline Android utility app designed for quickly totaling and analyzing expense items. Built with **Kotlin** and **Jetpack Compose (Material 3)**, ExpenseSum delivers instant financial totals, average calculations, expense breakdowns, and calculation history without requiring accounts, cloud sync, or internet connectivity.

---

## ✨ Features

- ⚡ **Instant Expense Summation**: Quickly add expenses with custom labels and amounts to get immediate total sums, item counts, averages, and min/max metrics.
- 🌍 **Multi-Currency Support**: Native support for major global currencies including USD ($), EUR (€), GBP (£), INR (₹), JPY (¥), PHP (₱), and more with customized decimal formatting and currency positions.
- 📜 **Calculation History**: Save, name, search, view, and organize past expense calculations with timestamp tracking and itemized breakdowns.
- 📤 **Copy & Share**: Effortlessly copy expense summaries or formatted item lists to your clipboard or share them via external messaging and notes apps.
- 🎨 **Material 3 Design & Themes**: Fully customizable dark/light themes and Material You dynamic color support.
- 🔒 **100% Private & Offline**: Operates completely on-device without internet permissions (`android.permission.INTERNET` is neither requested nor required), tracking, or ad networks.
- ⚡ **Haptic & Visual Feedback**: Fluid animations, haptic responses on key interactions, and custom app preferences.

---

## 🛠️ Tech Stack & Architecture

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3 Design
- **Architecture**: MVVM (Model-View-ViewModel) + Unidirectional Data Flow (UDF)
- **State Management**: `StateFlow` & `StateFlow`-driven UI States
- **Navigation**: [Jetpack Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Data Persistence**: Key-Value / SharedPreferences architecture for local storage
- **Asynchronous Flow**: Kotlin Coroutines
- **Build System**: Gradle Version Catalog (`libs.versions.toml`) with Kotlin DSL
- **Minimum SDK**: API 26 (Android 8.0)
- **Target / Compile SDK**: API 37 (Android 15)

---

## 📱 App Screenshots & Structure

```
ExpenseSum/
├── app/
│   ├── src/main/java/com/example/expensesum/
│   │   ├── data/            # Local data persistence & serialization
│   │   ├── domain/          # Business logic, calculators, money parsers & formatters
│   │   ├── model/           # Data models and UI state definitions
│   │   ├── repository/      # History and Settings repositories
│   │   ├── ui/              # Compose screens (Home, History, Settings, About, Splash)
│   │   └── util/            # Platform helpers & haptics
│   └── src/test/            # Comprehensive JUnit unit test suite
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Ladybug (2024.2.1) or newer recommended
- **JDK**: Java 11 or higher
- **Android Device / Emulator**: Running Android 8.0 (API level 26) or higher

### Building & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/ExpenseSum.git
   cd ExpenseSum
   ```

2. **Open in Android Studio**:
   - Launch Android Studio and select **Open**.
   - Navigate to the cloned `ExpenseSum` directory and open it.

3. **Build the project**:
   - Run via Gradle wrapper:
     ```bash
     ./gradlew assembleDebug
     ```

4. **Run Unit Tests**:
   - Execute the test suite:
     ```bash
     ./gradlew test
     ```

---

## 🧪 Testing

ExpenseSum includes extensive unit tests covering core domain logic:
- Calculation logic (totals, averages, minimum/maximum items, edge cases)
- Money parsing & formatting algorithms across various currency formats
- History repository and settings persistence state handling
- ViewModel state mutations and validation workflows

---

## 📄 License

```
Copyright (c) 2025 ExpenseSum
Licensed under the MIT License.
```
