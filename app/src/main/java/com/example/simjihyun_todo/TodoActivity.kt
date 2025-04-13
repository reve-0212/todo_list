package com.example.simjihyun_todo

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.simjihyun_todo.databinding.ActivityTodoBinding

class TodoActivity : AppCompatActivity() {
  private lateinit var binding: ActivityTodoBinding
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityTodoBinding.inflate(layoutInflater)
    enableEdgeToEdge()
    setContentView(binding.root)

    ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    val id = intent.getIntExtra("todo_id", -1)
    val startDate = intent.getStringExtra("todo_start_date")
    val endDate = intent.getStringExtra("todo_end_date")
    val name = intent.getStringExtra("todo_name")
    val isImportant = intent.getStringExtra("todo_is_important")
    val isComplete = intent.getStringExtra("todo_is_complete")

    if (id != -1) {
      Log.d("todoList", "id : $id")
    }

  }
}