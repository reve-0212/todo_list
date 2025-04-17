package com.example.simjihyun_todo

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simjihyun_todo.databinding.FragmentTodoListBinding
import com.example.simjihyun_todo.databinding.ItemTodoBinding
import androidx.core.graphics.drawable.toDrawable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.apply

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class TodoListFragment : Fragment() {
  //  먼저 todoListBinding 에 null 을 넣는다
  private var todoListBinding: FragmentTodoListBinding? = null

  //  외부에서는 binding 을 통해서 안전하게 사용한다
  private val binding get() = todoListBinding!!

  /*다른 액티비티를 실행했다가 돌아왔을 때 결과를 처리하는 콜백함수
    startActivityForResult() : 다른 액티비티를 실행한다
    그 액티비티가 끝나면서 (finish) 결과를 넘긴다
    결과가 result_ok 고 intent 안에 updated 가 true 라는 값이 있다면
    리스트를 새로고침한다*/
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

//    mainActivity 에 있는 setTollbarTitle을 오늘 할일로 바꾼다
    (activity as? MainActivity)?.setToolbarTitle("오늘 할 일")

    // todo_recycler 와 completedRecycler 를 찾아서 각각의 recyclerView 에 할당한다
    val todoRecycler = binding.todoRecycler
    val completedRecycler = binding.completedRecycler
//    리스트가 있는지 없는지 확인하기 위해 넣은 변수
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

        //  왼쪽으로 밀면 해당 포지션에 있는 아이템의 아이디를 가져와서 삭제한다
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

        // recyclerView 에서 아이템을 왼쪽이나 오른쪽으로 스와이프 할 때 배경과 아이콘(휴지통) 을 그려주는 함수
        override fun onChildDraw(
          c: Canvas,
          recyclerView: RecyclerView,
          viewHolder: RecyclerView.ViewHolder,
          dX: Float,
          dY: Float,
          actionState: Int,
          isCurrentlyActive: Boolean
        ) {
          drawSwipeBackground(c, viewHolder, dX)
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

//    오늘 날짜를 todayDate 라는 변수에 저장한다
    val todayDate = LocalDateTime.now()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    Log.d("todoList", "todayDate : ${todayDate.format(dateFormatter)}")
//    yyyy-MM-dd 형식으로 포맷한다
    val today = todayDate.format(dateFormatter)

//    TODO_LIST 에 있는 오늘 날짜의 모든 정보를 가져온다
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.readableDatabase
    val cursor = db.rawQuery(
      "select id, name, start_date, end_date, complete_date, is_completed, is_important, memo from TODO_LIST where date(start_date) = date(?)",
      arrayOf(today)
    )

//    완료하지 않은 일이 들어갈 리스트
    val todoList = mutableListOf<TodoItem>()
//    완료할 일이 들어간 리스트
    val completedList = mutableListOf<TodoItem>()
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

//    다음 줄이 있다면 name 과 isCompleted 값으로 나눠서
//    완료 여부에 따라 completedList 와 todoList 로 나눠서 저장한다
//    만일 completeDate 가 없다면 null 을 반환하고, 있다면 yyyy-MM-dd HH:mm:ss 형식으로 바꾼다
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

//      item 에 db 에서 읽어온 값을 넣는다
      val item =
        TodoItem(
          id, name, startDate, endDate,
          completeDateParse, isCompleted, isImportant, memo
        )

//      item 이 완료했다면 (true) 라면 completedList 에 넣는다
//      아니라면 todoList 에 넣는다
      if (item.isCompleted) {
        completedList.add(item)
      } else {
        todoList.add(item)
      }
    }

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

  //  프래그먼트를 다시 붙여서 리스트를 새로고침한다
  @SuppressLint("DetachAndAttachSameFragment")
  private fun reload() {
    parentFragmentManager.beginTransaction()
      .replace(id, TodoListFragment())
      .commit()
  }

  //  빨간 배경과 휴지통이 나오게 도와줄 함수
  private fun drawSwipeBackground(c: Canvas, viewHolder: RecyclerView.ViewHolder, dX: Float) {
    val itemView = viewHolder.itemView
    val paint = Paint().apply { color = Color.RED }
    val icon = BitmapFactory.decodeResource(resources, R.drawable.trash_bin)

    val iconMargin = (itemView.height - icon.height) / 2


    // 배경 빨간색 그리기
    c.drawRect(
      itemView.right.toFloat() + dX,
      itemView.top.toFloat(),
      itemView.right.toFloat(),
      itemView.bottom.toFloat(),
      paint
    )

    // 아이콘 그리기
    c.drawBitmap(
      icon,
      itemView.right - iconMargin - icon.width.toFloat(),
      itemView.top + iconMargin.toFloat(),
      null
    )
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
    * false : 바로 parent 에 붙이지 않고, 나중에 RecyclerView 가 직접 붙이도록 한다 */
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
//      메모리 상에서도 중요도 정보 반영
      todo.isImportant = true
    }

//    노란 별을 누르면 blankStar 가 나오고 중요하지 않다고 업데이트한다
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

//    체크박스를 누르면 completeDate 에 값을 저장한다
//    isChecked 라면 Y, 아니라면 N 으로 바꾼다
//    그리고 화면을 새로고침한다
    binding.todoCheckbox.setOnCheckedChangeListener { _, isChecked ->
      val dbHelper = DBHelper(holder.itemView.context)
      val db = dbHelper.writableDatabase
      val now = LocalDateTime.now()

      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
//      완료되었다면 (isChecked) 가 true 라면 now 값을 formatter 형식(yyyy-MM-dd HH:mm:ss) 로 바꾼다
      val completeDateFormat = if (isChecked) now.format(formatter) else null

//      isChecked 라면 Y, 아니라면 N을 is_completed 에 넣는다
      db.execSQL(
        "update TODO_LIST set complete_date=?, is_completed = ? where id = ?",
        arrayOf(completeDateFormat, if (isChecked) "Y" else "N", todo.id)
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
