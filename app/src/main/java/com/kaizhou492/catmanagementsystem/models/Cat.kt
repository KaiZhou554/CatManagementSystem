package com.kaizhou492.catmanagementsystem.models

import com.google.gson.Gson

data class Cat(
    val id: Long,
    var name: String,
    val breed: String,
    val skinColor: String,
    val eyeColor: String,
    var interacted: Boolean = false,
    var emoji: String? = null,
    var interactionResetTime: Long? = null,
    var lastFedTime: Long = System.currentTimeMillis(),
    var brightness: Float = 1f,
    var saturation: Float = 1f
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): Cat = Gson().fromJson(json, Cat::class.java)
    }
}

data class CatteryState(
    val cats: List<Cat> = emptyList(),
    val adoptionsThisWeek: Int = 0,
    val weekStartTime: Long = System.currentTimeMillis(),
    val foodClickedThisWeek: Boolean = false,
    val waterClickedThisWeek: Boolean = false,
    val autoFeederEnabled: Boolean = false,
    val language: String = "zh",
    val pityCounter: Int = 0, // 小保底计数
    val guaranteeCounter: Int = 0 // 大保底计数
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {
        fun fromJson(json: String): CatteryState =
            Gson().fromJson(json, CatteryState::class.java)
    }
}
