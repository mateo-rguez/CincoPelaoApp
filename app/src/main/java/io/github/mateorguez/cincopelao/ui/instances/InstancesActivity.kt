package io.github.mateorguez.cincopelao.ui.instances

import io.github.mateorguez.cincopelao.ui.theme.appColors
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import io.github.mateorguez.cincopelao.ui.theme.CincoPelaoAppTheme

/** Gestión de instancias: crear, renombrar, borrar y elegir la activa. */
@AndroidEntryPoint
class InstancesActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CincoPelaoAppTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Instancias") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.appColors.bar,
                                titleContentColor = MaterialTheme.appColors.onBar,
                                navigationIconContentColor = MaterialTheme.appColors.onBar,
                                actionIconContentColor = MaterialTheme.appColors.onBar
                            ),
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                                }
                            },
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        // Al elegir una instancia basta con cerrar: las pantallas cambian de datos solas
                        InstancesScreen(onInstanceSelected = { finish() })
                    }
                }
            }
        }
    }
}
