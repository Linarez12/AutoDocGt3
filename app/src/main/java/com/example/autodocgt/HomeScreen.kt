package com.example.autodocgt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    
    var userName by remember { mutableStateOf("Usuario") }
    val auth = Firebase.auth
    val db = Firebase.firestore

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("usuarios").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    userName = document.getString("nombre") ?: currentUser.displayName ?: "Usuario"
                }
                .addOnFailureListener {
                    userName = currentUser.displayName ?: "Usuario"
                }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(primaryDarkBlue, onNavigateToSettings)
        },
        bottomBar = {
            HomeBottomNavigationBar(primaryDarkBlue)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGray)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Hola, $userName",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primaryDarkBlue
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Button(
                onClick = { /* TODO */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryDarkBlue)
            ) {
                Text("Consultar Multas", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun HomeTopBar(backgroundColor: Color, onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            painter = painterResource(id = R.drawable.img_bell_inicio),
            contentDescription = "Notificaciones",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = "AUTO DOC GT",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Icon(
            painter = painterResource(id = R.drawable.img_settings_inicio),
            contentDescription = "Configuración",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { onSettingsClick() }
        )
    }
}

@Composable
fun HomeBottomNavigationBar(backgroundColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(iconRes = R.drawable.img_home_inicio, label = "Inicio")
        BottomNavItem(iconRes = R.drawable.img_carro_documentos, label = "Mantenimiento")
        BottomNavItem(iconRes = R.drawable.img_file_plus_inicio, label = "Documentos")
        BottomNavItem(iconRes = R.drawable.img_calendar_inicio, label = "Recordatorios")
        BottomNavItem(iconRes = R.drawable.img_billetera_inicio, label = "Gastos")
    }
}

@Composable
fun BottomNavItem(iconRes: Int, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable { /* TODO */ }.padding(4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 10.sp
        )
    }
}
