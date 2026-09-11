# TextMe — an iMessage-style app for Android

Same core idea as iMessage: message other app users over the internet
(no phone number needed to sign up), and fall back to a real SMS — sent
from the phone's own SIM — when texting someone who doesn't have the app.

## How the routing works
1. User adds a contact/phone number.
2. `ContactResolver` checks Firestore's `users` collection for that phone number.
   - **Match found** → in-app chat: messages go through Firestore in real time,
     free, works over wifi/data, push notifications via FCM. Blue bubble.
   - **No match** → SMS fallback: `SmsFallbackSender` sends via the device's
     own `SmsManager` (Android only, requires a SIM, costs whatever the
     user's carrier plan costs — there's no server or third-party API
     involved). Green bubble, mirroring the real iMessage convention.
3. Either way the message shows up in the same thread UI, so the user never
   has to think about which channel was used.

## Bug fixes applied
A pass was made through the whole scaffold to find and fix real bugs:
- **Missing launcher icon** — the manifest referenced `@mipmap/ic_launcher` but
  no such resource existed, which fails the build. Added an adaptive icon.
- **Missing dependency** — `Icons.Default.Edit` was used without adding
  `androidx.compose.material:material-icons-core`, a compile error.
- **No sign-in, anywhere** — nothing ever called Firebase Auth, so
  `currentUid` was always null and every read/write in `FirebaseRepository`
  silently no-op'd with no visible error. Added an anonymous-auth bootstrap
  (a placeholder — see "sign-up flow" below) so the app is actually
  functional end to end.
- **Asymmetric chat IDs** — two people messaging each other used to compute
  *different* chat IDs (each one keyed off only the other person's uid),
  splitting one conversation into two one-sided threads. Fixed to a
  symmetric ID derived from both uids, sorted.
- **Chat metadata never saved** — only `lastMessage`/`lastTimestamp` were
  written to Firestore; `contactName`, `isAppChat`, etc. were never
  persisted, so the chat list would read back blank names and mislabel
  every chat as SMS. Now the full `Chat` object is written (merge-safe).
- **Wrong name on the recipient's side** — the mirrored chat entry for the
  other participant was reusing the sender's own local nickname for them
  (meaningless on their end). Fixed to describe the sender instead.
- **SMS API incompatible with minSdk** — `getSystemService(SmsManager::class.java)`
  only exists on API 31+; on a minSdk-26 device it returned null and sends
  silently failed. Switched to `SmsManager.getDefault()`.
- **Silent send failures** — failed sends (denied SMS permission, dropped
  Firestore write) used to vanish with no feedback. Now surfaced via Toast.
- **Dark-mode contrast** — received message bubbles were hardcoded to a
  light-mode-only gray/black regardless of theme. Now theme-aware.

## What's scaffolded vs. what you still need to do
Scaffolded:
- Data models, Firestore repository, contact resolver, SMS sender
- Chat list + chat thread + settings screens (Jetpack Compose)
- Bubble-color and light/dark customization
- Manifest permissions

Still to wire up (marked `// TODO` in code):
- **Firebase project**: create one at console.firebase.google.com, enable
  Firestore + Authentication (phone or email sign-in) + Cloud Messaging,
  download `google-services.json` into `app/`.
- **Sign-up flow**: the app now bootstraps with Firebase **anonymous** auth
  so it's functional, but anonymous users have no phone number/display name,
  so nobody can discover them yet. Replace with a real onboarding screen
  (phone/email + display name) that calls `repo.upsertCurrentUser(...)`.
- **New-message / contact picker**: `onNewMessage` in `MainActivity` is a
  stub — wire it to `android.provider.ContactsContract` to let the user pick
  a contact, normalize their number to E.164, then call `resolver.resolve()`.
- **Push notifications**: `PushMessagingService` receives the FCM payload
  but doesn't build a `Notification` yet. You'll also need a small Cloud
  Function (or Firestore-triggered function) that sends the FCM push when a
  new message doc is created — since there's no other backend, this is the
  one server-side piece you can't avoid if you want push notifications.
- **Incoming SMS**: this scaffold only *sends* SMS. To show a contact's SMS
  replies in the same thread, you'd add a `BroadcastReceiver` for
  `SMS_RECEIVED` and register your app as the default SMS app (a much bigger
  permission ask — Android requires this to read incoming texts at all).

## Building an installable APK
There's no APK bundled with this download — it has to be compiled on a
machine with the Android SDK (this project was only ever assembled as
source). Once you've opened the project in Android Studio at least once
(so the SDK/Gradle are set up):

```
./build_apk.sh debug      # fastest — installs on your own device via adb, unsigned
./build_apk.sh release    # signed — the kind you'd share with someone else or upload to Play Console
```

`debug` is enough to just try the app on your own phone: build it, then
either drag the resulting `app-debug.apk` onto a plugged-in device in
Android Studio, or run `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

`release` will generate a signing keystore the first time you run it (keep
`keystore.jks` and its password safe — losing it means you can never
publish an update under the same app identity again).

If you don't want to touch the command line at all, Android Studio's
**Build → Generate Signed App Bundle / APK** menu does the same thing
through a GUI wizard.

## Requirements
- Android Studio (Koala or newer), min SDK 26
- A Firebase project with `google-services.json` in `app/`
- Physical device or emulator with a SIM/telephony service for SMS to work
  (SMS won't send from most emulators without a configured radio)
