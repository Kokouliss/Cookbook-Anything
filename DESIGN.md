# How Cookbook Anything looks, moves and talks

One page, so the next change keeps the same character. The look is a printed cookbook: white paper, black ink, one red accent.

## Voice

Warm, brief, a little wry. Like a note left on the fridge.

- Say what happened and what to do next. No "Oops", no exclamation marks, no blaming the reader.
- An empty page gets one line with character and one button: "Grandma’s best? Write it down."
- Greek is written fresh, not translated word for word: informal «εσύ», capitals without accents.
- Every string lives in `values/strings.xml` and `values-el/strings.xml`. Change both.

## Type

- Literata (serif) from 20 sp up: screen titles, recipe names, section heads. Inter below that.
- Numbers that change or line up (servings, timers, counts) use `TABULAR_NUMBERS`.
- The whole scale is `CookbookTypography` in `ui/theme/Theme.kt`. Don't set sizes by hand.

## Icons

- One voice: Tabler outlines, 24 dp, 1.75 stroke. Outline when resting, filled when selected.
- `python3 tools/icons.py <tabler-icons>/icons app/src/main/res/drawable` writes them all. Add a new icon to its `MAP` rather than drawing one by hand.
- The bowl with three pieces (`ic_cookbook`) is the app's own glyph. Use it only for "Recipes".

## Touch

`ui/components/Touch.kt` is the whole vocabulary. Use these names, not raw haptic types.

| Name | When |
|------|------|
| `Check` / `Uncheck` | ticking an ingredient, a step, a favourite |
| `Done` | saved, cooked, scheduled, a stamp landing |
| `Refused` | the form won't save |
| `PickUp` | a row lifts for reordering |
| `Notch` | each step of a rating, a servings count, a reorder |

## Motion

- `MaterialTheme` uses the expressive motion scheme; our own movement uses `PaperSpring`.
- List rows carry `Modifier.animateItem()`.

## Home

- The recipes and nothing above them: no greeting, no card that repeats the calendar.
- Places to go (search, plan, shopping) sit in the top bar, each with its word; the rest is under "More".
- What changes the list sits right above it: filters on the left, "View" (picture size or list, and the order) on the right.

## Signature details

- **Stamps** (`Stamp`): red, tilted, in capitals. Only for something the cook earned: cooked, favourite.
- **Index tabs** (`IndexTab`): categories stand on a rule like the tabs of a ring binder.
- **Undo, not "Are you sure?"**: deleting a recipe hides it and offers Undo (`RecipeTrash`).
