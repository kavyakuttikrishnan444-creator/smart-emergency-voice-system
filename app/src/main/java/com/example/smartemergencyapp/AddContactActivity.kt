package com.example.smartemergencyapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddContactActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)

        val phoneInput = findViewById<EditText>(R.id.etPhone)
        val saveBtn = findViewById<Button>(R.id.btnSave)

        saveBtn.setOnClickListener {
            val phone = phoneInput.text.toString()

            if (phone.length < 10) {
                Toast.makeText(this, "Enter valid number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sharedPref = getSharedPreferences("emergency", MODE_PRIVATE)
            sharedPref.edit().putString("contact", phone).apply()

            Toast.makeText(this, "Emergency contact saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
