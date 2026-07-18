package com.budgetmaster.shared

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberCoroutineScope
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
import com.budgetmaster.auth.domain.model.AuthStatus
import com.budgetmaster.auth.domain.usecase.CheckAuthStatusUseCase
import com.budgetmaster.auth.domain.usecase.DeleteAccountUseCase
import com.budgetmaster.auth.domain.usecase.SignOutUseCase
import com.budgetmaster.auth.presentation.splash.SplashScreen
import com.budgetmaster.auth.presentation.splash.SplashViewModel
import com.budgetmaster.accounts.presentation.AccountsIntent
import com.budgetmaster.accounts.presentation.AccountsScreen
import com.budgetmaster.accounts.presentation.AccountsViewModel
import com.budgetmaster.accounts.presentation.components.AccountSwitcher
import com.budgetmaster.budgets.presentation.BudgetsScreen
import com.budgetmaster.budgets.presentation.GoalsScreen
import com.budgetmaster.core.db.DefaultData
import com.budgetmaster.core.session.SessionStore
import com.budgetmaster.core.session.SessionUser
import com.budgetmaster.core.util.isReducedMotionEnabled
import com.budgetmaster.core.currency.RefreshExchangeRatesUseCase
import com.budgetmaster.transactions.domain.usecase.MaterializeDueRecurringUseCase
import com.budgetmaster.core.designsystem.AppLogo
import com.budgetmaster.core.designsystem.Motion
import com.budgetmaster.core.designsystem.AppTheme
import com.budgetmaster.core.designsystem.DarkModeSetting
import com.budgetmaster.core.localization.LocalAppLocale
import com.budgetmaster.core.navigation.AuthRoute
import com.budgetmaster.core.prefs.AppSettings
import com.budgetmaster.core.db.AppDataSeeder
import com.budgetmaster.core.prefs.AppSettingsRepository
import com.budgetmaster.dashboard.presentation.DashboardScreen
import com.budgetmaster.reports.presentation.ReportsScreen
import com.budgetmaster.settings.presentation.SettingsScreen
import com.budgetmaster.transactions.presentation.TransactionsScreen
import com.budgetmaster.transactions.presentation.recurring.RecurringScreen
import kotlinx.coroutines.launch
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
// Deprecated in favour of androidx.compose.ui.tooling.preview.Preview, which does not resolve on
// wasmJs - taking the replacement would trade this warning for a broken web build. Deliberately
// left until the replacement covers every target. Same call as in PreviewLightDark.kt.
@Preview
fun App() {
    val settingsRepository = koinInject<AppSettingsRepository>()
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

    // Bind the signed-in user as the data owner and seed their user row + first wallet.
    // Feature repositories read SessionStore.currentUserId to scope their data per account.
    val seeder = koinInject<AppDataSeeder>()
    val sessionStore = koinInject<SessionStore>()
    val checkAuthStatus = koinInject<CheckAuthStatusUseCase>()
    val materializeDueRecurring = koinInject<MaterializeDueRecurringUseCase>()
    val refreshExchangeRates = koinInject<RefreshExchangeRatesUseCase>()
    LaunchedEffect(Unit) {
        checkAuthStatus().collect { status ->
            if (status is AuthStatus.Authenticated) {
                sessionStore.setCurrentUser(
                    SessionUser(
                        id = status.user.id,
                        displayName = status.user.displayName,
                        email = status.user.email,
                    ),
                )
                seeder.seedForUser(status.user.id, status.user.email)
            } else {
                // Not signed in: fall back to the local default user so the app stays usable.
                sessionStore.setCurrentUser(null)
                seeder.seedForUser(DefaultData.DEFAULT_USER_ID)
            }
            // There is no background scheduler, so recurring entries catch up on open. Safe to
            // re-run: each occurrence has a deterministic id.
            materializeDueRecurring()
        }
    }

    // Rates are what make a multi-currency net worth mean anything; nothing populated the cache
    // before, so a second-currency wallet was always "approximate". Re-keyed on the currency so
    // switching it fetches the new base. Fetches at most daily and fails silently.
    LaunchedEffect(settings.currency) {
        refreshExchangeRates(settings.currency)
    }

    val darkTheme = when (settings.darkMode) {
        DarkModeSetting.SYSTEM -> isSystemInDarkTheme()
        DarkModeSetting.LIGHT -> false
        DarkModeSetting.DARK -> true
    }

    CompositionLocalProvider(LocalAppLocale provides settings.language.tag) {
        // Re-key the subtree so string resources reload when the language changes.
        key(settings.language) {
            AppTheme(palette = settings.palette, darkTheme = darkTheme) {
                // Root themed backdrop so every screen (incl. the auth flow) has a
                // painted background and matching content color — otherwise onBackground
                // text renders on the raw page background and can be invisible on web.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppShell()
                }
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
        AuthRoute.Settings::class.qualifiedName,
        AuthRoute.Accounts::class.qualifiedName,
        AuthRoute.Recurring::class.qualifiedName
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
                        Column(modifier = Modifier.width(1200.dp)) {
                            AccountScopeBar(navController)
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
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        AccountScopeBar(navController)
                        MainNavGraph(navController = navController)
                    }
                }
            } else {
                // Mobile Layout: Scaffold Content + Bottom Navigation Bar
                Scaffold(
                    topBar = { AccountScopeBar(navController) },
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
    // A fade with a hair of scale reads well for both a tab switch and a push, without the
    // directional slide that looks wrong when the bottom bar jumps between unrelated tabs.
    val reducedMotion = isReducedMotionEnabled()
    val fadeSpec = tween<Float>(Motion.DurationMedium, easing = Motion.EasingExpressive)

    NavHost(
        navController = navController,
        startDestination = AuthRoute.Splash,
        enterTransition = {
            if (reducedMotion) EnterTransition.None else fadeIn(fadeSpec) + scaleIn(fadeSpec, initialScale = 0.98f)
        },
        exitTransition = { if (reducedMotion) ExitTransition.None else fadeOut(fadeSpec) },
        popEnterTransition = {
            if (reducedMotion) EnterTransition.None else fadeIn(fadeSpec) + scaleIn(fadeSpec, initialScale = 1.02f)
        },
        popExitTransition = { if (reducedMotion) ExitTransition.None else fadeOut(fadeSpec) },
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
                onNavigateToSettings = { navController.navigate(AuthRoute.Settings) },
                onViewAllTransactions = { navController.navigate(AuthRoute.Transactions) },
            )
        }

        composable<AuthRoute.Transactions> {
            TransactionsScreen(onManageRecurring = { navController.navigate(AuthRoute.Recurring) })
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

        composable<AuthRoute.Accounts> {
            AccountsScreen()
        }

        composable<AuthRoute.Recurring> {
            RecurringScreen()
        }

        composable<AuthRoute.Settings> {
            val signOutUseCase = koinInject<SignOutUseCase>()
            val deleteAccountUseCase = koinInject<DeleteAccountUseCase>()
            val backfillMessages = rememberMessageBackfill()
            val signOutScope = rememberCoroutineScope()
            SettingsScreen(
                onSignOut = {
                    signOutScope.launch {
                        // Clear the real auth session so getAuthStatus() flips to
                        // Unauthenticated; otherwise the next launch would route straight
                        // back to the dashboard.
                        signOutUseCase()
                        navController.navigate(AuthRoute.Login) {
                            popUpTo(AuthRoute.Dashboard) { inclusive = true }
                        }
                    }
                },
                onDeleteAccount = {
                    // Runs the credential + local-data deletion; on success, route to Login the
                    // same way sign-out does. The failure is returned for Settings to show inline.
                    deleteAccountUseCase().onSuccess {
                        navController.navigate(AuthRoute.Login) {
                            popUpTo(AuthRoute.Dashboard) { inclusive = true }
                        }
                    }
                },
                onReplayOnboarding = {
                    navController.navigate(AuthRoute.Onboarding) {
                        popUpTo(AuthRoute.Dashboard) { inclusive = true }
                    }
                },
                onBackfillMessages = backfillMessages
            )
        }
    }
}

/**
 * A slim bar hosting the global [AccountSwitcher], shown above the tab content on every
 * form factor so switching the active wallet re-scopes the dashboard/transactions/reports.
 */
@Composable
private fun AccountScopeBar(navController: androidx.navigation.NavHostController) {
    val accountsViewModel: AccountsViewModel = koinViewModel()
    val accountsState by accountsViewModel.state.collectAsState()
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AccountSwitcher(
                state = accountsState,
                onSelect = { accountsViewModel.onIntent(AccountsIntent.SelectActive(it)) },
                onManage = { navController.navigate(AuthRoute.Accounts) },
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
    NavigationItem(Res.string.nav_budgets, Icons.Default.PieChart, AuthRoute.Budgets),
    NavigationItem(Res.string.nav_goals, Icons.Default.Flag, AuthRoute.Goals),
    NavigationItem(Res.string.nav_reports, Icons.Default.BarChart, AuthRoute.Reports),
    NavigationItem(Res.string.nav_settings, Icons.Default.Settings, AuthRoute.Settings)
)
