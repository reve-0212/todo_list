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
//  바인딩
  private var futureTodoListBinding: FragmentFutureTodoListBinding? = null

//  메모리 누수 방지용
  private val futureBinding get() = futureTodoListBinding!!

  private var selectedDate: String = ""

  lateinit var futureAdapter: FutureTodoAdapter
  lateinit var completedAdapter: FutureTodoAdapter

  //  다른 액티비티를 실행했다가 돌아왔을 때 결과를 처리하는 콜백함수
  private val todoActivityLauncher = registerForActivityResult(
    //    startActivityForResult() : 다른 액티비티를 실행한다
//    그 액티비티가 끝나면서 (finish) 결과를 넘긴다
//    결과가 result_ok 고 intent 안에 updated 가 true 라는 값이 있다면
//    데이터를 새로고침한다
//    밑의 reload() 함수와 연관있음
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
    var calendar: Calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

//    selectedDate 값을 arguments(함수를 호출할 때 전달되는 값) 이 없으면 오늘 날짜로 설정한다
//    그 날짜를 CalendarView 에 적용해서 해당 날짜가 선택된 상태로 보이게 한다
    selectedDate = arguments?.getString("selectedDate") ?: dateFormat.format(calendar.time)
    futureBinding.calendarView.date = dateFormat.parse(selectedDate)?.time ?: calendar.timeInMillis

    loadTodosForDate(selectedDate)

    val futureRecycler = futureBinding.futureTodoRecycler
    val futureCompletedRecycler = futureBinding.futureCompletedRecycler

//    왼쪽으로 밀면 삭제
//    지금 내가 민 아이템의 위치 값을 받아온다
//    그 위치 값에 해당하는 아이템의 id 를 기준으로 삭제를 진행한다
//    loadTodosForDate 함수를 실행해서 화면을 다시 로드한다
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
    //    지금 내가 민 아이템의 위치 값을 받아온다
//    그 위치 값에 해당하는 아이템의 id 를 기준으로 삭제를 진행한다
//    loadTodosForDate 함수를 실행해서 화면을 다시 로드한다
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
//    futureTodoListBinding 을 null 로 고친다
    futureTodoListBinding = null
  }

  //  체크박스를 누르면 리스트를 새로고침한다
//  현재 Fragment 를 새로 만들어서 강제로 새로고침한다
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

//    adapter 에 아이템이 하나도 없다면 completedTitle 과 recyclerView 를 없앤다
//    있으면 출력한다
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

//    선택한 날의 데이터들을 가져온다
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.writableDatabase
    var cursor = db.rawQuery(
      "select id, name, start_date, end_date, complete_date, is_completed, is_important, memo " +
              "from TODO_LIST where date(start_date) = date(?)", arrayOf(selectedDate)
    )

//    안한 일 목록과 한 일 목록으로 나눈다
    val futureTodoList = mutableListOf<TodoItem>()
    val futureCompletedTodoList = mutableListOf<TodoItem>()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

//    다음 줄이 있다면 값들을 각각의 배열에 나눠서 저장한다
    while (cursor.moveToNext()) {
      val id = cursor.getInt(0)
      val name = cursor.getString(1)
      val startDate = LocalDateTime.parse(cursor.getString(2), formatter)
      val endDate = LocalDateTime.parse(cursor.getString(3), formatter)
      val completeDate = cursor.getString(4)
//      completeDate 가 없다면 null, 있으면 formatter 로 파싱 한 형태로 데이터를 저장한다
      val completeDateParse = if (completeDate.isNullOrEmpty()) {
        null
      } else {
        LocalDateTime.parse(completeDate, formatter)
      }
      val isCompleted = cursor.getString(5) == "Y"
      val isImportant = cursor.getString(6) == "Y"
      val memo = cursor.getString(7) ?: ""

//      결과를 item 에 넣는다
      val item =
        TodoItem(id, name, startDate, endDate, completeDateParse, isCompleted, isImportant, memo)

//      완료한 일(true) 라면 futureCompetedTodoList에 넣는다
//      반대일 경우 furueTodoList 에 넣는다
      if (item.isCompleted) {
        futureCompletedTodoList.add(item)
      } else {
        futureTodoList.add(item)
      }
    }
    cursor.close()
    db.close()

