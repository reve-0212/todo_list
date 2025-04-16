package com.example.simjihyun_todo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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
import com.example.simjihyun_todo.databinding.FragmentFutureTodoListBinding
import com.example.simjihyun_todo.databinding.ItemTodoBinding
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class FutureTodoListFragment : Fragment() {
  private var futureTodoListBinding: FragmentFutureTodoListBinding? = null
  private val futureBinding get() = futureTodoListBinding!!

  private var selectedDate: String = ""

  lateinit var futureAdapter: FutureTodoAdapter
  lateinit var completedAdapter: FutureTodoAdapter

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
//    (activity as? MainActivity)?.findViewById<Button>(R.id.write_todo_list)?.visibility = View.GONE
    return futureBinding.root
  }

  @SuppressLint("Recycle")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    var calendar: Calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    selectedDate = arguments?.getString("selectedDate") ?: dateFormat.format(calendar.time)
    futureBinding.calendarView.date = dateFormat.parse(selectedDate)?.time ?: calendar.timeInMillis

    loadTodosForDate(selectedDate)

    val futureRecycler = futureBinding.futureTodoRecycler
    val futureCompletedRecycler = futureBinding.futureCompletedRecycler
//    val title = futureBinding.completedTitle

//    왼쪽으로 밀면 삭제
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

//    완료한 일도 삭제
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

    /*LinearLayoutManager : RecyclerView 의 항목들을 세로 혹은 가로 방향으로 정렿한다
    * requireContext() : Fragment 안에서 Context(앱의 현재 상태) 를 가져온다*/
    futureRecycler.layoutManager = LinearLayoutManager(requireContext())
    futureCompletedRecycler.layoutManager = LinearLayoutManager(requireContext())

