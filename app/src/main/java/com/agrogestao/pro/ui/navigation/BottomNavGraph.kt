package com.agrogestao.pro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.reminders.DisabledTaskReminderGateway
import com.agrogestao.pro.data.reminders.TaskReminderGateway
import com.agrogestao.pro.ui.auth.AuthScreen
import com.agrogestao.pro.ui.auth.AuthViewModel
import com.agrogestao.pro.ui.auth.AuthViewModelFactory
import com.agrogestao.pro.ui.dashboard.DashboardScreen
import com.agrogestao.pro.ui.dashboard.DashboardViewModel
import com.agrogestao.pro.ui.dashboard.DashboardViewModelFactory
import com.agrogestao.pro.ui.kanban.KanbanScreen
import com.agrogestao.pro.ui.kanban.KanbanViewModel
import com.agrogestao.pro.ui.kanban.KanbanViewModelFactory
import com.agrogestao.pro.ui.relatorios.RelatorioCreditoScreen
import com.agrogestao.pro.ui.relatorios.RelatorioCreditoViewModel
import com.agrogestao.pro.ui.relatorios.RelatorioCreditoViewModelFactory
import com.agrogestao.pro.ui.safras.SafrasScreen
import com.agrogestao.pro.ui.safras.SafrasViewModel
import com.agrogestao.pro.ui.safras.SafrasViewModelFactory
import com.agrogestao.pro.ui.profile.ProfileScreen
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.TextMuted

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Auth : Screen("auth", "Conta", Icons.Default.Home)
    object Dashboard : Screen("dashboard", "Início", Icons.Default.Home)
    object Kanban : Screen("kanban", "Tarefas", Icons.AutoMirrored.Filled.Assignment)
    object Safras : Screen("safras", "Talhões", Icons.Default.Agriculture)
    object Relatorio : Screen("relatorio", "Custos", Icons.Default.Description)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
}

@Composable
fun MainAppNavigation(
    repository: AgroRepository,
    taskReminderGateway: TaskReminderGateway = DisabledTaskReminderGateway,
    requestedRoute: String? = null,
    onRequestedRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()

    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModelFactory(repository))
    val dashboardViewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(repository))
    val kanbanViewModel: KanbanViewModel = viewModel(
        factory = KanbanViewModelFactory(repository, taskReminderGateway)
    )
    val safrasViewModel: SafrasViewModel = viewModel(factory = SafrasViewModelFactory(repository))
    val relatorioViewModel: RelatorioCreditoViewModel = viewModel(factory = RelatorioCreditoViewModelFactory(repository))

    val items = listOf(
        Screen.Dashboard,
        Screen.Safras,
        Screen.Kanban,
        Screen.Relatorio,
        Screen.Profile
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isAuthRoute = currentRoute == Screen.Auth.route
    val producer by repository.producerProfile.collectAsState(initial = null)

    LaunchedEffect(producer?.isLoggedIn, currentRoute, requestedRoute) {
        if (producer?.isLoggedIn == true && currentRoute == Screen.Auth.route) {
            navController.navigate(requestedRoute ?: Screen.Dashboard.route) {
                popUpTo(Screen.Auth.route) { inclusive = true }
            }
            if (requestedRoute != null) onRequestedRouteHandled()
        } else if (
            producer?.isLoggedIn == true &&
            requestedRoute != null &&
            currentRoute != requestedRoute
        ) {
            navController.navigate(requestedRoute) { launchSingleTop = true }
            onRequestedRouteHandled()
        } else if (producer?.isLoggedIn == false && currentRoute != Screen.Auth.route) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (!isAuthRoute) {
                Surface(
                    shadowElevation = 2.dp,
                    color = SurfaceCard
                ) {
                    NavigationBar(
                        modifier = Modifier.height(68.dp),
                        containerColor = SurfaceCard,
                        tonalElevation = 0.dp
                    ) {
                        items.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = screen.icon,
                                        contentDescription = screen.title,
                                        tint = if (selected) PrimaryAgroGreen else TextMuted
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) PrimaryAgroGreen else TextMuted
                                    )
                                },
                                selected = selected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryAgroGreen,
                                    selectedTextColor = PrimaryAgroGreen,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = PrimaryAgroGreen.copy(alpha = 0.12f)
                                ),
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Auth.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    viewModel = authViewModel,
                    onAuthSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToTasks = { navController.navigate(Screen.Kanban.route) },
                    onNavigateToSafras = { navController.navigate(Screen.Safras.route) }
                )
            }
            composable(Screen.Kanban.route) {
                KanbanScreen(
                    viewModel = kanbanViewModel,
                    onBack = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Safras.route) {
                SafrasScreen(
                    viewModel = safrasViewModel,
                    onBack = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Relatorio.route) {
                RelatorioCreditoScreen(
                    viewModel = relatorioViewModel,
                    onBack = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    dashboardViewModel = dashboardViewModel,
                    reportViewModel = relatorioViewModel,
                    onBack = { navController.navigate(Screen.Dashboard.route) { launchSingleTop = true } },
                    onNavigateToTasks = { navController.navigate(Screen.Kanban.route) },
                    onNavigateToReports = { navController.navigate(Screen.Relatorio.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Dashboard.route) }
                )
            }
        }
    }
}
