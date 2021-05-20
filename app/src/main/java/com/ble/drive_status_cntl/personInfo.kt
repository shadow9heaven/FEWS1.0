package com.ble.drive_status_cntl


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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

class personInfo : AppCompatActivity(),View.OnClickListener{
    lateinit var bt_register : Button
    lateinit var bt_back:Button
    lateinit var sp_drink : Spinner
    lateinit var sp_birthyear : Spinner
    lateinit var password:String
    lateinit var email:String
    lateinit var resStr :String
    lateinit var ed_username:EditText
    lateinit var ed_height:EditText
    lateinit var ed_weight:EditText
    lateinit var oid: String
    lateinit var Bsonpassword:String
    lateinit var cb_heartdisease:CheckBox
    lateinit var cb_hypertension:CheckBox
    lateinit var cb_scooter:CheckBox
    lateinit var cb_car:CheckBox
    lateinit var cb_truck:CheckBox
    lateinit var license:List<Int>
    lateinit var disease:List<Int>
    var birthyear=1961
    var timestamp:Long=0L
    var drink:Int=0
    var numberOfReq=0
    var istest:Boolean=false
    var ispost:Boolean =false
    var status:Boolean=false
    val rigester_url = "http://59.120.189.128:8081/data/biologueData"
    var ckeck_url="http://59.120.189.128:8081/data/biologueQuery"
    ////personal file
    lateinit var commandPath : File
    val filename = "emulated/0/personalFile_4_28.txt"
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_person_info)
        Log.e("register","3")

        status=intent.getBooleanExtra("status",false)
        findID()
        birth_spin()
        drink_spin()
        email= intent.getStringExtra("email").toString()
        password= intent.getStringExtra("password").toString()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "Request External Storage")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                9
            )
        }
        commandPath = File(Environment.getStorageDirectory().absolutePath)
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

    private fun findID(){
        bt_back=findViewById(R.id.bt_back)
        bt_back.setOnClickListener(this)
        bt_register=findViewById(R.id.bt_register)
        bt_register.setOnClickListener(this)
        sp_birthyear = findViewById(R.id.sp_birthyear)
        sp_drink=findViewById(R.id.sp_Drink)
        ed_height=findViewById(R.id.ed_height)
        ed_weight=findViewById(R.id.ed_weight)
        ed_username=findViewById(R.id.ed_username)
        cb_heartdisease=findViewById(R.id.cb_heartdisease)
        cb_hypertension=findViewById(R.id.cb_hypertension)
        cb_scooter=findViewById(R.id.cb_scooter)
        cb_car=findViewById(R.id.cb_car)
        cb_truck=findViewById(R.id.cb_truck)
    }
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.bt_register -> {
                if (status) {
                    userinput()
                }
                else {
                    license_check()
                    disease_check()
                    pack_personalfile2()
                    setResult(RESULT_OK, getIntent())
                    finish()
                }
            }
            R.id.bt_back->{
                finish()
            }
        }
    }
    private fun pack_personalfile1():Boolean{
        var file = File(commandPath, filename)
        var filestring:String?
        var personObject=JSONObject()
        filestring = file.readText(Charsets.UTF_8)
        personObject.put("oid",oid.toString())
        personObject.put("email",email.toString())
        personObject.put("password",password.toString())
        personObject.put("username",ed_username.text.toString())
        personObject.put("height",ed_height.text.toString().toInt())
        personObject.put("weight",ed_weight.text.toString().toInt())
        personObject.put("birth",birthyear.toInt())
        personObject.put("drink",drink.toInt())
        personObject.put("disease",JSONArray(disease))
        personObject.put("license",JSONArray(license))
        try {
            if (filestring.equals("null")) {
                Log.e("test","here")
                writeLog("[$personObject]")
            }
            else{
                filestring=filestring.removePrefix("[").removeSuffix("]")
                filestring=filestring+","+personObject
                writeLog("[$filestring]")
            }
        } catch (e:IOException) {
            Log.e("Error","$e")
            e.printStackTrace();
        }
        return false
    }
    private fun pack_personalfile2(){
        var file = File(commandPath, filename)
        var filestring:String?
        filestring = file.readText(Charsets.UTF_8)
        var personObject=JSONObject()
        personObject.put("oid","null")
        personObject.put("email","null")
        personObject.put("password","null")
        personObject.put("username",ed_username.text.toString())
        personObject.put("height",ed_height.text.toString().toInt())
        personObject.put("weight",ed_weight.text.toString().toInt())
        personObject.put("birth",birthyear.toInt())
        personObject.put("drink",drink.toInt())
        personObject.put("disease",JSONArray(disease))
        personObject.put("license",JSONArray(license))
        try {
            if (filestring.equals("null")) {

                Log.e("test","here")
                writeLog("[$personObject]")

            }
            else{
                filestring=filestring.removePrefix("[").removeSuffix("]")
                filestring=filestring+","+personObject
                writeLog("[$filestring]")
            }
        } catch (e:IOException) {
            Log.e("Error","$e")
            e.printStackTrace();
        }
        return
    }
    private fun generateHashedPass(pass: String): String {
        return BCrypt.hashpw(pass, BCrypt.gensalt())
    }
    private fun userinput(){
        timestamp= Date().time
        if (!email.isNullOrEmpty()){
            if (!password.isNullOrEmpty()){
                Bsonpassword=generateHashedPass(password)
                license_check()
                disease_check()
                checkRegister()
            }
            else Toast.makeText(this, "Password can't be empty.", Toast.LENGTH_SHORT).show()
        }
        else Toast.makeText(this, "Email can't be empty.", Toast.LENGTH_SHORT).show()
    }
    private fun packemail():JSONObject{
        val emailobejct=JSONObject()
        emailobejct.put("post_t", 0)/////string
        var jsonemail = JSONObject()
        jsonemail.put("\$regex", email)
        emailobejct.put("email", jsonemail)
        return emailobejct
    }
    private fun checkRegister(){
        getJSON(packemail())
        SystemClock.sleep(500)
        if (resStr.isNullOrEmpty()){
            getJSON(packemail())
            SystemClock.sleep(500)
            if (resStr.isNullOrEmpty()){
                fetchJSON()
            }
            else {
                Toast.makeText(this, "This email is used.", Toast.LENGTH_SHORT).show()
            }
        }
        else{
            Toast.makeText(this, "This username is used.", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getJSON(jsonObject:JSONObject){
        resStr=""
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonObject.toString().toRequestBody(mediaType)
        Log.e("Check", jsonObject.toString())
        val request: Request = Request.Builder()
            .url(ckeck_url)
            .post(body)
            .build()
        numberOfReq++;
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("fail : $e")
                Log.e("Error","connect failed.")
            }

            override fun onResponse(call: Call, response: Response) {
                numberOfReq--
                ispost = true
                resStr = response.body?.string().toString()
                Log.e("Response", "${resStr}")
                resStr = resStr.removePrefix("[")
                resStr = resStr.removeSuffix("]")
            }
        })
    }
    private fun fetchJSON(){
        var jsonObject= JSONObject()
        jsonObject.put("post_t", 0)/////string
        jsonObject.put("email", email.toString())
        jsonObject.put("password", Bsonpassword)
        jsonObject.put("username", ed_username.text.toString())/////string
        jsonObject.put("birthyear", birthyear.toInt())
        jsonObject.put("height", ed_height.text.toString().toInt())
        jsonObject.put("weight", ed_weight.text.toString().toInt())
        jsonObject.put("drink", drink.toInt())
        jsonObject.put("disease",JSONArray(disease))
        jsonObject.put("license",JSONArray(license))
        jsonObject.put("timestamp", Date().time)
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonObject.toString().toRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(rigester_url)
            .post(body)
            .build()
        numberOfReq++;
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("fail : $e")
                Log.e("Error", "$e")
            }

            override fun onResponse(call: Call, response: Response) {
                numberOfReq--
                ispost = true
                resStr = response.body?.string().toString()
                Log.e("Register Succeed", "${resStr}")
                oid=resStr
                pack_personalfile1()
                setResult(RESULT_OK, getIntent())
                Log.e("PERSONINFO","FINISH")
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
}