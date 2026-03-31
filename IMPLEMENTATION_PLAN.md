# Implementation Plan

Generated: 2026-03-31

## Goal

Upgrade `main` so that:

- admin login is properly integrated into the app flow,
- route logging becomes a real account-backed feature instead of local-only device data,
- find-a-belayer becomes a real shared board instead of local-only device data,
- the work can later be split into small human-sized commits.

## Current Status

- Shared auth/admin wiring is implemented on `main`.
- Route logging is now Firestore-backed and tied to the signed-in account.
- Find-a-belayer is now Firestore-backed and shared across signed-in users.
- The admin dashboard now shows live Firestore counts and recent activity across accounts, feed posts, route logs, and belayer posts.
- Ownership checks were added so delete flows do not rely only on hidden buttons.
- `./gradlew assembleDebug` passes after these changes.

## Scope

### 1. Shared Auth + Admin Wiring

- Add a shared role model for user vs admin.
- Default newly registered users to the `user` role.
- Route already signed-in admins to an admin dashboard.
- Prevent admins from using the normal member login path.
- Add an explicit admin login entry point from the auth screen.
- Add admin dashboard screens to the manifest and app navigation.

### 2. Route Logging Upgrade

- Port the route-log UI from `Iwan` onto `main`.
- Replace local `SharedPreferences` storage with Firestore-backed per-user data.
- Support:
  - create route log entries,
  - realtime list of the current user’s entries,
  - summary stats,
  - delete own entries,
  - proper empty/loading/error states.

### 3. Find-a-Belayer Upgrade

- Port the belayer board UI from `Iwan` onto `main`.
- Replace local `SharedPreferences` storage with Firestore-backed shared board data.
- Support:
  - create belayer posts,
  - realtime shared list,
  - delete own posts,
  - a usable contact path instead of a dead stub.

### 4. Admin Dashboard Improvement

- Replace placeholder admin actions with useful live data.
- Show current admin identity and live counts for:
  - users,
  - posts,
  - route logs,
  - belayer posts.
- Add a simple moderation surface for belayer posts if time permits.

### 5. Integration

- Add quick entry points to the new screens from the existing `main` product flow.
- Keep all existing social features on `main` intact.
- Verify `assembleDebug` after the feature work lands.

## Intended Commit Split

1. Shared auth roles, admin login wiring, manifest updates.
2. Route log models, layouts, activity, Firestore integration.
3. Belayer board models, layouts, activity, Firestore integration.
4. Admin dashboard improvements and final navigation polish.
5. Verification fixes and cleanup.

## Known Constraint

The app can improve admin gating on the client, but truly strong admin security still depends on backend-side enforcement such as Firebase custom claims and security rules. That part is outside the Android codebase alone.
