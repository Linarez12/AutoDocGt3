package com.example.autodocgt

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun Recordatorios(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onMaintenanceClick: () -> Unit = {},
    onDocumentsClick: () -> Unit = {},
    onExpensesClick: () -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)

    val db = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }
    var recordatorios by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("usuarios").document(currentUser.uid).collection("documentos")
                .whereEqualTo("recordatorioActivo", true)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        recordatorios = snapshot.documents.mapNotNull { doc ->
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
            RemindersTopBar(
                backgroundColor = primaryDarkBlue,
                onBackClick = onBack
            )
        },
        bottomBar = {
            HomeBottomNavigationBar(
                backgroundColor = primaryDarkBlue,
                currentRoute = "reminders",
                onHomeClick = onHomeClick,
                onMaintenanceClick = onMaintenanceClick,
                onDocumentsClick = onDocumentsClick,
                onRemindersClick = {},
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
            if (recordatorios.isEmpty()) {
                Spacer(modifier = Modifier.weight(1f))
                Text("No hay recordatorios activos", color = Color.Gray)
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(recordatorios) { doc ->
                        ReminderCard(
                            reminder = doc,
                            onDelete = {
                                val docId = doc["id"] as? String
                                val currentUser = auth.currentUser
                                if (docId != null && currentUser != null) {
                                    db.collection("usuarios").document(currentUser.uid)
                                        .collection("documentos").document(docId)
                                        .update("recordatorioActivo", false)
                                        .addOnSuccessListener {
                                            android.widget.Toast.makeText(context, "Recordatorio borrado", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RemindersTopBar(backgroundColor: Color, onBackClick: () -> Unit) {
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
            text = "Recordatorios",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun ReminderCard(
    reminder: Map<String, Any>,
    onDelete: () -> Unit
) {
    val nombre = reminder["nombre"] as? String ?: ""
    val fecha = reminder["fecha_vencimiento"] as? String ?: ""

    val color = remember(fecha) {
        try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = formatter.parse(fecha)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            
            if (date == null) return@remember Color.Black
            
            val diff = date.time - today.time
            val days = TimeUnit.MILLISECONDS.toDays(diff)
            
            when {
                days < 0 -> Color.Red
                days <= 90 -> Color(0xFFFBC02D)
                else -> Color(0xFF4CAF50)
            }
        } catch (e: Exception) {
            Color.Black
        }
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
            Icon(
                painter = painterResource(id = R.drawable.calendario),
                contentDescription = "Calendario",
                tint = Color.Unspecified,
                modifier = Modifier.size(60.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(text = "Fecha:$fecha", fontSize = 14.sp, color = color, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDelete,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16528E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Borrar", fontSize = 14.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
