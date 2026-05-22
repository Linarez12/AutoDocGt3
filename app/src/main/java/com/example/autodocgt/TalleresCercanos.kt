package com.example.autodocgt

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

data class WorkshopResult(
    val name: String,
    val address: String,
    val distanceKm: Double,
    val rating: Float,
    val isOpen: Boolean,
    val lat: Double,
    val lon: Double,
    val placeId: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TalleresCercanos(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val primaryDarkBlue = Color(0xFF16528E)
    val backgroundGray = Color(0xFFE8E8E8)
    val apiKey = "AIzaSyCz63meTZhmR9e574M02z1oDNwfdfKaK1U"

    var searchQuery by remember { mutableStateOf("") }
    var workshops by remember { mutableStateOf<List<WorkshopResult>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(14.6349, -90.5069), 12f)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasLocationPermission = isGranted }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    suspend fun searchWorkshops(query: String, location: LatLng) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                val searchUrl = "https://places.googleapis.com/v1/places:searchText"
                val url = URL(searchUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-Goog-Api-Key", apiKey)
                connection.setRequestProperty(
                    "X-Goog-FieldMask", 
                    "places.id,places.displayName.text,places.formattedAddress,places.location,places.rating,places.regularOpeningHours.openNow"
                )
                connection.doOutput = true

                val jsonBody = JSONObject().apply {
                    put("textQuery", query)
                    put("includedType", "car_repair")
                    
                    val centerObj = JSONObject().apply {
                        put("latitude", location.latitude)
                        put("longitude", location.longitude)
                    }
                    val circleObj = JSONObject().apply {
                        put("center", centerObj)
                        put("radius", 10000.0) 
                    }
                    val locationBiasObj = JSONObject().apply {
                        put("circle", circleObj)
                    }
                    put("locationBias", locationBiasObj)
                }.toString()

                connection.outputStream.use { os ->
                    val input = jsonBody.toByteArray(Charsets.UTF_8)
                    os.write(input, 0, input.size)
                }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e("WorkshopSearch", "HTTP Error ${connection.responseCode}: $errorResponse")
                    withContext(Dispatchers.Main) { isLoading = false }
                    return@withContext
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                Log.d("WorkshopSearch", "API Response: $response")
                val json = JSONObject(response)

                val list = mutableListOf<WorkshopResult>()
                if (json.has("places")) {
                    val placesArr = json.getJSONArray("places")
                    for (i in 0 until placesArr.length()) {
                        val obj = placesArr.getJSONObject(i)
                        
                        val loc = obj.optJSONObject("location")
                        val lat = loc?.optDouble("latitude") ?: 0.0
                        val lng = loc?.optDouble("longitude") ?: 0.0

                        val name = obj.optJSONObject("displayName")?.optString("text") ?: "Taller"
                        val address = obj.optString("formattedAddress", "Guatemala")
                        val rating = obj.optDouble("rating", 0.0).toFloat()
                        
                        var isOpen = false
                        if (obj.has("regularOpeningHours")) {
                            isOpen = obj.getJSONObject("regularOpeningHours").optBoolean("openNow", false)
                        }

                        list.add(WorkshopResult(
                            name = name,
                            address = address,
                            distanceKm = calculateDistance(location.latitude, location.longitude, lat, lng),
                            rating = rating,
                            isOpen = isOpen,
                            lat = lat,
                            lon = lng,
                            placeId = obj.optString("id", "")
                        ))
                    }
                }

                withContext(Dispatchers.Main) {
                    workshops = list.sortedBy { it.distanceKm }
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("WorkshopSearch", "Exception during search", e)
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        val userLatLng = LatLng(location.latitude, location.longitude)
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                        scope.launch { searchWorkshops("talleres mecanicos", userLatLng) }
                    } else {
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                            .addOnSuccessListener { currLoc: Location? ->
                                if (currLoc != null) {
                                    val userLatLng = LatLng(currLoc.latitude, currLoc.longitude)
                                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                                    scope.launch { searchWorkshops("talleres mecanicos", userLatLng) }
                                } else {
                                    scope.launch { searchWorkshops("talleres mecanicos", cameraPositionState.position.target) }
                                }
                            }
                            .addOnFailureListener {
                                scope.launch { searchWorkshops("talleres mecanicos", cameraPositionState.position.target) }
                            }
                    }
                }.addOnFailureListener {
                    scope.launch { searchWorkshops("talleres mecanicos", cameraPositionState.position.target) }
                }
            } catch (e: SecurityException) {
                Log.e("WorkshopSearch", "Error de seguridad al obtener ubicación", e)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(modifier = modifier.fillMaxSize().background(backgroundGray)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(primaryDarkBlue).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = Color.White,
                modifier = Modifier.size(28.dp).clickable { onBack() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text("Talleres Cercanos", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)).background(Color.White, RoundedCornerShape(24.dp)),
                placeholder = { Text("¿Qué servicio buscas?") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryDarkBlue) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            scope.launch { searchWorkshops(searchQuery, cameraPositionState.position.target) }
                        }) { Icon(Icons.Default.Search, null, tint = primaryDarkBlue) }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    scope.launch { searchWorkshops(searchQuery, cameraPositionState.position.target) }
                }),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.Black,
                    unfocusedTextColor = Color.Black,
                    focusedBorderColor = Color.Transparent, 
                    unfocusedBorderColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(16.dp))) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                    uiSettings = MapUiSettings(myLocationButtonEnabled = true, zoomControlsEnabled = false)
                ) {
                    workshops.forEach { ws ->
                        Marker(state = MarkerState(position = LatLng(ws.lat, ws.lon)), title = ws.name, snippet = ws.address)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Talleres encontrados:", color = primaryDarkBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryDarkBlue)
                }
            } else {
                if (workshops.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Text("No se encontraron resultados en esta zona.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                        items(workshops) { ws -> 
                            WorkshopCard(
                                workshop = ws,
                                onClick = {
                                    scope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(ws.lat, ws.lon), 15f))
                                    }
                                },
                                onRouteClick = {
                                    try {
                                        val gmmIntentUri = android.net.Uri.parse("google.navigation:q=${ws.lat},${ws.lon}")
                                        val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No se pudo abrir Google Maps", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) 
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkshopCard(
    workshop: WorkshopResult,
    onClick: () -> Unit,
    onRouteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(Color(0xFF16528E)))
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, null, tint = Color(0xFF16528E), modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(workshop.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, color = Color.Black)
                    Text(workshop.address, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                    Text("★ ${workshop.rating}", color = Color(0xFFFFC107), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(if (workshop.isOpen) "Abierto" else "Cerrado", color = if (workshop.isOpen) Color(0xFF4CAF50) else Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("${String.format("%.1f", workshop.distanceKm)} km", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onRouteClick,
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16528E)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Ruta", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    return r * 2 * atan2(sqrt(a), sqrt(1 - a))
}