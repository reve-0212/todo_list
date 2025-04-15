package com.example.simjihyun_todo

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.simjihyun_todo.databinding.ActivityTodoBinding
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("TYPE_INTERSECTION_AS_REIFIED_WARNING")
class TodoActivity : AppCompatActivity() {
  private lateinit var binding: ActivityTodoBinding

  //  사용자가 뭔가 변경했는지 확인하기 위해 넣은 변수
  var isModified = false

  var id = 0
  var originalMemo = ""

  @SuppressLint("Recycle", "DefaultLocale", "SetTextI18n")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityTodoBinding.inflate(layoutInflater)
    enableEdgeToEdge()
    setContentView(binding.root)

    setSupportActionBar(binding.toolbar)
    val toolbar = supportActionBar
    toolbar?.setDisplayShowTitleEnabled(false)
    toolbar?.setDisplayHomeAsUpEnabled(true)

    ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    //    TodoListFragment 에서 값 받아오기
    id = intent.getIntExtra("todo_id", -1)
    val startDate = intent.getStringExtra("todo_start_date")
    val endDate = intent.getStringExtra("todo_end_date")
    val name = intent.getStringExtra("todo_name")
    var isImportant = intent.getBooleanExtra("todo_is_important", false)
    val isComplete = intent.getBooleanExtra("todo_is_completed", false)
    val memo = intent.getStringExtra("todo_memo") ?: ""

//    출력
    if (id != -1) {
      Log.d("todoList", "id : $id")
      Log.d("todoList", "startDate : $startDate")
      Log.d("todoList", "endDate : $endDate")
      Log.d("todoList", "name : $name")
      Log.d("todoList", "isImportant : $isImportant")
      Log.d("todoList", "isComplete : $isComplete")
      Log.d("todoList", "memo : $memo")
    }

//    받아온 값으로 내부 화면 바꾸기
    binding.todoName.text = name
    binding.todoCheckbox.isChecked = isComplete
    binding.updateMemo.setText(memo)
    originalMemo = memo

//    중요도에 따라서 별 아이콘 토글
    if (isImportant) {
      binding.blankStar.visibility = View.GONE
      binding.filledStar.visibility = View.VISIBLE
    } else {
      binding.blankStar.visibility = View.VISIBLE
      binding.filledStar.visibility = View.GONE
    }

    //    체크박스를 누르면 isChecked 라면 Y, 아니라면 N 으로 바꾼다 그리고 화면을 새로고침한다
    binding.todoCheckbox.setOnCheckedChangeListener { _, isChecked ->
      val dbHelper = DBHelper(this@TodoActivity)
      val db = dbHelper.writableDatabase
      db.execSQL(
        "update TODO_LIST set is_completed = ? where id = ?",
        arrayOf(if (isChecked) "Y" else "N", id)
      )
      db.close()
      isModified = true
    }

    //    빈 별을 누르면 노란 별로 바꾸고, 중요도를 N 에서 Y 로 수정한다
    binding.blankStar.setOnClickListener {
      binding.blankStar.visibility = View.GONE
      binding.filledStar.visibility = View.VISIBLE

      val dbHelper = DBHelper(this@TodoActivity)
      val db = dbHelper.writableDatabase
      db.execSQL(
        "update TODO_LIST set is_important = 'Y' where id = ?",
        arrayOf(id)
      )
      db.close()
      isModified = true
    }

    binding.filledStar.setOnClickListener {
      binding.blankStar.visibility = View.VISIBLE
      binding.filledStar.visibility = View.GONE

      val dbHelper = DBHelper(this@TodoActivity)
      val db = dbHelper.writableDatabase
      db.execSQL(
        "update TODO_LIST set is_important = 'N' where id = ?",
        arrayOf(id)
      )
      db.close()
      isModified = true
    }

//    날짜, 시간 관련 포매터 및 초기값 설정
//    유저가 수정한 날짜와 시간을 저장한다
    var selectedStartDateTime: LocalDateTime? = null
    var selectedEndDateTime: LocalDateTime? = null

//    db 저장용 포매터
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//    화면 출력용 날짜 포맷
    val dateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//    화면 출력용 시간 포맷
    val timeOnlyFormatter = DateTimeFormatter.ofPattern("HH:mm")

//    초기 텍스트 설정 (yyyy-MM-dd HH:mm:ss 를 공백을 기준으로 나눠서 화면에 출력한다
    binding.updateStartDateText.text = startDate?.split(" ")[0]
    binding.updateEndDateText.text = endDate?.split(" ")[0]
    binding.updateEndTimeText.text = endDate?.split(" ")[1]

