package com.hughod.workprioritiser

import com.google.gson.Gson

data class DownloadData(val id: String, val urls: List<String>, var priorityUrl: String = "")

data class DownloadItemData(val id: String, val url: String) {
    fun toJson(): String? = Gson().toJson(this)
}

fun String.fromJson(): DownloadItemData = Gson().fromJson(this, DownloadItemData::class.java)
