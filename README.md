# Cookbook Anything

A personal cookbook app for Android. Keep your own recipes with ingredients, steps, photos and notes, organise them into your own categories, and plan them on a calendar. Available in Greek and English.

## What it does today

- Recipes with a list of ingredients and a list of steps, both with check boxes you tick while cooking
- Photos on the recipe and on each step, from the gallery or the camera
- Comments on a recipe
- Categories you create yourself, and a filter on the recipe list
- A calendar where a recipe is planned for a date and time, with a reminder notification
- Sharing a recipe as a PDF, or as a recipe file that another person opens in their own copy of the app
- Greek and English, following the phone or chosen in Settings
- Search by recipe name or ingredient
- Cooking mode: one step at a time in large text, with the screen kept on
- A shopping list built from the recipes planned in the calendar
- Servings and times, with ingredient amounts scaled for more or fewer people
- A backup of the whole cookbook in one file, and restore from it
- Automatic weekly backups into a folder you pick (phone storage or a cloud drive), with "Last backup" in Settings and a line on the recipe list when one is overdue
- Paste a whole recipe: the text is split into name, servings, ingredients and steps. Text shared from another app opens as a new recipe the same way
- Paste or share a link: a recipe site's page gives the exact recipe (name, servings, times, ingredients, steps, picture) from its schema.org record, microdata or visible "Ingredients"/"Υλικά" section; an Instagram, TikTok or Facebook post or reel gives its caption and picture or video thumbnail, split like pasted text. A site that refuses plain requests from apps is loaded again, unseen, in the phone's own browser engine
- Read a recipe from a photo of a page, in Greek or English (Google Cloud Vision, see below); offline, the photo waits and is read when the phone is back online
- Ingredients and steps are dragged into order by their handle, and swiped away with Undo
- Cooking mode shows one step per card: the ingredients that step uses, a timer button when the step says "20 min", swipe between cards
- "Cooked 3 times, last on 12 Nov" on each recipe, and sorting by last edited, A to Z, cooked recently or not for a while
- A "Tonight" home-screen widget with the next planned recipe
- The reminder notification has "Start cooking" and "Shopping list" buttons, and a morning reminder lists what the shopping list is still missing for today
- The shopping list merges the same ingredient from several recipes, and sends the list to List Anything when it is installed (or shares it as text)
- Categories ignore accents and case: "Σούπες" and "σουπες" are one category, sorted A to Z
- Scaling understands Greek abbreviations (κ.σ., κ.γ., φλ.) and fractions (½, ¼)
- An account with email and password (register, sign in, reset password, delete the account), kept for the coming cloud sync. Recipes stay on the phone for now
- Light and dark, chosen in Settings
- The free version holds 5 recipes at a time

Everything is stored on the phone and works offline. Still to come: cloud sync of the recipes to the account, and the one-time lifetime purchase through Google Play.

Reading a recipe from a photo uses Google Cloud Vision, because the on-device text recognition (ML Kit) has no Greek model. See "Recipe from a photo" below for the one switch it needs.

## Trying it on a phone

Every pull request builds an installable app. Open the pull request on GitHub, go to **Checks**, open the **Build** run and download **cookbook-anything-debug-apk**. Unzip it, copy the `.apk` to an Android phone (Android 9 or newer) and open it to install.

In these test builds the "Unlock unlimited recipes" dialog has an **Unlock (test build)** button, because real purchases are not wired in yet.

## Building it yourself

Open the folder in Android Studio, or run:

```
./gradlew :app:assembleDebug
```

The APK ends up in `app/build/outputs/apk/debug/`.

### Sign-in (Firebase)

Sign-in needs the Firebase config file `app/google-services.json`. It is not in the repository, because the repository is public. Without it the app still builds and runs, and the Account section in Settings says that sign-in is not set up in this build.

To get sign-in into the builds:

