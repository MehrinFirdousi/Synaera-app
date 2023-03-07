package com.example.synaera

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.synaera.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    lateinit var users : ArrayList<User>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        users = ArrayList()

        users.add(User("Qusai", "qusai@gmail.com", "123"))

        binding.signupBttn.setOnClickListener{
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        binding.loginBttn.setOnClickListener{
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            for (user in users) {
                if (email.equals(user.email, true) && password == user.password) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }


}