package com.budgetmaster.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.budgetmaster.core.navigation.AuthRoute
import com.budgetmaster.auth.presentation.biometric.BiometricScreen
import com.budgetmaster.auth.presentation.biometric.BiometricViewModel
import com.budgetmaster.auth.presentation.forgotpassword.ForgotPasswordScreen
import com.budgetmaster.auth.presentation.forgotpassword.ForgotPasswordViewModel
import com.budgetmaster.auth.presentation.login.LoginScreen
import com.budgetmaster.auth.presentation.login.LoginViewModel
import com.budgetmaster.auth.presentation.onboarding.OnboardingScreen
import com.budgetmaster.auth.presentation.onboarding.OnboardingViewModel
import com.budgetmaster.auth.presentation.register.RegisterScreen
import com.budgetmaster.auth.presentation.register.RegisterViewModel
import com.budgetmaster.auth.presentation.splash.SplashScreen
import com.budgetmaster.auth.presentation.splash.SplashViewModel
import com.budgetmaster.dashboard.presentation.DashboardScreen
import com.budgetmaster.transactions.presentation.TransactionsScreen
import com.budgetmaster.budgets.presentation.BudgetsScreen
import com.budgetmaster.budgets.presentation.GoalsScreen
import com.budgetmaster.reports.presentation.ReportsScreen
import com.budgetmaster.settings.presentation.SettingsScreen
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main application entry point for the shared Compose Multiplatform UI.
 * Configures the dark theme, navigation graph, and responsive layouts.
 */
