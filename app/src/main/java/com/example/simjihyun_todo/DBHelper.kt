package com.example.simjihyun_todo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DBHelper(context: Context) : SQLiteOpenHelper(context, "todoList", null, 1) {
  override fun onCreate(db: SQLiteDatabase?) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val startDate = LocalDateTime.now()
//    오늘 날을 기준으로 23시 59분 59 초까지
    val endDate = LocalDate.now().atTime(23,59,59)

//    db 생성
    db?.execSQL("create table TODO_LIST(" +
            "id integer primary key autoincrement, " +
            "name varchar(100) not null," +
            "start_date varchar(100) not null, " +
            "end_date varchar(100) not null," +
            "is_completed char(1) not null default 'N', " +
            "is_important char(1) not null default 'N'," +
            "memo varchar(200) )"
    )

//    기본값 넣기
    db?.execSQL("insert into TODO_LIST(name, start_date, end_date)" +
            "values ('일정1', '${startDate.format(formatter)}', '${endDate.format(formatter)}')")
  }

  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    TODO("Not yet implemented")
  }

}