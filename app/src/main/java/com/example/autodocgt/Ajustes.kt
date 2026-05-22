package com.example.autodocgt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

@Composable
fun Ajustes(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onMyAccountClick: () -> Unit = {},
    onMyVehiclesClick: () -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUser = auth.currentUser
    val context = androidx.compose.ui.platform.LocalContext.current
    
    var userName by remember { mutableStateOf("Usuario") }
    var userEmail by remember { mutableStateOf(currentUser?.email ?: "usuario@gmail.com") }
    var photoBase64 by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            db.collection("usuarios").document(user.uid).get()
                .addOnSuccessListener { document ->
                    userName = document.getString("nombre") ?: user.displayName ?: "Usuario"
                    photoBase64 = document.getString("foto") ?: ""
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
                text = "Ajustes",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB0BEC5)),
                contentAlignment = Alignment.Center
            ) {
                if (photoBase64.isNotEmpty()) {
                    var bitmapMap: android.graphics.Bitmap? = null
                    try {
                        val imageBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                        bitmapMap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    } catch (e: Exception) {
                    }
                    if (bitmapMap != null) {
                        Image(
                            bitmap = bitmapMap.asImageBitmap(),
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(70.dp), tint = Color.White)
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(70.dp),
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = userName, 
                fontSize = 24.sp, 
                fontWeight = FontWeight.Bold, 
                color = primaryDarkBlue
            )
            Text(
                text = userEmail, 
                fontSize = 15.sp, 
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Notificaciones",
                        color = primaryDarkBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    NotificationSwitch("Recordatorios", "notifications_enabled")
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    NotificationSwitch("Mostrar icono de campana", "show_bell_icon")
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    NotificationSwitch("Notificación al exportar reporte", "export_notification_enabled")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    MenuItem("Mi cuenta >", onClick = onMyAccountClick)
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    MenuItem("Mis vehiculos >", onClick = onMyVehiclesClick)
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    MenuItem("Exportar reporte de gastos >", onClick = { exportarReportePdf(context, db, auth) })
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    
                    Text(
                        text = "Cerrar sesion",
                        color = Color.Red,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                auth.signOut()
                                onLogout()
                            }
                            .padding(vertical = 15.dp),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun NotificationSwitch(label: String, prefKey: String = "notifications_enabled", defaultChecked: Boolean = true) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE) }
    var checked by remember { mutableStateOf(sharedPrefs.getBoolean(prefKey, defaultChecked)) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label, 
            fontWeight = FontWeight.Bold, 
            fontSize = 15.sp,
            color = Color.Black
        )
        Switch(
            checked = checked,
            onCheckedChange = { 
                checked = it
                sharedPrefs.edit().putBoolean(prefKey, it).apply()
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF16528E),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
fun MenuItem(label: String, onClick: (() -> Unit)? = null) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick?.invoke() }
            .padding(vertical = 15.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Color.Black
    )
}

@Preview(showBackground = true)
@Composable
fun AjustesPreview() {
    Ajustes()
}

