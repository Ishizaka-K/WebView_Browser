package com.example.webviewbrowser.core.mvi

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MviViewModelTest {

    private data class FakeState(val count: Int) : MviState
    private sealed interface FakeIntent : MviIntent {
        data object Increment : FakeIntent
        data object Notify : FakeIntent
    }
    private data class FakeEffect(val message: String) : MviEffect

    private class FakeViewModel : MviViewModel<FakeState, FakeIntent, FakeEffect>(FakeState(0)) {
        override fun onIntent(intent: FakeIntent) {
            when (intent) {
                FakeIntent.Increment -> dispatch(intent)
                FakeIntent.Notify -> emitEffect(FakeEffect("hi"))
            }
        }

        override fun reduce(state: FakeState, intent: FakeIntent): FakeState = when (intent) {
            FakeIntent.Increment -> state.copy(count = state.count + 1)
            FakeIntent.Notify -> state
        }
    }

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `intent updates state via reducer`() = runTest {
        val vm = FakeViewModel()
        vm.onIntent(FakeIntent.Increment)
        vm.onIntent(FakeIntent.Increment)
        assertEquals(2, vm.state.value.count)
    }

    @Test
    fun `effect is emitted`() = runTest {
        val vm = FakeViewModel()
        vm.effect.test {
            vm.onIntent(FakeIntent.Notify)
            assertEquals("hi", awaitItem().message)
        }
    }
}
