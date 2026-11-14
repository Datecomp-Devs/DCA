package com.datecomp.dca.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.datecomp.dca.onboarding.GuideScreen
import com.datecomp.dca.onboarding.OnboardingViewModel
import com.datecomp.dca.permissions.PermissionsViewModel
import com.datecomp.dca.targets.AppsScreen
import com.datecomp.dca.targets.EditTargetScreen
import com.datecomp.dca.targets.TargetsViewModel

/**
 * Navegación interna de DCA. Tres destinos:
 * 1. guide  — guía inicial de permisos (solo primera vez).
 * 2. apps   — lista de apps y su configuración (pantalla principal).
 * 3. edit   — edición de mensaje y cooldown de una app concreta.
 */
@Composable
fun DcaNavHost(
    targetsViewModel: TargetsViewModel = viewModel(),
    permissionsViewModel: PermissionsViewModel = viewModel(),
    onboardingViewModel: OnboardingViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val onboardingDone by onboardingViewModel.onboardingDone.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) ROUTE_APPS else ROUTE_GUIDE,
    ) {
        composable(ROUTE_GUIDE) {
            GuideScreen(
                permissions = permissionsViewModel,
                onDone = {
                    onboardingViewModel.finish()
                    navController.navigate(ROUTE_APPS) {
                        popUpTo(ROUTE_GUIDE) { inclusive = true }
                    }
                },
            )
        }
        composable(ROUTE_APPS) {
            AppsScreen(
                targets = targetsViewModel,
                permissions = permissionsViewModel,
                onEditTarget = { pkg -> navController.navigate("$ROUTE_EDIT/$pkg") },
            )
        }
        composable("$ROUTE_EDIT/{pkg}") { backStackEntry ->
            val pkg = backStackEntry.arguments?.getString("pkg").orEmpty()
            EditTargetScreen(
                targets = targetsViewModel,
                packageName = pkg,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private const val ROUTE_GUIDE = "guide"
private const val ROUTE_APPS = "apps"
private const val ROUTE_EDIT = "edit"
