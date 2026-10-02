# Contact Grid – Android speed-dial widget

A home-screen widget with a 3×3 grid of contact photos. Tap a photo → the phone calls that person.

## Features
- 9 contacts, photo + name; contacts without a photo get a colored initials tile
- Opens at 4×4 cells (fits every phone launcher); drag the corners to stretch it up to a full screen
- Editor: tap a square to pick a contact (only contacts with a phone number are shown), long-press to clear, Save
- Edit later via: app icon, tapping an empty square, or long-press widget → *Reconfigure/Settings* (Android 12+)
- Several widgets can be placed; each keeps its own list
- Calls directly if "Phone" permission is granted, otherwise opens the dialer with the number pre-filled
- No internet permission, no analytics, data stays in the app's private storage

## Build – option A: GitHub Actions (no local tools)
1. Create a new (private) GitHub repo and push this folder.
2. Actions tab → **Build APK** runs automatically (~3–4 min).
3. Download the `ContactGrid-apk` artifact → `app-debug.apk`.

## Build – option B: Android Studio
Open the folder → let Gradle sync → **Build ▸ Build APK(s)** (or Run on a connected phone).
CLI: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`

## Install on the phone
Copy the APK to the phone and open it (allow "Install unknown apps" for your file manager once),
or `adb install app-debug.apk`.

Then: long-press home screen → Widgets → **Contact Grid** → drag to screen → pick contacts → Save.
Grant **Contacts** (photos) and **Phone** (direct call) when asked.

## Notes
- Samsung One UI: if long-press doesn't show an edit option, use the app icon or an empty square.
- Stack: Kotlin, AGP 8.9.1, Gradle 8.14.3, minSdk 26 (Android 8), targetSdk 34.
"# AndroidContactsWidgit" 