    //    시작하는 날짜 바꾸기
    binding.updateStartDateText.setOnClickListener {
      val today = LocalDate.now()

      val datePicker = DatePickerDialog(
        this,
        { _, year, month, day ->
//          currentTime 에 selectedStartDateTime 이 없다면 현재 날짜와 시간을 대입한다
          val currentTime = selectedStartDateTime ?: LocalDateTime.now()
//          기존 시간으 유지하면서 날짜만 바꾼다
//          어떻게 ? currentTime.hour, currentTime.minute
//          -> currentTime에는 selectedEndDate 가 null 이 아닌 이상 내가 저장한 시간값이 있을 텐데
//            그 시간값의 시간(hour)와 분(minute) 을 updatedDateTime 에 넣는다
//          즉, 시간은 가만히 놔두면서 연 월 일만 바꾼다
          val updatedDateTime =
            LocalDateTime.of(year, month + 1, day, currentTime.hour, currentTime.minute)
          selectedStartDateTime = updatedDateTime

//          db헬퍼를 열어서 endDate 를 바꾼다
//          updatedDateTime 을 formatter (yyyy-MM-dd HH:mm:ss) 로 바꾼 값으로 바꿈
          val dbHelper = DBHelper(this@TodoActivity)
          val db = dbHelper.writableDatabase
          db.execSQL(
            "update TODO_LIST set start_date= ? where id = ?",
            arrayOf(updatedDateTime.format(formatter), id)
          )
          db.close()

//          종료 날짜를 yyyy-MM-dd 형식으로 바꾼 뒤에 화면에 출력한다
          binding.updateStartDateText.text = updatedDateTime.format(dateOnlyFormatter)
          isModified = true
          Log.d("todoList", "updateStartDate : $updatedDateTime")
          selectedStartDateTime = updatedDateTime
        },
        today.year, today.monthValue - 1, today.dayOfMonth
      )
      datePicker.datePicker.minDate = System.currentTimeMillis()
      datePicker.show()
    }

//    끝나는 날짜 바꾸기
    binding.updateEndDateText.setOnClickListener {
      val today = LocalDate.now()

      val datePicker = DatePickerDialog(
        this,
        { _, year, month, day ->
//          currentTime 에 selectedEndDateTime 이 없다면 현재 날짜와 시간을 대입한다
          val currentTime = selectedEndDateTime ?: LocalDateTime.now()
//          기존 시간으 유지하면서 날짜만 바꾼다
//          어떻게 ? currentTime.hour, currentTime.minute
//          -> currentTime에는 selectedEndDate 가 null 이 아닌 이상 내가 저장한 시간값이 있을 텐데
//            그 시간값의 시간(hour)와 분(minute) 을 updatedDateTime 에 넣는다
//          즉, 시간은 가만히 놔두면서 연 월 일만 바꾼다
          val updatedDateTime =
            LocalDateTime.of(year, month + 1, day, currentTime.hour, currentTime.minute)
          selectedEndDateTime = updatedDateTime

//          db헬퍼를 열어서 endDate 를 바꾼다
//          updatedDateTime 을 formatter (yyyy-MM-dd HH:mm:ss) 로 바꾼 값으로 바꿈
          val dbHelper = DBHelper(this@TodoActivity)
          val db = dbHelper.writableDatabase
          db.execSQL(
            "update TODO_LIST set end_date= ? where id = ?",
            arrayOf(updatedDateTime.format(formatter), id)
          )
          db.close()

//          종료 날짜를 yyyy-MM-dd 형식으로 바꾼 뒤에 화면에 출력한다
          binding.updateEndDateText.text = updatedDateTime.format(dateOnlyFormatter)
          isModified = true
          Log.d("todoList", "updateEndDate : $updatedDateTime")
        },
        today.year, today.monthValue - 1, today.dayOfMonth
      )
//      startDate 는 yyyy-MM-dd HH:mm:ss 형식의 문자열이다
//      localDateTimeparse 로 localDateTime 형식으로 바꾼 뒤
//      그걸 epoch millis 로 변환해서 minDate 에 넣는다
      val minStartDateTime = selectedStartDateTime ?: LocalDateTime.parse(startDate, formatter)
      val startDateMillis = minStartDateTime
        .atZone(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
      datePicker.datePicker.minDate = startDateMillis
      datePicker.show()
    }

//    시간 바꾸기
    binding.updateEndTimeText.setOnClickListener {
//    currentTime 에 selectedEndDateTime 이 없다면 현재 날짜와 시간을 대입한다
      val current = selectedEndDateTime ?: LocalDateTime.now()

      val timePicker = TimePickerDialog(
        this,
        { _, selectedHour, selectedMinute ->
//          current : 이미 날짜 정보를 포함하고 있는 LocalDateTime
//          .withHour() / .withMinute() : 날짜는 그대로 두고 ,시간만 바뀐 새로운 LocalDateTime 객체 리턴
//          withHour() 와 withMinute 은 시간/ 분만 바꾼 복사본을 반환하는 거라 날짜는 그대로 유지된다
          val updatedDateTime = current.withHour(selectedHour).withMinute(selectedMinute)
          selectedEndDateTime = updatedDateTime
          Log.d("todoList", "시간 : $updatedDateTime")

//          db 헬퍼를 열어서 db에 넣는다
          val dbHelper = DBHelper(this@TodoActivity)
          val db = dbHelper.writableDatabase
          db.execSQL(
            "update TODO_LIST set end_date = ? where id = ? ",
            arrayOf(updatedDateTime.format(formatter), id)
          )
          db.close()

//          updateEndTime 의 text 를 timeOnlyFormatter(HH:mm) 으로 바꿔서 넣는다
          binding.updateEndTimeText.text = updatedDateTime.format(timeOnlyFormatter)
          isModified = true
        }, current.hour, current.minute, true
      )
      timePicker.show()
    }

    binding.deleteTodoLayout.setOnClickListener {
      val dbHelper = DBHelper(this@TodoActivity)
      val db = dbHelper.writableDatabase
      db.execSQL("delete from TODO_LIST where id = ?", arrayOf(id))
      db.close()

      val intent = Intent(this, MainActivity::class.java)
      startActivity(intent)
    }
  }

