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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.Date

@Composable
fun ExpensesScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMaintenanceClick: () -> Unit = {},
    onDocumentsClick: () -> Unit = {},
    onRemindersClick: () -> Unit = {},
    onNavigateToAddExpense: () -> Unit = {}
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

    val currentMonthGastos = remember(gastos) {
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        gastos.filter { gasto ->
            val fechaStr = gasto["fecha"] as? String ?: return@filter false
            try {
                val date = formatter.parse(fechaStr)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ExpensesTopBar(
                backgroundColor = primaryDarkBlue,
                onBackClick = onBack
            )
        },
        bottomBar = {
            HomeBottomNavigationBar(
                backgroundColor = primaryDarkBlue,
                currentRoute = "expenses",
                onHomeClick = onHomeClick,
                onMaintenanceClick = onMaintenanceClick,
                onDocumentsClick = onDocumentsClick,
                onRemindersClick = onRemindersClick,
                onExpensesClick = {} // Ya estamos en gastos
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
            
            // Tarjeta de la Tabla de Gastos
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Tabla de Gastos",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    var grandTotal = 0.0
                    
                    if (vehiculos.isEmpty()) {
                        Text("No hay vehículos registrados.", color = Color.Gray)
                    } else {
                        vehiculos.forEachIndexed { index, v ->
                            val carGastos = currentMonthGastos.filter { it["vehiculoId"] == v["id"] }
                            val sumCombustible = carGastos.filter { it["tipoCategoria"] == "Combustible" }.sumOf { (it["monto"] as? Number)?.toDouble() ?: 0.0 }
                            val sumMantenimiento = carGastos.filter { it["tipoCategoria"] == "Mantenimiento" }.sumOf { (it["monto"] as? Number)?.toDouble() ?: 0.0 }
                            val sumOtros = carGastos.filter { it["tipoCategoria"] == "Otros" }.sumOf { (it["monto"] as? Number)?.toDouble() ?: 0.0 }
                            val carTotal = sumCombustible + sumMantenimiento + sumOtros
                            
                            if (carTotal > 0 || carGastos.isNotEmpty()) {
                                grandTotal += carTotal

                                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                    Text(text = "Carro no.${index + 1} (${v["placa"]})", fontWeight = FontWeight.Bold, color = primaryDarkBlue, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Combustible:", color = Color.DarkGray)
                                        Text("Q${"%.2f".format(sumCombustible)}", color = Color.DarkGray)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Mantenimiento:", color = Color.DarkGray)
                                        Text("Q${"%.2f".format(sumMantenimiento)}", color = Color.DarkGray)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Otros:", color = Color.DarkGray)
                                        Text("Q${"%.2f".format(sumOtros)}", color = Color.DarkGray)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Vehículo:", fontWeight = FontWeight.Bold, color = Color.Black)
                                        Text("Q${"%.2f".format(carTotal)}", fontWeight = FontWeight.Bold, color = Color.Black)
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL DEL MES:", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryDarkBlue)
                            Text("Q${"%.2f".format(grandTotal)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryDarkBlue)
                        }
                    }
                }
            }

            Button(
                onClick = { onNavigateToAddExpense() },
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("+ Agregar gasto", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { /* TODO: View expenses report */ },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(bottom = 16.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("Ver reporte de gastos", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ExpensesTopBar(backgroundColor: Color, onBackClick: () -> Unit) {
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
            text = "Gastos del mes",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
