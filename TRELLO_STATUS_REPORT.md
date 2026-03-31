# Trello Status Report

Generated: 2026-03-31

## Repo Snapshot

- Fetched latest remote refs from `origin`.
- Current local branch: `Iwan` at `fff748c`, which is `ahead 1` of `origin/Iwan`.
- `origin/main` moved to `b2de994`.
- New/fetched remote branches seen during fetch: `origin/Leaderboard`, `origin/Simon`, `origin/universal-navigation-bar`.
- No repo-local `README.md`, `AGENTS.md`, or `CLAUDE.md` files were present in this checkout.

## Branch Reality

- `origin/main` is the current integration line for the social-app features.
- `origin/Simon` is fully contained in `origin/main` and adds no commits that are newer than `main`.
- `origin/universal-navigation-bar` is fully contained in `origin/main` and adds no commits that are newer than `main`.
- `origin/Leaderboard` is fully contained in `origin/main`, but is an older stale snapshot.
- `origin/Iwan` is the only remote branch that still diverges from `origin/main`.
- Local `Iwan` has one extra unpushed commit on top of `origin/Iwan`: `fff748c Register admin login and dashboard activities`.

## Build Verification

- `HEAD` (`fff748c`, local `Iwan`): `assembleDebug` passed.
- `origin/Iwan` (`9f10881`): `assembleDebug` passed.
- `origin/main` (`b2de994`): `assembleDebug` passed.
- `origin/Simon` (`d3864ac`): `assembleDebug` passed.
- `origin/universal-navigation-bar` (`c8fb0a0`): `assembleDebug` passed.
- `origin/Leaderboard` (`bbd6d1b`): `assembleDebug` failed because `app/google-services.json` is missing while the Google Services plugin is still required.

## Trello Card Mapping

| Trello card | Status from code | Branch evidence | Notes |
| --- | --- | --- | --- |
| User Sign up and login | Done | `origin/main`, `origin/Iwan`, local `Iwan` | `main` has Firebase register/login and routes users into profile setup/home. `Iwan` keeps member auth and adds admin-vs-user role checks. |
| User profiles | Done on `main` only | `origin/main` | Profile setup, bio/image save, own vs other-user profile viewing, edit/logout flow all exist on `main`. These screens are removed from `Iwan`. |
| Social Feed | Done on `main` only | `origin/main` | Feed listens to Firestore posts, supports likes, comments, and author profile navigation. Removed from `Iwan`. |
| Posting | Done on `main` only | `origin/main` | Create-post flow exists with text, image upload, URL/video detection, and Firestore post creation. Removed from `Iwan`. |
| make a universal navigation bar | Done and merged into `main` | `origin/universal-navigation-bar` -> `origin/main` | Shared bottom navigation is present in `main` and the dedicated branch is stale. |
| Leaderboards | Done and merged into `main` | `origin/Simon`, `origin/universal-navigation-bar`, `origin/main` | `Simon` personal-board work and universal branch delete-time work are already in `main`. |
| add admin login | In testing / not fully landed | `origin/Iwan`, local `Iwan` | Admin login + dashboard code exists on `Iwan`, but the remote branch tip does not register those activities in the manifest. The local unpushed commit fixes that. Dashboard actions are still stubs. |
| Route Logging | Implemented, but still testing/partial | `origin/Iwan`, local `Iwan` | Route log UI exists and works, but storage is local `SharedPreferences`, not shared backend data. |
| Find a belayer | Implemented, but still doing/partial | `origin/Iwan`, local `Iwan` | Posting/listing exists, but data is local `SharedPreferences` only and messaging is explicitly a stub. |
| Session tracking | Not really implemented | `origin/Iwan`, local `Iwan` | Only placeholder copy about admin/root session state was found; there is no actual session-tracking feature. |
| Performance Analytics | Not found | none | No implementation found. |
| project mode | Not found | none | No implementation found. |
| outdoor mode | Not found | none | No implementation found. |
| Blogs | Partial only | `origin/main`, `origin/Simon` | Posting code infers a `blog` post type for long-form content, but there is no separate blog feature/screen. |
| Advice & Tips | Not found | none | No implementation found. |
| Add forget password feature | Not found | none | No forgot/reset-password flow found. |
| Make a readme for the project | Not done | none | No `README.md` exists in the repo. |

## Important Conclusions

- There is **not** one branch that contains everything on the Trello board.
- `origin/main` contains the finished social-app line:
  - signup/login
  - profiles
  - feed
  - posting
  - universal nav
  - leaderboard
- `origin/Iwan` is a **separate diverged line** that adds:
  - admin login/dashboard
  - find-a-belayer
  - route logging
- But `origin/Iwan` also removes the social-app screens from `main`, so it is not an incremental branch on top of the current integrated product.
- The local unpushed `Iwan` commit matters: without it, the remote `Iwan` branch has admin activities in code but not declared in the manifest.

## Evidence Highlights

- `origin/main` auth flow writes/reads Firebase user records and routes into profile setup/home.
- `origin/main` profile flow saves bio and profile image, and profile view supports own-profile edit vs viewing other users.
- `origin/main` home feed listens to Firestore `posts` and supports likes/comments.
- `origin/main` create-post flow creates `status`, `blog`, `video`, and `image` style posts.
- `origin/main` leaderboard supports global board, personal history, time submission, and delete-own-time.
- Local `Iwan` / `origin/Iwan` route logging and belayer posting both persist to local `SharedPreferences` stores rather than a shared backend.
- Local `Iwan` strings and admin dashboard code explicitly mark moderation, user management, root session verification, and DM wiring as scaffolding / coming soon.

## Recommended Next Step

- If you want one branch to represent the latest usable app, use `origin/main` as the base.
- If you want the `Iwan` work too, it should be merged selectively into `main` rather than replacing `main` with `Iwan`, because `Iwan` drops the social features that are already done.
