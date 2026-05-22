package com.example.autodocgt

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
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
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailsScreen(
    document: Map<String, Any>,
    vehicleName: String,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val context = LocalContext.current

    val db = remember { Firebase.firestore }
    val auth = remember { Firebase.auth }

    val documentId = document["id"] as? String ?: ""
    val tipo = document["tipo"] as? String ?: ""
    val fechaVencimiento = document["fecha_vencimiento"] as? String ?: ""
    val nombre = document["nombre"] as? String ?: ""
    val photoBase64 = document["foto"] as? String ?: ""

    val fechaAgregado = document["fecha_agregado"] as? String ?: remember {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date())
    }

    val isExpired = remember(fechaVencimiento) {
        try {
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = formatter.parse(fechaVencimiento)
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            date != null && date.before(today)
        } catch (e: Exception) {
            false
        }
    }

    val reminderText = if (isExpired) "Vencido" else "Activo"
    val reminderColor = if (isExpired) Color.Red else Color(0xFF4CAF50)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DocumentDetailsTopBar(
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Imagen del documento",
                        color = primaryDarkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (photoBase64.isNotEmpty()) {
                        val bitmapImage = remember(photoBase64) {
                            try {
                                val imageBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmapImage != null) {
                            Image(
                                bitmap = bitmapImage.asImageBitmap(),
                                contentDescription = "Foto Documento",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            DocumentImagePlaceholder()
                        }
                    } else {
                        DocumentImagePlaceholder()
                    }
                }
            }

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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Informacion",
                        color = primaryDarkBlue,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    DocumentInfoRow(label = "Tipo", value = tipo)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                    DocumentInfoRow(label = "Vencimiento", value = fechaVencimiento)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                    DocumentInfoRow(label = "Nombre:", value = nombre)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                    DocumentInfoRow(label = "Agregado", value = fechaAgregado)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)

                    DocumentInfoRow(label = "Recordatorio", value = reminderText, valueColor = reminderColor)

                    if (tipo != "Licencia de conducir") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                        DocumentInfoRow(
                            label = "Vehículo",
                            value = if (vehicleName.isNotEmpty()) vehicleName else "Sin asignar"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val currentUser = auth.currentUser
                    if (currentUser != null && documentId.isNotEmpty()) {
                        db.collection("usuarios")
                            .document(currentUser.uid)
                            .collection("documentos")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Documento eliminado", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al eliminar el documento", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(context, "No se pudo identificar el documento", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("Borrar documento", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DocumentDetailsTopBar(backgroundColor: Color, onBackClick: () -> Unit) {
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
            text = "Detalles del Documento",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun DocumentInfoRow(label: String, value: String, valueColor: Color = Color.Black) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DocumentImagePlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE8E8E8)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.img_file_plus_inicio),
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("Sin imagen disponible", color = Color.Gray, fontSize = 14.sp)
        }
    }
}
