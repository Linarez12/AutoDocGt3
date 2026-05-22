package com.example.autodocgt

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.Image
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun Mantenimiento(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onDocumentsClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {},
    onNearbyWorkshopsClick: () -> Unit = {},
    onNavigateToAddMaintenance: () -> Unit = {},
    onNavigateToDetails: (Map<String, Any>, String) -> Unit = { _, _ -> }
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val context = LocalContext.current

    val db = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }
    var mantenimientos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var vehiculos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
        }
    )

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("vehiculos").whereEqualTo("userId", currentUser.uid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        vehiculos = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data?.toMutableMap()
                            if (data != null) {
                                data["id"] = doc.id
                                data
                            } else null
                        }
                    }
                }
            db.collection("usuarios").document(currentUser.uid).collection("gastos")
                .whereEqualTo("tipoCategoria", "Mantenimiento")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        mantenimientos = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data?.toMutableMap()
                            if (data != null) {
                                data["id"] = doc.id
                                data
                            } else null
                        }
                    }
                }
        }
        
        val permissionsToRequest = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsNeeded = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsNeeded.isNotEmpty()) {
            permissionsLauncher.launch(permissionsNeeded)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MaintenanceTopBar(
                backgroundColor = primaryDarkBlue,
                onBackClick = onBack
            )
        },
        bottomBar = {
            HomeBottomNavigationBar(
                backgroundColor = primaryDarkBlue,
                currentRoute = "maintenance",
                onHomeClick = onHomeClick,
                onMaintenanceClick = {},
                onDocumentsClick = onDocumentsClick,
                onRemindersClick = onRemindersClick,
                onExpensesClick = onExpensesClick
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGray)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (mantenimientos.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                Text("No hay mantenimientos registrados", color = Color.Gray)
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    vehiculos.forEachIndexed { index, v ->
                        val carMaint = mantenimientos.filter { it["vehiculoId"] == v["id"] }
                        if (carMaint.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Auto no.${index + 1} ",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = primaryDarkBlue,
                                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                )
                            }
                            items(carMaint) { m ->
                                val vStr = "Auto no.${index + 1} "
                                MaintenanceCard(m, vStr, onDetailsClick = { onNavigateToDetails(m, vStr) })
                            }
                        }
                    }

                    val otrosMaint = mantenimientos.filter { 
                        it["vehiculoId"] == null || it["vehiculoId"] == "" || vehiculos.none { v -> v["id"] == it["vehiculoId"] }
                    }
                    
                    if (otrosMaint.isNotEmpty()) {
                        item {
                            Text(
                                text = "Otros Mantenimientos",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = primaryDarkBlue,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        items(otrosMaint) { m ->
                            MaintenanceCard(m, "Vehículo desconocido", onDetailsClick = { onNavigateToDetails(m, "Vehículo desconocido") })
                        }
                    }
                }
            }
            
            Button(
                onClick = { onNearbyWorkshopsClick() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("Buscar talleres cercanos", color = Color.White, fontSize = 16.sp)
            }

            Button(
                onClick = { onNavigateToAddMaintenance() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("+ Registro de mantenimiento", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun MaintenanceTopBar(backgroundColor: Color, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { onBackClick() }
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "Mantenimiento",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun MaintenanceCard(mantenimiento: Map<String, Any>, vehiculoStr: String, onDetailsClick: () -> Unit) {
    val tipoServicio = mantenimiento["tipoServicio"] as? String ?: "Servicio"
    val kmActual = mantenimiento["kmActual"] as? String ?: "0"
    val kmProximo = mantenimiento["kmProximo"] as? String ?: ""

    val iconRes = when (tipoServicio) {
        "Cambio de aceite" -> R.drawable.cambiodeaceite
        "Bujias" -> R.drawable.bujias
        "Frenos" -> R.drawable.frenos
        "Bateria" -> R.drawable.bateria
        "Revisión general" -> R.drawable.revisiongeneral
        else -> R.drawable.img_settings_inicio
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = "Mantenimiento Icon",
                colorFilter = if (iconRes == R.drawable.img_settings_inicio) androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF16528E)) else null,
                modifier = Modifier.size(60.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = tipoServicio, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Ultimo: $kmActual Km", color = Color.DarkGray, fontSize = 14.sp)
                if (kmProximo.isNotBlank() && kmProximo != "0") {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Proximo: $kmProximo Km", color = Color.DarkGray, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDetailsClick,
                        modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16528E)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("Ver Detalles", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
