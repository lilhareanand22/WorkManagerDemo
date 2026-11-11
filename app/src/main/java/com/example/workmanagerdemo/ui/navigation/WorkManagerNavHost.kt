package com.example.workmanagerdemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.workmanagerdemo.ui.screens.dashboard.DashboardScreen
import com.example.workmanagerdemo.ui.screens.onetimework.OneTimeWorkScreen
import com.example.workmanagerdemo.ui.screens.progresswork.ProgressWorkScreen
import com.example.workmanagerdemo.ui.screens.periodicwork.PeriodicWorkScreen
import com.example.workmanagerdemo.ui.screens.expeditedwork.ExpeditedWorkScreen
import com.example.workmanagerdemo.ui.screens.chainedwork.ChainedWorkScreen
import com.example.workmanagerdemo.ui.screens.uniquework.UniqueWorkScreen
import com.example.workmanagerdemo.ui.screens.foregroundwork.ForegroundWorkScreen

/**
 * Root navigation graph for the WorkManager Demo app.
 *
 * Uses Jetpack Navigation Compose with a [NavHostController].
 * The start destination is [WorkManagerScreen.Dashboard].
 *
 * Navigation flow:
 *   Dashboard → any of the 7 demo screens (via card tap)
 *   Demo screen → Dashboard (via top app bar back button)
 *
 * Each composable destination receives a [navController] lambda so it can
 * navigate back without holding a reference to the full controller.
 */
@Composable
fun WorkManagerNavHost(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController   = navController,
        startDestination = WorkManagerScreen.Dashboard.route
    ) {
        composable(WorkManagerScreen.Dashboard.route) {
            DashboardScreen(
                onNavigateTo = { screen -> navController.navigate(screen.route) }
            )
        }

        composable(WorkManagerScreen.OneTimeWork.route) {
            OneTimeWorkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WorkManagerScreen.ProgressWork.route) {
            ProgressWorkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WorkManagerScreen.PeriodicWork.route) {
            PeriodicWorkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WorkManagerScreen.ExpeditedWork.route) {
            ExpeditedWorkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WorkManagerScreen.ChainedWork.route) {
            ChainedWorkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WorkManagerScreen.UniqueWork.route) {
            UniqueWorkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WorkManagerScreen.ForegroundWork.route) {
            ForegroundWorkScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WorkManagerScreen.CustomFactory.route) {
            com.example.workmanagerdemo.ui.screens.customfactory.CustomFactoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WorkManagerScreen.InterviewQuestions.route) {
            com.example.workmanagerdemo.ui.screens.interview.InterviewQuestionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
