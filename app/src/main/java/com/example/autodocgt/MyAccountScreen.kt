package com.example.autodocgt

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAccountScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val context = LocalContext.current
    
    val auth = Firebase.auth
    val db = Firebase.firestore
    val currentUser = auth.currentUser
    
    var userName by remember { mutableStateOf(currentUser?.displayName ?: "Usuario") }
    var userEmail by remember { mutableStateOf(currentUser?.email ?: "") }
    var photoBase64 by remember { mutableStateOf("") }
    
    val isGoogleLogin = remember {
        currentUser?.providerData?.any { it.providerId == "google.com" } == true
    }
    
    var showPhotoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        currentUser?.let { user ->
            try {
                val doc = db.collection("usuarios").document(user.uid).get().await()
                if (doc.exists()) {
                    userName = doc.getString("nombre") ?: user.displayName ?: "Usuario"
                    photoBase64 = doc.getString("foto") ?: ""
                }
            } catch (e: Exception) {
                // error loading
            }
        }
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val maxDimension = 600
            val ratio = kotlin.math.min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
            val scaledBitmap = if (ratio < 1.0f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true) else bitmap
            val baos = java.io.ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
            photoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val maxDimension = 600
                val ratio = kotlin.math.min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
                val scaledBitmap = if (ratio < 1.0f) android.graphics.Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true) else bitmap
                val baos = java.io.ByteArrayOutputStream()
                scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos)
                photoBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    if (showPhotoDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoDialog = false },
            title = { Text("Cambiar Foto de Perfil") },
            text = { Text("Elige una opción para actualizar tu foto.") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoDialog = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) { Text("Cámara") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoDialog = false
                    galleryLauncher.launch("image/*")
                }) { Text("Galería") }
            }
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGray)
    ) {
        // Top Bar
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
                text = "Mi Cuenta",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFB0BEC5))
                    .clickable { showPhotoDialog = true },
                contentAlignment = Alignment.Center
            ) {
                if (photoBase64.isNotEmpty()) {
                    var bitmapMap: android.graphics.Bitmap? = null
                    try {
                        val imageBytes = Base64.decode(photoBase64, Base64.DEFAULT)
                        bitmapMap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    } catch (e: Exception) {
                        // ignore
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
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(70.dp), tint = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Toca la foto para cambiarla", color = Color.Gray, fontSize = 12.sp)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Nombre de Usuario", color = primaryDarkBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userName,
                        onValueChange = { userName = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(text = "Correo Electrónico (No modificable)", color = primaryDarkBlue, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = userEmail,
                        onValueChange = { },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = Color.Gray,
                            unfocusedContainerColor = Color(0xFFF5F5F5)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isGoogleLogin) "Autenticado mediante Google" else "Autenticado mediante Correo/Contraseña",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    currentUser?.let {
                        db.collection("usuarios").document(it.uid)
                            .update(mapOf(
                                "nombre" to userName,
                                "foto" to photoBase64
                            ))
                            .addOnSuccessListener {
                                Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("Guardar Cambios", color = Color.White, fontSize = 16.sp)
            }
            
        }
    }
}
