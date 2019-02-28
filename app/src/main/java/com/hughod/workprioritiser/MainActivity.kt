package com.hughod.workprioritiser

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.WorkManager
import kotlin.math.roundToInt

const val SECOND = 1000L
const val DOWNLOADED_ITEMS_PREFS = "downloaded_editions"

class MainActivity : AppCompatActivity() {

    private val urls = listOf(
        "ZE URL #1",
        "ZE URL #2",
        "ZE URL #3",
        "ZE URL #4",
        "ZE URL #5",
        "ZE URL #6",
        "ZE URL #7",
        "ZE URL #8",
        "ZE URL #9",
        "ZE URL #10",
        "ZE URL #11",
        "ZE URL #12",
        "ZE URL #13",
        "ZE URL #14",
        "ZE URL #15",
        "ZE URL #16",
        "ZE URL #17",
        "ZE URL #18",
        "ZE URL #19",
        "ZE URL #20"
    )
    private val downloadedUrls = LinkedHashSet<String>()

    private val sharedPreferences: SharedPreferences by lazy {
        getSharedPreferences(DOWNLOADED_ITEMS_PREFS, Context.MODE_PRIVATE)
    }
    private val downloader by lazy {
        Downloader(
            WorkManager.getInstance(),
            this,
            sharedPreferences
        )
    }
    private val textView by lazy { findViewById<TextView>(R.id.downloaded_list)!!.apply { movementMethod = ScrollingMovementMethod() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val downloadData = DownloadData("edition id", urls, urls[6])

        downloader.reset()

        downloader.startWork(downloadData)

        downloader.progress { info ->
            downloadedUrls.addAll(info)
            updateTextView()
        }

        Handler().postDelayed({
            downloadData.priorityUrl = urls[14]
            downloader.rePrioritise(downloadData)
        }, SECOND.times(5))

        Handler().postDelayed({
            downloadData.priorityUrl = urls[urls.size - 1]
            downloader.rePrioritise(downloadData)
        }, SECOND.times(10))

        Handler().postDelayed({
            downloadData.priorityUrl = urls[11]
            downloader.rePrioritise(downloadData)
        }, SECOND.times(20))

        Handler().postDelayed({
            downloadData.priorityUrl = urls[urls.size - 2]
            downloader.rePrioritise(downloadData)
        }, SECOND.times(30))
    }

    override fun onDestroy() {
        downloader.progress(null)
        super.onDestroy()
    }

    private fun updateTextView() {
        val sb = StringBuilder()

        downloadedUrls.forEachIndexed { i, it ->
            if (downloadedUrls.contains(it)) sb.append("\n\t${i + 1}:\t").append(it).append("\n")
        }

        val percentage = (downloadedUrls.size.toDouble().div(urls.size.toDouble()) * 100).roundToInt()

        findViewById<TextView>(R.id.downloaded_percentage).text = "${downloadedUrls.size}/${urls.size} - $percentage%"

        textView.text = sb.toString()
    }
}
