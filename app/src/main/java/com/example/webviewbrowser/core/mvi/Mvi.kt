package com.example.webviewbrowser.core.mvi

/** MVI の状態を表すマーカー。イミュータブルな data class を実装に使う。 */
interface MviState

/** ユーザー操作や外部イベントを表すマーカー。 */
interface MviIntent

/** 1 回限りの副作用（共有、SnackBar、画面遷移）を表すマーカー。 */
interface MviEffect
