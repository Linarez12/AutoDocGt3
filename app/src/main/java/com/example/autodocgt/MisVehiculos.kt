package com.example.autodocgt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun MisVehiculos(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onNavigateToDetails: (Map<String, Any>) -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUser = auth.currentUser

    var vehicles by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    fun loadVehicles() {
        currentUser?.let { user ->
            db.collection("vehiculos").whereEqualTo("userId", user.uid).get()
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

    LaunchedEffect(Unit) {
        loadVehicles()
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
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White
                )
            }
            Text(
                text = "Mis Vehículos",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        if (vehicles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No tienes vehículos agregados", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles) { vehicle ->
                    val id = vehicle["id"] as? String ?: ""
                    val placa = vehicle["placa"] as? String ?: "S/P"
                    val modelo = vehicle["modelo"] as? String ?: "S/M"
                    val year = vehicle["year"] as? String ?: "S/A"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$placa - $modelo - $year",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.Black
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Ver detalles",
                                    color = primaryDarkBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { onNavigateToDetails(vehicle) }
                                )
                            }
                            
                            IconButton(onClick = {
                                if (id.isNotEmpty()) {
                                    db.collection("vehiculos").document(id).delete()
                                        .addOnSuccessListener { loadVehicles() }
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
