package com.example.simjihyun_todo.refactored

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simjihyun_todo.DBHelper
import com.example.simjihyun_todo.FutureTodoAdapter
import com.example.simjihyun_todo.MainActivity
import com.example.simjihyun_todo.R
import com.example.simjihyun_todo.TodoActivity
import com.example.simjihyun_todo.TodoItem
import com.example.simjihyun_todo.databinding.FragmentFutureTodoListBinding
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class RefactoredFutureTodoListFragment : Fragment() {
  //  뷰 바인딩용
  private var futureTodoListBinding: FragmentFutureTodoListBinding? = null

  //  메모리 누수 방지용
  private val futureBinding get() = futureTodoListBinding!!

  //  내가 고른 날짜
  private var selectedDate: String = ""

  //  어댑터
  lateinit var futureAdapter: FutureTodoAdapter
  lateinit var completedAdapter: FutureTodoAdapter

//  resultCode 가 OK 면 내가 고른 날짜로 새로고침한다
  private val todoActivityLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val isUpdated = result.data?.getBooleanExtra("updated", false) ?: false
      if (isUpdated) {
        reload(selectedDate)
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    futureTodoListBinding = FragmentFutureTodoListBinding.inflate(inflater, container, false)
    (activity as? MainActivity)?.setToolbarTitle("다음에 할 일")
    return futureBinding.root
  }

  @SuppressLint("Recycle")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    val calendar: Calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

//    selectedDate 를 받는다. 만약에 없다면 오늘 날짜로 설정한다
    selectedDate = arguments?.getString("selectedDate") ?: dateFormat.format(calendar.time)
    futureBinding.calendarView.date = dateFormat.parse(selectedDate)?.time ?: calendar.timeInMillis

//    selectedDate 의 데이터들을 가져온다
    loadTodosForDate(selectedDate)

//    recycler를 연결한다
    val futureRecycler = futureBinding.futureTodoRecycler
    val futureCompletedRecycler = futureBinding.futureCompletedRecycler

//    아직 완료안한 아이템을 왼쪽으로 밀면 삭제를 가능하게 한다
//    또한, recycler 를 새로고침한다
    val itemTouchHelper = createSwipeToDeleteHelper(
      futureBinding.futureTodoRecycler,
      getItem = { pos -> futureAdapter.getItem(pos) },
      onDelete = { pos ->
        val id = futureAdapter.getItem(pos).id
        val dbHelper = DBHelper(requireContext())
        val db = dbHelper.writableDatabase
        db.execSQL("delete from TODO_LIST where id =?", arrayOf(id))
        db.close()
        loadTodosForDate(selectedDate)
      })
    itemTouchHelper.attachToRecyclerView(futureBinding.futureTodoRecycler)

//    위와 같다
    val completedTouchHelper = createSwipeToDeleteHelper(
      futureBinding.futureCompletedRecycler,
      getItem = { pos -> completedAdapter.getItem(pos) },
      onDelete = { pos ->
        val id = completedAdapter.getItem(pos).id
        val dbHelper = DBHelper(requireContext())
        val db = dbHelper.writableDatabase
        db.execSQL("delete from TODO_LIST where id = ?", arrayOf(id))
        db.close()
        reload(selectedDate)
      }
    )
    completedTouchHelper.attachToRecyclerView(futureBinding.futureCompletedRecycler)

    futureRecycler.layoutManager = LinearLayoutManager(requireContext())
    futureCompletedRecycler.layoutManager = LinearLayoutManager(requireContext())

//    캘린더뷰 관련 기능
    futureBinding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
      calendar.set(Calendar.YEAR, year)
      calendar.set(Calendar.MONTH, month)
      calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
      val selected = dateFormat.format(calendar.time)

      loadTodosForSelectedDate(selected)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    futureTodoListBinding = null
  }

  @SuppressLint("DetachAndAttachSameFragment")
  private fun reload(selectedDate: String) {
    val fragment = RefactoredFutureTodoListFragment()
    val args = Bundle()
    args.putString("selectedDate", selectedDate)
    fragment.arguments = args

    parentFragmentManager.beginTransaction()
      .replace(id, fragment)
      .commit()
  }

  private fun updateCompletedViewVisibility() {
    val adapter = futureBinding.futureCompletedRecycler.adapter as FutureTodoAdapter

    if (adapter.itemCount == 0) {
      futureBinding.completedTitle.visibility = View.GONE
      futureBinding.futureCompletedRecycler.visibility = View.GONE
    } else {
      futureBinding.completedTitle.visibility = View.VISIBLE
      futureBinding.futureCompletedRecycler.visibility = View.VISIBLE
    }
  }

  private fun launchEdit(todo: TodoItem) {
    val intent = Intent(requireContext(), TodoActivity::class.java)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    intent.putExtra("todo_id", todo.id)
    intent.putExtra("todo_start_date", todo.startDate.format(formatter))
    intent.putExtra("todo_end_date", todo.endDate.format(formatter))
    intent.putExtra("todo_name", todo.name)
    intent.putExtra("todo_is_important", todo.isImportant)
    intent.putExtra("todo_is_completed", todo.isCompleted)
    intent.putExtra("todo_memo", todo.memo)

    todoActivityLauncher.launch(intent)
  }

  private fun createSwipeToDeleteHelper(
    recyclerView: RecyclerView,
    getItem: (Int) -> TodoItem,
    onDelete: (Int) -> Unit
  ): ItemTouchHelper {
    return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
      override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
      ): Boolean {
        return false
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition
        onDelete(position)
      }

      override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
      ) {
        val itemView = viewHolder.itemView

        if (dX != 0f || isCurrentlyActive) {
          val background = Color.RED.toDrawable()
          background.setBounds(
            itemView.right + dX.toInt(),
            itemView.top,
            itemView.right,
            itemView.bottom
          )
          background.draw(c)

          val icon = ContextCompat.getDrawable(requireContext(), R.drawable.trash_bin)!!

          val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
          val iconTop = itemView.top + iconMargin
          val iconLeft = itemView.right - icon.intrinsicWidth - iconMargin
          val iconRight = itemView.right - iconMargin
          val iconBottom = iconTop + icon.intrinsicHeight

          icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
          icon.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
      }
    })
  }