1. In the [Firebase console](https://console.firebase.google.com/), open the project, then **Authentication**, **Sign-in method**, and switch on **Email/Password**.
2. In **Project settings**, under **Your apps**, download `google-services.json` for the Android app `com.cookbookanything.app`.
3. For builds on your own computer, put the file at `app/google-services.json` (it is ignored by git).
4. For the GitHub builds, open the repository on GitHub, then **Settings**, **Secrets and variables**, **Actions**, **New repository secret**. Name it `GOOGLE_SERVICES_JSON` and paste the whole content of the file. The workflow writes it into place before building.

Deleting the account from Settings deletes the Firebase user. Nothing else is stored in the cloud yet.

### Recipe from a photo (Cloud Vision)

The "Read a recipe from a photo" button in the editor sends the photo to Google Cloud Vision, which reads Greek and English, printed and handwritten, and fills the form like pasted text. It uses the same Firebase config as sign-in, so it only appears in builds that have `google-services.json`. Offline, the photo is kept and read as soon as the phone is back online; it then becomes a new recipe with the photo attached, announced by a notification.

1. Open the [Cloud Vision API page](https://console.cloud.google.com/apis/library/vision.googleapis.com), pick the Firebase project at the top, and press **Enable**. The project is on the Blaze plan, so billing is already in place: the first 1,000 photos a month are free, then about 1.50 USD per 1,000.
2. Recommended, so nobody else can run up the bill with the key from the app: in [Credentials](https://console.cloud.google.com/apis/credentials), open the **Android key (auto created by Firebase)**, set **Application restrictions** to Android apps with the package `com.cookbookanything.app` and the SHA-1 of each signing key (the debug key from `app/debug.keystore` and later the release keys), and under **API restrictions** allow Cloud Vision API together with the Firebase APIs already listed. The app sends the package name and certificate with every call, so the restriction works.

### Known limits

- The cooking timer rings through a notification that Android may delay by a few minutes on a sleeping phone; the countdown on the screen is exact.
- The morning reminder (09:00) and the weekly backup run through WorkManager, which can shift them by up to a few hours to save battery.
- The widget shows the next planned recipe from today on; it updates when recipes or the calendar change.
- The sort order on the recipe list is not remembered when the app restarts.
- Reading a photo needs the internet and takes a few seconds; a blurry or dark photo comes back as "No text was found".
- Links: a site whose bot check never lets the page through comes back as "Check the link" after about 25 seconds. Instagram, TikTok and Facebook sometimes answer with a login page instead of the post, which comes back as "No recipe was found"; copy the caption and paste it then. The caption is read as text, so a recipe written in prose rather than lines may land in the wrong place and need a tidy.

## How the code is laid out

| Where | What |
| --- | --- |
| `core/domain` | Pure Kotlin: the recipe model, repository interfaces, the free-tier rule, the shared recipe file format, calendar maths. Has unit tests. |
| `app/.../data` | Room database, photo storage, PDF and recipe file export/import, repositories |
| `app/.../feature` | One folder per screen: a ViewModel plus Compose UI |
| `app/.../navigation` | The screens wired together |
| `app/.../reminders` | Calendar notifications, the morning reminder and the cooking timer |
| `app/.../widget` | The "Tonight" home-screen widget (Glance) |
| `app/.../data/account` | Email and password accounts (Firebase Auth) |
| `app/.../data/text` | Reading a recipe off a photo (Cloud Vision) or a web page |
| `app/src/main/res/values` and `values-el` | English and Greek texts |

Stack: Kotlin, Jetpack Compose with Material 3, Hilt, Room 3, Navigation 3, WorkManager, Coil, Glance, Firebase Auth.

The colours live in `core/domain/.../design/Palette.kt` so that a unit test can check every text and background pair for contrast (WCAG AA). The look follows the AnyKind family: an index card from a recipe box, the family red, ochre and teal, serif headings, ink doodles on empty screens.

`app/debug.keystore` is a throwaway signing key for test builds only, so that each new test build can update the previous one without wiping your recipes. It must never sign a release.
