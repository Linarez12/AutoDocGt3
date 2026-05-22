package com.example.autodocgt

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.autodocgt.ui.theme.AutoDocGtTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminders_channel",
                "Recordatorios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones de recordatorios"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ReminderWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        val auth = Firebase.auth
        val startScreen = if (auth.currentUser != null) "home" else "login"
        
        setContent {
            AutoDocGtTheme {
                var currentScreen by remember { mutableStateOf(startScreen) }
                var selectedVehicleForDetails by remember { mutableStateOf<Map<String, Any>?>(null) }
                var selectedDocumentForDetails by remember { mutableStateOf<Map<String, Any>?>(null) }
                var selectedDocumentVehicleName by remember { mutableStateOf("") }
                var selectedMaintenanceForDetails by remember { mutableStateOf<Map<String, Any>?>(null) }
                var selectedMaintenanceVehicleName by remember { mutableStateOf("") }

                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val modifierWithPadding = Modifier.padding(innerPadding)

                    when (currentScreen) {
                        "login" -> {
                            LoginScreen(
                                modifier = modifierWithPadding,
                                onLoginSuccess = { currentScreen = "home" },
                                onNavigateToRegister = { currentScreen = "register" }
                            )
                        }
                        "register" -> {
                            BackHandler { currentScreen = "login" }
                            RegisterScreen(
                                modifier = modifierWithPadding,
                                onRegisterSuccess = { currentScreen = "home" },
                                onBackToLogin = { currentScreen = "login" }
                            )
                        }
                        "home" -> {
                            HomeScreen(
                                modifier = modifierWithPadding,
                                onNavigateToSettings = { currentScreen = "settings" },
                                onNavigateToMaintenance = { currentScreen = "maintenance" },
                                onNavigateToDocuments = { currentScreen = "documents" },
                                onNavigateToReminders = { currentScreen = "reminders" },
                                onNavigateToExpenses = { currentScreen = "expenses" },
                                onNavigateToAddVehicle = { currentScreen = "add_vehicle" },
                                onNavigateToVehicleDetails = { vehicle ->
                                    selectedVehicleForDetails = vehicle
                                    currentScreen = "vehicle_details"
                                }
                            )
                        }
                        "settings" -> {
                            BackHandler { currentScreen = "home" }
                            SettingsScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onLogout = { currentScreen = "login" },
                                onMyAccountClick = { currentScreen = "my_account" },
                                onMyVehiclesClick = { currentScreen = "my_vehicles" }
                            )
                        }
                        "my_account" -> {
                            BackHandler { currentScreen = "settings" }
                            MyAccountScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "settings" },
                                onLogout = { currentScreen = "login" }
                            )
                        }
                        "my_vehicles" -> {
                            BackHandler { currentScreen = "settings" }
                            MyVehiclesScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "settings" },
                                onNavigateToDetails = { vehicle ->
                                    selectedVehicleForDetails = vehicle
                                    currentScreen = "vehicle_details"
                                }
                            )
                        }
                        "maintenance" -> {
                            BackHandler { currentScreen = "home" }
                            MaintenanceScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onDocumentsClick = { currentScreen = "documents" },
                                onRemindersClick = { currentScreen = "reminders" },
                                onExpensesClick = { currentScreen = "expenses" },
                                onNearbyWorkshopsClick = { currentScreen = "nearby_workshops" },
                                onNavigateToAddMaintenance = { currentScreen = "add_maintenance" },
                                onNavigateToDetails = { mantenimiento, vehicleStr ->
                                    selectedMaintenanceForDetails = mantenimiento
                                    selectedMaintenanceVehicleName = vehicleStr
                                    currentScreen = "mantenimiento_detalles"
                                }
                            )

                        }
                        "reminders" -> {
                            BackHandler { currentScreen = "home" }
                            RemindersScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onMaintenanceClick = { currentScreen = "maintenance" },
                                onDocumentsClick = { currentScreen = "documents" },
                                onExpensesClick = { currentScreen = "expenses" }
                            )
                        }
                        "documents" -> {
                            BackHandler { currentScreen = "home" }
                            DocumentsScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onMaintenanceClick = { currentScreen = "maintenance" },
                                onRemindersClick = { currentScreen = "reminders" },
                                onExpensesClick = { currentScreen = "expenses" },
                                onNavigateToAddDocument = { currentScreen = "add_document" },
                                onNavigateToDocumentDetails = { doc, vehicleLabel ->
                                    selectedDocumentForDetails = doc
                                    selectedDocumentVehicleName = vehicleLabel
                                    currentScreen = "document_details"
                                }
                            )
                        }
                        "document_details" -> {
                            BackHandler { currentScreen = "documents" }
                            if (selectedDocumentForDetails != null) {
                                DocumentDetailsScreen(
                                    document = selectedDocumentForDetails!!,
                                    vehicleName = selectedDocumentVehicleName,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "documents" }
                                )
                            } else {
                                currentScreen = "documents"
                            }
                        }
                        "expenses" -> {
                            BackHandler { currentScreen = "home" }
                            ExpensesScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" },
                                onHomeClick = { currentScreen = "home" },
                                onMaintenanceClick = { currentScreen = "maintenance" },
                                onDocumentsClick = { currentScreen = "documents" },
                                onRemindersClick = { currentScreen = "reminders" },
                                onNavigateToAddExpense = { currentScreen = "add_expense" },
                                onNavigateToReport = { currentScreen = "reporte_gastos" }
                            )
                        }
                        "reporte_gastos" -> {
                            BackHandler { currentScreen = "expenses" }
                            ReporteGastosScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "expenses" },
                                onDetailsClick = { gasto, vehicleName ->
                                    selectedMaintenanceForDetails = gasto
                                    selectedMaintenanceVehicleName = vehicleName
                                    val tipoCategoria = gasto["tipoCategoria"] as? String ?: ""
                                    when (tipoCategoria) {
                                        "Combustible" -> currentScreen = "combustible_detalles"
                                        "Mantenimiento" -> currentScreen = "mantenimiento_detalles_from_report"
                                        else -> currentScreen = "otros_gastos_detalles"
                                    }
                                }
                            )
                        }
                        "add_expense" -> {
                            BackHandler { currentScreen = "expenses" }
                            AddExpenseScreen(
                                initialTab = 0,
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "expenses" }
                            )
                        }
                        "add_maintenance" -> {
                            BackHandler { currentScreen = "maintenance" }
                            AddExpenseScreen(
                                initialTab = 1,
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "maintenance" }
                            )
                        }
                        "add_document" -> {
                            BackHandler { currentScreen = "documents" }
                            AddDocumentScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "documents" }
                            )
                        }
                        "add_vehicle" -> {
                            BackHandler { currentScreen = "home" }
                            AddVehicleScreen(
                                modifier = modifierWithPadding,
                                onBack = { currentScreen = "home" }
                            )
                        }
                        "vehicle_details" -> {
                            BackHandler { currentScreen = "home" }
                            if (selectedVehicleForDetails != null) {
                                VehicleDetailsScreen(
                                    vehicle = selectedVehicleForDetails!!,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "home" },
                                    onEdit = { vehicle ->
                                        selectedVehicleForDetails = vehicle
                                        currentScreen = "edit_vehicle"
                                    }
                                )
                            } else {
                                currentScreen = "home"
                            }
                        }
                        "edit_vehicle" -> {
                            BackHandler { currentScreen = "vehicle_details" }
                            if (selectedVehicleForDetails != null) {
                                EditVehicleScreen(
                                    vehicle = selectedVehicleForDetails!!,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "home" }
                                )
                            } else {
                                currentScreen = "home"
                            }
                        }
                        "nearby_workshops" -> {
                            BackHandler { currentScreen = "maintenance" }
                            NearbyWorkshopsScreen(
                                onBack = { currentScreen = "maintenance" },
                                modifier = modifierWithPadding
                            )
                        }
                        "mantenimiento_detalles" -> {
                            BackHandler { currentScreen = "maintenance" }
                            if (selectedMaintenanceForDetails != null) {
                                MantenimientoDetalles(
                                    mantenimiento = selectedMaintenanceForDetails!!,
                                    vehiculoStr = selectedMaintenanceVehicleName,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "maintenance" }
                                )
                            } else {
                                currentScreen = "maintenance"
                            }
                        }
                        "mantenimiento_detalles_from_report" -> {
                            BackHandler { currentScreen = "reporte_gastos" }
                            if (selectedMaintenanceForDetails != null) {
                                MantenimientoDetalles(
                                    mantenimiento = selectedMaintenanceForDetails!!,
                                    vehiculoStr = selectedMaintenanceVehicleName,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "reporte_gastos" }
                                )
                            } else {
                                currentScreen = "reporte_gastos"
                            }
                        }
                        "combustible_detalles" -> {
                            BackHandler { currentScreen = "reporte_gastos" }
                            if (selectedMaintenanceForDetails != null) {
                                CombustibleDetalles(
                                    gasto = selectedMaintenanceForDetails!!,
                                    vehiculoStr = selectedMaintenanceVehicleName,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "reporte_gastos" }
                                )
                            } else {
                                currentScreen = "reporte_gastos"
                            }
                        }
                        "otros_gastos_detalles" -> {
                            BackHandler { currentScreen = "reporte_gastos" }
                            if (selectedMaintenanceForDetails != null) {
                                OtrosGastosDetalles(
                                    gasto = selectedMaintenanceForDetails!!,
                                    vehiculoStr = selectedMaintenanceVehicleName,
                                    modifier = modifierWithPadding,
                                    onBack = { currentScreen = "reporte_gastos" }
                                )
                            } else {
                                currentScreen = "reporte_gastos"
                            }
                        }
                    }
                }

            }
        }
    }
}
