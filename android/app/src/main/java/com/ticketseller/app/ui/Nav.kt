package com.ticketseller.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ticketseller.app.ui.screens.HomeScreen

@Composable
fun AppNav(){
  val nav = rememberNavController()
  NavHost(navController = nav, startDestination = "home"){
    composable("home"){ HomeScreen() }
  }
}
