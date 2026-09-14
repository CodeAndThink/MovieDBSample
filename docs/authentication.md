# Email/password authentication

Flow: LoginFragment → LoginViewModel → AuthRepository → AuthRepositoryImpl →
EmailPasswordAuthProvider → FirebaseEmailPasswordAuthProvider → Firebase Authentication.

`AuthRepository` is the domain contract. Its login and signUp methods return
`Result<AuthUser>`. `AuthUser` contains an identity ID, email, display name and
email verification status. Firebase SDK models never reach the presentation layer.
The repository converts provider exceptions into failed results, preserving coroutine
cancellation. The Firebase adapter uses cancellable Task.await(). Cancelling the coroutine
does not guarantee cancellation of an authentication request already sent to Firebase.

## Firebase setup

Use the existing app/google-services.json for this Android application. In the corresponding
Firebase project, enable Authentication → Sign-in method → Email/Password and create a test
user in Authentication → Users. Launching the app opens Login without a session or Main with
an existing Firebase session. Successful authentication opens Main or a pending deep-link destination. No live credentials are bundled in the project.

Firestore documents in `accounts` are not Authentication users. This implementation does not
read or create account documents because their schema and identity mapping are unspecified.
To load a profile later, use a separate AccountRepository and associate documents with the
Authentication UID. Do not put passwords in Firestore.

## Replacing Firebase

Create another subclass of EmailPasswordAuthProvider, implement login/signUp and return
AuthUser. Change the provider binding in AuthModule. Repository and LoginViewModel need no
Firebase-specific changes. The parent contract is deliberately scoped to email/password;
Google OAuth, phone or other credential types should get separate narrow contracts rather
than unsupported email/password methods.

## Successful response

Firebase signInWithEmailAndPassword returns Task<AuthResult>. After awaiting it, its user
is mapped to AuthUser. FirebaseUser exposes uid, email, displayName and isEmailVerified.
A backend ID token can be requested separately with FirebaseUser.getIdToken(false);
it is not returned in LoginState and is not an account profile from Firestore.

## Current boundaries

Firebase manages its persisted session internally. The app resumes that session on a fresh
launch; Remember me only exists in form state and does not change Firebase session persistence. Email verification is exposed as data, not enforced as a login gate.

## Verification

Run ./gradlew :app:assembleDebug :app:testDebugUnitTest.
Repository tests cover replacement with a non-Firebase provider, identity and credential
forwarding, failure propagation, cancellation and registration dispatch. A real sign-in
requires the console setup and a test user; unit tests do not contact Firebase.

## Signup screen

SignupFragment → SignupViewModel → AuthRepository.signUp → EmailPasswordAuthProvider.signUp.
The form validates email, a nonblank password of at least six characters and matching
confirmation. Firebase may enforce stricter password rules; the adapter translates these
and other registration failures into provider-independent SignupException reasons.
Errors remain in state so they survive rotation. Requests block duplicate submission,
form editing and back navigation until completion. Passwords remain in ViewModel memory
across configuration changes, are not saved in Android view state and are cleared on success.
After successful registration, the pending link destination (or Home) opens and the authentication back stack is cleared.
The login link returns to the existing Login screen without creating an account.
The layout scrolls on smaller screens and supports the keyboard Done action.
No Firestore account document is created by this screen.

Signup ViewModel tests cover validation, success and clearing secrets, duplicate submission,
editing during loading, failure and retry, password confirmation, and returning to Login.
