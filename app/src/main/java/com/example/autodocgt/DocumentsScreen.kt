package com.example.autodocgt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun DocumentsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMaintenanceClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {},
    onNavigateToAddDocument: () -> Unit = {},
    onNavigateToDocumentDetails: (Map<String, Any>, String) -> Unit = { _, _ -> }
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    
    val db = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }
    var documentos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var vehiculos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("usuarios").document(currentUser.uid).collection("documentos")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        documentos = snapshot.documents.mapNotNull { doc ->
                            val data = doc.data?.toMutableMap()
                            if (data != null) {
                                data["id"] = doc.id
                                data
                            } else null
                        }
                    }
                }
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
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocumentsTopBar(
                backgroundColor = primaryDarkBlue,
                onBackClick = onBack
            )
        },
        bottomBar = {
            HomeBottomNavigationBar(
                backgroundColor = primaryDarkBlue,
                currentRoute = "documents",
                onHomeClick = onHomeClick,
                onMaintenanceClick = onMaintenanceClick,
                onDocumentsClick = {}, // Ya estamos en documentos
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
            if (documentos.isEmpty()) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No hay documentos registrados", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                val licencias = documentos.filter { it["tipo"] == "Licencia de conducir" }
                if (licencias.isNotEmpty()) {
                    item {
                        Text(
                            text = "Licencias",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = primaryDarkBlue,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(licencias) { doc ->
                        DocumentCard(
                            document = doc,
                            onDetailsClick = { onNavigateToDocumentDetails(doc, "") }
                        )
                    }
                }

                vehiculos.forEachIndexed { index, v ->
                    val carDocs = documentos.filter { it["vehiculoId"] == v["id"] }
                    if (carDocs.isNotEmpty()) {
                        item {
                            Text(
                                text = "Auto no.${index + 1}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = primaryDarkBlue,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }
                        items(carDocs) { doc ->
                            DocumentCard(
                                document = doc,
                                onDetailsClick = { onNavigateToDocumentDetails(doc, "Auto no.${index + 1}") }
                            )
                        }
                    }
                }

                val otrosDocs = documentos.filter { 
                    it["tipo"] != "Licencia de conducir" && 
                    (it["vehiculoId"] == null || it["vehiculoId"] == "") 
                }
                
                if (otrosDocs.isNotEmpty()) {
                    item {
                        Text(
                            text = "Otros Documentos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = primaryDarkBlue,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                        )
                    }
                    items(otrosDocs) { doc ->
                        val vehicleId = doc["vehiculoId"] as? String ?: ""
                        val associatedVehicleIndex = vehiculos.indexOfFirst { it["id"] == vehicleId }
                        val vehicleLabel = if (associatedVehicleIndex != -1) "Auto no.${associatedVehicleIndex + 1}" else ""
                        DocumentCard(
                            document = doc,
                            onDetailsClick = { onNavigateToDocumentDetails(doc, vehicleLabel) }
                        )
                    }
                }
            }
            }
            
            Button(
                onClick = onNavigateToAddDocument,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("+ Agregar documento", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun DocumentsTopBar(backgroundColor: Color, onBackClick: () -> Unit) {
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
            text = "Documentos",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun DocumentCard(
    document: Map<String, Any>,
    onDetailsClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val tipo = document["tipo"] as? String ?: ""
    val fecha = document["fecha_vencimiento"] as? String ?: ""
    val nombre = document["nombre"] as? String ?: ""

    val isExpired = remember(fecha) {
        try {
            val formatter = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val date = formatter.parse(fecha)
            val today = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.time
            date != null && date.before(today)
        } catch (e: Exception) {
            false
        }
    }
    val dateColor = if (isExpired) Color.Red else Color(0xFF4CAF50)

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(0xFFE8E8E8), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.img_file_plus_inicio),
                    contentDescription = null,
                    tint = Color(0xFF16528E),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(text = tipo, fontSize = 14.sp, color = Color.Gray)
                Text(text = "Vence: $fecha", fontSize = 14.sp, color = dateColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDetailsClick,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16528E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Ver Detalles", fontSize = 12.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            val db = com.google.firebase.ktx.Firebase.firestore
                            val auth = com.google.firebase.ktx.Firebase.auth
                            val docId = document["id"] as? String
                            val currentUser = auth.currentUser
                            if (docId != null && currentUser != null) {
                                db.collection("usuarios").document(currentUser.uid)
                                    .collection("documentos").document(docId)
                                    .update("recordatorioActivo", true)
                                    .addOnSuccessListener {
                                        android.widget.Toast.makeText(context, "Recordatorio agregado", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                            }
                        },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16528E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Recordatorio", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
