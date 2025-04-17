import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.simjihyun_todo.DBHelper
import com.example.simjihyun_todo.MainActivity
import com.example.simjihyun_todo.R
import com.example.simjihyun_todo.databinding.FragmentRecordTodoBinding
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RecordTodoFragment : Fragment() {
//  ViewBinding 변수를 선언한다
  private var recordTodoListBinding: FragmentRecordTodoBinding? = null
  private val recordBinding get() = recordTodoListBinding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
//    ViewBinding 을 초기화한다
    recordTodoListBinding = FragmentRecordTodoBinding.inflate(inflater, container, false)

//    MainActivity 의 툴바 타이틀을 바꾼다
//    write_todo_list 라는 아이디를 가진 floatingActionButton 을 안보이게 한다
    (activity as? MainActivity)?.setToolbarTitle("지금까지 한 일")
    (activity as? MainActivity)?.findViewById<FloatingActionButton>(R.id.write_todo_list)?.visibility = View.GONE
    return recordBinding.root
  }

  @SuppressLint("Recycle")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

//    완료한 일과 하지 않은 일의 개수를 표시한다
    whatIDo()
    whatIDont()

//    파이 차트를 설정하고 데이터를 로딩한다
    recordBinding.todoPieChart.let { pieChart ->
      setupPieChart(pieChart)
      loadPieData(pieChart)
    }
  }

//  Fragment 가 종료될 때 ViewBInding 메모리 누수 방지를 위해 null 처리한다
  override fun onDestroy() {
    super.onDestroy()
    recordTodoListBinding = null
  }

  //    내가 한 일의 갯수만큼 whatIDo 의 갯수를 바꾼다
  private fun whatIDo() {
    var whatIDoCount = 0
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.readableDatabase
    var cursor = db.rawQuery(
      "select count(id) from TODO_LIST where is_completed = 'Y' ", null
    )

    if (cursor.moveToFirst()) {
      whatIDoCount = cursor.getInt(0)
    }

    cursor.close()
    db.close()

    Log.d("todoList", "whatIDoCount : $whatIDoCount")
    recordBinding.whatIDoText.text = whatIDoCount.toString()
  }

  //    내가 안한 일의 갯수만큼 whatIDontText 값을 바꾼다
  private fun whatIDont() {
    var whatIDontCount = 0
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.readableDatabase
    var cursor = db.rawQuery(
      "select count(id) from TODO_LIST where is_completed = 'N' ", null
    )

    if (cursor.moveToFirst()) {
      whatIDontCount = cursor.getInt(0)
    }

    cursor.close()
    db.close()

    Log.d("todoList", "whatIDontCount : $whatIDontCount")
    recordBinding.whatIDontText.text = whatIDontCount.toString()
  }

//  외부에서 가져온 pieChart 를 사용한다
  private fun setupPieChart(pieChart: PieChart) {
//    퍼센트로 표시한다
    pieChart.setUsePercentValues(true)
//  설명 텍스트는 제거한다
    pieChart.description.isEnabled = false
//  가운데 구멍을 표시한다
    pieChart.isDrawHoleEnabled = true
//  가운데 구멍의 색깔을 지정한다
    pieChart.setHoleColor(Color.WHITE)
//  투명한 원의 알파값을 지정한다 (0: 투명 / 255 : 완전 불투명. 기본값은 100)
    pieChart.setTransparentCircleAlpha(110)
//  가운데 텍스트를 설정한다
    pieChart.centerText = "완료율"
//  가운데 텍스트의 크기
    pieChart.setCenterTextSize(18f)
//  항목의 라벨 색깔
    pieChart.setEntryLabelColor(Color.BLACK)
//  라벨의 텍스트 크기
    pieChart.setEntryLabelTextSize(12f)
//  회전 비활성화
    pieChart.isRotationEnabled = false

//  범례를 설정한다
    val legend = pieChart.legend
    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
    legend.orientation = Legend.LegendOrientation.HORIZONTAL
    legend.setDrawInside(false)
    legend.textSize = 14f
  }

//  파이 차트에 들어갈 데이터를 불러온다
  @SuppressLint("Recycle")
  private fun loadPieData(pieChart: PieChart) {
    val dbHelper = DBHelper(requireContext())
    val db = dbHelper.readableDatabase

    var done = 0
    var notDone = 0

//  완료 한 값을 기준으로 그루핑해서 가져온다
    val cursor =
      db.rawQuery("select is_completed, count(*) from TODO_LIST group by is_completed", null)

    while (cursor.moveToNext()) {
//      Y 혹은 N 의 값이 들어있다
      val type = cursor.getString(0)
      val count = cursor.getInt(1)

//      만일 타입이 Y 라면 done 에 넣고 아니라면 notDone 에 넣는다
      if (type == "Y") done = count
      else if (type == "N") notDone = count
    }

    cursor.close()
    db.close()

//  파이차트 항목을 만든다
    val entries = ArrayList<PieEntry>()
//  만일 완료한 일/ 완료하지 않은 일이 0개를 넘는다면
//  done 을 float 형태로 바꾸어서 "완료한 일" 이라는 라벨을 붙인다
    if (done > 0) entries.add(PieEntry(done.toFloat(), "완료한 일"))
    if (notDone > 0) entries.add(PieEntry(notDone.toFloat(), "안한 일"))

//  데이터셋을 생성한다
    val dataSet = PieDataSet(entries, "")
    dataSet.colors = listOf(
      Color.rgb(218, 204, 255),
      Color.rgb(255, 247, 204)
    )

//  실제 파이차트에 표시할 데이터를 설정한다
    val data = PieData(dataSet)
    data.setDrawValues(true)
    data.setValueTextSize(16f)
    data.setValueTextColor(Color.BLACK)
    data.setValueFormatter(PercentFormatter(pieChart))

    pieChart.data = data
//  화면을 갱신한다
    pieChart.invalidate()
  }
}
