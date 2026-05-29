package com.example.autodocgt

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgregarVehiculo(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    
    var marca by remember { mutableStateOf("") }
    var modelo by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("") }
    var placa by remember { mutableStateOf("") }
    var colorVehiculo by remember { mutableStateOf("") }
    var combustible by remember { mutableStateOf("") }
    var kilometraje by remember { mutableStateOf("") }
    var expandedCombustible by remember { mutableStateOf(false) }
    var photoBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPhotoDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    
    val context = LocalContext.current
    
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        photoBitmap = bitmap
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                photoBitmap = BitmapFactory.decodeStream(inputStream)
            } catch (e: Exception) {}
        }
    }
    
    val db = Firebase.firestore
    val auth = Firebase.auth
    
    val combustibleOptions = listOf("Gasolina", "Diesel", "Eléctrico", "Híbrido")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGray)
    ) {
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
                            anio = formatter.format(Date(millis))
                        }
                    }) {
                        Text("Aceptar")
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

        if (showPhotoDialog) {
            AlertDialog(
                onDismissRequest = { showPhotoDialog = false },
                title = { Text("Agregar Fotografía", color = primaryDarkBlue, fontWeight = FontWeight.Bold) },
                text = { Text("¿Desde dónde deseas agregar la fotografía del vehículo?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showPhotoDialog = false
                            cameraLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
                    ) {
                        Text("Cámara", color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showPhotoDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Text("Galería", color = primaryDarkBlue)
                    }
                }
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryDarkBlue)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Agregar Vehiculo",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(primaryDarkBlue)
                    .clickable {
                        if (photoBitmap == null) {
                            showPhotoDialog = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoBitmap == null) {
                    Text(
                        text = "+ Foto del Vehiculo",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Image(
                        bitmap = photoBitmap!!.asImageBitmap(),
                        contentDescription = "Foto del vehículo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { photoBitmap = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Borrar foto",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Informacion",
                        color = primaryDarkBlue,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(text = "Marca:", color = Color.Gray, fontSize = 14.sp)
                    OutlinedTextField(
                        value = marca,
                        onValueChange = { marca = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color.Black)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(text = "Modelo:", color = Color.Gray, fontSize = 14.sp)
                    OutlinedTextField(
                        value = modelo,
                        onValueChange = { modelo = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color.Black)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Fecha:", color = Color.Gray, fontSize = 14.sp)
                            Box {
                                OutlinedTextField(
                                    value = anio,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = TextStyle(color = Color.Black)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { showDatePicker = true }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Placa:", color = Color.Gray, fontSize = 14.sp)
                            OutlinedTextField(
                                value = placa,
                                onValueChange = { placa = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = TextStyle(color = Color.Black)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(text = "Color:", color = Color.Gray, fontSize = 14.sp)
                    OutlinedTextField(
                        value = colorVehiculo,
                        onValueChange = { colorVehiculo = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color.Black)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(text = "Tipo de Combustible:", color = Color.Gray, fontSize = 14.sp)
                    ExposedDropdownMenuBox(
                        expanded = expandedCombustible,
                        onExpandedChange = { expandedCombustible = !expandedCombustible }
                    ) {
                        OutlinedTextField(
                            value = combustible,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCombustible) },
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color.Black)
                        )
                        ExposedDropdownMenu(
                            expanded = expandedCombustible,
                            onDismissRequest = { expandedCombustible = false }
                        ) {
                            combustibleOptions.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        combustible = selectionOption
                                        expandedCombustible = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(text = "Kilometraje actual:", color = Color.Gray, fontSize = 14.sp)
                    OutlinedTextField(
                        value = kilometraje,
                        onValueChange = { newValue ->
                            kilometraje = newValue.filter { it.isDigit() }
                        },
                        trailingIcon = { 
                            Text(
                                text = "KM", 
                                modifier = Modifier.padding(end = 12.dp), 
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(color = Color.Black)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (marca.isEmpty() || modelo.isEmpty() || placa.isEmpty()) {
                        Toast.makeText(context, "Por favor completa los campos principales (Marca, Modelo, Placa)", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        isLoading = true
                        var photoBase64 = ""
                        if (photoBitmap != null) {
                            val maxDimension = 600
                            val originalWidth = photoBitmap!!.width
                            val originalHeight = photoBitmap!!.height
                            
                            val ratio = kotlin.math.min(
                                maxDimension.toFloat() / originalWidth,
                                maxDimension.toFloat() / originalHeight
                            )
                            
                            val scaledBitmap = if (ratio < 1.0f) {
                                android.graphics.Bitmap.createScaledBitmap(
                                    photoBitmap!!,
                                    (originalWidth * ratio).toInt(),
                                    (originalHeight * ratio).toInt(),
                                    true
                                )
                            } else {
                                photoBitmap!!
                            }

                            val baos = java.io.ByteArrayOutputStream()
                            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 60, baos)
                            val byteArray = baos.toByteArray()
                            photoBase64 = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
                        }
                        
                        val vehiculoData = hashMapOf(
                            "userId" to uid,
                            "marca" to marca,
                            "modelo" to modelo,
                            "anio" to anio,
                            "placa" to placa,
                            "color" to colorVehiculo,
                            "combustible" to combustible,
                            "kilometraje" to if (kilometraje.isNotEmpty()) "$kilometraje KM" else "",
                            "foto" to photoBase64,
                            "fechaCreacion" to System.currentTimeMillis()
                        )
                        db.collection("vehiculos").add(vehiculoData)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Vehículo agregado con éxito", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Toast.makeText(context, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "Sesión no válida", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("+ Guardar Vehiculo", color = Color.White, fontSize = 16.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
