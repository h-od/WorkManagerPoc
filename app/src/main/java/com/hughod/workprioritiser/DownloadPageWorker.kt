package com.hughod.workprioritiser

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import androidx.work.Worker
import androidx.work.WorkerParameters

class DownloadPageWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(DOWNLOADED_ITEMS_PREFS, Context.MODE_PRIVATE)

    override fun doWork(): Result = with(getDataJson().fromJson()) {
        if (!sharedPreferences.hasEntryThatContains(this)) {

            simulateDownload() //todo actually download instead

            println("downloaded: $this") //todo replace with debug logs

            sharedPreferences.updateSharedPrefs(this)

        } else {
            println("skipped: $this")
        }

        Result.success()
    }

    private fun getDataJson(): String = inputData.getString(WORK_DATA)
        ?: throw IllegalArgumentException("A Json representation of DownloadItemData is required")

    private fun SharedPreferences.hasEntryThatContains(downloadItemData: DownloadItemData): Boolean {
        val stringSet = getStringSet(downloadItemData.id, HashSet<String>()) ?: return false

        for (string in stringSet) if (string == downloadItemData.url) return true

        return false
    }

    @SuppressLint("ApplySharedPref")
    private fun SharedPreferences.updateSharedPrefs(downloadItemData: DownloadItemData) {
        val set: MutableSet<String> = getStringSet(downloadItemData.id, HashSet<String>())
            ?: HashSet()

        set.add(downloadItemData.url)

        edit().putStringSet(downloadItemData.id, set).commit()
    }

    private fun simulateDownload() {
        Thread.sleep((2 * SECOND))
    }

    companion object {
        const val WORK_DATA = "WORK_DATA"
    }
}
