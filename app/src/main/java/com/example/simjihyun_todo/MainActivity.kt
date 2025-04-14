package com.example.simjihyun_todo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.add
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.example.simjihyun_todo.databinding.ActivityMainBinding
import com.example.simjihyun_todo.databinding.LayoutBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity(R.layout.activity_main) {
  private lateinit var binding: ActivityMainBinding

  @SuppressLint("Recycle")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    enableEdgeToEdge()

//    시스템 UI 에 맞게 padding 조절
    ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

//    처음 앱을 켰을 때 할일 목록 프래그먼트를 화면에 추가한다
    if (savedInstanceState == null) {
      supportFragmentManager.commit {
        setReorderingAllowed(true)
        add<TodoListFragment>(R.id.todo_fragment_container)
      }
    }

//    목록 버튼 누르면 목록 프래그먼트로 이동한다
    binding.todoListShow.setOnClickListener {
      supportFragmentManager.commit {
        setReorderingAllowed(true)
        replace<TodoListFragment>(R.id.todo_fragment_container)
      }
    }

//    할일 추가 버튼을 누르면 입력 다이얼로그가 나온다
    binding.writeTodoList.setOnClickListener {
      Log.d("todoList", "writeTodoList 누름")

      val sheetBinding = LayoutBottomSheetBinding.inflate(layoutInflater)
      val dialog = BottomSheetDialog(this)
      dialog.setContentView(sheetBinding.root)

//      오늘 날짜 구해서 yyyy-MM-dd 형식으로 바꾼다
      val today = LocalDate.now()
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
      val selectedStartDate = today.format(formatter)
      var selectedEndDate = selectedStartDate

//      출력
      Log.d("todoList", "selectedStartDate : $selectedStartDate")
      Log.d("todoList", "selectedEndDate : $selectedEndDate")

//      밑에 있는 달력 버튼을 누르면 날짜를 설정할 수 있게 datePickerDialog 가 나온다
//      설정한 날짜를 yyyy-MM-dd 형식으로 바꾼 뒤 selectedEndDate 에 저장한다
      sheetBinding.endDateButton.setOnClickListener {
        val datePicker = DatePickerDialog(
          this,
          { _, year, month, day ->
            val picked = LocalDate.of(year, month + 1, day)
            selectedEndDate = picked.format(formatter)
            Log.d("todoList", "changed selectedEndData : $selectedEndDate")
          },
          today.year, today.monthValue - 1, today.dayOfMonth
        )
        datePicker.show()
      }

//      저장 버튼을 누르면 name 이 null 이 아닐 경우에만 db에 저장한다
      sheetBinding.btnSave.setOnClickListener {
        val name = sheetBinding.inputName.text.toString()
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val startDateStr = formatter.format(selectedStartDate)
        val endDateStr = formatter.format(selectedEndDate)

        if (name.isNotBlank()) {
          val dbHelper = DBHelper(this)
          val db = dbHelper.writableDatabase
          db.execSQL(
            "insert into TODO_LIST(name,start_date, end_date) values (?,?,?)",
            arrayOf(name, startDateStr, endDateStr)
          )
          db.close()

          //프래그먼트 새로고침
          supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<TodoListFragment>(R.id.todo_fragment_container)
          }
        }
        dialog.dismiss()
      }
      dialog.show()
    }
  }
}