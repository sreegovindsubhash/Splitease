package com.splitease.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavController

/**
 * Returns a wrapper around [action] that suppresses rapid consecutive
 * invocations using a shared, navigation-level throttle state.
 *
 * ## Why per-destination guards are insufficient
 *
 * `navController.popBackStack()` is synchronous.  When the first tap fires,
 * the destination is removed from the back stack before the next Compose
 * frame runs.  During the crossfade transition the newly-exposed destination
 * is already receiving touch input.  That destination has its *own*
 * per-destination guard instance whose timestamp was never stamped, so it
 * accepts the second tap and pops the stack a second time, producing a
 * blank NavHost.
 *
 * ## Fix: one shared throttle state for the whole nav graph
 *
 * [sharedState] is a [LongRef] allocated once at the NavHost level and
 * passed into every guarded back-arrow in the graph.  Because all back
 * arrows share the same last-event timestamp, the second tap is suppressed
 * no matter which destination's button happens to receive it.
 *
 * ## Lambda identity stability
 *
 * The returned lambda is keyed only on [sharedState], not on [action].
 * [action] is wrapped with [rememberUpdatedState] so that the lambda always
 * calls the latest version of the callback without ever being recreated
 * (and without resetting any throttle state).
 *
 * @param sharedState A [LongRef] allocated once at the NavHost call-site
 *   with [rememberNavBackThrottleState] and shared across all guarded
 *   back-arrows in the same nav graph.
 * @param throttleMs Minimum gap in milliseconds between two accepted calls.
 *   500 ms covers any realistic double-tap while remaining imperceptible for
 *   deliberate navigation.
 * @param action The navigation action to execute (typically
 *   `navController.popBackStack()`).
 */
@Composable
fun rememberSingleEventHandler(
    sharedState: LongRef,
    throttleMs: Long = 500L,
    action: () -> Unit,
): () -> Unit {
    // Always call the latest action without recreating the returned lambda.
    val currentAction = rememberUpdatedState(action)
    // The returned lambda is stable: it is keyed only on sharedState and
    // throttleMs, both of which are constant for the lifetime of the NavHost.
    return remember(sharedState, throttleMs) {
        {
            val now = System.currentTimeMillis()
            if (now - sharedState.value >= throttleMs) {
                sharedState.value = now
                currentAction.value()
            }
        }
    }
}

/**
 * Pops the back stack only when there is a previous entry to return to.
 *
 * Calling [NavController.popBackStack] when the back stack contains only the
 * root destination empties the stack and leaves the [NavHost] composing
 * nothing — a blank white screen.  This extension makes every back action
 * unconditionally safe: if there is nowhere to go back to, the call is a
 * silent no-op and the root destination remains on screen.
 *
 * This is the primary correctness guard.  The shared-timestamp throttle in
 * [rememberSingleEventHandler] is retained as defence-in-depth but the app
 * must never rely on it alone for stack safety.
 */
fun NavController.safePopBackStack() {
    if (previousBackStackEntry != null) {
        popBackStack()
    }
}

/**
 * Allocates the shared throttle state for a nav graph.
 *
 * Call this once at the top of [SplitEaseNavGraph], above the [NavHost],
 * then pass the result to every [rememberSingleEventHandler] call inside the
 * graph.  The [LongRef] is initialised to `-throttleMs` so that the very
 * first back tap is always accepted.
 *
 * @param throttleMs Must match the value passed to [rememberSingleEventHandler].
 */
@Composable
fun rememberNavBackThrottleState(throttleMs: Long = 500L): LongRef =
    remember { LongRef(-throttleMs) }

/**
 * Mutable holder for a [Long] timestamp.
 *
 * Kept on the heap (rather than as a Compose [androidx.compose.runtime.State])
 * so that writes inside click-handler lambdas do not trigger recomposition.
 * Compose click handlers always run on the main thread, so no
 * synchronisation is required.
 */
class LongRef(var value: Long)
