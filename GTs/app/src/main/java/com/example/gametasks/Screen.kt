package com.example.gametasks

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object Setting : Screen("setting")

    // ⭕️ 「/別名」や引数を受け取れるように拡張
    object AddGame : Screen("add_game?gameId={gameId}") {
        // 新規追加のときは引数なしで呼ぶための関数
        fun createRoute() = "add_game"
        // 編集・詳細表示のときはゲームのIDを付けて呼ぶための関数
        fun createRoute(gameId: Int) = "add_game?gameId=$gameId"
    }
}