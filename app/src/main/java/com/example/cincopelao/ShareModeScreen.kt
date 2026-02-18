package com.example.cincopelao

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ShareModeScreen(
    onTransmitClick: () -> Unit,
    onReceiveClick: () -> Unit
) {
    // Fondo principal
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        //contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            //verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {

            Spacer(modifier = Modifier.height(10.dp))

            // Logo grande
            Icon(
                painter = painterResource(id = R.drawable.sharemode),
                contentDescription = "Logo de transferencia",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(300.dp).clipToBounds()//.border(1.dp, Color.White)
            )

            //Spacer(modifier = Modifier.height(10.dp))

            // Texto explicativo
            Text(
                text = "Conecta dos dispositivos para compartir elementos ya configurados entre tus compañeros cercanos, sin cuentas ni servidores.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Botones
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { onTransmitClick() },
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text("Transmitir")
                }

                Button(
                    onClick = { onReceiveClick() },
                    modifier = Modifier
                        .weight(1f)
                ) {
                    Text("Recibir")
                }
            }
        }
    }
}
