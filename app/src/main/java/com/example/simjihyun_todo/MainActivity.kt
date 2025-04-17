package com.example.simjihyun_todo

import RecordTodoFragment
import RefactoredTodoListFragment
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//AppCompatActivity 를 상속하고 레이아웃 리소스로 activity_main 을 사용한다
class MainActivity : AppCompatActivity(R.layout.activity_main) {
  //  view Binding 사용을 위해 변수를 선언한다
  private lateinit var binding: ActivityMainBinding

  @SuppressLint("Recycle")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    enableEdgeToEdge()

//    툴바의 기본 앲 이름 나오는걸 없애고 오늘 할일로 수정한다
    val toolbarBodyTemplate = binding.toolbar
//    툴바를 앱의 액션바로 설정하고, 앱 이름 대신 "오늘 할일" 이라는 타이틀을 표시한다
    setSupportActionBar(toolbarBodyTemplate)
    supportActionBar?.setDisplayShowTitleEnabled(false)
    toolbarBodyTemplate.title = "오늘 할 일"

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

//    다음 할일 버튼을 누르면 다음 할일 프래그먼트로 이동한다
    binding.futureTodoListShow.setOnClickListener {
      supportFragmentManager.commit {
        setReorderingAllowed(true)
        replace<FutureTodoListFragment>(R.id.todo_fragment_container)
      }
    }

//    기록 버튼을 누르면 기록 프래그먼트로 이동한다
    binding.completedListShow.setOnClickListener {
      supportFragmentManager.commit{
        setReorderingAllowed(true)
        replace<RecordTodoFragment>(R.id.todo_fragment_container)
      }
    }


//    할일 추가 버튼을 누르면 BottomSheetDialog 가 나온다
    binding.writeTodoList.setOnClickListener {

//      현재 화면의 프래그먼트 확인 (나중에 새로고침 할 때 필요)
      val currentFragment = supportFragmentManager.findFragmentById(R.id.todo_fragment_container)

//      bottomSheetBinding 과 연결한다
      val sheetBinding = LayoutBottomSheetBinding.inflate(layoutInflater)
//      BottomSheet 형식으로 할일 추가 다이얼로그를 출력한다
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
//            selectedStartDate 는 내가 고른 year, month, day 값이 들어간다
            selectedStartDate = selectedStartDate.withYear(year)
              .withMonth(month + 1)
              .withDayOfMonth(day)

//            startDateButton 의 text 값을 dateFormatter(MM-dd) 형식으로 바꾼다
            Log.d("todoList", "changed selectedStartDate : $selectedStartDate")
            sheetBinding.startDateButton.text = selectedStartDate.format(dateFormatter)

//            선택한 시작 날짜에 맞춰 종료 날짜 기본값을 23:59:59 로 설정한다
            Log.d("todoList", "changed selectedEndDate : $selectedEndDate")
            selectedEndDate = selectedStartDate.withHour(23).withMinute(59).withSecond(59)

//            endDateButton 의 text 값을 dateFormatter(MM-dd)형식으로 바꾼다
            sheetBinding.endDateButton.text = selectedEndDate.format(dateFormatter)
          }, selectedStartDate.year, selectedStartDate.monthValue - 1, selectedStartDate.dayOfMonth
        )
//        시작일 datePicker 의 최소 날짜는 오늘 날짜로 한다
        datePicker.datePicker.minDate = System.currentTimeMillis()
        datePicker.show()
      }

//      밑에 있는 달력 버튼을 누르면 날짜를 설정할 수 있게 datePickerDialog 가 나온다
//      설정한 날짜를 yyyy-MM-dd 형식으로 바꾼 뒤 selectedEndDate 에 저장한다
      sheetBinding.endDateButton.setOnClickListener {
        val datePicker = DatePickerDialog(
          this,
          { _, year, month, day ->
//            selectedEndDate 에 내가 고른 연도, 월, 일을 넣는다
            selectedEndDate = selectedEndDate.withYear(year)
              .withMonth(month + 1)
              .withDayOfMonth(day)

//            endDateButton 의 text 를 selectedEndDate 를 (MM-dd) 로 바꾼 형식으로 넣는다
            Log.d("todoList", "changed selectedEndData : $selectedEndDate")
            sheetBinding.endDateButton.text = selectedEndDate.format(dateFormatter)
          },
          selectedEndDate.year, selectedEndDate.monthValue - 1, selectedEndDate.dayOfMonth
        )

//        내가 설정한 startDate 를 내 시스템 시간대(한국/seoul) -> instant -> 밀리초로 바꾼다
//        이걸 datePickerDialog 로 바꿔서 그 날짜 이전을 선택 못하게 함
        val millis =
          selectedStartDate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        datePicker.datePicker.minDate = millis
        datePicker.show()
      }

//      시계 버튼을 누르면 시간을 설정할 수 있는 timePicker 가 나온다
//      설정한 날자에서 HH:mm:ss 값을 수정하고 selectedEndDate 에 저장한다
      sheetBinding.endTimeButton.setOnClickListener {
//        currentHour 와 currentMinute 에 selectedEndDate 에서 넣은 시간과 분 값을 저장한다
        val currentHour = selectedEndDate.hour
        val currentMinute = selectedEndDate.minute

        val timePicker = TimePickerDialog(
          this,
          { _, hour, minute ->
//            selectedEndDate 에 내가 고른 시간, 분, 초 값을 넣는다
            selectedEndDate = selectedEndDate.withHour(hour).withMinute(minute).withSecond(0)
            Log.d("todoList", "시간 : ${selectedEndDate.format(formatter)}")
//            endTimeButton 의 text 값을 hh:mm 식으로 바꾼다
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

//            지금 프래그먼트가 어디인지에 따라 새로 고침하는 프래그먼트가 달라진다
            when (currentFragment) {
              is FutureTodoListFragment -> {
                replace<FutureTodoListFragment>(R.id.todo_fragment_container)
              }
              else -> {
                replace<TodoListFragment>(R.id.todo_fragment_container)
              }
            }
          }
        } else {
//          이름이 공백이면 실행한다
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

  //  다른 화면(액티비티)에서 돌아올 때 거기서 어떠한 작업을 완료한 경우 프래그먼트를 새로 불러와서 새로고침한다
//  onActivityResult() : 다른 액티비티를 띄웠다가 결과를 받아오는 메서드
  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?,
    caller: ComponentCaller
  ) {
    super.onActivityResult(requestCode, resultCode, data, caller)
//  만일 다른 액티비티에서 결과를 result_ok 로 주고 data 라는 intent 에 updated = true 가 들어있다면
//  프래그먼트를 다시 불러온다 (새로고침)
    if (resultCode == RESULT_OK && data?.getBooleanExtra("updated", false) == true) {
      supportFragmentManager.commit {
        setReorderingAllowed(true)
        replace<TodoListFragment>(R.id.todo_fragment_container)
      }
    }
  }

  //  다른 프래그먼트에서 툴바 타이틀을 바꾼다
  fun setToolbarTitle(title: String) {
    binding.toolbar.title = title
  }
}