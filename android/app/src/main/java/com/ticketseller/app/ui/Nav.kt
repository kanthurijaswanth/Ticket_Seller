package com.ticketseller.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ticketseller.app.ui.screens.auth.AuthViewModel
import com.ticketseller.app.ui.screens.auth.LoginScreen
import com.ticketseller.app.ui.screens.auth.SignupScreen
import com.ticketseller.app.ui.screens.chat.ChatScreen
import com.ticketseller.app.ui.screens.listing.ListingDetailScreen
import com.ticketseller.app.ui.screens.main.MainShell
import com.ticketseller.app.ui.screens.orders.OrderDetailScreen
import com.ticketseller.app.ui.screens.orders.PaymentComingSoonScreen
import com.ticketseller.app.ui.screens.profile.EditProfileScreen
import com.ticketseller.app.ui.screens.sell.SellScreen

object Routes {
    const val Login = "login"
    const val Signup = "signup"
    const val Main = "main"
    const val Sell = "sell"
    const val ListingDetail = "listing/{id}"
    const val Chat = "chat/{threadId}"
    const val OrderDetail = "order/{id}"
    const val Payment = "payment/{orderId}"
    const val EditProfile = "profile/edit"

    fun listing(id: String) = "listing/$id"
    fun chat(threadId: String) = "chat/$threadId"
    fun order(id: String) = "order/$id"
    fun payment(id: String) = "payment/$id"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val authVm: AuthViewModel = hiltViewModel()
    val signedIn by authVm.isSignedIn.collectAsState()

    val start = if (signedIn) Routes.Main else Routes.Login

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.Login) {
            LoginScreen(
                onSignedIn = { nav.navigate(Routes.Main) { popUpTo(Routes.Login) { inclusive = true } } },
                onGoSignup = { nav.navigate(Routes.Signup) },
            )
        }
        composable(Routes.Signup) {
            SignupScreen(
                onSignedUp = { nav.navigate(Routes.Main) { popUpTo(Routes.Login) { inclusive = true } } },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.Main) {
            MainShell(
                onSell = { nav.navigate(Routes.Sell) },
                onListing = { id -> nav.navigate(Routes.listing(id)) },
                onChat = { tid -> nav.navigate(Routes.chat(tid)) },
                onOrder = { id -> nav.navigate(Routes.order(id)) },
                onEditProfile = { nav.navigate(Routes.EditProfile) },
                onSignOut = {
                    authVm.signOut()
                    nav.navigate(Routes.Login) { popUpTo(0) }
                },
            )
        }
        composable(Routes.Sell) {
            SellScreen(onDone = { nav.popBackStack() }, onBack = { nav.popBackStack() })
        }
        composable(
            Routes.ListingDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            ListingDetailScreen(
                listingId = id,
                onBack = { nav.popBackStack() },
                onOpenChat = { tid -> nav.navigate(Routes.chat(tid)) },
                onOpenPayment = { oid -> nav.navigate(Routes.payment(oid)) },
            )
        }
        composable(
            Routes.Chat,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType }),
        ) { entry ->
            val tid = entry.arguments?.getString("threadId").orEmpty()
            ChatScreen(threadId = tid, onBack = { nav.popBackStack() })
        }
        composable(
            Routes.OrderDetail,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            OrderDetailScreen(
                orderId = id,
                onBack = { nav.popBackStack() },
                onPay = { nav.navigate(Routes.payment(id)) },
            )
        }
        composable(
            Routes.Payment,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("orderId").orEmpty()
            PaymentComingSoonScreen(orderId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.EditProfile) {
            EditProfileScreen(onDone = { nav.popBackStack() })
        }
    }
}
