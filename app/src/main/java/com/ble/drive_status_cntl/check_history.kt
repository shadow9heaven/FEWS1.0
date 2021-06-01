package com.ble.drive_status_cntl

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import com.bluetooth.bth_k2.DataPoint
import com.bluetooth.bth_k2.GraphView
import java.lang.Exception
import java.util.*


class check_history : AppCompatActivity() {

    lateinit var dateText : TextView
    lateinit var hourText : TextView
    lateinit var spr_hour : Spinner
    var GV: GraphView? = null

    val hourlist = arrayListOf(
            "00:00-01:00", "01:00-02:00", "02:00-03:00", "03:00-04:00", "04:00-05:00", "05:00-06:00"
            ,"06:00-07:00", "07:00-08:00", "08:00-09:00", "09:00-10:00", "10:00-11:00", "11:00-12:00"
            ,"12:00-13:00", "13:00-14:00", "14:00-15:00", "15:00-16:00", "16:00-17:00", "17:00-18:00"
            ,"18:00-19:00", "19:00-20:00", "20:00-21:00", "21:00-22:00", "22:00-23:00", "23:00-00:00"

        )
    lateinit var dateButton : Button
    private var mYear = 0
    private var mMonth = 0
    private var mDay = 0
    private var mHour = 0

    var date_choosed= false

    var user = "guest"
    var userID = "hametorigun"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_history)

        try{
            user  = intent?.getStringExtra("user")!!
            userID = intent?.getStringExtra("userid")!!
            //ECG_DATA_DIRECTORY = user
        }
        catch(e : Exception){

        }
        dateText = findViewById(R.id.dateText)
        hourText = findViewById(R.id.hourText)
        GV = findViewById(R.id.gv_check_BCG)

        dateButton = findViewById(R.id.dateButton)

        spr_hour = findViewById(R.id.spr_hour)
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item , hourlist)

        spr_hour.adapter = adapter2
        spr_hour.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.e("Spinner", "select $p2")
                mHour = p2
                if(date_choosed){
//////////////////////request data


//////////////////////request data
                }

            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.e("Spinner", "Nothing select")
            }
        }

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
            date_choosed = true
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