fun exportarReportePdf(context: Context, db: FirebaseFirestore, auth: FirebaseAuth) {
    val currentUser = auth.currentUser
    if (currentUser == null) {
        Toast.makeText(context, "Inicia sesión primero", Toast.LENGTH_SHORT).show()
        return
    }

    Toast.makeText(context, "Generando reporte...", Toast.LENGTH_SHORT).show()

    db.collection("vehiculos").whereEqualTo("userId", currentUser.uid).get().addOnSuccessListener { vehiculosSnapshot ->
        val vehiculos = vehiculosSnapshot.documents.mapNotNull { doc ->
            val data = doc.data?.toMutableMap()
            if (data != null) { data["id"] = doc.id; data } else null
        }

        db.collection("usuarios").document(currentUser.uid).collection("gastos").get().addOnSuccessListener { gastosSnapshot ->
            val gastosList = gastosSnapshot.documents.mapNotNull { doc ->
                val data = doc.data?.toMutableMap()
                if (data != null) { data["id"] = doc.id; data } else null
            }
            
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val sortedGastos = gastosList.sortedByDescending {
                val fechaStr = it["fecha"] as? String ?: ""
                try { formatter.parse(fechaStr)?.time ?: 0L } catch(e: Exception) { 0L }
            }

            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                var pageNumber = 1
                var page = pdfDocument.startPage(pageInfo)
                var canvas = page.canvas
                val paint = Paint()
                val paintLine = Paint().apply {
                    color = android.graphics.Color.parseColor("#CCCCCC")
                    strokeWidth = 1f
                }

                val fechaGeneracion = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date())

                paint.color = android.graphics.Color.parseColor("#16528E")
                paint.textSize = 22f
                paint.isFakeBoldText = true
                var yPosition = 50f
                canvas.drawText("Reporte de Gastos", 50f, yPosition, paint)

                paint.color = android.graphics.Color.GRAY
                paint.textSize = 10f
                paint.isFakeBoldText = false
                yPosition += 18f
                canvas.drawText("Generado el: $fechaGeneracion", 50f, yPosition, paint)

                yPosition += 12f
                canvas.drawLine(50f, yPosition, 545f, yPosition, paintLine)
                yPosition += 16f

                paint.color = android.graphics.Color.parseColor("#16528E")
                paint.textSize = 11f
                paint.isFakeBoldText = true
                canvas.drawText("Fecha",      50f,  yPosition, paint)
                canvas.drawText("Vehículo",   130f, yPosition, paint)
                canvas.drawText("Categoría",  210f, yPosition, paint)
                canvas.drawText("Detalle",    300f, yPosition, paint)
                canvas.drawText("Notas",      430f, yPosition, paint)
                canvas.drawText("Monto",      510f, yPosition, paint)
                yPosition += 6f
                canvas.drawLine(50f, yPosition, 545f, yPosition, paintLine)
                yPosition += 14f

                paint.color = android.graphics.Color.BLACK
                paint.isFakeBoldText = false
                paint.textSize = 10f

                var total = 0.0
                var totalCombustible = 0.0
                var totalMantenimiento = 0.0
                var totalOtros = 0.0

                fun nuevaPagina() {
                    pdfDocument.finishPage(page)
                    pageNumber++
                    page = pdfDocument.startPage(
                        PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
                    )
                    canvas = page.canvas
                    yPosition = 50f
                    paint.color = android.graphics.Color.parseColor("#16528E")
                    paint.textSize = 11f
                    paint.isFakeBoldText = true
                    canvas.drawText("Fecha",      50f,  yPosition, paint)
                    canvas.drawText("Vehículo",   130f, yPosition, paint)
                    canvas.drawText("Categoría",  210f, yPosition, paint)
                    canvas.drawText("Detalle",    300f, yPosition, paint)
                    canvas.drawText("Notas",      430f, yPosition, paint)
                    canvas.drawText("Monto",      510f, yPosition, paint)
                    yPosition += 6f
                    canvas.drawLine(50f, yPosition, 545f, yPosition, paintLine)
                    yPosition += 14f
                    paint.color = android.graphics.Color.BLACK
                    paint.isFakeBoldText = false
                    paint.textSize = 10f
                }

                for ((index, gasto) in sortedGastos.withIndex()) {
                    if (yPosition > 780f) nuevaPagina()

                    if (index % 2 == 0) {
                        val bgPaint = Paint().apply {
                            color = android.graphics.Color.parseColor("#F5F8FF")
                        }
                        canvas.drawRect(50f, yPosition - 11f, 545f, yPosition + 5f, bgPaint)
                    }

                    val fecha = gasto["fecha"] as? String ?: ""
                    val vId = gasto["vehiculoId"] as? String ?: ""
                    val vIndex = vehiculos.indexOfFirst { it["id"] == vId }
                    val autoText = if (vIndex >= 0) "Auto ${vIndex + 1}" else "Otro"

                    val cat = gasto["tipoCategoria"] as? String ?: ""
                    val sub = when (cat) {
                        "Mantenimiento" -> gasto["tipoServicio"] as? String ?: ""
                        "Combustible"   -> gasto["tipoCombustible"] as? String ?: "Gasolina"
                        else            -> gasto["tipoGasto"] as? String ?: cat
                    }
                    val detalleText = sub.take(18)

                    val notas = when {
                        (gasto["notas"] as? String)?.isNotEmpty() == true -> (gasto["notas"] as String).take(14)
                        (gasto["descripcion"] as? String)?.isNotEmpty() == true -> (gasto["descripcion"] as String).take(14)
                        else -> "-"
                    }

                    val montoNum = (gasto["monto"] as? Number)?.toDouble() ?: 0.0
                    val montoStr = "Q%.2f".format(montoNum)
                    total += montoNum
                    when (cat) {
                        "Combustible"   -> totalCombustible   += montoNum
                        "Mantenimiento" -> totalMantenimiento += montoNum
                        else            -> totalOtros         += montoNum
                    }

                    paint.color = android.graphics.Color.BLACK
                    canvas.drawText(fecha,       50f,  yPosition, paint)
                    canvas.drawText(autoText,    130f, yPosition, paint)
                    canvas.drawText(cat.take(12), 210f, yPosition, paint)
                    canvas.drawText(detalleText, 300f, yPosition, paint)
                    canvas.drawText(notas,       430f, yPosition, paint)

                    paint.isFakeBoldText = true
                    canvas.drawText(montoStr,    510f, yPosition, paint)
                    paint.isFakeBoldText = false

                    yPosition += 18f
                    canvas.drawLine(50f, yPosition - 4f, 545f, yPosition - 4f, paintLine)
                }

                yPosition += 10f
                if (yPosition > 760f) nuevaPagina()

                paint.color = android.graphics.Color.parseColor("#16528E")
                paint.textSize = 13f
                paint.isFakeBoldText = true
                canvas.drawText("TOTAL GLOBAL:", 300f, yPosition, paint)
                canvas.drawText("Q%.2f".format(total), 460f, yPosition, paint)
                yPosition += 24f

                if (yPosition > 740f) nuevaPagina()

                canvas.drawLine(50f, yPosition, 545f, yPosition, paintLine)
                yPosition += 16f

                paint.color = android.graphics.Color.parseColor("#16528E")
                paint.textSize = 13f
                paint.isFakeBoldText = true
                canvas.drawText("Resumen por Categoría", 50f, yPosition, paint)
                yPosition += 18f

                paint.textSize = 11f
                paint.isFakeBoldText = false
                paint.color = android.graphics.Color.BLACK

                canvas.drawText("Combustible:",   50f,  yPosition, paint)
                paint.isFakeBoldText = true
                canvas.drawText("Q%.2f".format(totalCombustible), 200f, yPosition, paint)
                paint.isFakeBoldText = false
                val pctCombustible = if (total > 0) (totalCombustible / total * 100) else 0.0
                canvas.drawText("(%.1f%% del total)".format(pctCombustible), 320f, yPosition, paint)
                yPosition += 16f

                canvas.drawText("Mantenimiento:",  50f,  yPosition, paint)
                paint.isFakeBoldText = true
                canvas.drawText("Q%.2f".format(totalMantenimiento), 200f, yPosition, paint)
                paint.isFakeBoldText = false
                val pctMant = if (total > 0) (totalMantenimiento / total * 100) else 0.0
                canvas.drawText("(%.1f%% del total)".format(pctMant), 320f, yPosition, paint)
                yPosition += 16f

                canvas.drawText("Otros gastos:",   50f,  yPosition, paint)
                paint.isFakeBoldText = true
                canvas.drawText("Q%.2f".format(totalOtros), 200f, yPosition, paint)
                paint.isFakeBoldText = false
                val pctOtros = if (total > 0) (totalOtros / total * 100) else 0.0
                canvas.drawText("(%.1f%% del total)".format(pctOtros), 320f, yPosition, paint)

                pdfDocument.finishPage(page)

                var file: File? = null
                try {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    file = File(downloadsDir, "Reporte_Gastos_${System.currentTimeMillis()}.pdf")
                    pdfDocument.writeTo(FileOutputStream(file))
                } catch (e: Exception) {
                    val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    file = File(fallbackDir, "Reporte_Gastos_${System.currentTimeMillis()}.pdf")
                    pdfDocument.writeTo(FileOutputStream(file))
                }
                pdfDocument.close()

                Toast.makeText(context, "PDF guardado en Descargas", Toast.LENGTH_LONG).show()

                // Solo enviar notificación si el usuario lo tiene activado
                val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val exportNotifEnabled = sharedPrefs.getBoolean("export_notification_enabled", true)
                if (exportNotifEnabled) {
                    sendExportNotification(context, "Reporte Exportado", "El reporte de gastos ha sido guardado en Descargas.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error al crear PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error al obtener gastos", Toast.LENGTH_SHORT).show()
        }
    }.addOnFailureListener {
        Toast.makeText(context, "Error al obtener vehículos", Toast.LENGTH_SHORT).show()
    }
}

fun sendExportNotification(context: Context, title: String, message: String) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
    }
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val builder = NotificationCompat.Builder(context, "reminders_channel")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(title)
        .setContentText(message)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    with(NotificationManagerCompat.from(context)) {
        try {
            notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            // Permission might be denied
        }
    }
}
