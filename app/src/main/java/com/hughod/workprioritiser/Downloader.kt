package com.hughod.workprioritiser

import android.annotation.SuppressLint
import android.content.SharedPreferences
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.work.*
import com.hughod.workprioritiser.DownloadPageWorker.Companion.WORK_DATA

class Downloader(
    private val workManager: WorkManager,
    private val lifecycleOwner: LifecycleOwner,
    private val sharedPreferences: SharedPreferences
) {

    private var progressFun: ((List<String>) -> Unit)? = null
    private var trackingSet: MutableSet<String> = mutableSetOf()

    fun startWork(downloadData: DownloadData): Boolean {

        val work = buildWorkForEdition(downloadData)

        var continuation = workManager.beginWith(work.removeAt(0))

        work.forEach { continuation = continuation.then(it) }

        continuation.enqueue()

        workManager.getWorkInfosByTagLiveData(downloadData.id).observe(lifecycleOwner, Observer {
            trackingSet = sharedPreferences.getStringSet(downloadData.id, trackingSet) ?: trackingSet
            progressFun?.invoke(trackingSet.toList())
        })

        return work.isNotEmpty()
    }

    fun progress(function: ((List<String>) -> Unit)?) {
        progressFun = function
    }

    fun rePrioritise(downloadData: DownloadData) {
        cancelWork(downloadData.id) {
            startWork(downloadData)
        }
    }

    @SuppressLint("ApplySharedPref")
    fun reset() {
        workManager.pruneWork()
        sharedPreferences.edit().clear().commit()
    }

    private fun buildWorkForEdition(downloadData: DownloadData): ArrayList<OneTimeWorkRequest> {
        val workRequests = ArrayList<OneTimeWorkRequest>()

        val sorted = if (downloadData.priorityUrl.isNotBlank()) {
            downloadData.urls.sort(downloadData.priorityUrl)
        } else downloadData.urls

        for (url in sorted) {
            val workRequest = OneTimeWorkRequestBuilder<DownloadPageWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(WORK_DATA, DownloadItemData(downloadData.id, url).toJson())
                        .build())
                .addTag(url) // so we can cancel specific work (by url)
                .addTag(downloadData.id) // so we can cancel all work by id
                .build()

            if (url == downloadData.priorityUrl) workRequests.add(0, workRequest)
            else workRequests.add(workRequest)
        }

        return workRequests
    }

    private fun cancelWork(id: String, success: () -> Unit): Boolean {
        val cancelAllOperation = workManager.cancelAllWorkByTag(id)

        cancelAllOperation.state.observe(lifecycleOwner, Observer {
            when (it) {
                is Operation.State.SUCCESS -> success()
            }
        })

        return true
    }

    private fun List<String>.sort(string: String): List<String> {
        val sorted = ArrayList<String>()

        val subList = this.subList(indexOf(string), size)
        val subList1 = this.subList(0, indexOf(string))

        sorted.addAll(subList)
        sorted.addAll(subList1)

        return sorted
    }
}