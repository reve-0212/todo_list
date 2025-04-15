package com.example.simjihyun_todo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simjihyun_todo.databinding.FragmentTodoListBinding
import com.example.simjihyun_todo.databinding.ItemTodoBinding
import androidx.core.graphics.drawable.toDrawable
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

//start date 가 오늘인것만
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class TodoListFragment : Fragment() {
  //  먼저 todoListBinding 에 null 을 넣는다
  private var todoListBinding: FragmentTodoListBinding? = null

  //  외부에서는 binding 을 통해서 안전하게 사용한다
  private val binding get() = todoListBinding!!

  private val todoActivityLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
      val isUpdated = result.data?.getBooleanExtra("updated", false) ?: false
      if (isUpdated) {
        reload()
      }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
//    이후 onCreateView 에서 FragementTodoListBinding 을 넣는다
    todoListBinding = FragmentTodoListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
//    메모리 누수를 방지하기 위해 todoListBinding 을 null 으로 바꾼다
    todoListBinding = null
  }

  @SuppressLint("Recycle", "SimpleDateFormat")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    (activity as? MainActivity)?.setToolbarTitle("오늘 할 일")

    //    todo_recycler 를 찾아서 recyclerView 에 할당한다
    val todoRecycler = binding.todoRecycler
    val completedRecycler = binding.completedRecycler
    val title = binding.completedTitle

//    왼쪽으로 스와이프하면 삭제되는 기능을 넣기 위해 itemTouchHelper 를 사용한다
    val itemTouchHelper =
      ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
          recyclerView: RecyclerView,
          viewHolder: RecyclerView.ViewHolder,
          target: RecyclerView.ViewHolder
        ): Boolean {
          return false
        }

        //  왼쪽으로 밀면 해당 포지션에 있는 아이템의 이름을 가져와서 삭제한다
        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
          val position = viewHolder.adapterPosition
          val id = (todoRecycler.adapter as TodoAdapter).getItem(position).id

          val dbHelper = DBHelper(requireContext())
          val db = dbHelper.writableDatabase
          db.execSQL("delete from TODO_LIST where id = ?", arrayOf(id))
          db.close()
          // 새로고침
          reload()
        }

        // 왼쪽으로 밀면 휴지통 나오게
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

//    itemTouchHelper 와 todoRecycler 를 연결한다
    itemTouchHelper.attachToRecyclerView(todoRecycler)

//    완료한 일도 삭제 가능하도록
    val completedTouchHelper =
      ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        override fun onMove(
          recyclerView: RecyclerView,
          viewHolder: RecyclerView.ViewHolder,
          target: RecyclerView.ViewHolder
        ): Boolean {
          return false
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
          val position = viewHolder.adapterPosition
          val id = (completedRecycler.adapter as TodoAdapter).getItem(position).id

          val dbHelper = DBHelper(requireContext())
          val db = dbHelper.writableDatabase
          db.execSQL("delete from TODO_LIST where id = ?", arrayOf(id))
          db.close()
          reload()
        }
      })

    completedTouchHelper.attachToRecyclerView(completedRecycler)

    /*LinearLayoutManager : RecyclerView 의 항목들을 세로 혹은 가로 방향으로 정렿한다
    * requireContext() : Fragment 안에서 Context(앱의 현재 상태) 를 가져온다*/
    todoRecycler.layoutManager = LinearLayoutManager(requireContext())
    completedRecycler.layoutManager = LinearLayoutManager(requireContext())

    val todayDate = LocalDateTime.now()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    Log.d("todoList", "todayDate : ${todayDate.format(dateFormatter)}")
    val today = todayDate.format(dateFormatter)

//    TODO_LIST 에 있는 오늘 날짜의 모든 name과 is_completed 를 가져온다
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.readableDatabase
    val cursor = db.rawQuery(
      "select id, name, start_date, end_date, is_completed, is_important, memo from TODO_LIST where date(start_date) = date(?)",
      arrayOf(today)
    )
    val todoList = mutableListOf<TodoItem>()
    val completedList = mutableListOf<TodoItem>()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

