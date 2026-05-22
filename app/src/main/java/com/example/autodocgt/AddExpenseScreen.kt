package com.example.autodocgt

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    initialTab: Int = 0,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val textFieldBackground = Color.White
    val context = LocalContext.current

    val db = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }

    var selectedTabIndex by remember { mutableStateOf(initialTab) }
    val tabs = listOf("Combustible", "Mantenimiento", "Otros")

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = textFieldBackground,
        unfocusedContainerColor = textFieldBackground,
        focusedBorderColor = Color.Gray,
        unfocusedBorderColor = Color.Gray,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black
    )

    // Vehículos
    var selectedVehicle by remember { mutableStateOf<Map<String, Any>?>(null) }
    var vehicles by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var expandedVehicle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("vehiculos").whereEqualTo("userId", currentUser.uid).get()
                .addOnSuccessListener { querySnapshot ->
                    vehicles = querySnapshot.documents.mapNotNull { doc ->
                        val data = doc.data?.toMutableMap()
                        if (data != null) {
                            data["id"] = doc.id
                            data
                        } else null
                    }
                }
        }
    }

    // Compartidos
    var fecha by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("") }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Campos Combustible
    var montoCombustible by remember { mutableStateOf("") }
    var gasolinera by remember { mutableStateOf("") }
    var tipoCombustible by remember { mutableStateOf("") }
    var expandedGasolinera by remember { mutableStateOf(false) }
    var expandedTipoComb by remember { mutableStateOf(false) }
    val gasolineraOptions = listOf("Puma", "Shell", "Texaco", "Uno", "Don Arturo", "Otra")
    val tipoCombOptions = listOf("Regular", "Super", "V-Power", "Diesel")

    // Campos Mantenimiento
    var tipoServicio by remember { mutableStateOf("") }
    var expandedServicio by remember { mutableStateOf(false) }
    val servicioOptions = listOf("Cambio de aceite", "Bujias","Frenos","Bateria","Revisión general", "Otro")
    var kmActual by remember { mutableStateOf("") }
    var kmProximo by remember { mutableStateOf("") }
    var taller by remember { mutableStateOf("") }
    var costoTotal by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    // Campos Otros
    var montoOtros by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
            photoBase64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)
            Toast.makeText(context, "Factura agregada", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                val baos = java.io.ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
                photoBase64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT)
                Toast.makeText(context, "Factura seleccionada", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Agregar Factura") },
            text = { Text("Elige una opción para agregar la factura.") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoDialog = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Cámara") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoDialog = false
                    galleryLauncher.launch("image/*")
                }) { Text("Galería") }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        fecha = formatter.format(Date(millis))
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val title = if (selectedTabIndex == 1) "Agregar Mantenimiento" else "Agregar Gastos"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(primaryDarkBlue)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onBack() }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGray)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = primaryDarkBlue,
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .background(
                                if (isSelected) primaryDarkBlue else Color(0xFF5A8DBE),
                                RoundedCornerShape(8.dp)
                            )
                            .height(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(text = title, color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Escanear Factura
            Button(
                onClick = { showPhotoDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text(
                    if (photoBase64.isEmpty()) "+ Agregar Factura" else "Factura Agregada",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tabs[selectedTabIndex],
                        color = primaryDarkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // SELECCIÓN DE VEHÍCULO (COMÚN)
                    Text(text = "Vehículo:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedVehicle?.let { v -> "Auto no.${vehicles.indexOf(v) + 1}" } ?: "",
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            placeholder = { Text("Seleccione un vehículo") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown") },
                            colors = textFieldColors,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.matchParentSize().background(Color.Transparent).clickable { expandedVehicle = true })
                        DropdownMenu(expanded = expandedVehicle, onDismissRequest = { expandedVehicle = false }) {
                            vehicles.forEachIndexed { index, v ->
                                DropdownMenuItem(
                                    text = { Text("Auto no.${index + 1}") },
                                    onClick = { selectedVehicle = v; expandedVehicle = false }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    when (selectedTabIndex) {
                        0 -> { // Combustible
                            Text(text = "Fecha:", color = Color.Gray, fontSize = 14.sp)
                            Box {
                                OutlinedTextField(
                                    value = fecha, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Monto:", color = Color.Gray, fontSize = 14.sp)
                                    OutlinedTextField(
                                        value = montoCombustible, onValueChange = { montoCombustible = it.filter { c -> c.isDigit() || c == '.' } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = textFieldColors,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "Gasolinera:", color = Color.Gray, fontSize = 14.sp)
                                    Box {
                                        OutlinedTextField(
                                            value = gasolinera, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                            colors = textFieldColors,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Spacer(modifier = Modifier.matchParentSize().clickable { expandedGasolinera = true })
                                        DropdownMenu(expanded = expandedGasolinera, onDismissRequest = { expandedGasolinera = false }) {
                                            gasolineraOptions.forEach { opt ->
                                                DropdownMenuItem(text = { Text(opt) }, onClick = { gasolinera = opt; expandedGasolinera = false })
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Tipo de combustible:", color = Color.Gray, fontSize = 14.sp)
                            Box {
                                OutlinedTextField(
                                    value = tipoCombustible, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.matchParentSize().clickable { expandedTipoComb = true })
                                DropdownMenu(expanded = expandedTipoComb, onDismissRequest = { expandedTipoComb = false }) {
                                    tipoCombOptions.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { tipoCombustible = opt; expandedTipoComb = false })
                                    }
                                }
                            }
                        }
                        1 -> { // Mantenimiento
                            Text(text = "Tipo de servicio:", color = Color.Gray, fontSize = 14.sp)
                            Box {
                                OutlinedTextField(
                                    value = tipoServicio, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.matchParentSize().clickable { expandedServicio = true })
                                DropdownMenu(expanded = expandedServicio, onDismissRequest = { expandedServicio = false }) {
                                    servicioOptions.forEach { opt ->
                                        DropdownMenuItem(text = { Text(opt) }, onClick = { tipoServicio = opt; expandedServicio = false })
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Fecha:", color = Color.Gray, fontSize = 14.sp)
                            Box {
                                OutlinedTextField(
                                    value = fecha, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "KM actual:", color = Color.Gray, fontSize = 14.sp)
                                    OutlinedTextField(
                                        value = kmActual, onValueChange = { kmActual = it.filter { c -> c.isDigit() } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = textFieldColors,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = "KM proximo:", color = Color.Gray, fontSize = 14.sp)
                                    OutlinedTextField(
                                        value = kmProximo, onValueChange = { kmProximo = it.filter { c -> c.isDigit() } },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = textFieldColors,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Taller:", color = Color.Gray, fontSize = 14.sp)
                            OutlinedTextField(
                                value = taller, onValueChange = { taller = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Costo total:", color = Color.Gray, fontSize = 14.sp)
                            OutlinedTextField(
                                value = costoTotal, onValueChange = { costoTotal = it.filter { c -> c.isDigit() || c == '.' } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Notas:", color = Color.Gray, fontSize = 14.sp)
                            OutlinedTextField(
                                value = notas, onValueChange = { notas = it },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        2 -> { // Otros
                            Text(text = "Fecha:", color = Color.Gray, fontSize = 14.sp)
                            Box {
                                OutlinedTextField(
                                    value = fecha, onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
                                    colors = textFieldColors,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Spacer(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Monto:", color = Color.Gray, fontSize = 14.sp)
                            OutlinedTextField(
                                value = montoOtros, onValueChange = { montoOtros = it.filter { c -> c.isDigit() || c == '.' } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Descripcion:", color = Color.Gray, fontSize = 14.sp)
                            OutlinedTextField(
                                value = descripcion, onValueChange = { descripcion = it },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                colors = textFieldColors,
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val currentUser = auth.currentUser
                    if (currentUser != null && selectedVehicle != null && fecha.isNotEmpty()) {
                        val gastoData = mutableMapOf<String, Any>(
                            "vehiculoId" to (selectedVehicle?.get("id") as? String ?: ""),
                            "fecha" to fecha,
                            "tipoCategoria" to tabs[selectedTabIndex],
                            "facturaBase64" to photoBase64
                        )

                        var isValid = true
                        when (selectedTabIndex) {
                            0 -> {
                                if (montoCombustible.isEmpty()) isValid = false
                                gastoData["monto"] = montoCombustible.toDoubleOrNull() ?: 0.0
                                gastoData["gasolinera"] = gasolinera
                                gastoData["tipoCombustible"] = tipoCombustible
                            }
                            1 -> {
                                if (tipoServicio.isEmpty() || costoTotal.isEmpty()) isValid = false
                                gastoData["tipoServicio"] = tipoServicio
                                gastoData["monto"] = costoTotal.toDoubleOrNull() ?: 0.0
                                gastoData["kmActual"] = kmActual
                                gastoData["kmProximo"] = kmProximo
                                gastoData["taller"] = taller
                                gastoData["notas"] = notas
                            }
                            2 -> {
                                if (montoOtros.isEmpty() || descripcion.isEmpty()) isValid = false
                                gastoData["monto"] = montoOtros.toDoubleOrNull() ?: 0.0
                                gastoData["descripcion"] = descripcion
                            }
                        }

                        if (isValid) {
                            db.collection("usuarios").document(currentUser.uid).collection("gastos")
                                .add(gastoData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Guardado exitosamente", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                        } else {
                            Toast.makeText(context, "Por favor llena los campos requeridos", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Selecciona vehículo y fecha", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp)
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                val btnText = if (selectedTabIndex == 1) "+ Guardar mantenimiento" else "+ Guardar gasto"
                Text(btnText, color = Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
