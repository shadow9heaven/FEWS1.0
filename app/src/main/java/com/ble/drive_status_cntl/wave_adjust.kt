package com.ble.drive_status_cntl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View

class wave_adjust : AppCompatActivity() {
    //var

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wave_adjust)
    }

    fun clickapply(view: View) {


        setResult(RESULT_OK)
        finish()
    }
    fun clickback(view: View) {
        finish()
    }
}