//package com.example.simjihyun_todo
//
//import android.annotation.SuppressLint
//import android.app.DatePickerDialog
//import android.os.Bundle
//import android.util.Log
//import androidx.fragment.app.Fragment
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toolbar
//import com.example.simjihyun_todo.databinding.FragmentFutureTodoListBinding
//import com.prolificinteractive.materialcalendarview.CalendarDay
//import java.time.LocalDate
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//
//class FutureTodoListFragment : Fragment() {
//  private var futureTodoListBinding: FragmentFutureTodoListBinding? = null
//  private val binding get() = futureTodoListBinding!!
//
//  private lateinit var dbHelper: DBHelper
//
//  override fun onCreateView(
//    inflater: LayoutInflater, container: ViewGroup?,
//    savedInstanceState: Bundle?
//  ): View? {
//    futureTodoListBinding = FragmentFutureTodoListBinding.inflate(inflater, container, false)
//    return binding.root
//  }
//
//  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//    super.onViewCreated(view, savedInstanceState)
//    (activity as? MainActivity)?.setToolbarTitle("다음에 할 일")
//
//    val calendarView = binding.calendarView
//    val todoList = binding.todoList
//
//    calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
//      val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
//      val dbHelper = DBHelper(requireContext())
//      val db = dbHelper.readableDatabase
//
//      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
//      val cursor = db.rawQuery(
//        "SELECT name FROM TODO_LIST WHERE DATE(start_date) = ?",
//        arrayOf(selectedDate.format(formatter))
//      )
//
//      val todos = mutableListOf<String>()
//      while (cursor.moveToNext()) {
//        todos.add(cursor.getString(0))
//      }
//
//      cursor.close()
//      db.close()
//
//      if (todos.isEmpty()) {
//        todoList.text = "할 일이 없습니다"
//      } else {
//        todoList.text = todos.joinToString("\n")
//      }
//    }
//  }
//
//}
//
//@SuppressLint("Recycle")
//private fun markTodoDatesOnCalendar() {
//  val db = dbHelper.readableDatabase
//  val cursor = db.rawQuery("select distinct start_date from TODO_LIST", null)
//  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//
//  while (cursor.moveToNext()) {
//    val startDateString = cursor.getString(0)
//    val date = LocalDateTime.parse(startDateString, formatter).toLocalDate()
//
////      마커
//    val calendarDay = CalendarDay.from(date.year, date.monthValue - 1, date.dayOfMonth)
//    binding.calendarView.addDecorator(DateDecorator(requireContext(), calendarDay))
//  }
//  cursor.close()
//  db.close()
//}
//
//@SuppressLint("Recycle")
//private fun showTodosForDate(date: LocalDate) {
//  val db = dbHelper.readableDatabase
//  val start = date.atTime(0, 0)
//  val end = date.atTime(23, 59, 59)
//  val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//
//  val cursor = db.rawQuery(
//    "select name from TODO_LIST where start_date between ? and ?",
//    arrayOf(start.format(formatter), end.format(formatter))
//  )
//
//  val result = mutableListOf<String>()
//  while (cursor.moveToNext()) {
//    result.add(cursor.getString(0))
//  }
//  cursor.close()
//  db.close()
//
//  Log.d("todoList", result.joinToString("\n"))
//}
//
//override fun onDestroyView() {
//  super.onDestroyView()
//  futureTodoListBinding = null
//}
//}