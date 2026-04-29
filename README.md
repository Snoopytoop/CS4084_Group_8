# CS4084 Group 8 — Climber Social

A social media app for rock climbers, similar to Strava. Users can share posts, log climbing routes, find belay partners, message other climbers, and track progress on a leaderboard.

## Features

- **Authentication** — Email/password registration and login via Firebase Auth, plus a separate admin login
- **Profiles** — User profiles with bio, profile image, and post history
- **Posts & feed** — Create, like, and comment on posts in the home feed
- **Blogs** — Long-form blog posts with validation
- **Route log** — Record climbed routes with offline-first sync back to Firestore
- **Find a belayer** — Post and browse belay partner requests
- **Direct messaging** — One-to-one chat with conversation inbox
- **Leaderboard** — Rankings based on climbing activity
- **Search** — Find users across the app
- **Admin dashboard** — Moderation tools for admin accounts

## Tech stack

- **Platform:** Android (Java), `minSdk 24`, `targetSdk 36`
- **UI:** AndroidX, Material Components, ConstraintLayout, RecyclerView, ViewBinding
- **Backend:** Firebase
  - Authentication (email/password)
  - Cloud Firestore (data)
  - Storage (images)
- **Build:** Gradle (Kotlin DSL)

## Getting started

1. Clone the repo
2. Open in Android Studio
3. Add your `google-services.json` to `app/` (required — not committed)
4. Sync Gradle and run on an emulator or device (API 24+)

## Project structure

- `app/src/main/java/com/example/cs4084_group_8/` — Activities, adapters, models
- `app/src/main/res/layout/` — Portrait layouts
- `app/src/main/res/layout-land/` — Landscape layouts

## Course

CS4084 — Mobile Application Development, Group 8.
