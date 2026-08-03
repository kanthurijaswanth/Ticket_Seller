package com.ticketseller.app.ui.theme

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Thin VM so composables can reach the singleton [ThemeController] via hiltViewModel(). */
@HiltViewModel
class ThemeViewModel @Inject constructor(
    val controller: ThemeController,
) : ViewModel()
