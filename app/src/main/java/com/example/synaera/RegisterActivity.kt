package com.example.synaera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.synaera.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    var users = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = DatabaseHelper(this)

        val binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.signupBttn.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val name = binding.nameEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()
            val passwordConfirm = binding.confirmPasswordEditText.text.toString()

            if (email != "" && name != "" && password != "" && passwordConfirm == password) {
                db.addUser(User(0, email, name, password))
                finish()
            }
        }


    }
}