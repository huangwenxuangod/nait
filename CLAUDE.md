# CLAUDE.md

This file provides guidance to Claude Code and other LLM assistants when working with code in this repository.

## Development Commands

### Android Client (Kotlin + Jetpack Compose)
- `./gradlew assembleDebug` - Build debug APK
- `./gradlew bundleDebug` - Build debug App Bundle
- `./gradlew test` - Run local unit tests (JUnit + MockK)
- `./gradlew connectedAndroidTest` - Run instrumented tests on device/emulator
- `./gradlew lint` - Run Android Lint check
- `./gradlew ktlintCheck` - Run Kotlin linter
- `./gradlew ktlintFormat` - Auto-format Kotlin code

### Cloud Backend (Supabase + Edge Functions)
- `supabase init` - Initialize Supabase project locally
- `supabase start` - Start local Supabase development stack
- `supabase stop` - Stop local Supabase development stack
- `supabase functions serve <function-name>` - Serve Edge Function locally for testing
- `supabase functions deploy <function-name>` - Deploy Edge Function to Supabase Cloud
- `supabase db push` - Push local schema changes to remote database

---

## Architecture Overview

The product direction is now **指尖 SOP / Nail-It**.

Nail-It is a **Tutorial-to-Try-On-to-Execution Layer** for DIY nail users. It takes a short-form nail tutorial, extracts the core style and production steps, generates a virtual try-on on the user's hand photo, and then rebuilds the tutorial into a hands-free SOP.

Important note: the Android skeleton still uses legacy `StyleMirror` package and class names. Treat those as implementation leftovers, not as the current product definition.

```text
+------------------------------------------------------------------------+
|                              Nail-It Architecture                      |
+------------------------------------------------------------------------+
|                                                                        |
|  [ Android Client (Kotlin) ]              [ Supabase Cloud Backend ]   |
|  +---------------------------+            +-------------------------+  |
|  | - Link Intake UI          |            | - Supabase Storage      |  |
|  | - Hand Photo Capture      |   HTTPS    |   (Hands, Try-On, SOP)  |  |
|  | - Step Card Interaction   | ---------> | - Supabase Postgres     |  |
|  | - Voice / Tap Navigation  | <--------- |   (Sessions, Results)   |  |
|  +---------------------------+  Realtime  | - Edge Functions        |  |
|                                           |   (AI Orchestration)    |  |
|                                           +-------------------------+  |
|                                                        |               |
|                                                        v               |
|                                           [ AI Parsing + Try-On Layer ]|
|                                           - Style extraction          |
|                                           - Virtual try-on rendering  |
|                                           - Structured SOP output     |
+------------------------------------------------------------------------+
```

### 1. Android Client (Native Kotlin)
- **UI & Navigation**: Jetpack Compose and Navigation Compose for single-activity declarative UI.
- **Dependency Injection**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`).
- **Data Layer**: Room DB for local caching of sessions and generated guides; DataStore for user preferences such as `install_id`.
- **Async & Network**: Kotlin Coroutines & Flow; Ktor or Retrofit for HTTP requests.
- **Hardware Integration**:
  - **CameraX**: Captures hand photos for try-on.
  - **Speech / touch shortcuts**: Powers hands-free or low-friction step switching in SOP mode.
  - **Local timers**: Handles UV/curing countdown without waiting on backend round-trips.

### 2. Cloud Backend (Supabase)
- **Authentication**: No mandatory login for MVP. Uses a client-generated `install_id` and one `session_id` per Nail-It flow.
- **Storage**: Supabase Storage for tutorial frames, hand photos, try-on outputs, and SOP media.
- **Database**: Supabase PostgreSQL for storing parsing results, try-on outputs, BOM lists, and SOP guides.
- **Edge Functions**: TypeScript-based functions that orchestrate parsing, try-on, and SOP generation tasks.
- **Realtime**: Supabase Realtime for streaming async task completion back to the Android app.

### 3. AI Orchestration Layer
- **Style Parser**: Extracts style tags, visual elements, materials, and high-level steps from the source tutorial.
- **Virtual Try-On Generator**: Places the extracted nail style on the user's hand photo for decision support.
- **Execution Package Generator**: Produces a structured BOM and a concise, step-by-step SOP.
- **Structured Outputs**: All major AI tasks should resolve into validated JSON payloads before reaching the app.

---

## Core MVP User Flow

1. **Home Screen**: Detect or accept a short video link.
2. **Parsing Transition**: Show extraction progress while the backend analyzes the tutorial.
3. **Hand Capture**: Guide the user to upload or shoot a hand photo.
4. **AI Try-On**: Render the nail style on the user's hand.
5. **BOM Checklist**: Show required tools and style-specific materials.
6. **Immersive SOP**: Walk the user through one step at a time with voice/tap navigation and timers.

---

## Code Style & Guidelines

### Kotlin & Android
- **Indentation**: 4 spaces.
- **Naming Conventions**:
  - Classes/Interfaces: `PascalCase`
  - Variables/Functions: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Layouts/Resources: `lowercase_snake_case`
- **Jetpack Compose**:
  - Prefix composable functions with capital letters.
  - Keep composables stateless where possible by hoisting state to the ViewModel or parent composable.
  - Prefer clear state models over premature optimization.
- **Architecture**: MVVM pattern. ViewModels expose state via Kotlin `StateFlow` and consume intents via functions.

### TypeScript & Supabase Edge Functions
- **Indentation**: 2 spaces.
- **Linting & Formatting**: Deno standards.
- **Error Handling**: Wrap API calls and database queries in try-catch blocks. Return standard HTTP error codes and descriptive JSON payloads.
- **Environment Variables**: Read secret tokens from Deno environment variables. Never hardcode keys.

### Git Commits
Use Conventional Commits:
- `feat`
- `fix`
- `docs`
- `style`
- `refactor`
- `test`
- `chore`
