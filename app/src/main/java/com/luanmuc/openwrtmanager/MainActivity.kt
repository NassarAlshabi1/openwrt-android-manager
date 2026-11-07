package com.luanmuc.openwrtmanager
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

import android.os.Bundle
import com.luanmuc.openwrtmanager.ui.theme.ThemeManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.luanmuc.openwrtmanager.ui.addrouter.AddRouterScreen
import com.luanmuc.openwrtmanager.ui.advanced.AdvancedScreen
import com.luanmuc.openwrtmanager.ui.backup.BackupScreen
import com.luanmuc.openwrtmanager.ui.devicemanager.DeviceManagerScreen
import com.luanmuc.openwrtmanager.ui.diagnostic.DiagnosticScreen
import com.luanmuc.openwrtmanager.ui.diagnostic_enhanced.DiagnosticEnhancedScreen
import com.luanmuc.openwrtmanager.ui.ai_diagnostic.AiDiagnosticScreen
import com.luanmuc.openwrtmanager.ui.terminal.TerminalScreen
import com.luanmuc.openwrtmanager.ui.widget_config.WidgetConfigScreen
import com.luanmuc.openwrtmanager.ui.wifi_enhanced.WifiEnhancedScreen
import com.luanmuc.openwrtmanager.ui.components.MiBottomNavigation
import com.luanmuc.openwrtmanager.ui.components.MiNavItem
import com.luanmuc.openwrtmanager.ui.devices.DevicesScreen
import com.luanmuc.openwrtmanager.ui.devices.OnlineDevicesScreen
import com.luanmuc.openwrtmanager.ui.ddns.DdnsScreen
import com.luanmuc.openwrtmanager.ui.firewall.FirewallScreen
import com.luanmuc.openwrtmanager.ui.home.HomeScreen
import com.luanmuc.openwrtmanager.ui.network.NetworkScreen
import com.luanmuc.openwrtmanager.ui.notification.NotificationScreen
import com.luanmuc.openwrtmanager.ui.plugin_detail.PluginDetailScreen
import com.luanmuc.openwrtmanager.ui.plugins.PluginsScreen
import com.luanmuc.openwrtmanager.ui.plugins.RepoScreen
import com.luanmuc.openwrtmanager.ui.firmware.FirmwareScreen
import com.luanmuc.openwrtmanager.ui.firmware.FirmwareViewModel
import com.luanmuc.openwrtmanager.ui.profile.ProfileScreen
import com.luanmuc.openwrtmanager.ui.plugin_dependency.PluginDependencyScreen
import com.luanmuc.openwrtmanager.ui.system.SystemScreen
import com.luanmuc.openwrtmanager.ui.storage.StorageScreen
import com.luanmuc.openwrtmanager.ui.remote.RemoteManageScreen
import com.luanmuc.openwrtmanager.ui.traffic.TrafficScreen
import com.luanmuc.openwrtmanager.ui.theme.OpenWrtManagerTheme
import com.luanmuc.openwrtmanager.ui.wifi.WifiScreen
import com.luanmuc.openwrtmanager.ui.webview.WebViewPluginScreen

/**
 * 底部导航项
 */
sealed class Screen(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector) {
    data object Home : Screen("home", R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home)
    data object Devices : Screen("devices", R.string.nav_devices, Icons.Outlined.Devices, Icons.Filled.Devices)
    data object Plugins : Screen("plugins", R.string.nav_plugins, Icons.Outlined.Extension, Icons.Filled.Extension)
    data object Profile : Screen("profile", R.string.nav_profile, Icons.Outlined.Person, Icons.Filled.Person)
    
