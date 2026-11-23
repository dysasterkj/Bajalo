package com.kjota.bajalo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.kjota.bajalo.MainViewModel
import com.kjota.bajalo.R
import com.kjota.bajalo.Tabs

@Composable
fun BajaloRoot(vm: MainViewModel) {
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.messages.collect { snack.showSnackbar(it) }
    }

    val login = vm.loginSite
    if (login != null) {
        LoginScreen(site = login, onDone = { vm.closeLogin() })
        return
    }

    AuroraBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snack) },
            bottomBar = { BottomBar(vm) },
        ) { inner ->
            Box(
                Modifier
                    .padding(inner)
                    .consumeWindowInsets(inner)
                    .imePadding()
            ) {
                when (vm.tab) {
                    Tabs.DOWNLOAD -> HomeScreen(vm)
                    Tabs.STATUS -> StatusScreen(vm)
                    Tabs.LIBRARY -> LibraryScreen(vm)
                    else -> SettingsScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun BottomBar(vm: MainViewModel) {
    val items = listOf(
        Triple(Tabs.DOWNLOAD, "Descargar", R.drawable.ic_download),
        Triple(Tabs.STATUS, "Estados", R.drawable.ic_status),
        Triple(Tabs.LIBRARY, "Descargas", R.drawable.ic_library),
        Triple(Tabs.SETTINGS, "Ajustes", R.drawable.ic_settings),
    )
    NavigationBar(containerColor = Color(0x1AFFFFFF)) {
        items.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = vm.tab == tab,
                onClick = { vm.tab = tab },
                icon = { Icon(painterResource(icon), contentDescription = null) },
                label = { Text(label) },
            )
        }
    }
}
