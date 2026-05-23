package com.example.autodocgt

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
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
fun AgregarDocumento(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val textFieldBackground = Color.White
    val context = LocalContext.current

    var documentType by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var documentName by remember { mutableStateOf("") }

    var showDialog by remember { mutableStateOf(false) }
    var selectedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    val db = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }

    val options = listOf("Tarjeta de circulación", "Calcomanía", "Seguro", "Licencia de conducir", "Otro")
    var expanded by remember { mutableStateOf(false) }
    
    var selectedVehicle by remember { mutableStateOf<Map<String, Any>?>(null) }
    var vehicles by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var expandedVehicle by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("vehiculos").whereEqualTo("userId", currentUser.uid).get()
                .addOnSuccessListener { querySnapshot ->
                    val list = mutableListOf<Map<String, Any>>()
                    for (doc in querySnapshot.documents) {
                        val data = doc.data?.toMutableMap() ?: mutableMapOf()
                        data["id"] = doc.id
                        list.add(data)
                    }
                    vehicles = list
                }
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                selectedBitmap = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: android.graphics.Bitmap? ->
        bitmap?.let {
            selectedBitmap = it
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Seleccionar imagen") },
            text = { Text("¿Desde dónde quieres cargar el documento?") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Cámara") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
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
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis != null) {
                        val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }
                        expiryDate = format.format(Date(selectedDateMillis))
                    }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AddDocumentTopBar(
                backgroundColor = primaryDarkBlue,
                onBackClick = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGray)
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp))
                    .background(primaryDarkBlue, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Documento escaneado",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = "+ Escanear documento",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Datos del documento",
                        color = primaryDarkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Text(text = "Tipo de documento:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = documentType,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            readOnly = true,
                            placeholder = { Text("Seleccione un tipo", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = textFieldBackground,
                                unfocusedContainerColor = textFieldBackground,
                                focusedBorderColor = Color.Gray,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { expanded = true }
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            options.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        documentType = selectionOption
                                        if (selectionOption == "Licencia de conducir") {
                                            selectedVehicle = null
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (documentType != "Licencia de conducir") {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(text = "Vehículo:", color = Color.Gray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedVehicle?.let { v -> "Auto no.${vehicles.indexOf(v) + 1}" } ?: "",
                                onValueChange = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                readOnly = true,
                                placeholder = { Text("Seleccione un vehículo", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = textFieldBackground,
                                    unfocusedContainerColor = textFieldBackground,
                                    focusedBorderColor = Color.Gray,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Transparent)
                                    .clickable { expandedVehicle = true }
                            )
                            DropdownMenu(
                                expanded = expandedVehicle,
                                onDismissRequest = { expandedVehicle = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                vehicles.forEachIndexed { index, v ->
                                    DropdownMenuItem(
                                        text = { Text("Auto no.${index + 1}") },
                                        onClick = {
                                            selectedVehicle = v
                                            expandedVehicle = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Fecha de vencimiento:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = expiryDate,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            readOnly = true,
                            placeholder = { Text("DD/MM/AAAA", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = textFieldBackground,
                                unfocusedContainerColor = textFieldBackground,
                                focusedBorderColor = Color.Gray,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Transparent)
                                .clickable { showDatePicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Nombre:", color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = documentName,
                        onValueChange = { documentName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = { Text("Ej. Mi tarjeta", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = textFieldBackground,
                            unfocusedContainerColor = textFieldBackground,
                            focusedBorderColor = Color.Gray,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    if (documentType.isNotEmpty() && documentName.isNotEmpty() && expiryDate.isNotEmpty() && (documentType == "Licencia de conducir" || selectedVehicle != null)) {
                        val currentUser = auth.currentUser
                        if (currentUser != null) {
                            var photoBase64 = ""
                            if (selectedBitmap != null) {
                                val baos = java.io.ByteArrayOutputStream()
                                selectedBitmap!!.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, baos)
                                val byteArray = baos.toByteArray()
                                photoBase64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
                            }
                            
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val currentDate = sdf.format(Calendar.getInstance().time)

                            val docData = hashMapOf(
                                "tipo" to documentType,
                                "fecha_vencimiento" to expiryDate,
                                "nombre" to documentName,
                                "foto" to photoBase64,
                                "vehiculoId" to if (documentType == "Licencia de conducir") "" else (selectedVehicle?.get("id") as? String ?: ""),
                                "fecha_agregado" to currentDate
                            )
                            db.collection("usuarios").document(currentUser.uid).collection("documentos")
                                .add(docData)
                                .addOnSuccessListener {
                                    Toast.makeText(context, "Documento guardado", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .addOnFailureListener {
                                    Toast.makeText(context, "Error al guardar", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "No hay sesión activa", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("+ Guardar documento", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun AddDocumentTopBar(backgroundColor: Color, onBackClick: () -> Unit) {
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
            text = "Agregar Documento",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
