package com.example.autodocgt

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val notificationsEnabled = sharedPrefs.getBoolean("notifications_enabled", true)
            if (!notificationsEnabled) {
                return Result.success()
            }

            val auth = Firebase.auth
            val currentUser = auth.currentUser ?: return Result.success()

            val db = Firebase.firestore
            val snapshot = db.collection("usuarios")
                .document(currentUser.uid)
                .collection("documentos")
                .whereEqualTo("recordatorioActivo", true)
                .get()
                .await()

            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time

            for (doc in snapshot.documents) {
                val nombre = doc.getString("nombre") ?: "Recordatorio"
                val fechaStr = doc.getString("fecha_vencimiento") ?: ""
                
                if (fechaStr.isEmpty()) continue

                val date = formatter.parse(fechaStr) ?: continue
                val diff = date.time - today.time
                val days = TimeUnit.MILLISECONDS.toDays(diff)

                if (days == 7L) {
                    sendNotification(doc.id.hashCode(), nombre, "Tu recordatorio vence en 1 semana ($fechaStr).")
                } else if (days == 3L) {
                    sendNotification(doc.id.hashCode(), nombre, "Tu recordatorio vence en 3 días ($fechaStr).")
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.retry()
        }
    }

    private fun sendNotification(notificationId: Int, title: String, content: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, "reminders_channel")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(notificationId, builder.build())
        }
    }
}
