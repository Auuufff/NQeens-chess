# Architecture

## Overview

Single-module Android app, Kotlin + Compose, MVI with unidirectional data flow, Hilt for DI.

The app is **logic-bound, not IO-bound**: no network, one key-value store, one real rule. The
structure follows that — a pure-Kotlin `domain` package testable without a coroutine, a mock or an
Android runtime, and a thin presentation layer that renders it. Everything else is flat; the cost
is that package layering, not the compiler, enforces the boundaries.

## Package layout

```
com.anchtech.nqueens/
├── NQueensApplication.kt                    @HiltAndroidApp
├── MainActivity.kt                          @AndroidEntryPoint, single activity
├── common/            Constants.kt, di/, extension/
├── domain/                                  pure Kotlin, no Android imports
│   ├── model/         Square.kt, PositionStatus.kt
│   ├── repository/    SettingsRepository.kt          (interface)
│   └── usecase/       EvaluatePositionUseCase.kt
├── data/              SettingsDataStore.kt           (implements the interface)
└── presentation/
    ├── base/          BaseViewModel, BaseComposeViewModel, BaseState, BaseAction
    ├── component/     Zoomable.kt
    ├── theme/
    └── screen/
        ├── root/      RootScreen.kt (NavHost), RootViewModel.kt (theme)
        ├── setup/     Screen · Navigation · ViewModel · State · Action, components/, model/
        └── game/      Screen · Navigation · ViewModel · State · Action, components/,
                       GameFeedback.kt (haptics + SoundPool)
```

Dependency direction is one-way: `presentation → domain ← data`. `domain` declares the
`SettingsRepository` interface; `data` implements it.

## Domain

- **No `Board` type.** A position is a size and a set of occupied squares, and the screen state
  holds both regardless. Nothing enforces that the two travel together.
- **One use case holds both rules.** Conflicts and the win condition come out of a single scan, so
  there is one place to find the game's logic.
- **Placement never fails.** Conflicts are *derived*, not rejected, which keeps the domain total:
  no error paths, nothing to unwrap.
- **Interfaces only over volatile collaborators** — `SettingsRepository` because it touches
  DataStore, `TimeSource` because it is nondeterministic. The use case is pure, so it is injected
  as a concrete class and tests use the real one.
- **One repository, not one per feature.** Best times and the theme choice are the same preference
  file, and neither is substituted without the other. DataStore rather than Room, for one
  `Map<Int, Duration>` and a flag.

## MVI

State flows down, events flow up, transient effects go out a side channel.

`BaseViewModel` centralises failure handling — a `CoroutineExceptionHandler` over `viewModelScope`
— so **ViewModels contain no try/catch**. `BaseComposeViewModel` exposes `state: StateFlow`
mutated through `updateState`, and `action: SharedFlow` emitted through `sendAction`.

**Actions carry transient effects only** — the sound and the haptic. The flow is replay-0 and the
screen collects it inside `repeatOnLifecycle(STARTED)`, so an effect emitted off-composition is
dropped rather than replayed when the user returns. **Anything durable renders from state:** the
victory overlay is `state.isSolved`, so it survives rotation and process recreation. The rule is
*if the user could still be looking at it a second later, it is state.*

Derived values are computed over fields already in state. Two are stored deliberately:
`conflicts`/`isSolved`, because computing them needs the injected use case that a data class
cannot reach, and `isNewRecord`, because it is a decision taken at the moment of solving — the old
best is read and the badge decided in one suspending block, otherwise the badge flips while the
celebration is still on screen.

**Callbacks are part of the state**, bound in `init`. Screens need no ViewModel reference, content
composables preview, and tests drive the exact surface the UI touches. The cost is that state is
not a pure value and a default instance is inert.

The timer ticks once a second off a monotonic `TimeMark` and freezes on solve, alongside the
record decision. Backgrounded time counts toward the total — a deliberate simplification.

## Screens and navigation

