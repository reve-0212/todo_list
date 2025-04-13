package com.example.simjihyun_todo

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) : SQLiteOpenHelper(context, "todoList", null, 1) {
  override fun onCreate(db: SQLiteDatabase?) {
//    db 생성
    db?.execSQL("create table TODO_LIST(" +
            "id integer primary key autoincrement, " +
            "name varchar(100) not null," +
            "start_date date not null, " +
            "end_date date not null," +
            "is_completed char(1) not null default 'N', " +
            "is_important char(1) not null default 'N' )"
    )

//    기본값 넣기
    db?.execSQL("insert into TODO_LIST(name, start_date, end_date)" +
            "values ('일정1','2025-04-12','2025-04-12')")
  }

  override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
    TODO("Not yet implemented")
  }

}