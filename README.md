# MinichatUI — Android (prototype)

Native Android/Compose frontend for MinichatUI, with two modes:

- **User mode** — chat directly with your own OpenAI-compatible or Anthropic
  key, entered on-device. No server involved; the key never leaves the
  phone. Ports the `streamDirectChat` / `ADAPTERS` logic from
  `static/js/lib/adapters.js` and `static/js/api.js`.
- **Remote mode** — connects to an existing MinichatUI Cloudflare Worker
  instance using the same gate → login → session-cookie flow as the web
  app. Scope for this prototype: **gate + login only**. Signup, invites,
  recovery, and the admin panel are intentionally left web-only — if
  there's no account yet, the login screen points you to the web app.

## What's here

- Plain Jetpack Compose UI — no `androidliquidglass` yet, on purpose. This
  is the architecture/data-flow prototype; glass skinning comes after this
  works end to end.
- `ChatViewModel` holds a single `ChatRepository`-shaped flow that routes to
  either `ProviderClient` (User mode) or `RemoteApi` (Remote mode), both
  feeding the same `ChatScreen` UI.
- Colors in `ui/theme/Theme.kt` are pulled 1:1 from `static/styles.css`
  (`--bg`, `--accent`, etc.) so it reads as the same product as the web app.

## Building

No local Android Studio needed — GitHub Actions (`.github/workflows/build.yml`)
builds a debug APK on every push to `main` and uploads it as a workflow
artifact. Push this to a new repo and grab the APK from the Actions tab.

To build locally instead:

```
./gradlew assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Known gaps (prototype, not final)

- Remote-mode session isn't persisted across app restarts yet (cookie jar
  is in-memory only) — you'll need to log in again each cold start.
- BYOK key in User mode is stored in plain DataStore, not encrypted at
  rest — fine for a personal single-device prototype, not for anything
  shared.
- No local chat history persistence yet for either mode — chats live only
  in memory for the current session (Remote mode's chat list/create/rename
  endpoints are wired in `RemoteApi` but not yet used by the UI).
- No markdown rendering in message bubbles yet (web app uses `marked` +
  `DOMPurify`).
