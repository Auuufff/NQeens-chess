# Architecture (v2)

Revision of `ARCHITECTURE.md` after an independent design review. Changes from v1 are
listed at the end under [What changed in v2](#what-changed-in-v2).

## Overview

Single-module Android app, Kotlin + Compose, MVI with unidirectional data flow.

The guiding constraint is that this app is **logic-bound, not IO-bound**. There is no
network and one key-value store — the only real complexity is the N-Queens rule itself.
So the architecture spends its structure where the difficulty is: a pure-Kotlin `domain`
package that can be tested exhaustively without a coroutine, a mock, or an Android
runtime, and a thin presentation layer that renders it.

Everything else is deliberately flat. Two screens do not justify Gradle modules, a
mapping layer, or a use case per operation.

## Package layout

```
com.anchtech.nqueens/
├── NQueensApplication.kt                    @HiltAndroidApp
├── MainActivity.kt                          @AndroidEntryPoint, single activity
├── common/
│   ├── Constants.kt                         (board size bounds)
│   ├── di/            AppModule.kt, DataModule.kt
│   └── extension/     ComposeExtensions.kt, DurationExtensions.kt
├── domain/                                  ← pure Kotlin, no Android imports
│   ├── model/
│   │   ├── Square.kt
│   │   └── PositionStatus.kt
│   ├── repository/
│   │   └── BestTimesRepository.kt           (interface)
│   └── usecase/
│       └── EvaluatePositionUseCase.kt
├── data/
│   └── BestTimesDataStore.kt                (DataStore, implements the interface)
└── presentation/
    ├── base/          BaseViewModel.kt, BaseComposeViewModel.kt,
    │                  BaseState.kt, BaseAction.kt
    ├── component/     Zoomable.kt
    ├── theme/
    └── screen/
        ├── root/      RootScreen.kt         (NavHost)
        ├── setup/
        │   ├── components/  BoardPreview.kt, BoardSizeSelector.kt, BestTimesCard.kt
        │   ├── model/       UiBestTime.kt
        │   └── SetupScreen · Navigation · ViewModel · State · Action
        └── game/
            ├── components/  GameBoard.kt, QueensLeft.kt, VictoryOverlay.kt
            └── GameScreen · Navigation · ViewModel · State · Action
```

Dependency direction is one-way: `presentation → domain ← data`. `domain` declares the
`BestTimesRepository` interface; `data` implements it. Nothing in `domain` imports from
`data` or `presentation`.

## Domain

There is no `Board` type. A board is a size and a set of occupied squares, and the screen
state has to hold both regardless — a wrapper would be a second home for the same values.

```kotlin
data class Square(val row: Int, val col: Int)
```

```kotlin
object Constants {
    const val MIN_BOARD_SIZE = 4       // below 4 there are no solutions
    const val MAX_BOARD_SIZE = 27      // the board is zoomable, so the cap is aesthetic
    const val DEFAULT_BOARD_SIZE = 8

    val BOARD_SIZES = MIN_BOARD_SIZE..MAX_BOARD_SIZE
}
```

```kotlin
data class PositionStatus(
    val conflicts: Set<Square> = emptySet(),
    val isSolved: Boolean = false,
)
```

**Both game rules live in one use case.** Conflict detection and the win condition are
evaluated together, from one scan:

```kotlin
class EvaluatePositionUseCase @Inject constructor() {

    operator fun invoke(size: Int, queens: Set<Square>): PositionStatus {
        val conflicts = queens.filterTo(mutableSetOf()) { queen ->
            queens.any { it != queen && threatens(it, queen) }
        }
        return PositionStatus(
            conflicts = conflicts,
            isSolved = queens.size == size && conflicts.isEmpty(),
        )
    }

    private fun threatens(a: Square, b: Square): Boolean =
        a.row == b.row ||
            a.col == b.col ||
            abs(a.row - b.row) == abs(a.col - b.col)
}
```

```kotlin
interface BestTimesRepository {
    val bestTimes: Flow<Map<Int, Duration>>            // board size → best time
    suspend fun record(size: Int, time: Duration)      // keeps the lower value
}
```

Four properties of this design matter:

**Placement never fails.** Tapping a square always succeeds; conflicts are *derived*, not
rejected. This matches the requirement ("highlight conflicts", not "prevent bad moves")
and keeps the domain total — no error paths, no `Result`, nothing to unwrap.

**Every rule is in `domain`, and there is exactly one place to find it.** Both "which
queens are attacking each other" and "is this position a win" come out of a single call.
Grepping `EvaluatePositionUseCase` finds all of the game's logic. Everything the state
computes for itself (`queensLeft`, `isNewRecord`) is arithmetic, not a rule.

**It takes no interface.** DIP inverts dependencies on *volatile* things — IO, clocks, the
platform. `BestTimesRepository` is an interface because it touches DataStore; `TimeSource`
is injected because it is nondeterministic. A pure function over a `Set` is neither, so an
interface here would be a seam with nothing behind it. It is injected as a concrete class,
and tests use the real one. (`@Inject constructor()` is `javax.inject`, not Android — the
package stays Android-free, at the cost of one annotation from the DI framework.)

**Cost is bounded by placements, not by board size.** Because placement never fails, a
player can put a queen on every square: `k` is bounded by `n²`, not `n`. Worst case is
therefore O(n⁴) — about 160,000 comparisons at n=12 with all 144 squares filled, once per
tap, on a background-free pure function. Realistic play is `k ≈ n`, i.e. a few hundred
comparisons. The counting-array alternative (four occupancy arrays for row / column / both
diagonals) is O(k) but strictly more code; this function is the seam if it ever matters.

## MVI

State flows down, events flow up, transient effects go out a side channel.

`BaseViewModel` extends `ViewModel` and implements `CoroutineScope` over
`viewModelScope.coroutineContext` — so it inherits `viewModelScope`'s `SupervisorJob`, and
one failed child does not cancel the rest — plus a `CoroutineExceptionHandler` that logs
and forwards to an overridable `onError(Throwable)`. Consequence: **ViewModels contain no
try/catch.** Failures land in one place and are logged. The game screen carries no error
field: the only fallible work is reading or writing a best time, which cannot affect play,
and an error banner over a victory would be worse than a missing record.

`BaseComposeViewModel<STATE : BaseState, ACTION : BaseAction>` adds:

- `state: StateFlow<STATE>`, mutated through `updateState { it.copy(...) }`
- `action: SharedFlow<ACTION>`, emitted through `sendAction(...)`

**`updateState` takes `(STATE) -> STATE`, not `STATE.() -> STATE`.** The receiver form
reads better (`copy(size = size)`) and is a footgun: the state becomes the innermost
implicit receiver, so any field sharing a name with a ViewModel property or an enclosing
local is silently shadowed. `copy(size = size)` then assigns the state's own value to
itself and compiles clean — the same trap as `person.apply { name = name }`. Three
characters of `it.` make the right-hand side unambiguous, and it is worth the small loss
of elegance because the failure is silent.

`updateState` delegates to `MutableStateFlow.update`, which may re-run its block under
contention — so the block must stay side-effect free. Evaluating the position inside it is
safe precisely because `EvaluatePositionUseCase` is pure; anything that reads a clock or
suspends is computed before the block and passed in.

### Actions are for transient effects only

The action flow is replay-0 with `extraBufferCapacity = 8`. Replay-0 is deliberate: a
haptic emitted while the screen is off-composition should be *dropped*, not queued and
replayed when the user returns. The buffer is sized so that several actions emitted in one
frame — placement plus victory on the winning tap — cannot overflow `tryEmit` and be
silently discarded.

**Anything durable renders from state, not from an action.** The victory overlay is driven
by `state.isSolved`, so it survives rotation and process recreation. Only the sound and
haptic go through the channel:

```kotlin
sealed interface GameAction : BaseAction {
    data object QueenPlaced : GameAction     // haptic
    data object QueenRemoved : GameAction    // haptic, distinct from placing
    data object Solved : GameAction          // celebration sfx — the overlay is state
}
```

The v1 rule of thumb ("one-shot things are actions") was too broad: it put the win screen
on a channel that by design forgets, so rotating during the celebration left a frozen board
and no overlay. The rule is now: *if the user could still be looking at it a second later,
it is state.*

### State

```kotlin
data class GameState(
    val size: Int = Constants.DEFAULT_BOARD_SIZE,
    val queens: Set<Square> = emptySet(),
    val conflicts: Set<Square> = emptySet(),
    val isSolved: Boolean = false,
    val time: String = "00:00",
    val isNewRecord: Boolean = false,
    val onCellClick: (Square) -> Unit = {},
    val onResetClick: () -> Unit = {},
) : BaseState {
    val queensLeft: Int = size - queens.size
}
```

**Two stored derived fields, each written in exactly one place.** `conflicts` and `isSolved`
are stored rather than derived because computing them needs the injected use case, which a
data class cannot reach; `size`, `queens`, `conflicts` and `isSolved` are therefore only ever
assigned together. Time is stored preformatted for the same reason a mapping never sits in a
constructor body — a `get()` or an init-block conversion re-runs on every `copy`. `isNewRecord`
is stored because it is a *decision taken at a moment* — the instant the puzzle is solved —
not a standing relationship between fields. Everything else (`queensLeft`, `isSolved`) is a
`get()` over fields that are already state, and cannot drift.

**`isNewRecord` is decided once, in `finish()`.** An earlier draft held the previous best in
state and derived the badge from it. That is subtly wrong: the previous best has to be read
asynchronously, so until the read lands there is nothing to compare against and every solve
looks like a record — and once the new time is written, the comparison flips to `false`
while the celebration is still on screen. Reading the old best and deciding the badge in the
same suspending block removes both failure modes and two members from the ViewModel.

**`@Immutable`** tells the Compose compiler the type will not change once constructed, which
matters for the grid (see [Rendering the board](#rendering-the-board)). It is honest here:
every field is a `val` and the collections are never mutated after construction.

**Callbacks are part of the state**, bound once in `init`. The screen needs no ViewModel
reference, so the content composable is trivially previewable, and tests drive the exact
surface the UI touches:

```kotlin
viewModel.state.value.onCellClick(Square(0, 0))
```

The cost: `GameState` is not a pure value — `equals`/`toString` include lambdas, and a
default-constructed `GameState()` is inert. Acceptable, because it is only ever constructed
by the ViewModel.

### ViewModel

```kotlin
@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val evaluatePosition: EvaluatePositionUseCase,
    private val bestTimes: BestTimesRepository,
    private val timeSource: TimeSource,
) : BaseComposeViewModel<GameState, GameAction>(GameState()) {

    private val boardSize: Int = savedStateHandle.toRoute<GameRoute>().size
    private var startMark: TimeMark = timeSource.markNow()
    private var timerJob: Job? = null

    init {
        updateState {
            it.copy(size = boardSize, onCellClick = ::handleCellClick, onResetClick = ::handleReset)
        }
        startTimer()
    }

    private fun handleCellClick(square: Square) {
        if (state.value.isSolved) return
        val placing = square !in state.value.queens
        // The board holds at most one queen per row; a tap on a full board does nothing.
        if (placing && state.value.queens.size >= boardSize) return
        updateState {
            val queens = if (placing) it.queens + square else it.queens - square
            val status = evaluatePositionUseCase(boardSize, queens)
            it.copy(queens = queens, conflicts = status.conflicts, isSolved = status.isSolved)
        }
        sendAction(if (placing) GameAction.QueenPlaced else GameAction.QueenRemoved)
        if (state.value.isSolved) finish()
    }

    private fun finish() = launch {
        timerJob?.cancel()
        val elapsed = startMark.elapsedNow()               // freeze the clock
        val previousBest = bestTimesRepository.bestTimes.first()[boardSize]
        updateState {
            it.copy(
                time = elapsed.formatAsClock(),
                isNewRecord = previousBest == null || elapsed < previousBest,
            )
        }
        sendAction(GameAction.Solved)
        bestTimesRepository.record(boardSize, elapsed)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = launch {
            while (true) {
                val elapsed = startMark.elapsedNow()
                val formatted = elapsed.formatAsClock()
                updateState { it.copy(time = formatted) }
                // Sleep to the next second boundary measured from the start mark, not for a
                // whole second: a fixed delay runs slightly long and eventually skips a second.
                val tickMillis = TICK.inWholeMilliseconds
                delay((tickMillis - elapsed.inWholeMilliseconds % tickMillis).milliseconds)
            }
        }
    }
}
```

**The board size is read with `toRoute()`.** A keyed `SavedStateHandle` read was tried first,
to keep the ViewModel constructible on a plain JVM. It does not survive contact: `toRoute()`
decodes through `android.net.Uri` and `Bundle`, and under `isReturnDefaultValues = true` those
return zeroes *silently* rather than throwing, so the ViewModel builds a `GameRoute(size = 0)`
and the failures surface as bare assertion errors far from the cause. The fix is to accept
Robolectric for this one test class — `@RunWith(RobolectricTestRunner::class)` with
`SavedStateHandle(route = GameRoute(size))` from `navigation-testing`, which is what Now in
Android does. `SavedStateHandle` in the constructor is the ordinary pattern; the cost is a
~5s test class rather than a 0.07s one, and it stays confined to `GameViewModelTest`.

**Timer policy, stated explicitly.** One tick per second from a monotonic `TimeMark`. It
stops on solve and the final time is frozen at that instant, alongside the record decision, so
neither can change afterwards. Each tick sleeps to the next second boundary measured from the
start mark rather than for a flat second — a flat `delay` runs a little long every pass, and
after a few hundred ticks the display skips a second.
Time spent backgrounded counts toward the total — the mark is wall-clock — which is a
deliberate simplification for a puzzle that takes minutes, not a bug.

## Setup screen

The first screen. Board size selection is a gameplay requirement, so it gets a real
specification rather than a directory name.

```kotlin
data class SetupState(
    val sizes: List<Int> = Constants.BOARD_SIZES.toList(),
    val selected: Int = Constants.DEFAULT_BOARD_SIZE,
    val bestTimes: Map<Int, Duration> = emptyMap(),
    val onSizeSelected: (Int) -> Unit = {},
    val onStartClick: () -> Unit = {},
) : BaseState {
    val bestForSelected: Duration? get() = bestTimes[selected]
}
```

A horizontal row of filter chips, one per size in `Constants.BOARD_SIZES`, plus a start button.
Because the list comes from `Constants.BOARD_SIZES`, `n ≥ 4` is enforced by construction — there
is no invalid input to validate, and no error path to test. The upper bound is enforced the
same way.

This screen is also where **best times are displayed** — one row per size that has a
recorded time, read from `BestTimesRepository.bestTimes`. That satisfies "store *and
display* best times", which a single `bestTime` on the game screen does not.

## Navigation

Single activity, Navigation Compose, type-safe serializable routes. Each screen owns a
`<Screen>ScreenNavigation.kt` holding its route, a `NavController` extension to reach it,
and a `NavGraphBuilder` extension to register it.

```kotlin
@Serializable
data class GameRoute(val size: Int)

internal fun NavController.navigateToGame(size: Int, navOptions: NavOptions? = null) =
    navigate(GameRoute(size), navOptions)

internal fun NavGraphBuilder.gameScreen(onBackClick: () -> Unit) {
    composable<GameRoute> { GameScreen(onBackClick = onBackClick) }
}
```

`RootScreen` owns the `NavHost` and passes the callbacks down:

```kotlin
@Composable
fun RootScreen() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = SetupRoute) {
        setupScreen(onStartGame = { size -> navController.navigateToGame(size) })
        gameScreen(onBackClick = { navController.popBackStack() })
    }
}
```

**No `AppState` / `LocalAppState` CompositionLocal.** v1 carried one over from my other
projects to hold the root `NavController`. For two screens it is an implicit dependency
that buys nothing over passing `() -> Unit` callbacks, and it makes the nav graph harder to
test. Screens stay ignorant of where they sit in the graph either way.

## Rendering the board

The board is one `Canvas` and two gesture detectors. Squares are drawn, not composed.

```kotlin
BoxWithConstraints(modifier.fillMaxSize()) {           // the whole area below the header
    val side = min(maxWidth, maxHeight)
    Zoomable {
        Canvas(
            Modifier
                .size(side)
                .clip(MaterialTheme.shapes.medium)
                .pointerInput(boardSize, sidePx, onCellClick) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)   // never consumed
                        val up = waitForUpOrCancellation()
                        if (up != null) squareAt(up.position, sidePx, boardSize)?.let(onCellClick)
                    }
                },
        ) {
            drawSquares(boardSize, colors)
            conflicts.forEach { drawConflict(it) }
            queens.forEach { drawQueen(it) }
        }
    }
}
```

**Zoom is a separate component.** `presentation/component/Zoomable.kt` owns the transform
and nothing else: the gesture, the scale and offset, the pan clamp, and the `graphicsLayer`.
It takes a modifier, a maximum zoom and its content. `GameBoard` is left with drawing a board
and deciding which square a point falls in. The split is what lets
the canvas be exactly the board — so `Modifier.clip` gives the rounded corners that
previously needed a `clipPath` in the draw, and `squareAt` is a bounds check and a division
with no transform maths in it at all.

**The viewport is not the board.** `GameBoard` takes the whole area below the header
(`Modifier.weight(1f)`) and places a square inside it. At rest that square is width-sized and
anchored under the header, exactly where a fixed `aspectRatio(1f)` box put it. Zoomed, it is
free to grow past the screen and be panned around the full area, rather than being magnified
inside a square window with half the screen left empty.

**Zoom is a `graphicsLayer`, not a transform inside the draw.** A pinch updates one matrix on
an existing RenderNode; the display list is untouched. Doing it with `withTransform` inside
the draw lambda re-records every square on every frame of the gesture, which at 27×27 is the
1.5 ms below paid ~60 times a second for nothing.

**A composable per square does not work here, for two separate reasons.**

The first is cost. A cell that draws itself is a draw node, and none of them own a layer, so
one square changing dirties the shared display list and all n² are re-recorded. Measured on
a 27×27 board with `dumpsys gfxinfo … framestats`, UI-thread draw recording was **11.1 ms**
per frame that way and is **1.5 ms** drawing the same board in one `Canvas`. Compose and
layout were never the problem — layout is 0.03 ms either way, so the nested `Row`/`Column`
the grid used was free; it was the 729 draw nodes that cost.

The second is correctness, and it is the reason the grid had to go rather than merely being
optimised. `Modifier.clickable` consumes the pointer down. `transformable`'s gesture
detector cancels on the first consumed change it sees:

```kotlin
val canceled = event.changes.fastAny { it.isConsumed } || …
```

So the second finger of a pinch landed on a cell, the cell consumed its down, and the zoom
gesture was cancelled before it could start. Zoom could not work at all while the board was
a grid of clickable squares. With one node handling both gestures there is no child left to
consume anything.

**The tap detector consumes nothing.** This is the constraint that shapes the whole gesture
design: `clickable` and `detectTapGestures` both consume the pointer down, and
`detectTransformGestures` cancels on the first consumed change it sees, so anything tappable
inside a pinch-zoomable parent swallows the second finger of every pinch. Reading the down
with `awaitFirstDown(requireUnconsumed = false)` and waiting on `waitForUpOrCancellation()`
consumes nothing, and that same call reports null once the pinch takes over and starts
consuming — so the pinch wins, the tap is dropped, and neither gesture has to know about the
other. It costs the ripple and the click semantics `clickable` would bring; the board supplies
its own `contentDescription`.

The `pointerInput` keys are the values the gesture reads, not the lambda. Keying on a lambda
restarts the detector whenever that lambda's identity changes, which is invisible until
something unstable is captured and taps start being dropped mid-gesture.

Because the tap lands on the canvas, which sits inside the zoom layer, Compose delivers it
already in board coordinates. `squareAt` only bounds-checks and divides — and the check is on
the offset, not on the derived row and column, since `(-0.5f).toInt()` is 0 in Kotlin and
truncating first would put a tap above the board onto row 0.

**Zoom tracks the centroid.** `detectTransformGestures` reports where the fingers are, and
the translation is adjusted so the point under them stays put — `t' = c − (c − t)·s'/s`.
Zooming about the viewport centre instead makes the content feel detached from the gesture.

**Pan is clamped where it is stored**: the content may be panned only while it overflows the
viewport, and never past its own edges, so panning into an edge banks no travel that has to be
undone before it moves back.

The clamp is two `coerceIn` calls rather than anything cleverer, and that is a consequence of
where the scale is anchored. `transformOrigin` is the content's **top centre**, so it grows
symmetrically sideways and downward from where layout put it — centred horizontally, anchored
under the header. Both limits are then plain overflow. Anchoring the scale at the top-left
instead makes the content drift sideways as it grows, and every bound has to carry a
`start * scale` correction term.

**The queen glyph is measured once per cell size** through `TextMeasurer`, sized off the
unzoomed cell — the transform magnifies it along with its square — and converted with
`Float.toSp()` so a large system font scale cannot push it outside a board that has no room
to grow.

**Animation state is a map of `Square → Animatable`**, synced by a `LaunchedEffect` and read
at draw time. Only occupied or attacked squares hold an entry, so it stays a board's worth
at most. Its first pass snaps rather than animating: squares already in place when the board
is first drawn were restored, not just played, which is what keeps `@Preview` and
post-rotation rendering correct.

**Accessibility, stated as a trade-off.** The board exposes a single `contentDescription`
naming its size and how many queens are placed. Individual squares are no longer addressable
by TalkBack or by UI tests, which is the price of drawing rather than composing them. It can
be bought back with an overlay grid of semantics-only nodes — `semantics { onClick { … } }`
supplies an accessibility action without any pointer input, so unlike `clickable` it would
not re-break the pinch — at the cost of n² layout nodes, which measurement says are free.

**Touch targets.** At n = 27 a cell is far below the 48 dp recommendation, which is why the
board is pinch-zoomable to 6× and pannable within bounds. The board is also gapless, so
there is no dead space between targets.

## Screen wiring

```kotlin
@Composable
internal fun GameScreen(
    onBackClick: () -> Unit,
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    viewModel.action.collectAsEffect { action ->
        when (action) {
            GameAction.QueenPlaced -> haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            GameAction.QueenRemoved -> haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            GameAction.Solved -> haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        }
    }

    GameScreenContent(state = state, onBackClick = onBackClick)
}
```

`collectAsEffect` collects inside `repeatOnLifecycle(STARTED)`. That is what makes
"actions are dropped while off-composition" true rather than aspirational — with a plain
`LaunchedEffect` the collector would survive backgrounding and fire haptics behind the
user's back.

The victory overlay is `AnimatedVisibility(visible = state.isSolved)`, showing elapsed
time, `state.isNewRecord`, and reset/back actions. Being state-driven, it is correct after
rotation.

**Sound is out of scope; haptics and animation are in.** The task lists sfx as a
nice-to-have. Shipping audio means an asset, a `SoundPool`, a mute affordance and a
lifecycle — for a take-home that is cost without signal. Haptics cover the same feedback
need at zero asset weight, and the celebration is a Compose animation. This is a descope,
stated rather than implied.

## Testing strategy

Three tiers, heaviest coverage where the logic is. **JUnit4 throughout** — instrumented
Compose tests require it anyway, and running two frameworks in one module to gain nothing
is a config tax. Coroutines are handled by a 15-line `MainDispatcherRule`.

**Test doubles are hand-written fakes — no MockK, no Mockito.** Every collaborator is
either a narrow interface (`BestTimesRepository` is two methods) or a pure function. Fakes
assert on real behaviour rather than call records: a `FakeBestTimesRepository` backed by a
`MutableMap` lets a test check what was actually stored rather than that a method was
invoked. Keeping interfaces small enough to fake by hand is itself a design constraint.

Only *volatile* collaborators get doubles. `EvaluatePositionUseCase` is used for real in
every test — faking a pure function would make the test assert against fiction.

### 1 — `EvaluatePositionUseCase` (plain JUnit, no coroutines, no doubles)

Constructed directly and called. No Android dependency, so these run at compiler speed.
Cases: conflicts along rows, columns, and each diagonal direction independently; a queen
conflicting on two axes at once; the empty board; a full board that is invalid; a valid
solution at n=4; more than `n` queens placed.

**Plus an oracle, in test sources only.** Hand-picked cases only catch rules the author
already suspects are wrong — a sign error on the anti-diagonal survives symmetric fixtures.
So the test source set contains a backtracking search that decides safety *using the
production use case*, and asserts the solution counts against the known sequence
(OEIS A000170):

```kotlin
private fun countSolutions(n: Int, evaluate: EvaluatePositionUseCase): Int {
    fun place(row: Int, queens: Set<Square>): Int =
        if (row == n) 1
        else (0 until n).sumOf { col ->
            val next = queens + Square(row, col)
            if (evaluate(n, next).conflicts.isEmpty()) place(row + 1, next) else 0
        }
    return place(0, emptySet())
}

@Test
fun `rule reproduces the known solution counts`() {
    val evaluate = EvaluatePositionUseCase()
    listOf(4 to 2, 5 to 10, 6 to 4, 7 to 40, 8 to 92).forEach { (n, expected) ->
        assertEquals(expected, countSolutions(n, evaluate))
    }
}
```

Roughly fifteen lines, nothing shipped in the APK, and it is the difference between "I
tested the rule" and "I proved it". A subtly over- or under-inclusive threat check cannot
produce these counts.

### 2 — ViewModels (JUnit4 + `MainDispatcherRule` + Turbine)

`FakeBestTimesRepository`, a `TestTimeSource`, and the real use case.

`GameViewModel`: placing and removing queens, conflict marking, win detection, reset,
`queensLeft` never going negative, the timer advancing under `runTest`'s virtual clock and
stopping on solve, the best time being recorded, `isNewRecord` being true only when the
new time actually beats the stored one, and reset clearing it. Assert on
`viewModel.state.value` rather than counting Turbine emissions — `updateState` is
synchronous, so emission counts are brittle against any extra `launch` in `init`. The callbacks-in-state convention means tests invoke the same lambdas
the UI does:

```kotlin
@Test
fun `two queens on a diagonal are both marked`() = runTest {
    viewModel.state.test {
        awaitItem().onCellClick(Square(0, 0))
        awaitItem().onCellClick(Square(1, 1))
        val state = awaitItem()
        assertTrue(Square(0, 0) in state.conflicts)
        assertTrue(Square(1, 1) in state.conflicts)
    }
}
```

`SetupViewModel`: best times surfacing per size, selection, and the start callback.

Note on the virtual clock: `runTest` drives `delay`, while `TestTimeSource` must be advanced
explicitly. The timer test advances both together — they are two clocks, and the test is
where that has to be made deliberate.

### 3 — `BestTimesDataStore` (the one piece of real IO)

Instrumented, against a temp-file DataStore: a written time reads back; a slower time does
not replace a faster one; separate sizes do not collide.

### 4 — Compose UI (few, high value)

Board renders n×n cells; tapping places and removes a queen; a conflicting queen is marked;
the victory overlay appears on solve **and is still present after
`recreate()`**; reset clears the board. Assertions go through the semantics the cells
already expose.

## Build and verification

Third-party dependencies are declared in `gradle/libs.versions.toml` and applied in
`app/build.gradle.kts`. Latest stable at the time of writing, taken from Google Maven and
Maven Central:

| Dependency | Version | Why |
|---|---|---|
| `com.google.dagger:hilt-android` / `hilt-compiler` | 2.60.1 | DI |
| `com.google.devtools.ksp` | 2.3.11 | Hilt's annotation processor |
| `androidx.hilt:hilt-navigation-compose` | 1.4.0 | `hiltViewModel()` |
| `androidx.navigation:navigation-compose` | 2.9.8 | type-safe routes |
| `org.jetbrains.kotlin.plugin.serialization` | 2.4.10 | compiler plugin — tracks the Kotlin version |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.11.0 | runtime for `@Serializable` routes |
| `androidx.datastore:datastore-preferences` | 1.2.1 | best times |
| `androidx.lifecycle:lifecycle-runtime-compose` / `-viewmodel-compose` | 2.11.0 | `collectAsStateWithLifecycle`, `repeatOnLifecycle` |
| `app.cash.turbine:turbine` | 1.2.1 | flow assertions (tests) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.11.0 | `runTest` (tests) |
| `androidx.navigation:navigation-testing` | 2.9.8 | nav graph tests |

`testOptions { unitTests { isReturnDefaultValues = true } }` is set so JVM unit tests can
construct ViewModels directly: `BaseViewModel` logs through `android.util.Log`, which
otherwise throws "not mocked" the first time the error path is exercised.

**Verified, not assumed.** The open question was whether KSP, the Hilt Gradle plugin and
the Kotlin serialization plugin would attach on this toolchain — AGP 9.3.0 with Gradle
9.7.1 and the configuration cache enabled, where no `org.jetbrains.kotlin.android` plugin
is applied because AGP 9 supplies Kotlin itself. Both of these pass on the real project:

```bash
./gradlew :app:assembleDebug        # BUILD SUCCESSFUL — kspDebugKotlin,
                                    # hiltCollectClassesDebug, hiltAggregateDepsDebug all run
./gradlew :app:testDebugUnitTest    # BUILD SUCCESSFUL — kspDebugUnitTestKotlin runs
```

Both store a configuration cache entry, so nothing here forces it off. The manual
`AppContainer` fallback that this section previously described as a contingency is not
needed and has been dropped.

`minSdk` stays at the scaffold's 34. Nothing in the design needs a lower floor, and
lowering it only widens the behavioural surface — edge-to-edge, haptic constant
availability, predictive back — that would then have to be handled and tested.

## Decisions and trade-offs

| Decision | Rationale | What it costs |
|---|---|---|
| Single Gradle module | Two screens; package layering is sufficient separation | No compiler-enforced layer boundary — convention and review |
| No `Board` type | `size` + `queens` is the whole position and the state holds both anyway | The pair is only meaningful together; nothing enforces they travel together |
| One use case, both rules | Conflicts and the win condition come from one scan, so `domain` genuinely owns all game logic | A result type (`PositionStatus`) exists only to return two values |
| No interface on the use case | Pure and deterministic — nothing volatile to invert; tests use the real one | Cannot be substituted, which is correct: substituting it would test fiction |
| Victory overlay from `state.isSolved` | Survives rotation and recreation; actions carry only transient effects | One more field the state must expose |
| `status` stored, everything else derived | Computing it needs the injected use case, which a data class cannot reach | `queens` and `status` must be written together — one `copy`, one place |
| Callbacks inside state | Screens need no ViewModel reference; tests drive the UI's exact surface | `GameState` is not a pure value; a default instance is inert |
| `SavedStateHandle` by key, not `toRoute()` | ViewModel stays constructible in a plain JVM test | The key string must match the route property name |
| Hilt | Android-only, compile-time graph validation, conventional | Annotation processing, and an unverified interaction with AGP 9.3 |
| JUnit4 everywhere | Compose tests need it regardless; one framework, no plugin | No parameterised tests without extra ceremony |
| Hand-written fakes | Assert on behaviour, not call records; forces small interfaces | A few lines per fake |
| DataStore over Room | One `Map<Int, Duration>` | Would need Room if best times grew into a history |
| Sound descoped, haptics kept | Same feedback, no assets, no mute affordance, no lifecycle | One nice-to-have partially delivered — stated, not hidden |

**Deliberately not included:** KMP source sets, a network layer, DTOs and mappers, a use
case per operation, an `AppState` CompositionLocal, per-feature Gradle modules, and a custom
design system. Each is a real pattern at larger scale and pure overhead here.

**Known scope limit:** game progress does not survive process death — only the board size
does, since it is a route argument. A puzzle session lasts minutes and the ViewModel covers
configuration changes; persisting `queens` and the start mark into `SavedStateHandle` would
be a small addition if that changed.

## Extension points

Seams most likely to be pulled on in the follow-up session:

- **Hints** — the backtracking search already exists in test sources; promoting it to
  `domain` and seeding it with the placed queens gives the next safe square. One domain type
  plus one action.
- **Undo / redo** — the position is an immutable `Set<Square>`, so a `List<Set<Square>>` in
  the ViewModel is the entire feature.
- **Per-size leaderboards** — `BestTimesRepository` is already keyed by size and the setup
  screen already lists it.
- **A second piece type** — `threatens` is the single function to generalise; nothing else
  in the app knows how a queen moves.

## Submission checklist

Required by the task and easy to lose points on:

- **README** with build, run and test commands, plus a summary of these decisions.
- **Demo video** of the app.
- **Disclosure of code-generation tool use**, which the task asks for explicitly.

## What changed in v2

Fixes from the design review of v1:

1. **Victory overlay moved from a one-shot action to `state.isSolved`** — v1's win screen
   vanished on rotation and left the board frozen.
2. **Action buffer sized to 8** — v1 could emit three actions in one frame against a
   2-slot buffer and silently drop the celebration via `tryEmit`.
3. **The win rule moved into `domain`.** v1's use case never received `size`, so the win
   condition lived in a presentation-layer data class while the doc claimed the domain owned
   the rules. `EvaluatePositionUseCase` now returns conflicts and solved together.
4. **The board is drawn, not composed.** v1 and v2 both specified a composable per square.
   Measurement killed it: 11.1 ms of draw recording per frame at 27×27, and — because
   `clickable` consumes the pointer down — a grid of cells cancels the pinch gesture
   outright, so zoom could never have worked.
5. **`QueenRemoved` added** — v1 played the placement haptic when removing a queen.
6. **`queensLeft` clamped at zero** and the complexity bound corrected: `k ≤ n²`, so worst
   case is O(n⁴), not the O(n²) v1 claimed.
7. **`error` added to `GameState`** — v1's `onError` wrote to a field that did not exist.
8. **Setup screen specified** — board size selection is a gameplay requirement and v1 gave
   it only a directory name. `Constants.BOARD_SIZES` makes `n ≥ 4` true by construction.
9. **Best times displayed**, per size, on the setup screen — v1 stored them and showed one.
10. **Solver restored as a test-only oracle** with the OEIS solution counts, exercising the
    production rule. Removing it from `domain` was right; removing it from the tests was not.
11. **`toRoute()` replaced with a keyed `SavedStateHandle` read** so ViewModel tests stay on
    the JVM.
12. **Timer fully specified** — tick rate, freeze on solve, the record decision taken once
    inside `finish()` so `isNewRecord` cannot flip mid-celebration or survive a reset, and a
    stated background policy.
17. **`updateState` changed to `(STATE) -> STATE`.** The receiver form silently shadowed
    `size` and `elapsed` behind the state's own fields, so `copy(size = size)` was a no-op —
    every board would have been 4×4 and the solve time would never have been frozen.
13. **`AppState` / `LocalAppState` removed** in favour of callback parameters.
14. **JUnit5 dropped** for JUnit4 throughout.
15. **`minSdk` 34 → 24**, versions to pin listed, and the Hilt/AGP 9.3 risk stated as an
    explicit prerequisite with a fallback.
16. **Board sizing, touch targets and `Constants.MAX_BOARD_SIZE` addressed**; sound explicitly
    descoped; submission checklist added, including the task's disclosure requirement.
