package com.ble.drive_status_cntl

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    lateinit var bt_drive_panel :Button
    lateinit var bt_upload :Button
    var userName = "guest"

    var ecgdatalog : ArrayList<String> = arrayListOf<String>()
    var bcgdatalog : ArrayList<String> = arrayListOf<String>()
    var getlog = false
    var Car_index =0
    lateinit var sp_car : Spinner

    var carlist = arrayListOf(
        "toyota" , "lanbogini" , "farari"
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sp_car = findViewById(R.id.sp_car)
        val adapter2 = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item , carlist)

        sp_car.adapter = adapter2
        sp_car.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.e("Spinner", "select $p2")
                Car_index = p2
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Log.e("Spinner", "Nothing select")
            }
        }


    }
    fun clickpanel(view: View) {
        val intent = Intent(this, health_panel::class.java)

        intent.putExtra("user",userName)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, 10)

    }
    fun clickupload(view: View) {
        val intent = Intent(this, upload_data::class.java)
        if(getlog){
            intent.putExtra("ecgdatalog",ecgdatalog)
            intent.putExtra("bcgdatalog",bcgdatalog)

        }
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, 11)
    }
    fun clickECG(view: View) {

        val intent = Intent(this, ecg_collect::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, 12)

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==10 && resultCode == RESULT_OK){

            bcgdatalog = data?.getStringArrayListExtra("bcgdatalog")!!
            getlog = true
        }
        else if(requestCode == 20 && resultCode == RESULT_OK){
            userName = data?.getStringExtra("user")!!

            Toast.makeText(this, "user:"+ userName, Toast.LENGTH_LONG).show()
        }

        else{

        }

    }


    fun clickexit(view: View) {
            moveTaskToBack(true);
            exitProcess(-1)
    }//exit the app

    fun clickswitchuser(view: View) {
        val intent = Intent(this, changeuser::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, 20)

    }


    fun clicklogout(view: View) {

    }

    fun clickeditfile(view: View) {

    }


}