//  내가 고른 날짜의 데이터를 가져올 수 있게 한다
  private fun loadTodosForSelectedDate(selectedDate: String) {
    this.selectedDate = selectedDate
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.writableDatabase
    val cursor = db.rawQuery(
      "select id, name, start_date, end_date, complete_date, is_completed, is_important, memo from TODO_LIST where date(start_date) = date(?)",
      arrayOf(selectedDate)
    )

//  한 일과 안한 일 목록으로 나눈다
    val futureTodoList = mutableListOf<TodoItem>()
    val futureCompletedTodoList = mutableListOf<TodoItem>()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

//  전부 다 가져온다
    while (cursor.moveToNext()) {
      val id = cursor.getInt(0)
      val name = cursor.getString(1)
      val startDate = LocalDateTime.parse(cursor.getString(2), formatter)
      val endDate = LocalDateTime.parse(cursor.getString(3), formatter)
      val completeDate = cursor.getString(4)
      val completeDateParse =
        if (completeDate.isNullOrEmpty()) null else LocalDateTime.parse(completeDate, formatter)
      val isCompleted = cursor.getString(5) == "Y"
      val isImportant = cursor.getString(6) == "Y"
      val memo = cursor.getString(7) ?: ""

//      item 변수에 가져온 값을 다 넣는다
      val item =
        TodoItem(id, name, startDate, endDate, completeDateParse, isCompleted, isImportant, memo)

//      완료한 일이면 futureCompletedTodoList, 아니면 futureTodoList 에 넣는다
      if (item.isCompleted) {
        futureCompletedTodoList.add(item)
      } else {
        futureTodoList.add(item)
      }
    }
    cursor.close()
    db.close()

    futureAdapter.updateItems(futureTodoList)
    completedAdapter.updateItems(futureCompletedTodoList)

    updateCompletedViewVisibility()

    if (futureTodoList.isEmpty() && futureCompletedTodoList.isEmpty()) {
      futureBinding.noTodo.visibility = View.VISIBLE
      futureBinding.recyclerViewLayout.visibility = View.GONE
    } else {
      futureBinding.noTodo.visibility = View.GONE
      futureBinding.recyclerViewLayout.visibility = View.VISIBLE
    }
  }