//    다음 줄이 있다면 0번째 값(name)과 1번째 값(is_completed) 를
//    완료 여부에 따라 completedList 와 todoList 로 나눠서 저장한다
    while (cursor.moveToNext()) {
      val id = cursor.getInt(0)
      val name = cursor.getString(1)
      val startDate = LocalDateTime.parse(cursor.getString(2), formatter)
      val endDate = LocalDateTime.parse(cursor.getString(3), formatter)
      val isCompleted = cursor.getString(4) == "Y"
      val isImportant = cursor.getString(5) == "Y"
      val memo = cursor.getString(6) ?: ""

      val item = TodoItem(id, name, startDate, endDate, isCompleted, isImportant, memo)
      if (item.isCompleted) {
        completedList.add(item)
      } else {
        todoList.add(item)
      }
    }
    Log.d("todoList", "할일 목록 : $todoList")
    Log.d("todoList", "완료 목록 : $completedList")

//    만일 완료된 일이 없다면 관련 뷰를 숨긴다
    if (completedList.isEmpty()) {
      title.visibility = View.GONE
      completedRecycler.visibility = View.GONE
    } else {
      title.visibility = View.VISIBLE
      completedRecycler.visibility = View.VISIBLE
    }

    cursor.close()
    db.close()

//    각각의 RecyclerView 에 어댑터를 연결한다
    todoRecycler.adapter = TodoAdapter(todoList, false, { reload() }) { todo ->
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
    completedRecycler.adapter = TodoAdapter(completedList, true, { reload() }) { todo ->
      val intent = Intent(requireContext(), TodoActivity::class.java)
      intent.putExtra("todo_id", todo.id)
      intent.putExtra("todo_start_date", todo.startDate.format(formatter))
      intent.putExtra("todo_end_date", todo.endDate.format(formatter))
      intent.putExtra("todo_name", todo.name)
      intent.putExtra("todo_is_important", todo.isImportant)
      intent.putExtra("todo_is_completed", todo.isCompleted)
      intent.putExtra("todo_memo", todo.memo)

      todoActivityLauncher.launch(intent)
    }
  }

  //  체크박스를 누르면 리스트를 새로고침한다
  @SuppressLint("DetachAndAttachSameFragment")
  private fun reload() {
    parentFragmentManager.beginTransaction()
      .replace(id, TodoListFragment())
      .commit()
  }
}

@Suppress("TYPE_INTERSECTION_AS_REIFIED_WARNING")
class TodoAdapter(
//  항목 이름 리스트
  private val todos: List<TodoItem>,
//  완료된 항목인지 아닌지 구분한다
  private val isCompletedList: Boolean,
//  체크박스를 클릭하면 호출될 콜백함수
  private val onStatusChanged: () -> Unit,
  private val onItemClick: (TodoItem) -> Unit
) :
  RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

  inner class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
    /*RecyclerView 의 어댑터에서 ViewHolder 를 만들 때 사용한다
    * parent : RecyclerView 안에서 각 항목(item) 의 부모. 이 item 을 어디 붙일지 나타냄
    * LayoutInflater.from(parent.context) : xml 을 실제 코드에서 사용할 수 있도록 View 로 만든다
    * parent.context : 부모가 가지고 있는 context
    * false : 지금 당장 parent 에 붙이지는 말고, 그냥 view 만 만들어달라 요청*/
    val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return TodoViewHolder(binding)
  }

  fun getItem(position: Int): TodoItem {
    return todos[position]
  }

  override fun getItemCount(): Int {
    return todos.size
  }

  @SuppressLint("Recycle")
  override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
    val todo = todos[position]
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
      onStatusChanged()
    }

//    항목을 클릭하면 TodoActivity 로 간다
    holder.itemView.setOnClickListener {
      onItemClick(todo)
    }
  }
}
