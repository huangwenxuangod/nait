# Nail-It Android Skeleton

This repository is now positioned around **指尖 SOP / Nail-It**, an Android-first hackathon product for turning short-form nail tutorials into a `watch -> try -> do` workflow.

The codebase is still a lightweight Kotlin + Jetpack Compose skeleton, and some package/class names remain legacy `StyleMirror` identifiers for now. The documentation reflects the new Nail-It direction; the app code is the clean base we will evolve from.

## Product Snapshot

Nail-It helps users:

1. Parse a nail tutorial from a short video link
2. Virtually try the extracted style on their own hand photo
3. Convert the tutorial into a material checklist and hands-free SOP

This is not a generic beauty app. It is a **Tutorial-to-Try-On-to-Execution Layer** for DIY nail users.

## Project Structure

```text
.
├── AGENTS.md
├── CLAUDE.md
├── README.md
├── StyleMirror_Core_PRD.md
├── StyleMirror_Backend_API_PRD.md
├── nail-industry-research.md
├── nail-industry-china.md
├── nail-industry-regulation.md
└── app/
    └── src/main/
        ├── java/com/stylemirror/app/
        │   ├── MainActivity.kt
        │   ├── StyleMirrorApplication.kt
        │   └── ui/
        │       ├── StyleMirrorApp.kt
        │       ├── screens/
        │       └── theme/
        └── res/
```

## Current App State

- Single-activity Android app skeleton
- Jetpack Compose navigation placeholders
- Screen stubs that can be repurposed into Nail-It flow pages
- No Supabase functions or domain models wired yet
- Product docs updated to the Nail-It concept and backend contract

## Current Target Flow

1. Home / short video link intake
2. Tutorial parsing transition
3. Hand photo capture
4. AI try-on result
5. BOM checklist
6. Immersive SOP execution

## Important Context

- `StyleMirror_Core_PRD.md` now acts as the main product PRD for Nail-It.
- `StyleMirror_Backend_API_PRD.md` defines the Supabase and Edge Function contract for the Nail-It MVP.
- The three `nail-industry-*.md` files capture the market, China channel, and compliance research that informed the current direction.

## Next Step

Use this skeleton as the base for the Nail-It implementation:

- re-map the placeholder screens to the new flow,
- scaffold the Supabase directory and contracts,
- then wire the first end-to-end demo for `parse -> try-on -> SOP`.
