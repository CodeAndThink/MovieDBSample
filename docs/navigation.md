# Navigation and deep links

## Structure

- `AppDestination`: typed allowlist of existing screens.
- `AppNavigator`: Navigation Component operations and back-stack policy.
- `NavigationViewModel`: checks AuthSession, coordinates navigation and stores the pending destination and queued command in SavedStateHandle.
- `DeepLinkService`: validates URLs and returns AppDestination, without session or Android UI dependencies.
- `AuthSession` / `FirebaseAuthSession`: replaceable session query and sign-out capability, independent of email/password registration.

Fragment ViewModels still emit UI effects. Fragments forward navigation effects to the shared
NavigationViewModel. MainActivity observes commands only while RESUMED and not showing a
loading overlay, then executes them through the navigator and acknowledges consumption.
The navigator is ActivityScoped; NavControllers detach when their hosts are destroyed.
MainFragment hosts its own Home/Settings graph. All explicit back-stack operations live in
AppNavigator; system Back is delegated to the primary Navigation hosts.

The navigation folder contains four files: AppDestination, DeepLinkService,
NavigationViewModel and AppNavigator. Concrete classes use constructor injection directly;
no resolver binding module or one-implementation navigator interface is needed.

## Supported links

- moviedb://app/search
- moviedb://app/home
- moviedb://app/settings
- moviedb://app/login
- moviedb://app/signup
- moviedb://app/detail/{movieId}

Scheme/authority/path must match exactly. Query parameters, fragments, ports, credentials,
encoded route aliases and unknown paths are rejected. Detail accepts a positive Int movie ID
without leading zeros. AppDestination uses sealed routes so Detail carries its required ID.
A new destination requires a typed route, graph destination, parser mapping, navigator handling and tests.

MainActivity declares a BROWSABLE custom-scheme filter and singleTop. Cold launches use
onCreate; warm launches use onNewIntent. Restoring an Activity does not replay its original
Intent. Repeated explicit deliveries remain valid, with singleTop navigation avoiding duplicates.

An invalid link keeps the current screen and displays a message. On a cold launch it falls
back to the regular Login/Home entry according to the current local session. No external URL
is automatically opened. HTTPS is deliberately not registered until a real domain and
/.well-known/assetlinks.json are available. iOS association files are not part of this project.
To add verified Android App Links later:

1. Add the exact owned host name to `HTTPS_HOSTS` in `DeepLinkService.kt`. The set is
   currently empty, so HTTPS links remain disabled. Enter only a host (no scheme, path,
   port or wildcard). `supportedOrigin` already accepts HTTPS origins from this set.
2. Add a separate HTTPS intent filter with `android:autoVerify="true"` and the same host
   in `AndroidManifest.xml`. Keep the existing custom-scheme filter.
3. Publish `/.well-known/assetlinks.json` with the app package and signing certificate.
4. Add resolver tests for that host and verify real links on a device.

The existing path mapping, query/fragment restrictions, authentication checks and navigator
are reused for HTTPS. No placeholder HTTPS domain is registered in the manifest.

## Authentication and back stack

- No local session: Home/Settings/Detail links save their destination and open Login.
- Visiting Sign Up or returning to Login preserves that pending destination.
- A newer protected link replaces the pending destination; invalid links do not overwrite it.
- Login/Sign Up success rechecks AuthSession and continues to the pending destination or Home.
- Entering Main removes all authentication screens from the root back stack.
- Settings sits above Home; Back returns to Home. Selecting Home removes Settings.
- Detail is a root destination alongside Main/Login, outside Main’s bottom navigation. A cold detail link creates Main underneath; an existing Main keeps its selected tab. Repeating the same movie keeps the current entry; a different movie replaces Detail. Back restores Main and its previous tab.
- Home item effects and Detail Back effects go through NavigationViewModel and AppNavigator.
- Signed-in users opening Login/Sign Up links go to Home.
- Settings → Log out clears the session and pending route, then resets the root stack to Login.
- Firebase local session presence controls routing only; Firestore/backend authorization remains independent.

Pending and queued routes are saved as route names (DETAIL:<movieId> for Detail), not raw URIs, credentials or tokens.
SavedStateHandle supports normal Android state restoration; force-stop/clearing data does not
preserve a pending flow. The current cold-launch behavior resumes a persisted Firebase session;
the old Remember me checkbox is not yet a separate persistence preference.

## Verification

./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest

Unit tests cover supported/rejected URIs, authentication gates, pending-link continuation and
restoration, invalid fallback, latest-link policy, logout, and consumption.
Instrumented tests exercise actual NavControllers, Sign Up recreation, duplicate destinations,
Main/Settings back behavior and authentication-stack clearing without creating Firebase users.

Manual examples:

adb shell am start -W -a android.intent.action.VIEW -d 'moviedb://app/signup' com.truongngo.moviedb
adb shell am start -W -a android.intent.action.VIEW -d 'moviedb://app/settings' com.truongngo.moviedb
adb shell am start -W -a android.intent.action.VIEW -d 'moviedb://app/unknown' com.truongngo.moviedb

## Verified emulator behavior (2026-09-14)

Installed the final debug APK on Pixel_8a (Android 16) and sent real ACTION_VIEW intents:

- Cold launch with moviedb://app/signup: Android resolved MainActivity and displayed the signup form.
- Warm launch with moviedb://app/settings while signed out: delivered to the existing Activity and displayed Login.
- Warm launch with moviedb://app/unknown: kept Login rather than navigating to another screen.
- Warm launch with moviedb://app/signup followed by Android system Back: returned to Login.

The preceding build passed 21 local unit tests and 3 instrumented tests. Pending destination
continuation after authentication was verified with a fake session in unit tests; this manual
check did not create or sign in a live Firebase account. Verified HTTPS App Links remain deferred
until an owned domain and assetlinks.json are available.

## Search

Search is a root destination with no bottom navigation. Home forwards SearchClicked through its effect to the shared navigation model. Detail opened from Search keeps Search underneath, including its query and results. Search debounces text changes by 500 ms, cancels older requests, rejects stale responses, and supports pagination/retry.