//    futureAdapter 와 completedAdapter 에 값들을 새로고침한다
    futureAdapter.updateItems(futureTodoList)
    completedAdapter.updateItems(futureCompletedTodoList)

//    만일 값이 없다면 사라지게 만든다
    updateCompletedViewVisibility()

//    위와 유사한 일을 한다. '할일이 없어요' 를 출력하기 위해서 있는 코드
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

//    오늘의 날짜로 검색한다
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.writableDatabase
    var cursor = db.rawQuery(
      "select id, name, start_date, end_date, complete_date, is_completed, is_important, memo " +
              "from TODO_LIST where date(start_date) = date(?)", arrayOf(selectedDate)
    )

//    futureTodoList 와 CompletedTodoList 에 나눠서 저장한다
    val futureTodoList = mutableListOf<TodoItem>()
    val futureCompletedTodoList = mutableListOf<TodoItem>()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    while (cursor.moveToNext()) {
      val id = cursor.getInt(0)
      val name = cursor.getString(1)
      val startDate = LocalDateTime.parse(cursor.getString(2), formatter)
      val endDate = LocalDateTime.parse(cursor.getString(3), formatter)
      val completeDate = cursor.getString(4)
//      competedDate 가 null 이면 null, 아니라면 "yyyy-MM-dd" 형식으로 값을 변경한다
      val completeDateParse = if (completeDate.isNullOrEmpty()) {
        null
      } else {
        LocalDateTime.parse(completeDate, formatter)
      }
      val isCompleted = cursor.getString(5) == "Y"
      val isImportant = cursor.getString(6) == "Y"
      val memo = cursor.getString(7) ?: ""

//      item 에 넣는다
      val item = TodoItem(id, name, startDate, endDate, completeDateParse, isCompleted, isImportant, memo)

//      완료한 일이라면 futureCompletedTodoList 에, 아니라면 futureTodoList 에 넣는다
      if (item.isCompleted) {
        futureCompletedTodoList.add(item)
      } else {
        futureTodoList.add(item)
      }
    }
    cursor.close()
    db.close()
    Log.d("todoList","------------------FutureTodoListFragment-----------------")
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

//    둘다 비어있다면 할일이 없다고 출력한다
    if (futureTodoList.isEmpty() && futureCompletedTodoList.isEmpty()) {
      futureBinding.noTodo.visibility = View.VISIBLE
      futureBinding.recyclerViewLayout.visibility = View.GONE
    } else {
      futureBinding.noTodo.visibility = View.GONE
      futureBinding.recyclerViewLayout.visibility = View.VISIBLE
    }

//    recyclerView 전환을 위한 어댑터
//    자신이 자신을 불러서 다시 실행하게끔 한다
    futureAdapter = FutureTodoAdapter(
      futureTodoList,
      isCompletedList = false,
//      상태가 변경되면 todo 와 position 값을 받아서 position 의 아이템을 지운다
//      completedAdapter 에는 todo 의 아이템을 넣는다
      onStatusChanged = { todo, position ->
        futureAdapter.removeItemAt(position)
        completedAdapter.addItem(todo)
//        완료한 뷰의 상태를 업데이트 한다
        updateCompletedViewVisibility()
//        launchEdit 을 실행해서 그 항목에 해당하는 상세 페이지로 넘어간다
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

//    노란 별을 빈별로 바꾸고 Y 에서 N 으로 중요도를 수정한다
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

//  특정 위치의 아이템을 삭제하고 화면을 갱신한다
  fun removeItemAt(position: Int): TodoItem {
    val item = futureTodos.removeAt(position)
    notifyItemRemoved(position)
    return item
  }

//  리스트의 맨 앞에 아이템을 추가한다
  fun addItem(todo: TodoItem) {
    futureTodos.add(0, todo)
    notifyItemInserted(0)
  }

//  position 값으로 아이템을 꺼낸다
  fun getItem(position: Int): TodoItem {
    return futureTodos[position]
  }

//  futureTodos 내부 값을 전부 비우고 newList 라는 새로운 값들로 채운다
//  그리고 새로고침한다
  @SuppressLint("NotifyDataSetChanged")
  fun updateItems(newList: List<TodoItem>) {
    futureTodos.clear()
    futureTodos.addAll(newList)
    notifyDataSetChanged()
  }

}
