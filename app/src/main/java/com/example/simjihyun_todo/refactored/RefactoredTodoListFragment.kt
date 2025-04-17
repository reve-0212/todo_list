import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simjihyun_todo.DBHelper
import com.example.simjihyun_todo.MainActivity
import com.example.simjihyun_todo.R
import com.example.simjihyun_todo.TodoActivity
import com.example.simjihyun_todo.TodoItem
import com.example.simjihyun_todo.databinding.FragmentTodoListBinding
import com.example.simjihyun_todo.databinding.ItemTodoBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

//리팩토링한 TodoListFragment
class RefactoredTodoListFragment : Fragment() {

  private var _binding: FragmentTodoListBinding? = null
  private val binding get() = _binding!!

  /*다른 액티비티를 실행했다가 돌아왔을 때 결과를 처리하는 콜백함수
  startActivityForResult() : 다른 액티비티를 실행한다
  그 액티비티가 끝나면서 (finish) 결과를 넘긴다
  결과가 result_ok 고 intent 안에 updated 가 true 라는 값이 있다면
  리스트를 새로고침한다*/
  private val todoActivityLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result ->
    val isUpdated = result.data?.getBooleanExtra("updated", false) ?: false
    if (result.resultCode == Activity.RESULT_OK && isUpdated) {
      reload()
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    _binding = FragmentTodoListBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//    툴바 제목 석정
    (activity as? MainActivity)?.setToolbarTitle("오늘 할 일")

//    오늘 날짜에 문자열을 생성한다
    val today = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.readableDatabase

//    오늘 날짜에 해당하는 할일 목록을 조회한다
    val cursor = db.rawQuery(
      "SELECT id, name, start_date, end_date, complete_date, is_completed, is_important, memo FROM TODO_LIST WHERE date(start_date) = date(?)",
      arrayOf(today)
    )

//    날짜 포맷을 정의한다
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val todoList = mutableListOf<TodoItem>()
    val completedList = mutableListOf<TodoItem>()

//    db 에서 가져온 데이터를 TodoItem 객체로 반환해서 리스트에 담는다
    while (cursor.moveToNext()) {
      val item = TodoItem(
        id = cursor.getInt(0),
        name = cursor.getString(1),
        startDate = LocalDateTime.parse(cursor.getString(2), formatter),
        endDate = LocalDateTime.parse(cursor.getString(3), formatter),
        completeDate = cursor.getString(4)?.takeIf { it.isNotEmpty() }?.let { LocalDateTime.parse(it, formatter) },
        isCompleted = cursor.getString(5) == "Y",
        isImportant = cursor.getString(6) == "Y",
        memo = cursor.getString(7) ?: ""
      )
      if (item.isCompleted) completedList.add(item) else todoList.add(item)
    }

    cursor.close()
    db.close()

//    recyclerView 를 초기화한다 (미완료, 완료 따로)
    setupRecyclerView(binding.todoRecycler, todoList, false)
    setupRecyclerView(binding.completedRecycler, completedList, true)

//    완료된 할 일이 없으면 완료 구역을 숨긴다
    with(binding) {
      completedTitle.visibility = if (completedList.isEmpty()) View.GONE else View.VISIBLE
      completedRecycler.visibility = if (completedList.isEmpty()) View.GONE else View.VISIBLE
    }
  }

//  recyclerView 를 만드는 함수
  private fun setupRecyclerView(recyclerView: RecyclerView, list: List<TodoItem>, isCompleted: Boolean) {
    recyclerView.layoutManager = LinearLayoutManager(requireContext())
    recyclerView.adapter = TodoAdapter(list, isCompleted, { reload() }) { startTodoActivity(it) }

//  스와이프 하면 삭제하는 기능
    val touchHelper = createSwipeToDeleteHelper {
      val id = (recyclerView.adapter as TodoAdapter).getItem(it).id
      val db = DBHelper(requireContext()).writableDatabase
      db.execSQL("DELETE FROM TODO_LIST WHERE id = ?", arrayOf(id))
      db.close()
      reload()
    }

    touchHelper.attachToRecyclerView(recyclerView)
  }

//  아이템을 왼쪽으로 스와이프해서 삭제하는 기능 설정 함수
  private fun createSwipeToDeleteHelper(onSwipedAction: (Int) -> Unit): ItemTouchHelper {
    return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
      override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        onSwipedAction(viewHolder.adapterPosition)
      }

      override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
        drawSwipeBackground(c, viewHolder, dX)
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
      }
    })
  }

//  스와이프 시 빨간 배경과 휴지통 아이콘을 표시한다
  private fun drawSwipeBackground(c: Canvas, viewHolder: RecyclerView.ViewHolder, dX: Float) {
    val itemView = viewHolder.itemView
    val paint = Paint().apply { color = Color.RED }
    val icon = BitmapFactory.decodeResource(resources, R.drawable.trash_bin)
    val iconMargin = (itemView.height - icon.height) / 2

    c.drawRect(itemView.right + dX, itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat(), paint)
    c.drawBitmap(icon, itemView.right - iconMargin - icon.width.toFloat(), itemView.top + iconMargin.toFloat(), null)
  }

