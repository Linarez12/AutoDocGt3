package com.example.autodocgt

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    vehicle: Map<String, Any>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onEdit: (Map<String, Any>) -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    
    val marca = vehicle["marca"] as? String ?: ""
    val modelo = vehicle["modelo"] as? String ?: ""
    val anio = vehicle["anio"] as? String ?: ""
    val placa = vehicle["placa"] as? String ?: "NO DISPONIBLE"
    val colorVehiculo = vehicle["color"] as? String ?: "No especificado"
    val photoBase64 = vehicle["foto"] as? String ?: ""
    val kilometraje = vehicle["kilometraje"] as? String ?: "0"
    val combustible = vehicle["combustible"] as? String ?: "N/A"
    val vehicleId = vehicle["id"] as? String ?: ""
    
    var documentCount by remember { mutableStateOf(0) }
    
    LaunchedEffect(vehicleId) {
        val auth = Firebase.auth
        val currentUser = auth.currentUser
        if (currentUser != null && vehicleId.isNotEmpty()) {
            Firebase.firestore.collection("usuarios")
                .document(currentUser.uid)
                .collection("documentos")
                .whereEqualTo("vehiculoId", vehicleId)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    documentCount = querySnapshot.size()
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(primaryDarkBlue)
                .padding(16.dp),
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
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$marca $modelo".uppercase(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = anio,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(28.dp))
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Auto No.1",
                        color = primaryDarkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (photoBase64.isNotEmpty()) {
                        val bitmapImage = remember(photoBase64) {
                            try {
                                val imageBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) { null }
                        }
                        if (bitmapImage != null) {
                            Image(
                                bitmap = bitmapImage.asImageBitmap(),
                                contentDescription = "Foto Vehículo",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            FallbackCarImage()
                        }
                    } else {
                        FallbackCarImage()
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                StatCard(
                    value = if (kilometraje.uppercase().endsWith("KM")) kilometraje else "$kilometraje KM", 
                    label = "Kilometraje", 
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(value = documentCount.toString(), label = "Documentos", modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(12.dp))
                StatCard(value = "0", label = "Mantenimiento", modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Informacion",
                        color = primaryDarkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InfoRow(label = "Placa", value = placa)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                    InfoRow(label = "Color", value = colorVehiculo)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                    InfoRow(label = "Combustible", value = combustible)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                    
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val currentDate = sdf.format(Date())
                    InfoRow(label = "Agregado", value = currentDate)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        val auth = Firebase.auth
                        val currentUser = auth.currentUser
                        if (currentUser != null && vehicleId.isNotEmpty()) {
                            val db = Firebase.firestore
                            
                            db.collection("usuarios")
                                .document(currentUser.uid)
                                .collection("documentos")
                                .whereEqualTo("vehiculoId", vehicleId)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    val batch = db.batch()
                                    for (doc in querySnapshot.documents) {
                                        batch.delete(doc.reference)
                                    }
                                    batch.commit().addOnCompleteListener {
                                        db.collection("vehiculos").document(vehicleId).delete()
                                            .addOnSuccessListener {
                                                onBack()
                                            }
                                    }
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Borrar", color = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(
                    onClick = { onEdit(vehicle) },
                    colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue),
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Editar", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value,
                color = Color(0xFF16528E),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FallbackCarImage() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.LightGray),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.vehiculo),
            contentDescription = "Carro Default",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
