package com.aaditya.mediashrinker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView:
            RecyclerView

    private lateinit var historyAdapter:
            HistoryAdapter

    private val historyList =
        mutableListOf<CompressionHistory>()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_history
        )

        historyRecyclerView =
            findViewById(
                R.id.historyRecyclerView
            )

        historyRecyclerView.layoutManager =
            LinearLayoutManager(this)

        loadHistory()

        historyAdapter =
            HistoryAdapter(historyList)

        historyRecyclerView.adapter =
            historyAdapter
    }

    private fun loadHistory() {

        val prefs =
            getSharedPreferences(
                "MediaShrinkerHistory",
                MODE_PRIVATE
            )

        val historyData =
            prefs.getStringSet(
                "history",
                mutableSetOf()
            )

        historyData?.forEach { item ->

            val parts =
                item.split("|")

            if (parts.size == 4) {

                historyList.add(

                    CompressionHistory(

                        parts[0],

                        parts[1],

                        parts[2],

                        parts[3]
                    )
                )
            }
        }
    }
}