    data object AddRouter : Screen("add_router", R.string.add_router_title, Icons.Filled.Home, Icons.Filled.Home)
    data object OnlineDevices : Screen("online_devices", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object System : Screen("system", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Network : Screen("network", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Wifi : Screen("wifi", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Firewall : Screen("firewall", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Ddns : Screen("ddns", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Advanced : Screen("advanced", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Storage : Screen("storage", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Diagnostic : Screen("diagnostic", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Repos : Screen("repos", 0, Icons.Filled.Extension, Icons.Filled.Extension)
    data object WebViewPlugin : Screen("webview_plugin", 0, Icons.Filled.Extension, Icons.Filled.Extension)
    data object Firmware : Screen("firmware", 0, Icons.Filled.Extension, Icons.Filled.Extension)
    data object Notification : Screen("notification", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Traffic : Screen("traffic", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object DeviceManager : Screen("device_manager", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object DiagnosticEnhanced : Screen("diagnostic_enhanced", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Backup : Screen("backup", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object WifiEnhanced : Screen("wifi_enhanced", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object Terminal : Screen("terminal", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object PluginDetail : Screen("plugin_detail", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object WidgetConfig : Screen("widget_config", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object RemoteManage : Screen("remote_manage", 0, Icons.Filled.Devices, Icons.Filled.Devices)
    data object PluginDependency : Screen("plugin_dependency", 0, Icons.Filled.Extension, Icons.Filled.Extension)
    data object AiDiagnostic : Screen("ai_diagnostic", 0, Icons.Filled.AutoAwesome, Icons.Filled.AutoAwesome)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Plugins,
    Screen.Profile
)

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        // 应用用户选择的语言（英语/阿拉伯语，含RTL）
        Loc.init(newBase)
        super.attachBaseContext(Loc.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OpenWrtManagerTheme {
                MainScreen()
            }
        }
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // 系统主题变化时更新
        ThemeManager.getInstance(this).updateSystemTheme()
    }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }
            
            if (showBottomBar) {
                val selectedIndex = bottomNavItems.indexOfFirst { 
                    currentDestination?.hierarchy?.any { dest -> dest.route == it.route } == true 
                }.coerceAtLeast(0)
                
                MiBottomNavigation(
                    selectedIndex = selectedIndex,
                    onItemSelected = { index ->
                        val screen = bottomNavItems.getOrNull(index) ?: return@MiBottomNavigation
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    items = bottomNavItems.map { 
                        MiNavItem(
                            label = stringResource(it.label),
                            icon = it.icon,
                            selectedIcon = it.selectedIcon
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onAddRouter = { navController.navigate(Screen.AddRouter.route) },
                    onNavigateToDevices = { navController.navigate(Screen.OnlineDevices.route) },
                    onNavigateToPlugins = { navController.navigate(Screen.Plugins.route) },
                    onNavigateToWifi = { navController.navigate(Screen.Wifi.route) },
                    onNavigateToNetwork = { navController.navigate(Screen.Network.route) },
                    onNavigateToSystem = { navController.navigate(Screen.System.route) },
                    onNavigateToFirewall = { navController.navigate(Screen.Firewall.route) },
                    onNavigateToDdns = { navController.navigate(Screen.Ddns.route) },
                    onNavigateToAdvanced = { navController.navigate(Screen.Advanced.route) }
                )
            }
            composable(Screen.Devices.route) {
                DevicesScreen(
                    onAddRouter = { navController.navigate(Screen.AddRouter.route) }
                )
            }
            composable(Screen.Plugins.route) {
                PluginsScreen(
                    onPluginClick = { pkg ->
                        if (pkg.installed) {
                            val pluginName = pkg.name.removePrefix("luci-app-")
                            val url = "/cgi-bin/luci/admin/" + pluginName.replace("-", "/")
                            navController.navigate(Screen.WebViewPlugin.route + "?url=$url&title=${pkg.name}")
                        }
                    },
                    onNavigateToRepos = { navController.navigate(Screen.Repos.route) }
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToSystem = { navController.navigate(Screen.System.route) },
                    onNavigateToNetwork = { navController.navigate(Screen.Network.route) },
                    onNavigateToWifi = { navController.navigate(Screen.Wifi.route) },
                    onNavigateToFirewall = { navController.navigate(Screen.Firewall.route) },
                    onNavigateToDdns = { navController.navigate(Screen.Ddns.route) },
                    onNavigateToAdvanced = { navController.navigate(Screen.Advanced.route) },
                    onNavigateToNotification = { navController.navigate(Screen.Notification.route) },
                    onNavigateToTraffic = { navController.navigate(Screen.Traffic.route) },
                    onNavigateToDeviceManager = { navController.navigate(Screen.DeviceManager.route) },
                    onNavigateToDiagnosticEnhanced = { navController.navigate(Screen.DiagnosticEnhanced.route) },
                    onNavigateToBackup = { navController.navigate(Screen.Backup.route) },
                    onNavigateToWifiEnhanced = { navController.navigate(Screen.WifiEnhanced.route) },
                    onNavigateToTerminal = { navController.navigate(Screen.Terminal.route) },
                    onNavigateToWidgetConfig = { navController.navigate(Screen.WidgetConfig.route) },
                    onNavigateToPlugins = { navController.navigate(Screen.Plugins.route) },
                    onNavigateToRemoteManage = { navController.navigate(Screen.RemoteManage.route) },
                    onNavigateToPluginDependency = { navController.navigate(Screen.PluginDependency.route) },
                    onNavigateToAiDiagnostic = { navController.navigate(Screen.AiDiagnostic.route) }
                )
            }
            composable(Screen.AddRouter.route) {
                AddRouterScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.popBackStack()
                        navController.navigate(Screen.Home.route)
                    }
                )
            }
            composable(Screen.OnlineDevices.route) {
                OnlineDevicesScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.System.route) {
                SystemScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToFirmware = { navController.navigate(Screen.Firmware.route) }
                )
            }
            composable(Screen.Network.route) {
                NetworkScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Wifi.route) {
                WifiScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Firewall.route) {
                FirewallScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Ddns.route) {
                DdnsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Advanced.route) {
                AdvancedScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToSystem = { navController.navigate(Screen.System.route) },
                    onNavigateToStorage = { navController.navigate(Screen.Storage.route) },
                    onNavigateToDiagnostic = { navController.navigate(Screen.Diagnostic.route) },
                    onOpenWebView = { url, title ->
                        navController.navigate(Screen.WebViewPlugin.route + "?url=$url&title=$title")
                    }
                )
            }
            composable(Screen.Storage.route) {
                StorageScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RemoteManage.route) {
                RemoteManageScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PluginDependency.route) {
                PluginDependencyScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AiDiagnostic.route) {
                AiDiagnosticScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Diagnostic.route) {
                DiagnosticScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Repos.route) {
                RepoScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Firmware.route) {
                FirmwareScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Notification.route) {
                NotificationScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Traffic.route) {
                TrafficScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DeviceManager.route) {
                DeviceManagerScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DiagnosticEnhanced.route) {
                DiagnosticEnhancedScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Backup.route) {
                BackupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WifiEnhanced.route) {
                WifiEnhancedScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Terminal.route) {
                TerminalScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.PluginDetail.route + "/{packageName}") { backStackEntry ->
                val packageName = backStackEntry.arguments?.getString("packageName") ?: ""
                PluginDetailScreen(
                    packageName = packageName,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WidgetConfig.route) {
                WidgetConfigScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.WebViewPlugin.route + "?url={url}&title={title}") { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url") ?: ""
                val title = backStackEntry.arguments?.getString("title") ?: Loc.s(R.string.s0001)
                WebViewPluginScreen(
                    url = url,
                    title = title,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