//  해당 날짜의 데이터들을 가져온다
  private fun loadTodosForDate(date: String) {
    selectedDate = date
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.writableDatabase
    val cursor = db.rawQuery(
      "select id, name, start_date, end_date, complete_date, is_completed, is_important, memo from TODO_LIST where date(start_date) = date(?)",
      arrayOf(selectedDate)
    )

    val futureTodoList = mutableListOf<TodoItem>()
    val futureCompletedTodoList = mutableListOf<TodoItem>()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    while (cursor.moveToNext()) {
      val id = cursor.getInt(0)
      val name = cursor.getString(1)
      val startDate = LocalDateTime.parse(cursor.getString(2), formatter)
      val endDate = LocalDateTime.parse(cursor.getString(3), formatter)
      val completeDate = cursor.getString(4)
      val completeDateParse =
        if (completeDate.isNullOrEmpty()) null else LocalDateTime.parse(completeDate, formatter)
      val isCompleted = cursor.getString(5) == "Y"
      val isImportant = cursor.getString(6) == "Y"
      val memo = cursor.getString(7) ?: ""

      val item =
        TodoItem(id, name, startDate, endDate, completeDateParse, isCompleted, isImportant, memo)

      if (item.isCompleted) {
        futureCompletedTodoList.add(item)
      } else {
        futureTodoList.add(item)
      }
    }
    cursor.close()
    db.close()

//  만약에 futurecompletedTodoList 가 비었으면 안보이게 ㅊ ㅓ리한다
    if (futureCompletedTodoList.isEmpty()) {
      futureBinding.completedTitle.visibility = View.GONE
      futureBinding.futureCompletedRecycler.visibility = View.GONE
    } else {
      futureBinding.completedTitle.visibility = View.VISIBLE
      futureBinding.futureCompletedRecycler.visibility = View.VISIBLE
    }

//  둘다 없다면 일정이 없다고 출력한다
    if (futureTodoList.isEmpty() && futureCompletedTodoList.isEmpty()) {
      futureBinding.noTodo.visibility = View.VISIBLE
      futureBinding.recyclerViewLayout.visibility = View.GONE
    } else {
      futureBinding.noTodo.visibility = View.GONE
      futureBinding.recyclerViewLayout.visibility = View.VISIBLE
    }

//  새로운 할일이 생기거나 삭제되면 자기 자신을 새로고침한다
    futureAdapter = FutureTodoAdapter(
      futureTodoList,
      isCompletedList = false,
      onStatusChanged = { todo, position ->
        futureAdapter.removeItemAt(position)
        completedAdapter.addItem(todo)
        updateCompletedViewVisibility()
      },
      onItemClick = { todo -> launchEdit(todo) }
    )

//  새로운 할일이 생기거나 삭제되면 자기 자신을 새로고침한다
    completedAdapter = FutureTodoAdapter(
      futureCompletedTodoList,
      isCompletedList = true,
      onStatusChanged = { todo, position ->
        completedAdapter.removeItemAt(position)
        futureAdapter.addItem(todo)
        updateCompletedViewVisibility()
      },
      onItemClick = { todo -> launchEdit(todo) }
    )

//  어댑터를 연결한다
    futureBinding.futureTodoRecycler.adapter = futureAdapter
    futureBinding.futureCompletedRecycler.adapter = completedAdapter
  }
}