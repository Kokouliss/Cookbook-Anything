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
- The free version holds 5 recipes at a time

Everything is stored on the phone and works offline. Still to come: Google sign-in with cloud sync, and the one-time lifetime purchase through Google Play.

## Trying it on a phone

Every pull request builds an installable app. Open the pull request on GitHub, go to **Checks**, open the **Build** run and download **cookbook-anything-debug-apk**. Unzip it, copy the `.apk` to an Android phone (Android 9 or newer) and open it to install.

In these test builds the "Unlock unlimited recipes" dialog has an **Unlock (test build)** button, because real purchases are not wired in yet.

## Building it yourself

Open the folder in Android Studio, or run:

```
./gradlew :app:assembleDebug
```

The APK ends up in `app/build/outputs/apk/debug/`.

## How the code is laid out

| Where | What |
| --- | --- |
| `core/domain` | Pure Kotlin: the recipe model, repository interfaces, the free-tier rule, the shared recipe file format, calendar maths. Has unit tests. |
| `app/.../data` | Room database, photo storage, PDF and recipe file export/import, repositories |
| `app/.../feature` | One folder per screen: a ViewModel plus Compose UI |
| `app/.../navigation` | The screens wired together |
| `app/.../reminders` | Calendar notifications |
| `app/src/main/res/values` and `values-el` | English and Greek texts |

Stack: Kotlin, Jetpack Compose with Material 3, Hilt, Room 3, Navigation 3, WorkManager, Coil.

`app/debug.keystore` is a throwaway signing key for test builds only, so that each new test build can update the previous one without wiping your recipes. It must never sign a release.
