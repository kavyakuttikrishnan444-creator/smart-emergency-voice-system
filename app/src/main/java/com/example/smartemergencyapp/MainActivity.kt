package com.example.smartemergencyapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.telephony.SmsManager
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var siren: Ringtone
    private var monitoringActive = false
    private val emergencyPin = "1234"

    private val emergencyNumbers = arrayOf(
        "9747325820",
        "7561815710"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        val audioManager =
            getSystemService(AUDIO_SERVICE) as AudioManager

        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )

        val alarmUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        siren =
            RingtoneManager.getRingtone(applicationContext, alarmUri)

        requestPermissions()

        startForegroundService(
            Intent(this, ForegroundService::class.java)
        )

        findViewById<Button>(R.id.btnEmergency).setOnClickListener {

            monitoringActive = true

            startListening()

            Toast.makeText(
                this,
                "Listening started",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun requestPermissions() {

        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        ActivityCompat.requestPermissions(
            this,
            permissions,
            101
        )
    }

    private fun startListening() {

        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )

        speechRecognizer.setRecognitionListener(
            object : RecognitionListener {

                override fun onResults(results: Bundle?) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    if (matches != null && matches.isNotEmpty()) {

                        val spokenText =
                            matches[0].lowercase()

                        Toast.makeText(
                            this@MainActivity,
                            spokenText,
                            Toast.LENGTH_SHORT
                        ).show()

                        if (spokenText.contains("help")) {

                            triggerEmergency()

                        } else if (spokenText.contains("safe")) {

                            sendSafeMessage()
                        }
                    }

                    if (monitoringActive) {
                        startListening()
                    }
                }

                override fun onError(error: Int) {

                    if (monitoringActive) {
                        startListening()
                    }
                }

                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
        )

        speechRecognizer.startListening(intent)
    }

    private fun triggerEmergency() {

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        if (!siren.isPlaying) {
            siren.play()
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    sendEmergency(location)

                } else {

                    Toast.makeText(
                        this,
                        "Location not found",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun sendEmergency(location: Location) {

        val msg =
            "HELP! I am in danger.\nLocation: https://maps.google.com/?q=${location.latitude},${location.longitude}"

        val smsManager =
            SmsManager.getDefault()

        for (number in emergencyNumbers) {

            smsManager.sendTextMessage(
                number,
                null,
                msg,
                null,
                null
            )
        }

        Toast.makeText(
            this,
            "Emergency SMS Sent",
            Toast.LENGTH_SHORT
        ).show()

        val callIntent =
            Intent(Intent.ACTION_CALL)

        callIntent.data =
            Uri.parse("tel:${emergencyNumbers[0]}")

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {

            startActivity(callIntent)
        }
    }

    private fun sendSafeMessage() {

        val pinInput = EditText(this)
        pinInput.hint = "Enter Emergency PIN"

        AlertDialog.Builder(this)
            .setTitle("Stop Emergency")
            .setMessage("Enter PIN to stop emergency")
            .setView(pinInput)

            .setCancelable(false)

            .setPositiveButton("OK") { _, _ ->

                val enteredPin = pinInput.text.toString()

                if (enteredPin == emergencyPin) {

                    monitoringActive = false

                    try {
                        speechRecognizer.stopListening()
                        speechRecognizer.cancel()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    try {
                        if (siren.isPlaying) {
                            siren.stop()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val safeMsg = "I am safe now. Emergency resolved."

                    val smsManager = SmsManager.getDefault()

                    for (number in emergencyNumbers) {

                        smsManager.sendTextMessage(
                            number,
                            null,
                            safeMsg,
                            null,
                            null
                        )
                    }

                    Toast.makeText(
                        this,
                        "Emergency stopped successfully",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "Incorrect PIN! Emergency continues.",
                        Toast.LENGTH_LONG
                    ).show()

                    monitoringActive = true
                    startListening()
                }
            }

            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                monitoringActive = true
                startListening()
            }
                .show()
    }

    override fun onDestroy() {
        super.onDestroy()

        speechRecognizer.destroy()
    }
}