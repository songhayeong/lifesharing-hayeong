package com.example.lifesharing.regist

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.lifesharing.MainActivity
import com.example.lifesharing.databinding.ActivityRegistAddBinding
import com.example.lifesharing.databinding.ActivityRegistFinishBinding

class Regist_Finish_Activity :AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityRegistFinishBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registFinishBackbtn.setOnClickListener{
            val intent = Intent(this, ActivityRegistAddBinding::class.java)
            startActivity(intent)
        }

        binding.registFinishHomebtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }
}