package com.example.autodocgt

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun Inicio(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToMaintenance: () -> Unit = {},
    onNavigateToDocuments: () -> Unit = {},
    onNavigateToReminders: () -> Unit = {},
    onNavigateToExpenses: () -> Unit = {},
    onNavigateToAddVehicle: () -> Unit = {},
    onNavigateToVehicleDetails: (Map<String, Any>) -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val context = LocalContext.current
    
    var userName by remember { mutableStateOf("Usuario") }
    var vehicles by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoadingVehicles by remember { mutableStateOf(true) }
    var hasUrgentReminder by remember { mutableStateOf(false) }
    val auth = Firebase.auth
    val db = Firebase.firestore
    
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var urgentDocId by remember { mutableStateOf("") }
    var urgentDocName by remember { mutableStateOf("") }
    var urgentDocDate by remember { mutableStateOf("") }
    var showUrgentModal by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
        }
    )

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("usuarios").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    userName = document.getString("nombre") ?: currentUser.displayName ?: "Usuario"
                }
                .addOnFailureListener {
                    userName = currentUser.displayName ?: "Usuario"
                }
                
            db.collection("vehiculos")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val list = mutableListOf<Map<String, Any>>()
                    for (doc in querySnapshot.documents) {
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        list.add(data)
                    }
                    list.sortBy { it["fechaCreacion"] as? Long ?: 0L }
                    vehicles = list
                    isLoadingVehicles = false
                }
                .addOnFailureListener { e ->
                    isLoadingVehicles = false
                    Toast.makeText(context, "Error al cargar vehículos: ${e.message}", Toast.LENGTH_LONG).show()
                }

            db.collection("usuarios").document(currentUser.uid).collection("documentos")
                .whereEqualTo("recordatorioActivo", true)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        var urgent = false
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        val today = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time

                        for (doc in snapshot.documents) {
                            val fecha = doc.getString("fecha_vencimiento") ?: continue
                            try {
                                val date = formatter.parse(fecha)
                                if (date != null) {
                                    val diff = date.time - today.time
                                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                                    if (days <= 7) {
                                        urgent = true
                                        if (days <= 3) {
                                            val dId = doc.id
                                            val isDismissed = sharedPrefs.getBoolean("dismissed_doc_$dId", false)
                                            if (!isDismissed && urgentDocId.isEmpty()) {
                                                urgentDocId = dId
                                                urgentDocName = doc.getString("nombre") ?: "Documento"
                                                urgentDocDate = fecha
                                                showUrgentModal = true
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) { }
                        }
                        hasUrgentReminder = urgent
                    }
                }
        } else {
            isLoadingVehicles = false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                backgroundColor = primaryDarkBlue,
                onSettingsClick = onNavigateToSettings,
                onBellClick = onNavigateToReminders,
                hasUrgentReminder = hasUrgentReminder
            )
        },
        bottomBar = {
            HomeBottomNavigationBar(
                backgroundColor = primaryDarkBlue,
                currentRoute = "home",
                onHomeClick = {},
                onMaintenanceClick = onNavigateToMaintenance,
                onDocumentsClick = onNavigateToDocuments,
                onRemindersClick = onNavigateToReminders,
                onExpensesClick = onNavigateToExpenses
            )
        }
    ) { innerPadding ->
        if (showUrgentModal && urgentDocId.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { showUrgentModal = false },
                title = { Text("¡Documento por vencer!", color = Color.Red, fontWeight = FontWeight.Bold) },
                text = { Text("Tu documento '${urgentDocName}' está próximo a vencer el día ${urgentDocDate}.\n\nPor favor renuévalo a tiempo para evitar inconvenientes.") },
                confirmButton = {
                    TextButton(onClick = { showUrgentModal = false }) {
                        Text("Cerrar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        sharedPrefs.edit().putBoolean("dismissed_doc_$urgentDocId", true).apply()
                        showUrgentModal = false
                        urgentDocId = ""
                    }) {
                        Text("No volver a mostrar")
                    }
                }
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGray)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Hola, $userName",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryDarkBlue
                )
                
                if (!isLoadingVehicles && vehicles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToAddVehicle,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar Vehículo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Nuevo Vehículo",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isLoadingVehicles) {
                    CircularProgressIndicator(color = primaryDarkBlue)
                } else if (vehicles.isEmpty()) {
                    Text(
                        text = "Aun no has agregado ningún vehículo",
                        color = Color.DarkGray,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = onNavigateToAddVehicle,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
                    ) {
                        Text("Agregar Vehiculo", color = Color.White)
                    }
                } else {
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { vehicles.size })
                    
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.weight(1f)
                        ) { page ->
                            val v = vehicles[page]
                            val marca = v["marca"] as? String ?: ""
                            val modelo = v["modelo"] as? String ?: ""
                            val placa = v["placa"] as? String ?: ""
                            val kilometraje = v["kilometraje"] as? String ?: ""
                            val photoBase64 = v["foto"] as? String ?: ""
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 4.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "Auto No.${page + 1}",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryDarkBlue
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1.2f)) {
                                                Text(text = "$marca $modelo", fontWeight = FontWeight.Bold, color = primaryDarkBlue, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Placa: $placa", color = primaryDarkBlue, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Kilometraje: $kilometraje", color = primaryDarkBlue, fontWeight = FontWeight.Medium)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = { onNavigateToVehicleDetails(v) },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                                    modifier = Modifier.height(36.dp)
                                                ) {
                                                    Text("Ver Detalles", color = Color.White, fontSize = 12.sp)
                                                }
                                            }
                                            Image(
                                                painter = painterResource(id = R.drawable.vehiculo),
                                                contentDescription = "Car",
                                                modifier = Modifier.weight(0.8f).height(90.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        Row(
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            repeat(vehicles.size) { iteration ->
                                                val color = if (pagerState.currentPage == iteration) primaryDarkBlue else Color.LightGray
                                                Box(
                                                    modifier = Modifier
                                                        .padding(2.dp)
                                                        .clip(androidx.compose.foundation.shape.CircleShape)
                                                        .background(color)
                                                        .size(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Fotografia del vehículo",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryDarkBlue
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        if (photoBase64.isNotEmpty()) {
                                            val bitmapImage = remember(photoBase64) {
                                                try {
                                                    val imageBytes = android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
                                                    android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                                                } catch (e: Exception) { null }
                                            }
                                            if (bitmapImage != null) {
                                                Image(
                                                    bitmap = bitmapImage.asImageBitmap(),
                                                    contentDescription = "Foto Vehículo",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .clip(RoundedCornerShape(16.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .clip(RoundedCornerShape(16.dp))
                                                        .background(Color.LightGray),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text("Foto no disponible", color = Color.DarkGray)
                                                }
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(200.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(Color.LightGray),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("Sin foto", color = Color.DarkGray)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun HomeTopBar(
    backgroundColor: Color, 
    onSettingsClick: () -> Unit,
    onBellClick: () -> Unit = {},
    hasUrgentReminder: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    val showBellIcon = sharedPrefs.getBoolean("show_bell_icon", true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (showBellIcon) {
            Box(modifier = Modifier.clickable { onBellClick() }) {
                Icon(
                    painter = painterResource(id = R.drawable.img_bell_inicio),
                    contentDescription = "Notificaciones",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                if (hasUrgentReminder) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color.Red, shape = androidx.compose.foundation.shape.CircleShape)
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.size(28.dp))
        }
        Text(
            text = "AUTO DOC GT",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Icon(
            painter = painterResource(id = R.drawable.img_settings_inicio),
            contentDescription = "Configuración",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { onSettingsClick() }
        )
    }
}

@Composable
fun HomeBottomNavigationBar(
    backgroundColor: Color,
    currentRoute: String = "home",
    onHomeClick: () -> Unit = {},
    onMaintenanceClick: () -> Unit = {},
    onDocumentsClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(iconRes = R.drawable.img_home_inicio, label = "Inicio", isSelected = currentRoute == "home", onClick = onHomeClick)
        BottomNavItem(iconRes = R.drawable.img_carro_documentos, label = "Mantenimiento", isSelected = currentRoute == "maintenance", onClick = onMaintenanceClick)
        BottomNavItem(iconRes = R.drawable.img_file_plus_inicio, label = "Documentos", isSelected = currentRoute == "documents", onClick = onDocumentsClick)
        BottomNavItem(iconRes = R.drawable.img_calendar_inicio, label = "Recordatorios", isSelected = currentRoute == "reminders", onClick = onRemindersClick)
        BottomNavItem(iconRes = R.drawable.img_billetera_inicio, label = "Gastos", isSelected = currentRoute == "expenses", onClick = onExpensesClick)
    }
}

@Composable
fun BottomNavItem(iconRes: Int, label: String, isSelected: Boolean = false, onClick: () -> Unit = {}) {
    val alpha = if (isSelected) 1f else 0.5f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable { onClick() }.padding(4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color.White.copy(alpha = alpha),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White.copy(alpha = alpha),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
