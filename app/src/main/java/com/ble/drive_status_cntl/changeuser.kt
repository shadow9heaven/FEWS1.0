package com.ble.drive_status_cntl

import android.bluetooth.le.ScanResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.ArrayList
import java.util.HashMap


class changeuser : AppCompatActivity() {

    lateinit var userfile : File
    lateinit var lv_user :ListView
    var adapter: SimpleAdapter? = null


    val testuserjson1 = arrayListOf( "{\"name\":\"jojo\" ,\"email\" :\"dreamfly878787\" , \"password\" : \" qazwsx\" } "
    , "{\"name\":\"kakyoin\" ,\"email\" :\"hirophant87\" , \"password\" : \" qazwsx\" } "
    ,"{\"name\":\"abuderu \" ,\"email\" :\"magicianred\" , \"password\" : \" qazwsx\" } "
    , "{\"name\":\"borunarefu\" ,\"email\" :\"silverchariot\" , \"password\" : \" qazwsx\" } "
    , "{\"name\":\"jojo\" ,\"email\" :\"hermitpurple\" , \"password\" : \" qazwsx\" } ")


    var userList = HashMap<String, String>()
    lateinit var userjson :JSONArray
    var devicename: List<HashMap<String, String>> = ArrayList()
    val listlabel = arrayOf("name", "email")
    val listid = intArrayOf(android.R.id.text1, android.R.id.text2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_changeuser)

        lv_user = findViewById(R.id.userlist)

        try{
            for(r in testuserjson1) {
                var jsonObject = JSONObject(r);
                var name = jsonObject.getString("name");
                var email = jsonObject.getString("email");
                //var password = jsonObject.getString("password");
                userList.put(name, email)
            }

        }
        catch(e :Exception){

        }
        //BLE_DeviceList.put(result!!.device.toString(), result)

        for (r in  userList) {

            val hashMap: HashMap<String, String> = HashMap()
            hashMap.put("name", r.key)
            hashMap.put("email", r.value)

            devicename += hashMap
            adapter = SimpleAdapter(
                    this,
                    devicename,
                    android.R.layout.simple_list_item_2,
                    listlabel,
                    listid
            );

            lv_user.setAdapter(adapter);
            lv_user.setOnItemClickListener{ parent, view, position, id ->
                clickuser(this, position)
            }
        }


    }

    private fun clickuser(changeuser: changeuser, position: Int) {
        Log.e("User: $position", userList.keys.elementAt(position))

        getIntent().putExtra("user", userList.keys.elementAt(position))
        setResult(RESULT_OK , getIntent())
        finish()


    }


    fun clickback(view: View) {
        setResult(RESULT_CANCELED, getIntent())
        finish()
    }
}