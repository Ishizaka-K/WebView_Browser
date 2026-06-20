package com.example.webviewbrowser.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI の共通基盤 ViewModel。
 *
 * - [state] は単一の StateFlow。
 * - [effect] は 1 回限りの副作用ストリーム。
 * - [reduce] は純粋関数として状態遷移を表す（副作用を起こさない）。
 * - 副作用は [onIntent] の実装側で実行し、結果を新たな Intent として再投入する。
 *
 * @param initialState 初期状態。
 */
abstract class MviViewModel<S : MviState, I : MviIntent, E : MviEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<E>(Channel.BUFFERED)
    val effect: Flow<E> = _effect.receiveAsFlow()

    /** 現在の状態のスナップショット。 */
    protected val currentState: S get() = _state.value

    /** Intent を処理する。実装側で reduce 呼び出しと副作用実行を行う。 */
    abstract fun onIntent(intent: I)

    /** 純粋関数として状態遷移を定義する。 */
    protected abstract fun reduce(state: S, intent: I): S

    /** reduce を適用して状態を更新する。 */
    protected fun dispatch(intent: I) {
        _state.update { reduce(it, intent) }
    }

    /** 任意の関数で状態を更新する（副作用結果の反映など）。 */
    protected fun setState(reducer: (S) -> S) {
        _state.update(reducer)
    }

    /** 副作用を発行する。 */
    protected fun emitEffect(value: E) {
        viewModelScope.launch { _effect.send(value) }
    }
}
