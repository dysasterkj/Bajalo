package com.kjota.bajalo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.kjota.bajalo.ui.BajaloRoot
import com.kjota.bajalo.ui.BajaloTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        // Solo la primera vez: si se rota la pantalla no se vuelve a procesar el enlace compartido.
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            BajaloTheme {
                BajaloRoot(vm)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    /** Recibe enlaces desde "Compartir" de otras apps (Instagram, Facebook, X, YouTube…). */
    private fun handleIntent(i: Intent?) {
        if (i?.action == Intent.ACTION_SEND && i.type?.startsWith("text/") == true) {
            val text = i.getStringExtra(Intent.EXTRA_TEXT) ?: return
            vm.onSharedText(text)
        }
    }
}
