import android.annotation.SuppressLint
import android.graphics.Color
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.simjihyun_todo.DBHelper
import com.example.simjihyun_todo.MainActivity
import com.example.simjihyun_todo.databinding.FragmentRecordTodoBinding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import java.text.SimpleDateFormat
import java.util.Locale

class RecordTodoFragment : Fragment() {
  private var recordTodoListBinding: FragmentRecordTodoBinding? = null
  private val recordBinding get() = recordTodoListBinding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    recordTodoListBinding = FragmentRecordTodoBinding.inflate(inflater, container, false)
    (activity as? MainActivity)?.setToolbarTitle("지금까지 한 일")
    return recordBinding.root
  }

  @SuppressLint("Recycle")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    whatIDo()
    whatIDont()

    iniBarChart(recordBinding.todoBarChart)
    setData(recordBinding.todoBarChart)
  }

  override fun onDestroy() {
    super.onDestroy()
    recordTodoListBinding = null
  }

  //  체크박스를 누르면 리스트를 새로고침한다
  @SuppressLint("DetachAndAttachSameFragment")
  private fun reload(selectedDate: String) {
    val fragment = RecordTodoFragment()
    val args = Bundle()
    args.putString("selectedDate", selectedDate)
    fragment.arguments = args

    parentFragmentManager.beginTransaction()
      .replace(id, fragment)
      .commit()
  }

  private fun whatIDo() {
    //    내가 한 일
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

  private fun whatIDont() {
    //    내가 안한 일
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

  //  바차트 설정
  private fun iniBarChart(barchart: BarChart) {
//    차트 회색 배경(false)
    barchart.setDrawGridBackground(false)
//  차트 그림자 (false)
    barchart.setDrawBarShadow(false)
//  차트 테두리(false)
    barchart.setDrawBorders(false)

    val description = Description()
//  오른쪽 하단 모서리 설명 레이블 텍스트(false)
    description.isEnabled = false
    barchart.description = description

//  x,y 바의 애니메이션 효과
    barchart.animateY(1000)
    barchart.animateX(1000)

//    x축
    val xAxis: XAxis = barchart.xAxis
//  x축 위치
    xAxis.position = XAxis.XAxisPosition.BOTTOM
//  그리드 선 수평 거리
    xAxis.granularity = 1f
//  x 축 텍스트 컬러
    xAxis.textColor = Color.RED
//  x 축 선 (default=true)
    xAxis.setDrawAxisLine(false)
//  격자선 (default=true)
    xAxis.setDrawGridLines(false)

//    y축
    val leftAxis: YAxis = barchart.axisLeft
//  좌측 선(default=true)
    leftAxis.setDrawAxisLine(false)
//  좌측 텍스트 컬러
    leftAxis.textColor = Color.BLUE

    val rightAxis: YAxis = barchart.axisRight
//  우측선 (default=true)
    rightAxis.setDrawAxisLine(false)
//  우측 텍스트 컬러
    rightAxis.textColor = Color.GREEN

//  타이틀
    val legend: Legend = barchart.legend
//  범례 모양
    legend.form = Legend.LegendForm.LINE
//  타이틀 텍스트 사이즈
    legend.textSize = 20f
//  타이틀 텍스트 컬러
    legend.textColor = Color.BLACK
//    범례 위치
    legend.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
    legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
//    범례 방향
    legend.orientation = Legend.LegendOrientation.HORIZONTAL
//    차트 내부에 범례가 위치하게 한다 (false)
    legend.setDrawInside(false)
  }

  private fun setData(barchart: BarChart) {
//    zoom in/out 가능 여부 설정
    barchart.setScaleEnabled(false)

    val valueList = ArrayList<BarEntry>()
    val title = "완료한 일"

//    임의 데이터
    for (i in 0 until 7) {
      valueList.add(BarEntry(i.toFloat(), i * 100f))
    }

    val barDataSet = BarDataSet(valueList, title)
//    바 색상 설정(ColorTemplate.LIBERTY_COLORS)
    barDataSet.setColors(
      Color.rgb(207, 248, 246), Color.rgb(148, 212, 212), Color.rgb(136, 180, 187),
      Color.rgb(118, 174, 175), Color.rgb(42, 109, 130)
    )

    val data = BarData(barDataSet)
    barchart.data = data
    barchart.invalidate()
  }
}
