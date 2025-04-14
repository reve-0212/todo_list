package com.example.simjihyun_todo

import java.time.LocalDateTime

data class TodoItem(
  val id: Int,
  var name: String,
  var startDate: LocalDateTime,
  var endDate: LocalDateTime,
  var isCompleted: Boolean,
  var isImportant: Boolean,
  var memo: String
)