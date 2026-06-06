# AGENTS.md

This file provides guidance to Code Agents (Codex, Cursor, Windsurf, etc.) when working with code in this repository.

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

The current product direction is **指尖 SOP / Nail-It**.

Nail-It is a **Tutorial-to-Try-On-to-Execution Layer** for DIY nail users. It turns a short-form nail tutorial into:

1. a parsed style and process summary,
2. a virtual try-on on the user's real hand,
3. a checklist and immersive SOP for execution.

Important implementation note:
- The repository still contains legacy `StyleMirror` package names and screen names in the Android skeleton.
- Treat those names as transitional scaffolding unless a task explicitly asks to rename them.

```text
+------------------------------------------------------------------------+
|                              Nail-It Architecture                      |
+------------------------------------------------------------------------+
|                                                                        |
|  [ Android Client (Kotlin) ]              [ Supabase Cloud Backend ]   |
|  +---------------------------+            +-------------------------+  |
|  | - Link Intake UI          |            | - Supabase Storage      |  |
|  | - Hand Photo Capture      |   HTTPS    |   (Hands, Try-On, SOP)  |  |
|  | - SOP Card Interaction    | ---------> | - Supabase Postgres     |  |
|  | - Voice / Timer Controls  | <--------- |   (Sessions, Results)   |  |
|  +---------------------------+  Realtime  | - Edge Functions        |  |
|                                           |   (AI Orchestration)    |  |
|                                           +-------------------------+  |
|                                                        |               |
|                                                        v               |
|                                           [ AI Parsing + Try-On Layer ]|
|                                           - Tutorial parsing         |
|                                           - Virtual try-on           |
|                                           - BOM + SOP generation     |
+------------------------------------------------------------------------+
```

### 1. Android Client (Native Kotlin)
- **UI & Navigation**: Jetpack Compose and Navigation Compose for single-activity declarative UI.
- **Dependency Injection**: Hilt (`@HiltAndroidApp`, `@AndroidEntryPoint`).
- **Data Layer**: Room DB for local caching of sessions and generated guides; DataStore for `install_id`, preferences, and lightweight progress state.
- **Async & Network**: Kotlin Coroutines & Flow; Ktor or Retrofit for HTTP requests.
- **Hardware Integration**:
  - **CameraX**: Used for hand-photo capture.
  - **Voice / gesture-light controls**: Used to advance through hands-free SOP steps.
  - **Local timers**: Used for curing/waiting steps.

### 2. Cloud Backend (Supabase)
- **Authentication**: No mandatory login for MVP. Use a client-generated `install_id` and per-flow `session_id`.
- **Storage**: Supabase Storage for tutorial frames, hand photos, try-on results, and SOP assets.
- **Database**: Supabase PostgreSQL for storing parsing results, try-on outputs, BOM lists, and SOP guides.
- **Edge Functions**: TypeScript-based functions act as the orchestration layer between the client, the database, and the AI layer.
- **Realtime**: Supabase Realtime streams async task completion back to the Android client.

### 3. AI Structured Outputs
- Always use strict JSON schemas when defining the system instructions for AI-backed tasks.
- Maintain consistency between TypeScript schemas in Edge Functions and Kotlin models in the Android app.
- Current high-level schema families include:
  - `SourceParseSchema`
  - `TryOnSchema`
  - `BomSchema`
  - `SopSchema`

---

## Core MVP User Flow

1. **Home Screen**: Detect or accept a short video link.
2. **Parsing Transition**: Show extraction progress.
3. **Hand Capture**: User uploads or shoots a hand photo.
4. **AI Try-On**: User previews the style on their own hand.
5. **BOM Checklist**: User confirms materials are ready.
6. **Immersive SOP**: One-step-at-a-time execution with voice/tap navigation and timers.

---

## Agent Instructions & Guidelines

### 1. Code Modification Safety
- Always run `./gradlew lint` or `./gradlew ktlintCheck` after making changes to Kotlin files.
- Keep edits target-specific. Avoid rewrites of entire classes unless explicitly requested.
- For Jetpack Compose, ensure state is properly hoisted.
- Do not hardcode localized strings; use `stringResource(R.string.id)` and update `res/values/strings.xml` as needed.

### 2. Supabase Integration
- Ensure all Supabase Edge Functions are written in TypeScript and conform to Deno runtime requirements.
- Keep database schema updates aligned with Kotlin entities and TypeScript contracts.
- Edge Functions must handle CORS headers correctly:
  ```typescript
  export const corsHeaders = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  };
  ```

### 3. Image and Task Flow Design
- Treat AI try-on as an async task, not a blocking UI operation.
- All AI-generated state that drives UI navigation should be persisted server-side.
- Do not let the client become the source of truth for session status.

### 4. CameraX and Immersive Flow
- CameraX should be lifecycle-bound where used.
- Long-running or CPU-heavy image preparation should stay off the main thread.
- Ensure resources are cleaned up properly when composables are disposed.
- Favor oversized tap targets and low-friction interaction patterns in SOP mode.

---

## Code Style & Linting Standards

- **Kotlin Formatting**: 4 spaces, no wildcards in imports, trailing commas for multi-line parameter lists.
- **Jetpack Compose**: PascalCase for `@Composable` functions, camelCase for parameters. Use `Modifier` as the first optional parameter of custom composables.
- **TypeScript Formatting**: 2 spaces, semicolons required, camelCase for variables and functions.
- **Git Commits**: Use Conventional Commits (`feat:`, `fix:`, `docs:`, `style:`, `refactor:`, `test:`, `chore:`).
