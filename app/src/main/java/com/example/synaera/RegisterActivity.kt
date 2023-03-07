package com.example.synaera

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.synaera.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    var users = ArrayList<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }
}