//package com.example.simjihyun_todo
//
//import android.content.Context
//import androidx.core.content.ContextCompat
//import com.prolificinteractive.materialcalendarview.CalendarDay
//import com.prolificinteractive.materialcalendarview.DayViewDecorator
//import com.prolificinteractive.materialcalendarview.DayViewFacade
//
//@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
//class DateDecorator(private val context: Context, private val date: CalendarDay) :
//  DayViewDecorator {
//  override fun shouldDecorate(day: CalendarDay?): Boolean {
//    return day == date
//  }
//
//  override fun decorate(view: DayViewFacade) {
//    view.setBackgroundDrawable(
//      ContextCompat.getDrawable(context, R.drawable.today_day_bg)
//    )
//  }
//
//}