  //  뒤로가기 누르면 값이 갱신되게 한다
  @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
  override fun onBackPressed() {
    val currentMemo = binding.updateMemo.text.toString()

//  지금 작성한 메모랑 가져온 memo 값이 다르면 업데이트한다
    if (currentMemo != originalMemo) {
      val dbHelper = DBHelper(this@TodoActivity)
      val db = dbHelper.writableDatabase
      db.execSQL("update TODO_LIST set memo = ? where id = ?", arrayOf(currentMemo, id))
      db.close()
      isModified = true
    }

//    업데이트 되었다면 mainActivity 로 간다
    if (isModified) {
      val resultIntent = Intent()
      resultIntent.putExtra("updated", true)
      setResult(RESULT_OK, resultIntent)
    }
    super.onBackPressed()
  }

  //  툴바의 뒤로가기를 누르면 updated 에 true 를 넣어서 result 를 보낸다
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        val currentMemo = binding.updateMemo.text.toString()

//        지금 작성한 메모랑 가져온 memo 값이 다르면 업데이트한다
        if (currentMemo != originalMemo) {
          val dbHelper = DBHelper(this@TodoActivity)
          val db = dbHelper.writableDatabase
          db.execSQL("update TODO_LIST set memo = ? where id = ?", arrayOf(currentMemo, id))
          db.close()
          isModified = true
        }

//        수정되었다면 resultIntent 를 보낸다
        if (isModified) {
          val resultIntent = Intent()
          resultIntent.putExtra("updated", true)
          setResult(RESULT_OK, resultIntent)
        }
        finish()
        true
      }
    }
    return super.onOptionsItemSelected(item)
  }
}