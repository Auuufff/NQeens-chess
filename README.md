# N-Queens

An Android puzzle game built on the N-Queens problem: place `n` queens on an `n×n` board so that no
two share a row, column or diagonal. Conflicts are highlighted as you play, the board is solved when
every queen is placed and none of them clash, and your fastest time per board size is kept.

- **Application ID:** `com.anchtech.nqueens`
- **Min SDK / Target / Compile:** 34 / 37 / 37
- **Kotlin / AGP / Gradle / JDK:** 2.4.10 / 9.3.0 / 9.7.1 / 17
- **Compose BOM:** 2026.08.00, Material3
- **Board sizes:** 4 – 27 (default 8)
- **Orientation:** portrait and landscape

---

## Build & Run

```bash
# Debug APK
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug

# Unit tests (167 tests, JVM only — no device needed)
./gradlew testDebugUnitTest

# Lint the Kotlin
./gradlew ktlintCheck
```

No API keys, no `google-services.json`, no local setup beyond a JDK 17 toolchain — clone and run.

---

## Testing

167 tests, all on the JVM. Heaviest where the logic is, thinnest where the framework is. JUnit4
throughout, since Compose tests require it anyway.

**Doubles are hand-written fakes — no MockK, no Mockito.** Every collaborator is a narrow interface
or a pure function, and a fake asserts on what was actually stored rather than on what was called.
Only volatile collaborators get doubles; the use case is used for real, because faking a pure
function would test fiction.

| Tier         | What                                               | How                                                 |
|--------------|----------------------------------------------------|-----------------------------------------------------|
| Rules        | `EvaluatePositionUseCase`                          | Plain JUnit — no coroutines, no doubles, no Android |
| ViewModels   | `GameViewModel`, `SetupViewModel`, `RootViewModel` | Fakes, `TestTimeSource`, the real use case          |
| UI & storage | `GameScreen`, `SetupScreen`, `SettingsDataStore`   | Robolectric, portrait and landscape qualifiers      |

ViewModel tests assert on `state.value` rather than on emission counts, which are brittle against
any extra `launch` in `init`. Reading the route with `toRoute()` costs Robolectric in one ViewModel
class; the rest run without it.

There is no `androidTest` source set. Robolectric covers the Compose and DataStore tiers on the JVM,
so the whole suite runs in one command without a device.

---

## Package layout

Single module. The app is **logic-bound, not IO-bound**: no network, one key-value store, one real
rule. The structure follows that — a pure-Kotlin `domain` package testable without a coroutine, a
mock or an Android runtime, and a thin presentation layer that renders it. Everything else is flat;
the cost is that package layering, not the compiler, enforces the boundaries.

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

---

## Architecture

Kotlin + Compose, MVI with unidirectional data flow, Hilt for DI, single activity.

### Domain

- **One use case holds both rules.** Conflicts and the win condition come out of a single scan.
- **Placement never fails.** Conflicts are derived, not rejected — no error paths.
- **Interfaces only over volatile collaborators** — `SettingsRepository` and `TimeSource`. The use
  case is pure, so tests use the real one.
- **One repository, not one per feature.** DataStore rather than Room, for one `Map<Int, Duration>`
  and a flag.

### MVI

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

### Screens and navigation

**Setup** selects the board size from `Constants.BOARD_SIZES`, so the bounds hold by construction
with no input to validate, and displays the best time per size. **Game** holds the board, timer,
queens-left and victory overlay. Each screen has a portrait and a landscape arrangement over the
same state and callbacks.

Navigation Compose with type-safe serializable routes. Each screen owns a
`<Screen>ScreenNavigation.kt` with its route and the `NavController` / `NavGraphBuilder`
extensions; `RootScreen` owns the `NavHost` and passes callbacks down. No `AppState`
CompositionLocal — for two screens it is an implicit dependency that buys nothing over callbacks
and makes the graph harder to test.

### Rendering the board

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

### Feedback

`GameFeedback` pairs the haptic and the sound for each event behind named methods, so the two
cannot drift apart; translating an action into a call stays the screen's job. `SoundPool` over
`MediaPlayer` for latency and overlapping clips, and no audio focus request — taking focus for an
80 ms click would pause the user's music.

It is composition-scoped rather than a singleton, so decoded audio is freed with the screen; the
cost is that the first frames after entry can be silent while the clips decode. There is no mute
setting yet: `USAGE_GAME` routes to the media stream, which silent mode does not attenuate.

---

## Build setup

Dependencies are declared in `gradle/libs.versions.toml`, latest stable, verified assembling and
testing on AGP 9 with the configuration cache. ktlint runs over both source sets with the
`android_studio` code style. `isReturnDefaultValues = true` lets JVM tests construct ViewModels
whose base class logs through `android.util.Log`. `minSdk` stays at 34: lowering it only widens the
behavioural surface — edge-to-edge, haptic constants, predictive back — that would then have to be
handled and tested.

---

## Scope

**Deliberately not included:** KMP source sets, a network layer, DTOs and mappers, a use case per
operation, an `AppState` CompositionLocal, per-feature Gradle modules, a custom design system.

**Known limit:** game progress does not survive process death — only the board size does, since it
is a route argument. Persisting `queens` and the start mark into `SavedStateHandle` would be a
small addition if that changed.

---