package com.example.simjihyun_todo

import android.annotation.SuppressLint
import android.app.ComponentCaller
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*시작하는 날짜에 오늘 날짜와 시간 말고도 미래 날짜와 시간을 넣을 수 있게 해서
* 목록 프래그먼트에는 오늘 해야할 일만
* 달력 프래그먼트를 만들어서 향후 해야 할일
* 마지막으로 기록 프래그먼트에서 지금까지 한 모든 일을 출력할 수 있게 하기*/

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

//    다음 할일 버튼을 누르면 다음 할일 버튼으로 이동한다
    binding.futureTodoListShow.setOnClickListener {
      supportFragmentManager.commit {
        setReorderingAllowed(true)
//        replace<FutureTodoListFragment>(R.id.todo_fragment_container)
      }
    }


//    할일 추가 버튼을 누르면 입력 다이얼로그가 나온다
    binding.writeTodoList.setOnClickListener {
      Log.d("todoList", "writeTodoList 누름")

      val sheetBinding = LayoutBottomSheetBinding.inflate(layoutInflater)
      val dialog = BottomSheetDialog(this)
      dialog.setContentView(sheetBinding.root)

//      오늘 날짜 구해서 yyyy-MM-dd 형식으로 바꾼 후 시간을 설정한다
      var selectedStartDate: LocalDateTime = LocalDate.now().atTime(0, 0, 0)
      var selectedEndDate: LocalDateTime = LocalDate.now().atTime(23, 59, 59)
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      val dateFormatter = DateTimeFormatter.ofPattern("MM-dd")
      val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

//      출력
      Log.d("todoList", "selectedStartDate : ${selectedStartDate.format(formatter)}")
      Log.d("todoList", "selectedEndDate : ${selectedEndDate.format(formatter)}")

//      시작일도 바꿀 수 있게
      sheetBinding.startDateButton.setOnClickListener {
        val datePicker = DatePickerDialog(
          this,
          { _, year, month, day ->
            selectedStartDate = selectedStartDate.withYear(year)
              .withMonth(month + 1)
              .withDayOfMonth(day)

            Log.d("todoList", "changed selectedStartDate : $selectedStartDate")
            sheetBinding.startDateButton.text = selectedStartDate.format(dateFormatter)

            Log.d("todoList", "changed selectedEndDate : $selectedEndDate")
            selectedEndDate = selectedStartDate.withHour(23).withMinute(59).withSecond(59)
            sheetBinding.endDateButton.text = selectedEndDate.format(dateFormatter)
          }, selectedStartDate.year, selectedStartDate.monthValue - 1, selectedStartDate.dayOfMonth
        )
//        최소 날짜는 오늘날짜
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
      }

//      밑에 있는 달력 버튼을 누르면 날짜를 설정할 수 있게 datePickerDialog 가 나온다
//      설정한 날짜를 yyyy-MM-dd 형식으로 바꾼 뒤 selectedEndDate 에 저장한다
      sheetBinding.endDateButton.setOnClickListener {
        val datePicker = DatePickerDialog(
          this,
          { _, year, month, day ->
            selectedEndDate = selectedEndDate.withYear(year)
              .withMonth(month + 1)
              .withDayOfMonth(day)

            Log.d("todoList", "changed selectedEndData : $selectedEndDate")
            sheetBinding.endDateButton.text = selectedEndDate.format(dateFormatter)
          },
          selectedEndDate.year, selectedEndDate.monthValue - 1, selectedEndDate.dayOfMonth
        )

//        지금 시간을 instant->밀리초 단위로 변환한다
//        그리고 최소 날짜를 millis 로 설정한다
        val millis =
          selectedStartDate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        datePicker.datePicker.minDate = millis
        datePicker.show()
      }

//      시계 버튼을 누르면 시간을 설정할 수 있는 timePicker 가 나온다
//      설정한 날자에서 HH:mm:ss 값을 수정하고 selectedEndDate 에 저장한다
      sheetBinding.endTimeButton.setOnClickListener {
        val currentHour = selectedEndDate.hour
        val currentMinute = selectedEndDate.minute

        val timePicker = TimePickerDialog(
          this,
          { _, hour, minute ->
            selectedEndDate = selectedEndDate.withHour(hour).withMinute(minute).withSecond(0)
            Log.d("todoList", "시간 : ${selectedEndDate.format(formatter)}")
            sheetBinding.endTimeButton.text = selectedEndDate.format(timeFormatter)
          }, currentHour, currentMinute, true
        )
        timePicker.show()
      }

//      저장 버튼을 누르면 name 이 null 이 아닐 경우에만 db에 저장한다
      sheetBinding.btnSave.setOnClickListener {
        val name = sheetBinding.inputName.text.toString()

        if (name.isNotBlank()) {
          val dbHelper = DBHelper(this)
          val db = dbHelper.writableDatabase
          db.execSQL(
            "insert into TODO_LIST(name,start_date, end_date) values (?,?,?)",
            arrayOf(name, selectedStartDate.format(formatter), selectedEndDate.format(formatter))
          )
          db.close()

          //프래그먼트 새로고침
          supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<TodoListFragment>(R.id.todo_fragment_container)
          }
        } else {
          Log.d("todoList", "이름 입력")
          val builder = AlertDialog.Builder(this@MainActivity)
          builder.setTitle("이름을 입력해주세요")
          builder.setPositiveButton("확인", null)
          builder.create()
          builder.show()
        }
        dialog.dismiss()
      }
      dialog.show()
    }
  }

  //  resultCode 가 result_ok 고 updated 가 true 면
//  fragment 를 새로고침해서 보여준다
  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    caller: ComponentCaller
  ) {
    super.onActivityResult(requestCode, resultCode, data, caller)
    if (resultCode == RESULT_OK && data?.getBooleanExtra("updated", false) == true) {
      supportFragmentManager.commit {
        setReorderingAllowed(true)
        replace<TodoListFragment>(R.id.todo_fragment_container)
      }
    }
  }

  fun setToolbarTitle(title: String) {
    binding.toolbar.title = title
  }
}