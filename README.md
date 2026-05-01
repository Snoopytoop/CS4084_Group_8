# CS4084 Group 8 — Climber Social

A social media app for rock climbers, similar to Strava. Users can share posts, log climbing routes, find belay partners, message other climbers, and track progress on a leaderboard.

---

## Features

### Authentication
- Email/password **registration** (requires username) and **login** via Firebase Auth
- On first login, users complete a **profile setup** step
- **Offline mode** — enter the app without an account; server-dependent features are disabled but Route Logbook still works
- Separate **admin login** portal (not accessible from the normal login screen)

### Home Feed
- Real-time feed of posts from all users, newest first
- **Like** posts (toggled, count updates live)
- **Comment** on posts — view and add comments in a dialog
- Quick-access buttons to all major sections
- Detects Firebase connectivity on launch — shows an **offline chip** and **Retry Connection** button if unreachable

### Posts
- Create posts with text content and an optional image (up to 10 MB)
- Post type is auto-detected:
  - Image attached → **image**
  - Contains a YouTube / Vimeo / video URL → **video**
  - Over 260 characters → **blog**
  - Otherwise → **status**
- Tap an author's name to open their profile

### Blog Posts
- Dedicated Blogs section for long-form posts
- Post owners see a badge and can delete their own entries

### Route Logbook
- Log climbing routes with: route name, grade, number of attempts, send status, and notes
- **Offline-first** — entries saved locally and synced automatically when back online
- Delete your own entries; sorted by date, newest first

### Find a Belayer
- Post a listing with: wall name, available days/times, belay capability (Lead / Top Rope / Both), climbing level, and notes
- Browse all active listings
- **Message** a poster directly to open a chat
- Delete your own listing
- Summary stats: total posts and number of unique walls

### Direct Messaging
- Start a private conversation from a belayer post, user profile, or search result
- Real-time chat with **pending / sent** delivery status indicators
- **Inbox** lists all conversations with latest message preview and timestamp

### User Profiles
- Profile photo (uploaded from gallery), username, and bio
- Fastest speedwall time shown on profile
- View any user's profile by tapping their name

### Search
- Search for other users by username
- Tap a result to open their profile

### Leaderboard (Speedwall)
- Global leaderboard ranked by fastest speedwall time
- Long-press your own entry to delete it

### Admin Dashboard
- Separate admin login (not shown on the normal login screen)
- View recent posts, route logs, and belayer listings
- Moderate content across the app

---

## Navigation

A **bottom navigation bar** is present on most screens:

| Button | Destination |
|---|---|
| Home | Home feed |
| Search | Search users |
| Create (pencil) | Create a new post |
| Leaderboard (trophy) | Speedwall leaderboard |
| Profile avatar | Your profile |

From the **Home screen**, quick-action buttons also link directly to:
- Route Logbook
- Messages (Inbox)
- Find a Belayer
- Blogs

---

## Offline Mode

On launch the app probes Firebase with a 12-second timeout:
- **Online** — all features enabled, pending route log entries synced automatically
- **Offline** — server-dependent features (posts, blogs, belayer, search, leaderboard, profile) are dimmed and blocked; Route Logbook still works fully
- The **Retry Connection** button re-probes without restarting
- Users can also explicitly enter offline mode from the login screen

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java (Android SDK 24+, target 36) |
| UI | Material Design 3 (MDC-Android), ViewBinding |
| Database | Firebase Firestore (real-time) |
| Auth | Firebase Authentication (email/password) |
| Storage | Firebase Storage (images) |
| Image loading | Glide 4.16 |
| Build | Gradle (Kotlin DSL), ProGuard/R8 minification on release |

---

## Getting Started

### Prerequisites
- Android Studio (Hedgehog or later)
- JDK 11
- A Firebase project with **Firestore**, **Authentication** (email/password), and **Storage** enabled

### Steps

1. **Clone the repo**
   ```bash
   git clone https://github.com/Snoopytoop/CS4084_Group_8.git
   cd CS4084_Group_8
   ```

2. **Add `google-services.json`**
   Download from the Firebase console and place at:
   ```
   app/google-services.json
   ```
   *(This file is not committed — each developer needs their own copy.)*

3. **Open in Android Studio**
   File → Open → select the project root, let Gradle sync.

4. **Run**
   Select a device or emulator (API 24+) and press ▶.

---

## Project Structure

```
app/src/main/java/com/example/cs4084_group_8/
  Activities          — MainActivity, HomeActivity, ChatActivity, ...
  Adapters            — PostAdapter, BlogPostAdapter, RouteLogAdapter, ...
  Models              — Post, BlogPost, RouteLogEntry, DirectMessage, ...
  Utilities           — ServerFeatureGate, NetworkStatus, OfflineSessionManager, ...

app/src/main/res/
  layout/             — Portrait layouts
  layout-land/        — Landscape layouts
  values/             — strings.xml, colors.xml, arrays.xml
```

---

## Course

CS4084 — Mobile Application Development, Group 8.
