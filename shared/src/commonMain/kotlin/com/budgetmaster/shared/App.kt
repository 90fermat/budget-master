package com.budgetmaster.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import budgetmaster.core.generated.resources.Res
import budgetmaster.core.generated.resources.nav_budgets
import budgetmaster.core.generated.resources.nav_goals
import budgetmaster.core.generated.resources.nav_history
import budgetmaster.core.generated.resources.nav_home
import budgetmaster.core.generated.resources.nav_reports
import budgetmaster.core.generated.resources.nav_settings
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
import com.budgetmaster.budgets.presentation.BudgetsScreen
import com.budgetmaster.budgets.presentation.GoalsScreen
import com.budgetmaster.core.designsystem.AppLogo
import com.budgetmaster.core.designsystem.AppTheme
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.localization.LocalAppLocale
import com.budgetmaster.core.navigation.AuthRoute
import com.budgetmaster.core.prefs.AppSettings
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.dashboard.presentation.DashboardScreen
import com.budgetmaster.reports.presentation.ReportsScreen
import com.budgetmaster.settings.presentation.SettingsScreen
import com.budgetmaster.transactions.presentation.TransactionsScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Main application entry point for the shared Compose Multiplatform UI.
 *
 * Applies the persisted user settings (brand palette, dark mode, language) through
 * [AppTheme] and [LocalAppLocale], then hosts the navigation graph inside the
 * adaptive layout shell (phone / tablet / desktop).
 */
@Composable
@Preview
fun App() {
    val settingsRepository = koinInject<AppSettingsRepository>()
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

    val darkTheme = when (settings.darkMode) {
        DarkModeSetting.SYSTEM -> isSystemInDarkTheme()
        DarkModeSetting.LIGHT -> false
        DarkModeSetting.DARK -> true
    }

    CompositionLocalProvider(LocalAppLocale provides settings.language.tag) {
        // Re-key the subtree so string resources reload when the language changes.
        key(settings.language) {
            AppTheme(palette = settings.palette, darkTheme = darkTheme) {
                AppShell()
            }
        }
    }
}

/**
 * Adaptive layout shell: bottom bar on phones, navigation rail on tablets,
 * permanent drawer on desktop/web (DESIGN_SYSTEM.md §8).
 */
@Composable
private fun AppShell() {
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
                            Column {
                                AppLogo(
                                    markSize = 30.dp,
                                    wordmarkSize = 20.sp,
                                    modifier = Modifier.padding(bottom = 24.dp, start = 12.dp)
                                )
                                navigationItems.forEach { item ->
                                    NavigationDrawerItem(
                                        label = { Text(stringResource(item.title)) },
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
                                                contentDescription = stringResource(item.title)
                                            )
                                        },
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
                        navigationItems.forEach { item ->
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
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = stringResource(item.title)
                                    )
                                },
                                label = { Text(stringResource(item.title)) },
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
                            navigationItems.forEach { item ->
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
                                            contentDescription = stringResource(item.title)
                                        )
                                    },
                                    label = { Text(stringResource(item.title)) }
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

/**
 * Declares the application NavHost containing authentication, onboarding, and dashboard routes.
 */
@Composable
private fun MainNavGraph(navController: androidx.navigation.NavHostController) {
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
                },
                onNavigateToLogin = {
                    navController.navigate(AuthRoute.Login) {
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
                },
                onReplayOnboarding = {
                    navController.navigate(AuthRoute.Onboarding) {
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
    val title: StringResource,
    val icon: ImageVector,
    val route: AuthRoute
)

/**
 * The dashboard navigation bar items (localized titles).
 */
private val navigationItems: List<NavigationItem> = listOf(
    NavigationItem(Res.string.nav_home, Icons.Default.Home, AuthRoute.Dashboard),
    NavigationItem(Res.string.nav_history, Icons.AutoMirrored.Filled.List, AuthRoute.Transactions),
    NavigationItem(Res.string.nav_budgets, Icons.Default.Info, AuthRoute.Budgets),
    NavigationItem(Res.string.nav_goals, Icons.Default.Star, AuthRoute.Goals),
    NavigationItem(Res.string.nav_reports, Icons.Default.Favorite, AuthRoute.Reports),
    NavigationItem(Res.string.nav_settings, Icons.Default.Settings, AuthRoute.Settings)
)