@Composable
@Preview
fun App() {
    // Premium obsidian/violet color scheme matching DESIGN_SYSTEM.md dark mode requirement
    val premiumDarkColors = darkColorScheme(
        primary = Color(0xFF6366F1), // Indigo Neon
        secondary = Color(0xFF10B981), // Emerald Inflow
        tertiary = Color(0xFF8B5CF6), // Amethyst
        error = Color(0xFFF87171), // Coral Outflow
        background = Color(0xFF0B0E14), // Deep Obsidian
        surface = Color(0xFF131924), // Charcoal Card Surface
        outline = Color(0xFF1F293D), // Steel Gray divider/borders
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onError = Color.White,
        onBackground = Color(0xFFF8FAFC), // Slate white body
        onSurface = Color(0xFFF8FAFC)
    )

    MaterialTheme(
        colorScheme = premiumDarkColors
    ) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        // Identify which destinations belong to the core dashboard sub-navigation tabs
        val mainDestinations = listOf(
            AuthRoute.Dashboard::class.qualifiedName,
            AuthRoute.Transactions::class.qualifiedName,
            AuthRoute.Budgets::class.qualifiedName,
            AuthRoute.Goals::class.qualifiedName,
            AuthRoute.Reports::class.qualifiedName,
            AuthRoute.Settings::class.qualifiedName
        )

        val isTabDestination = currentRoute in mainDestinations

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTablet = maxWidth >= 600.dp && maxWidth < 1240.dp
            val isDesktop = maxWidth >= 1240.dp

            if (isTabDestination) {
                // Adaptive layout container shell for post-auth dashboard views
                if (isDesktop) {
                    // Desktop Layout: Permanent Navigation Drawer + Centered Content Grid
                    PermanentNavigationDrawer(
                        drawerContent = {
                            Box(
                                modifier = Modifier
                                    .width(260.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(16.dp)
                            ) {
                                androidx.compose.foundation.layout.Column {
                                    Text(
                                        text = "BudgetMaster",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(bottom = 24.dp, start = 12.dp)
                                    )
                                    getNavigationItems().forEach { item ->
                                        NavigationDrawerItem(
                                            label = { Text(item.title) },
                                            selected = currentRoute == item.route::class.qualifiedName,
                                            onClick = {
                                                navController.navigate(item.route) {
                                                    popUpTo(navController.graph.findStartDestination().id) {
                                                        saveState = true
                                                    }
                                                    launchSingleTop = true
                                                    restoreState = true
                                                }
                                            },
                                            icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(modifier = Modifier.width(1200.dp)) {
                                MainNavGraph(navController = navController)
                            }
                        }
                    }
                } else if (isTablet) {
                    // Tablet Layout: Left Navigation Rail + Viewport contents
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(modifier = Modifier.weight(1f))
                            getNavigationItems().forEach { item ->
                                NavigationRailItem(
                                    selected = currentRoute == item.route::class.qualifiedName,
                                    onClick = {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                                    label = { Text(item.title) },
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            MainNavGraph(navController = navController)
                        }
                    }
                } else {
                    // Mobile Layout: Scaffold Content + Bottom Navigation Bar
                    Scaffold(
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                getNavigationItems().forEach { item ->
                                    NavigationBarItem(
                                        selected = currentRoute == item.route::class.qualifiedName,
                                        onClick = {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.title
                                            )
                                        },
                                        label = { Text(item.title) }
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .background(MaterialTheme.colorScheme.background)
                        ) {
                            MainNavGraph(navController = navController)
                        }
                    }
                }
            } else {
                // Auth navigation flow container without layout shells
                MainNavGraph(navController = navController)
            }
        }
    }
}

/**
 * Declares the application NavHost containing authentication, onboarding, and dashboard routes.
 */
@Composable
private fun MainNavGraph(
    navController: androidx.navigation.NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = AuthRoute.Splash
    ) {
        composable<AuthRoute.Splash> {
            val splashViewModel: SplashViewModel = koinViewModel()
            SplashScreen(
                viewModel = splashViewModel,
                onNavigateToOnboarding = {
                    navController.navigate(AuthRoute.Onboarding) {
                        popUpTo(AuthRoute.Splash) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(AuthRoute.Login) {
                        popUpTo(AuthRoute.Splash) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(AuthRoute.Dashboard) {
                        popUpTo(AuthRoute.Splash) { inclusive = true }
                    }
                }
            )
        }

        composable<AuthRoute.Onboarding> {
            val onboardingViewModel: OnboardingViewModel = koinViewModel()
            OnboardingScreen(
                viewModel = onboardingViewModel,
                onNavigateToBiometric = {
                    navController.navigate(AuthRoute.Biometric) {
                        popUpTo(AuthRoute.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable<AuthRoute.Login> {
            val loginViewModel: LoginViewModel = koinViewModel()
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToHome = {
                    navController.navigate(AuthRoute.Dashboard) {
                        popUpTo(AuthRoute.Login) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AuthRoute.Register)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(AuthRoute.ForgotPassword)
                }
            )
        }

        composable<AuthRoute.Register> {
            val registerViewModel: RegisterViewModel = koinViewModel()
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToHome = {
                    navController.navigate(AuthRoute.Dashboard) {
                        popUpTo(AuthRoute.Login) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable<AuthRoute.ForgotPassword> {
            val forgotPasswordViewModel: ForgotPasswordViewModel = koinViewModel()
            ForgotPasswordScreen(
                viewModel = forgotPasswordViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        composable<AuthRoute.Biometric> {
            val biometricViewModel: BiometricViewModel = koinViewModel()
            BiometricScreen(
                viewModel = biometricViewModel,
                onNavigateToHome = {
                    navController.navigate(AuthRoute.Dashboard) {
                        popUpTo(AuthRoute.Biometric) { inclusive = true }
                    }
                }
            )
        }

        composable<AuthRoute.Dashboard> {
            DashboardScreen(
                onQuickAction = { action ->
                    if (action == "Settings" || action == "Notifications") {
                        navController.navigate(AuthRoute.Settings)
                    }
                }
            )
        }

        composable<AuthRoute.Transactions> {
            TransactionsScreen()
        }

        composable<AuthRoute.Budgets> {
            BudgetsScreen()
        }

        composable<AuthRoute.Goals> {
            GoalsScreen()
        }

        composable<AuthRoute.Reports> {
            ReportsScreen()
        }

        composable<AuthRoute.Settings> {
            SettingsScreen(
                onSignOut = {
                    navController.navigate(AuthRoute.Login) {
                        popUpTo(AuthRoute.Dashboard) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Structural definition representing tab item data.
 */
private data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val route: AuthRoute
)

/**
 * Helper to yield the dashboard navigation bar items list.
 */
private fun getNavigationItems(): List<NavigationItem> = listOf(
    NavigationItem("Home", Icons.Default.Home, AuthRoute.Dashboard),
    NavigationItem("History", Icons.AutoMirrored.Filled.List, AuthRoute.Transactions),
    NavigationItem("Budgets", Icons.Default.Info, AuthRoute.Budgets),
    NavigationItem("Goals", Icons.Default.Star, AuthRoute.Goals),
    NavigationItem("Reports", Icons.Default.Favorite, AuthRoute.Reports),
    NavigationItem("Settings", Icons.Default.Settings, AuthRoute.Settings)
)
