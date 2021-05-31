package com.ble.drive_status_cntl

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.TimePicker
import com.bluetooth.bth_k2.DataPoint
import com.bluetooth.bth_k2.GraphView
import java.util.*


class check_history : AppCompatActivity() {

    lateinit var dateText : TextView
    lateinit var hourText : TextView

    lateinit var dateButton : Button
    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0
    var user = "guest"
    var userID = "hametorigun"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_history)


        dateText= findViewById(R.id.dateText)
        hourText= findViewById(R.id.hourText)

        dateButton= findViewById(R.id.dateButton)
    }

    fun clickdate(view: View) {
        val c = Calendar.getInstance()

        mYear = c[Calendar.YEAR]
        mMonth = c[Calendar.MONTH]
        mDay = c[Calendar.DAY_OF_MONTH]
        mHour = c[Calendar.HOUR_OF_DAY]


        DatePickerDialog(this, { view, year, month, day ->
            val format = "您設定的日期為:" + setDateFormat(year, month, day)
            dateText.text = format
        }, mYear, mMonth, mDay).show()
        TimePicker(this)
/*
        TimePickerDialog(this, { view, hour, minute ->
            val format = "您設定的時間為:" + setDateFormat(year, month, day)
            hourText.text = format
        },mHour).show()
*/

    }
    fun setDateFormat(year: Int, monthOfYear: Int, dayOfMonth: Int): String {
        return (year.toString() + "-"
                + (monthOfYear + 1).toString() + "-"
                + dayOfMonth.toString())
    }

    fun clickback(view: View) {
            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
    }

}