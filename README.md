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
- Read a recipe from photos of a page, a handwritten card or a screenshot, in Greek or English (Gemini, see below); offline, the photo waits and is read when the phone is back online
- Ingredients and steps are dragged into order by their handle, and swiped away with Undo
- Cooking mode shows one step per card: the ingredients that step uses, a timer button when the step says "20 min", swipe between cards
- "Cooked 3 times, last on 12 Nov" on each recipe, and sorting by last edited, A to Z, cooked recently or not for a while
- A "Tonight" home-screen widget with the next planned recipe
- The reminder notification has "Start cooking" and "Shopping list" buttons, and a morning reminder lists what the shopping list is still missing for today
- The shopping list merges the same ingredient from several recipes, and sends the list to List Anything when it is installed (or shares it as text)
- Categories ignore accents and case: "Σούπες" and "σουπες" are one category, sorted A to Z
- Scaling understands Greek abbreviations (κ.σ., κ.γ., φλ.) and fractions (½, ¼)
- An account with email and password (register, sign in, reset password, delete the account), kept for the coming cloud sync. Recipes stay on the phone for now
- A video on a recipe: a YouTube, Instagram, TikTok, Facebook or Vimeo link (or a video file), played on the recipe page. A recipe read from a video post, or from a recipe page with an embedded YouTube or Vimeo video, keeps that video
- Light and dark, chosen in Settings
- The free version holds 5 recipes at a time

Everything is stored on the phone and works offline. Still to come: cloud sync of the recipes to the account, and the one-time lifetime purchase through Google Play.

Reading a recipe from a photo, and listening to a reel whose caption holds no recipe, use Gemini, because the on-device text recognition (ML Kit) has no Greek model. See "Recipe from a photo or a reel" below for the switch it needs.

## Trying it on a phone

Every pull request publishes an installable app on the [Releases](https://github.com/Kokouliss/Cookbook-Anything/releases) page. Open the page on an Android phone (Android 9 or newer), download the `.apk` and open it to install.

- While a pull request is open it has a pre-release named **PR #number**, replaced each time the pull request changes.
- When it is merged, that pre-release goes away and a numbered release (`v0.1.<build>`) takes its place. The [latest release](https://github.com/Kokouliss/Cookbook-Anything/releases/latest) is always the newest merged build.

Every build has a higher build number than the one before and the same signing key, so it installs over the app you have and keeps your recipes. Android refuses to go back to a lower build number, so installing an older build means uninstalling first, which deletes the recipes on the phone.

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

### Recipe from a photo or a reel (Gemini)

The "Read a recipe from a photo" button in the editor sends the photos to Gemini (`gemini-2.5-flash`), which reads Greek and English, printed and handwritten, and answers with the name, ingredients and steps; the form is filled like pasted text. A link to a reel whose caption holds no recipe sends the video the same way. Offline, the photo is kept and read as soon as the phone is back online; it then becomes a new recipe with the photo attached, announced by a notification. When Google refuses, the message in the editor shows its answer.

It needs a Gemini API key. Google refuses the Firebase project's own key for Gemini ("403: Permission denied on the service"), so the key comes from Google AI Studio:

1. On [AI Studio API keys](https://aistudio.google.com/apikey), press **Create API key**, in a new project.
2. For the GitHub builds, add it as a repository secret named `GEMINI_API_KEY` (Settings, Secrets and variables, Actions). For builds on your own computer, pass `-PgeminiApiKey=...` to Gradle.
3. Recommended: in [Credentials](https://console.cloud.google.com/apis/credentials) of that project, restrict the key to Android apps with the package `com.cookbookanything.app` and the SHA-1 of each signing key, and to the Generative Language API. The key ends up inside the app, which anyone can download from the Releases page, so keep that project on the free tier (no billing account) and nobody can run up a bill with it.

Without the secret, the app falls back to the Firebase key from `google-services.json`.

### Known limits

- The cooking timer rings through a notification that Android may delay by a few minutes on a sleeping phone; the countdown on the screen is exact.
- The morning reminder (09:00) and the weekly backup run through WorkManager, which can shift them by up to a few hours to save battery.
- The widget shows the next planned recipe from today on; it updates when recipes or the calendar change.
- The sort order on the recipe list is not remembered when the app restarts.
- Reading a photo needs the internet and takes a few seconds; a blurry or dark photo comes back as "No text was found".
- Videos play in each site's own embed player. Instagram and Facebook sometimes show a sign-in box or refuse to play there; "Open in another app" then opens the video in their app. The player's own full-screen button does nothing, the video already fills the screen.
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
| `app/.../data/text` | Reading a recipe off photos or a reel (Gemini) or a web page |
| `app/src/main/res/values` and `values-el` | English and Greek texts |

Stack: Kotlin, Jetpack Compose with Material 3, Hilt, Room 3, Navigation 3, WorkManager, Coil, Glance, Firebase Auth.

The colours live in `core/domain/.../design/Palette.kt` so that a unit test can check every text and background pair for contrast (WCAG AA). The look is white and black with red accents: plain white pages in light mode, near-black in dark mode, neutral greys, and the AnyKind family red only for the main button, ticks and switches. Recipe names are in Literata (the Google Play Books serif, SIL Open Font License in `app/licenses/`, Greek included); everything else is the phone's sans. Action icons carry a word under them. The launcher icon and the AnyKind wordmark are his logo files.

`app/debug.keystore` is a throwaway signing key for test builds only, so that each new test build can update the previous one without wiping your recipes. It must never sign a Google Play release.

How the app should look, move and talk: [DESIGN.md](DESIGN.md).