//  위에서 받아온 it(자기 자신의 값) 으로 todoActivity 를 실행한다
  private fun startTodoActivity(todo: TodoItem) {
    val intent = Intent(requireContext(), TodoActivity::class.java).apply {
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
      putExtra("todo_id", todo.id)
      putExtra("todo_start_date", todo.startDate.format(formatter))
      putExtra("todo_end_date", todo.endDate.format(formatter))
      putExtra("todo_name", todo.name)
      putExtra("todo_is_important", todo.isImportant)
      putExtra("todo_is_completed", todo.isCompleted)
      putExtra("todo_memo", todo.memo)
    }
    todoActivityLauncher.launch(intent)
  }

//  화면 갱신을 위한 reload 함수
  private fun reload() {
    parentFragmentManager.beginTransaction()
      .replace(id, RefactoredTodoListFragment())
      .commit()
  }
}

//recyclerView 어댑터
class TodoAdapter(
//  표시할 todo 리스트
  private val todos: List<TodoItem>,
//  완료된 리스트인지 확인
  private val isCompletedList: Boolean,
//  체크 상태가 변경되면 실행한다
  private val onStatusChanged: () -> Unit,
//  아이템을 클릭하면 실행한다
  private val onItemClick: (TodoItem) -> Unit
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

  inner class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
    val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return TodoViewHolder(binding)
  }

  override fun getItemCount(): Int = todos.size

  fun getItem(position: Int): TodoItem = todos[position]

  override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
    val todo = todos[position]
    val binding = holder.binding

//    텍스트를 설정하고 체크박스 상태를 확인한다
    binding.todoText.text = todo.name
    binding.todoCheckbox.isChecked = isCompletedList
//    리스너 중복을 방지한다
    binding.todoCheckbox.setOnCheckedChangeListener(null)

//    완료된 항목은 취소선을 추가한다
    binding.todoText.paintFlags = if (isCompletedList) {
      binding.todoText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
    } else {
      binding.todoText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
    }

//    별 (중요 표시) 설정
    setupStarIcons(binding, todo, holder)
//    체크박스 동작 설정
    setupCheckBox(binding, todo, holder)
//    아이템 클릭 이벤트 연결
    holder.itemView.setOnClickListener { onItemClick(todo) }
  }

//  중요 표시 (별 아이콘) 처리
  private fun setupStarIcons(binding: ItemTodoBinding, todo: TodoItem, holder: TodoViewHolder) {
    val (context, id) = holder.itemView.context to todo.id

//  중요 상태에 따라 아이콘 전환
    binding.blankStar.visibility = if (todo.isImportant) View.GONE else View.VISIBLE
    binding.filledStar.visibility = if (todo.isImportant) View.VISIBLE else View.GONE

//  빈별을 누르면 중요함으로 변경한다
    binding.blankStar.setOnClickListener {
      toggleImportance(binding, true)
      updateTodoField(context, id, "is_important", "Y")
      todo.isImportant = true
    }

//  노란 별을 누르면 빈 별로 바뀐다
    binding.filledStar.setOnClickListener {
      toggleImportance(binding, false)
      updateTodoField(context, id, "is_important", "N")
      todo.isImportant = false
    }
  }

//  별 아이콘을 전환하는 함수
  private fun toggleImportance(binding: ItemTodoBinding, isImportant: Boolean) {
    binding.blankStar.visibility = if (isImportant) View.GONE else View.VISIBLE
    binding.filledStar.visibility = if (isImportant) View.VISIBLE else View.GONE
  }

//  체크 박스 상태 변경 처리
  private fun setupCheckBox(binding: ItemTodoBinding, todo: TodoItem, holder: TodoViewHolder) {
    binding.todoCheckbox.setOnCheckedChangeListener { _, isChecked ->
      val context = holder.itemView.context
      val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
      val completeDate = if (isChecked) now else null
      val isCompleted = if (isChecked) "Y" else "N"

//      완료 상태를 db 에 업데이트 한다
      val db = DBHelper(context).writableDatabase
      db.execSQL(
        "UPDATE TODO_LIST SET complete_date = ?, is_completed = ? WHERE id = ?",
        arrayOf(completeDate, isCompleted, todo.id)
      )
      db.close()
      onStatusChanged()
    }
  }

//  중요 여부 등 특정 필드를 업데이트 한다
  private fun updateTodoField(context: Context, id: Int, field: String, value: String) {
    val db = DBHelper(context).writableDatabase
    db.execSQL("UPDATE TODO_LIST SET $field = ? WHERE id = ?", arrayOf(value, id))
    db.close()
  }
}

