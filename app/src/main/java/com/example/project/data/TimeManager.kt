package com.example.project.data



data class TimeManager(
    val date: String? = null,
    val workTime: Int? = null,
    val extraTime: Int? = null,
    val durationInSecond: Long? = null,
    val late: Boolean? = null,
    val absent: Boolean? = null
)
