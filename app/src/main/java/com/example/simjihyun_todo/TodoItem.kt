package com.example.simjihyun_todo

import java.util.Date

data class TodoItem(
  val id: Int,
  var name: String,
  var startDate: Date,
  var endDate: Date,
  var isCompleted: Boolean,
  var isImportant: Boolean
)