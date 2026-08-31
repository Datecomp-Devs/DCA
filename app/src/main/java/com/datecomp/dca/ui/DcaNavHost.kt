package com.datecomp.dca.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Navegación interna de DCA. Tres destinos:
 * 1. guide  — guía inicial de permisos (solo primera vez).
 * 2. apps   — lista de apps y su configuración (pantalla principal).
 * 3. edit   — edición de mensaje y cooldown de una app concreta.
 */
@Composable
fun DcaNavHost(viewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val onboardingDone by viewModel.onboardingDone.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) ROUTE_APPS else ROUTE_GUIDE,
    ) {
        composable(ROUTE_GUIDE) {
            GuideScreen(
                viewModel = viewModel,
                onDone = {
                    viewModel.finishOnboarding()
                    navController.navigate(ROUTE_APPS) {
                        popUpTo(ROUTE_GUIDE) { inclusive = true }
                    }
                },
            )
        }
        composable(ROUTE_APPS) {
            AppsScreen(
                viewModel = viewModel,
                onEditTarget = { pkg -> navController.navigate("$ROUTE_EDIT/$pkg") },
            )
        }
        composable("$ROUTE_EDIT/{pkg}") { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("pkg").orEmpty()
            EditTargetScreen(
                viewModel = viewModel,
                packageName = pkg,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private const val ROUTE_GUIDE = "guide"
private const val ROUTE_APPS = "apps"
private const val ROUTE_EDIT = "edit"