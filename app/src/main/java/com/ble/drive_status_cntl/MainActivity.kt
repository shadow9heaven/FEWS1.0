package com.ble.drive_status_cntl

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    lateinit var bt_drive_panel :Button
    lateinit var bt_upload :Button
    var userName = "guest"
    var userID = "hametorigun"
    var carid = "car"

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

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val intent = Intent(this, Initial::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, 30)


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
        intent.putExtra("userid",userID)
        intent.putExtra("car" ,carid )
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
        when(requestCode) {
            10->{
                if (resultCode == RESULT_OK) {

                    bcgdatalog = data?.getStringArrayListExtra("bcgdatalog")!!
                    getlog = true
                }
            }
             20->{
                 if (resultCode == RESULT_OK) {
                     userName = data?.getStringExtra("user")!!


                     Toast.makeText(this, "user:" + userName, Toast.LENGTH_LONG).show()
                 }
             }
            30->{
                if (resultCode == RESULT_OK) {

                } else {
                    finish()
                }
            }
            40->{
                if (resultCode == RESULT_OK) {
                   carid = data?.getStringExtra("carname")!!
                } else {

                }

            }
            else->{

            }
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
        userName = "guest"
        val intent = Intent(this, Initial::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, 30)
    }

    fun clickeditfile(view: View) {

    }

    fun clickchangecar(view: View) {
        val intent = Intent(this, Carinfo::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityForResult(intent, 40)
    }


}