package com.btween.app.ui.navigation

sealed class Destination(val route: String) {

    data object Home : Destination("home")
    data object Library : Destination("library")
    data object Favorites : Destination("favorites")
    data object Search : Destination("search")
    data object Categories : Destination("categories")
    data object Settings : Destination("settings")
    data object Login : Destination("login")
    data object Register : Destination("register")

    data object AddEditQuote : Destination("add_edit_quote?quoteId={quoteId}") {
        const val ARG_QUOTE_ID = "quoteId"
        const val NEW_QUOTE_ID = -1L
        fun createRoute(quoteId: Long? = null) = "add_edit_quote?quoteId=${quoteId ?: NEW_QUOTE_ID}"
    }

    data object QuoteDetail : Destination("quote_detail/{quoteId}") {
        const val ARG_QUOTE_ID = "quoteId"
        fun createRoute(quoteId: Long) = "quote_detail/$quoteId"
    }
}
