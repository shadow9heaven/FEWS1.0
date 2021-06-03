package com.ble.drive_status_cntl


import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.reflect.typeOf

class EditProfile : AppCompatActivity(),View.OnClickListener{
    lateinit var bt_back:Button
    lateinit var bt_confirm:Button
    lateinit var sp_drink : Spinner
    lateinit var sp_birthyear : Spinner
    lateinit var resStr :String
    lateinit var ed_username:EditText
    lateinit var ed_height:EditText
    lateinit var ed_weight:EditText
    lateinit var _id: String
    lateinit var cb_heartdisease:CheckBox
    lateinit var cb_hypertension:CheckBox
    lateinit var cb_scooter:CheckBox
    lateinit var cb_car:CheckBox
    lateinit var cb_truck:CheckBox
    lateinit var license:List<Int>
    lateinit var disease:List<Int>
    var birthyear=1961
    var drink:Int=0
    var istest:Boolean=false
    var ispost:Boolean =false
    var status:Boolean=false
    var user=-1

    val url = "http://59.120.189.128:8082/users"
    ////personal file
    lateinit var commandPath : File
    val filename = "emulated/0/personalFile_4_28.txt"
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)
        user=intent.getIntExtra("user",-1)
        findID()
        birth_spin()
        drink_spin()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "Request External Storage")
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    9
            )
        }
        commandPath = File(Environment.getStorageDirectory().absolutePath)
        InitialSet()
    }
    private fun writeLog(input:String){
        var file=File(commandPath,filename)
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            file.writeText(input)
        } catch (e:IOException) {
            e.printStackTrace();
        }
    }
    private fun InitialSet(){
        var file = File(commandPath, filename)
        var filestring = file.readText(Charsets.UTF_8)
        val array = JSONArray(filestring)
        val personObject: JSONObject = JSONObject(array[user].toString())
        Log.e("PERSON","$personObject")
        var diseaseArray=JSONArray(personObject.getString("disease"))
        var licenseArray=JSONArray(personObject.getString("license"))
        ed_height.setText(personObject.getString("height").toString())
        ed_weight.setText(personObject.getString("weight").toString())
        ed_username.setText(personObject.getString("username").toString())
        Log.e("Name",personObject.getString("username").toString())
        Log.e("height",personObject.getString("height").toString())
        Log.e("weight",personObject.getString("weight").toString())
        sp_birthyear.setSelection(personObject.getString("birth").toInt()-1961)
        sp_drink.setSelection(personObject.getString("drink").toInt())
        if(diseaseArray[0]==1)cb_heartdisease.isChecked=true
        if(diseaseArray[1]==1)cb_hypertension.isChecked=true
        if(licenseArray[0]==1)cb_scooter.isChecked=true
        if(licenseArray[1]==1)cb_car.isChecked=true
        if(licenseArray[2]==1)cb_truck.isChecked=true
    }
    private fun findID(){
        bt_confirm=findViewById(R.id.ep_bt_confirm)
        bt_confirm.setOnClickListener(this)
        bt_back=findViewById(R.id.ep_bt_back)
        bt_back.setOnClickListener(this)
        sp_birthyear = findViewById(R.id.ep_sp_birthyear)
        sp_drink=findViewById(R.id.ep_sp_Drink)
        ed_height=findViewById(R.id.ep_ed_height)
        ed_weight=findViewById(R.id.ep_ed_weight)
        ed_username=findViewById(R.id.ep_ed_username)
        cb_heartdisease=findViewById(R.id.ep_cb_heartdisease)
        cb_hypertension=findViewById(R.id.ep_cb_hypertension)
        cb_scooter=findViewById(R.id.ep_cb_scooter)
        cb_car=findViewById(R.id.ep_cb_car)
        cb_truck=findViewById(R.id.ep_cb_truck)
    }
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.ep_bt_confirm -> {
                update()

            }
            R.id.ep_bt_back->{
                finish()
            }
        }
    }
    private fun update(){
        license_check()
        disease_check()
        var file = File(commandPath, filename)
        var filestring = file.readText(Charsets.UTF_8)
        val array = JSONArray(filestring)
        array.getJSONObject(user).put("username",ed_username.text.toString())
        array.getJSONObject(user).put("height",ed_height.text.toString().toInt())
        array.getJSONObject(user).put("weight",ed_weight.text.toString().toInt())
        array.getJSONObject(user).put("birth",birthyear.toInt())
        array.getJSONObject(user).put("drink",drink.toInt())
        array.getJSONObject(user).put("disease",JSONArray(disease))
        array.getJSONObject(user).put("license",JSONArray(license))
        writeLog(array.toString())
        Log.e("_id",array.getJSONObject(user).getString("_id").toString())
        if(array.getJSONObject(user).getString("_id").equals("null")   ||  !CheckConnectStatus())finish()
        else updateAPI(array.getJSONObject(user))
    }
    private fun updateAPI(personObjects: JSONObject){
        var get_id=url+"/userid="+personObjects.getString("_id")
        var jsonObject= JSONObject()
        jsonObject.put("username", personObjects.getString("username"))/////string
        jsonObject.put("birthyear",personObjects.getString("birth"))
        jsonObject.put("height", personObjects.getString("height"))
        jsonObject.put("weight", personObjects.getString("weight"))
        jsonObject.put("drink", personObjects.getString("drink"))
        jsonObject.put("disease",JSONArray(personObjects.getString("disease")))
        jsonObject.put("license",JSONArray(personObjects.getString("license")))
        jsonObject.put("timestamp", Date().time)
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonObject.toString().toRequestBody(mediaType)
        val request: Request = Request.Builder()
                .url(get_id)
                .put(body)
                .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("fail : $e")
                Log.e("Error", "$e")
            }
            override fun onResponse(call: Call, response: Response) {
                ispost = true
                resStr = response.body?.string()!!
                Log.e("long","${resStr.length}")
                setResult(RESULT_OK, getIntent())
                Log.e("PERSONINFO",resStr)
                finish()
            }
        })
    }
    private fun drink_spin(){
        val drinklist = arrayListOf(
                "Never", "Once a year", "Once every six months","Once every three months","Once every two months",
                "Once every months","Once every three weeks","Once every two weeks","Once every weeks",
                "Once every two days","EveryDay"
        )
        val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                drinklist
        )
        sp_drink.adapter = adapter
        sp_drink.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.e("Spinner", "select $p2")
                drink = p2
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }
    private fun birth_spin(){
        val birthyearlist = arrayListOf(
                "1961", "1962", "1963", "1964", "1965", "1966", "1967", "1968", "1969", "1970",
                "1971", "1972", "1973", "1974", "1975", "1976", "1977", "1978", "1979", "1980",
                "1981", "1982", "1983", "1984", "1985", "1986", "1987", "1988", "1989", "1990",
                "1991", "1992", "1993", "1994", "1995", "1996", "1997", "1998", "1999", "2000",
                "2001", "2002", "2003", "2004", "2005", "2006", "2007", "2008", "2009", "2010"
        )
        val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                birthyearlist
        )
        sp_birthyear.adapter = adapter
        sp_birthyear.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.e("Spinner", "select $p2")
                birthyear = birthyearlist[p2].toInt()
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }
    private fun license_check(){
        license= listOf()
        if(cb_scooter.isChecked)license += 1
        else license+=0
        if (cb_car.isChecked)license += 1
        else license+=0
        if (cb_truck.isChecked)license += 1
        else license+=0
        Log.e("license","$license")
    }
    private fun disease_check(){
        disease= listOf()
        if (cb_heartdisease.isChecked)disease+=1
        else disease+=0
        if (cb_hypertension.isChecked)disease+=1
        else disease+=0
        Log.e("disease","${disease}")
    }
    private fun CheckConnectStatus():Boolean{
        val ConnectionManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = ConnectionManager.activeNetworkInfo
        if (networkInfo != null && networkInfo.isConnected == true) {
            return true
        } else {
            return false
        }
    }
}