**Setup** selects the board size from `Constants.BOARD_SIZES`, so the bounds hold by construction
with no input to validate, and displays the best time per size. **Game** holds the board, timer,
queens-left and victory overlay. Each screen has a portrait and a landscape arrangement over the
same state and callbacks.

Single activity, Navigation Compose, type-safe serializable routes. Each screen owns a
`<Screen>ScreenNavigation.kt` with its route and the `NavController` / `NavGraphBuilder`
extensions; `RootScreen` owns the `NavHost` and passes callbacks down. No `AppState`
CompositionLocal — for two screens it is an implicit dependency that buys nothing over callbacks
and makes the graph harder to test.

## Rendering the board

The board is **drawn, not composed**: a checkerboard layer and a pieces layer, with one gesture
detector over both. Zoom lives in a separate component that owns the transform.

A composable per square failed twice over:

- **Cost.** Cells are layerless draw nodes, so one square changing re-records all n². Measured at
  27×27: 11.1 ms of draw recording per frame against 1.5 ms for one `Canvas`.
- **Correctness.** `clickable` consumes the pointer down and `detectTransformGestures` cancels on
  the first consumed change, so a grid of cells swallowed the second finger of every pinch. Zoom
  could not work at all.

The rest follows: the tap detector consumes nothing, zoom is applied as a `graphicsLayer` rather
than a transform inside the draw, and taps arrive already in board coordinates.

The trade-off is accessibility. The board is a single `contentDescription`, so squares are not
individually addressable by TalkBack or by UI tests; an overlay of semantics-only nodes would buy
that back without re-breaking the pinch. At n = 27 a cell is far below 48 dp, which is why the
board is zoomable and pannable.

## Feedback

`GameFeedback` pairs the haptic and the sound for each event behind named methods, so the two
cannot drift apart; translating an action into a call stays the screen's job. `SoundPool` over
`MediaPlayer` for latency and overlapping clips, and no audio focus request — taking focus for an
80 ms click would pause the user's music.

It is composition-scoped rather than a singleton, so decoded audio is freed with the screen; the
cost is that the first frames after entry can be silent while the clips decode. There is no mute
setting yet: `USAGE_GAME` routes to the media stream, which silent mode does not attenuate.

## Testing

Three tiers, heaviest where the logic is. JUnit4 throughout, since Compose tests require it anyway.

**Doubles are hand-written fakes — no MockK, no Mockito.** Every collaborator is a narrow interface
or a pure function, and a fake asserts on what was actually stored rather than on what was called.
Only volatile collaborators get doubles; the use case is used for real, because faking a pure
function would test fiction.

1. **The use case** — plain JUnit, no coroutines, no doubles. Plus a test-only backtracking oracle
   that decides safety *through the production rule* and checks solution counts against OEIS
   A000170: hand-picked cases only catch rules the author already suspects are wrong.
2. **ViewModels** — fakes, a `TestTimeSource` and the real use case. Assert on `state.value`, not
   on emission counts, which are brittle against any extra `launch` in `init`. Reading the route
   with `toRoute()` costs Robolectric in this one class.
3. **DataStore round-trips and a few Compose UI tests**, instrumented — including the victory
   overlay surviving `recreate()`.

Tier 1's hand-written cases and tier 2 exist today; the oracle and the instrumented tier are
specified here but not yet written.

## Build

Dependencies are declared in `gradle/libs.versions.toml`, latest stable, verified assembling and
testing on AGP 9 with the configuration cache. `isReturnDefaultValues = true` lets JVM tests
construct ViewModels whose base class logs through `android.util.Log`. `minSdk` stays at 34:
lowering it only widens the behavioural surface — edge-to-edge, haptic constants, predictive back
— that would then have to be handled and tested.

## Scope

**Deliberately not included:** KMP source sets, a network layer, DTOs and mappers, a use case per
operation, an `AppState` CompositionLocal, per-feature Gradle modules, a custom design system.

**Known limit:** game progress does not survive process death — only the board size does, since it
is a route argument. Persisting `queens` and the start mark into `SavedStateHandle` would be a
small addition if that changed.
