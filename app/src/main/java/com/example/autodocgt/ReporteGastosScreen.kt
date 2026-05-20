package com.example.autodocgt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ReporteGastosScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onDetailsClick: (Map<String, Any>, String) -> Unit = { _, _ -> }
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)

    val db = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }
    
    var gastos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var vehiculos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

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
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        gastos = snapshot.documents.mapNotNull { it.data }
                    }
                }
        }
    }

    val sortedGastos = remember(gastos) {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        gastos.sortedByDescending { 
            val fechaStr = it["fecha"] as? String ?: ""
            try {
                formatter.parse(fechaStr)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ReporteGastosTopBar(
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
                .padding(16.dp)
        ) {
            if (sortedGastos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay gastos registrados", color = Color.Gray)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(primaryDarkBlue, shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fecha", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text("Auto", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text("Total(Q)", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Detalle", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                        Text("Acción", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape = androidx.compose.foundation.shape.RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                ) {
                    items(sortedGastos) { gasto ->
                        val fecha = gasto["fecha"] as? String ?: ""
                        val vId = gasto["vehiculoId"] as? String ?: ""
                        val vIndex = vehiculos.indexOfFirst { it["id"] == vId }
                        val autoText = if (vIndex >= 0) "Auto no.${vIndex + 1}" else "Otro"
                        
                        val cat = gasto["tipoCategoria"] as? String ?: ""
                        val sub = if (cat == "Mantenimiento") (gasto["tipoServicio"] as? String ?: "") else (gasto["tipoGasto"] as? String ?: cat)
                        val detalleText = if (cat == "Combustible") "Gasolina" else sub
                        
                        val montoNum = (gasto["monto"] as? Number)?.toDouble() ?: 0.0
                        val monto = "%.2f".format(montoNum)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(fecha, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f))
                                Text(autoText, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text(monto, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(detalleText, fontSize = 12.sp, color = Color.Black, modifier = Modifier.weight(2f), maxLines = 2)
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Ver",
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(primaryDarkBlue, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                            .clickable { onDetailsClick(gasto, autoText) }
                                            .padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = Color(0xFFEEEEEE))
                    }
                    
                    item {
                        val grandTotal = sortedGastos.sumOf { (it["monto"] as? Number)?.toDouble() ?: 0.0 }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TOTAL GLOBAL", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = primaryDarkBlue, modifier = Modifier.weight(3.5f))
                            Text(String.format("Q%.2f", grandTotal), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = primaryDarkBlue, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            Spacer(modifier = Modifier.weight(0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReporteGastosTopBar(backgroundColor: Color, onBackClick: () -> Unit) {
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
            text = "Reporte de Gastos",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