//    달력에서 다른 날짜 누르면 실행
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

  //  체크박스를 누르면 리스트를 새로고침한다
  @SuppressLint("DetachAndAttachSameFragment")
  private fun reload(selectedDate: String) {
    val fragment = FutureTodoListFragment()
    val args = Bundle()
    args.putString("selectedDate", selectedDate)
    fragment.arguments = args

    parentFragmentManager.beginTransaction()
      .replace(id, fragment)
      .commit()
  }

  //  완료 뷰 상태 업데이트
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

  //  항목 클릭했을 때 TodoActivity 로 이동
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

  //  왼쪽으로 밀면 삭제 가능하게 도와줄 함수
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

      override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int
      ) {
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

          // 휴지통 아이콘을 가져온다
          val icon = ContextCompat.getDrawable(requireContext(), R.drawable.trash_bin)!!

          // 마진 등을 설정한다
          val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
          val iconTop = itemView.top + iconMargin
          val iconLeft = itemView.right - icon.intrinsicWidth - iconMargin
          val iconRight = itemView.right - iconMargin
          val iconBottom = iconTop + icon.intrinsicHeight

          // 그리고 그린다
          icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
          icon.draw(c)
        }

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
      }
    })
  }

  //  선택한 날의 할일 데이터들을 불러오는 함수
  private fun loadTodosForSelectedDate(selectedDate: String) {
    this.selectedDate = selectedDate
//    var calendar: Calendar = Calendar.getInstance()
//    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.writableDatabase
    var cursor = db.rawQuery(
      "select id, name, start_date, end_date, complete_date, is_completed, is_important, memo " +
              "from TODO_LIST where date(start_date) = date(?)", arrayOf(selectedDate)
    )

    val futureTodoList = mutableListOf<TodoItem>()
    val futureCompletedTodoList = mutableListOf<TodoItem>()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

//    다음 줄이 있다면 값 전체를 futureTodoList 에 저장한다
    while (cursor.moveToNext()) {
      val id = cursor.getInt(0)
      val name = cursor.getString(1)
      val startDate = LocalDateTime.parse(cursor.getString(2), formatter)
      val endDate = LocalDateTime.parse(cursor.getString(3), formatter)
      val completeDate = cursor.getString(4)
      val completeDateParse = if (completeDate.isNullOrEmpty()) {
        null
      } else {
        LocalDateTime.parse(completeDate, formatter)
      }
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

  //  오늘 날짜도 보려고 함수로 뺌
  private fun loadTodosForDate(date: String) {
    selectedDate = date

    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.writableDatabase
    var cursor = db.rawQuery(
      "select id, name, start_date, end_date, complete_date, is_completed, is_important, memo " +
              "from TODO_LIST where date(start_date) = date(?)", arrayOf(selectedDate)
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
      val completeDateParse = if (completeDate.isNullOrEmpty()) {
        null
      } else {
        LocalDateTime.parse(completeDate, formatter)
      }
      val isCompleted = cursor.getString(4) == "Y"
      val isImportant = cursor.getString(5) == "Y"
      val memo = cursor.getString(6) ?: ""

      val item = TodoItem(id, name, startDate, endDate, completeDateParse, isCompleted, isImportant, memo)
      if (item.isCompleted) {
        futureCompletedTodoList.add(item)
      } else {
        futureTodoList.add(item)
      }
    }
    cursor.close()
    db.close()

    Log.d("todoList", "futureTodoList : $futureTodoList")
    Log.d("todoList", "futureCompletedTodoList : $futureCompletedTodoList")

    //    만일 완료된 일이 없다면 관련 뷰를 숨긴다
    if (futureCompletedTodoList.isEmpty()) {
      futureBinding.completedTitle.visibility = View.GONE
      futureBinding.futureCompletedRecycler.visibility = View.GONE
    } else {
      futureBinding.completedTitle.visibility = View.VISIBLE
      futureBinding.futureCompletedRecycler.visibility = View.VISIBLE
    }

    if (futureTodoList.isEmpty() && futureCompletedTodoList.isEmpty()) {
      futureBinding.noTodo.visibility = View.VISIBLE
      futureBinding.recyclerViewLayout.visibility = View.GONE
    } else {
      futureBinding.noTodo.visibility = View.GONE
      futureBinding.recyclerViewLayout.visibility = View.VISIBLE
    }

    futureAdapter = FutureTodoAdapter(
      futureTodoList,
      isCompletedList = false,
      onStatusChanged = { todo, position ->
        futureAdapter.removeItemAt(position)
        completedAdapter.addItem(todo)
        updateCompletedViewVisibility()
      }, onItemClick = { todo -> launchEdit(todo) })

    completedAdapter = FutureTodoAdapter(
      futureCompletedTodoList,
      isCompletedList = true,
      onStatusChanged = { todo, position ->
        completedAdapter.removeItemAt(position)
        futureAdapter.addItem(todo)
        updateCompletedViewVisibility()
      }, onItemClick = { todo -> launchEdit(todo) }
    )

    futureBinding.futureTodoRecycler.adapter = futureAdapter
    futureBinding.futureCompletedRecycler.adapter = completedAdapter
  }
}

@Suppress("TYPE_INTERSECTION_AS_REIFIED_WARNING")
class FutureTodoAdapter(
  private val futureTodos: MutableList<TodoItem>,
  private val isCompletedList: Boolean,
  private val onStatusChanged: (TodoItem, Int) -> Unit,
  private val onItemClick: (TodoItem) -> Unit,
) : RecyclerView.Adapter<FutureTodoAdapter.FutureTodoViewHolder>() {

  inner class FutureTodoViewHolder(val binding: ItemTodoBinding) :
    RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int,
  ): FutureTodoViewHolder {
    val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return FutureTodoViewHolder(binding)
  }

  @SuppressLint("Recycle")
  override fun onBindViewHolder(
    holder: FutureTodoViewHolder,
    position: Int,
  ) {
//    holder.bind(futureTodos[position])
    val todo = futureTodos[position]
    val binding = holder.binding

//    체크 리스너를 초기화해서 오작동을 방지한다
    binding.todoCheckbox.setOnCheckedChangeListener(null)
    binding.todoText.text = todo.name
    binding.todoCheckbox.isChecked = isCompletedList

//    완료한 일이라면 취소선을 긋는다
    binding.todoText.paintFlags = if (isCompletedList) {
      binding.todoText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else {
      binding.todoText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }

//    중요한 일이라면 별이 나오게 한다
    if (todo.isImportant) {
      binding.blankStar.visibility = View.GONE
      binding.filledStar.visibility = View.VISIBLE
    } else {
      binding.blankStar.visibility = View.VISIBLE
      binding.filledStar.visibility = View.GONE
    }

//    빈 별을 누르면 노란 별로 바꾸고,
//    중요도를 N 에서 Y 로 수정한다
    binding.blankStar.setOnClickListener {
      binding.blankStar.visibility = View.GONE
      binding.filledStar.visibility = View.VISIBLE

      val dbHelper = DBHelper(holder.itemView.context)
      val db = dbHelper.writableDatabase
      db.execSQL(
        "update TODO_LIST set is_important = 'Y' where id = ?",
        arrayOf(todo.id)
      )
      db.close()
//      중요도 정보 반영
      todo.isImportant = true
    }

    binding.filledStar.setOnClickListener {
      binding.blankStar.visibility = View.VISIBLE
      binding.filledStar.visibility = View.GONE

      val dbHelper = DBHelper(holder.itemView.context)
      val db = dbHelper.writableDatabase
      db.execSQL(
        "update TODO_LIST set is_important = 'N' where id = ?",
        arrayOf(todo.id)
      )
      db.close()
      todo.isImportant = false
    }

//    체크박스를 누르면 isChecked 라면 Y, 아니라면 N 으로 바꾼다
//    그리고 화면을 새로고침한다
    binding.todoCheckbox.setOnCheckedChangeListener { _, isChecked ->
      val dbHelper = DBHelper(holder.itemView.context)
      val db = dbHelper.writableDatabase
      db.execSQL(
        "update TODO_LIST set is_completed = ? where id = ?",
        arrayOf(if (isChecked) "Y" else "N", todo.id)
      )
      db.close()

      todo.isCompleted = isChecked
      onStatusChanged(todo, holder.adapterPosition)
    }

//    항목을 클릭하면 TodoActivity 로 간다
    holder.itemView.setOnClickListener {
      onItemClick(todo)
    }
  }

  override fun getItemCount(): Int {
    return futureTodos.size
  }

  fun removeItemAt(position: Int): TodoItem {
    val item = futureTodos.removeAt(position)
    notifyItemRemoved(position)
    return item
  }

  fun addItem(todo: TodoItem) {
    futureTodos.add(0, todo)
    notifyItemInserted(0)
  }

  fun getItem(position: Int): TodoItem {
    return futureTodos[position]
  }

  @SuppressLint("NotifyDataSetChanged")
  fun updateItems(newList: List<TodoItem>) {
    futureTodos.clear()
    futureTodos.addAll(newList)
    notifyDataSetChanged()
  }

}
