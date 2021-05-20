package com.ble.drive_status_cntl

import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.util.*


class loginactivity : AppCompatActivity(), View.OnClickListener {
    lateinit var bt_login : Button
    lateinit var bt_back:Button
    lateinit var ed_email: EditText
    lateinit var ed_password: EditText
    lateinit var username :String
    lateinit var email:String
    lateinit var password:String
    lateinit var resStr :String
    lateinit var resStr_password:String
    lateinit var message:String
    lateinit var oid:String
    var timestamp:Long=0L
    var jsonObject = JSONObject()
    var numberOfReq=0
    var ispost:Boolean =false
    var url = "http://59.120.189.128:8081/data/biologueQuery"
    ///
    lateinit var commandPath : File
    val filename = "emulated/0/personalFile_4_28.txt"
    @RequiresApi(Build.VERSION_CODES.R)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loginactivity)
        findID()
        commandPath = File(Environment.getStorageDirectory().absolutePath)

    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==1&&resultCode == RESULT_OK){
            message = data?.getStringExtra("userID").toString()
            Log.e("loginactivity ", message)
        }
        else if(requestCode==2&&resultCode == RESULT_OK){

            setResult(RESULT_OK, getIntent())
            finish()
        }
        else {
            Log.e("resultCode", "${resultCode.toString()}")
        }
    }

    private fun findID(){
        bt_back=findViewById(R.id.bt_loginback)
        bt_back.setOnClickListener(this)
        ///
        bt_login= findViewById(R.id.bt_login)
        bt_login.setOnClickListener(this)
        ////
        ed_email=findViewById(R.id.ed_email)
        ///
        ed_password=findViewById(R.id.ed_password)
    }
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.bt_login -> {
                Login()
            }
            R.id.bt_loginback -> {
                finish()
            }
        }
    }
    private fun Login(){
        timestamp= Date().time
        email=ed_email.text.toString()
        password=ed_password.text.toString()
        getJSON()
        Log.e("time", "${Date().time}")
        SystemClock.sleep(500)
        if(numberOfReq==0&&ispost==true && !resStr.isNullOrEmpty())checkPassword(resStr)
        else Toast.makeText(this, "Account or email is wrong.$password", Toast.LENGTH_SHORT).show()
    }
    private fun getJSON(){
        jsonObject.put("post_t", 0)/////string
        var jsonemail = JSONObject()
        jsonemail.put("\$regex", email)
        jsonObject.put("email", jsonemail)
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonObject.toString().toRequestBody(mediaType)
        val request: Request = Request.Builder()
                .url(url)
                .post(body)
                .build()
        numberOfReq++;
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("fail : $e")
                Log.e("Error", "connect failed.")
            }

            override fun onResponse(call: Call, response: Response) {
                numberOfReq--
                ispost = true
                resStr = response.body?.string().toString()
                Log.e("Response", "${resStr}")
                resStr = resStr.removePrefix("[").removeSuffix("]")
            }
        })
    }
    private fun checkPassword(resStr: String){
        val Json:JSONObject= JSONObject(resStr)
        resStr_password=Json.getString("password")
        oid=Json.getJSONObject("_id")?.getString("\$oid").toString()
        Log.e("loginactivity", "$oid")
        if(BCrypt.checkpw(password, resStr_password)){
            pack_personfile(Json)
            Toast.makeText(this, "Login", Toast.LENGTH_SHORT).show()
            username=Json.getString("username").toString()
            getIntent().putExtra("user", username)
            setResult(RESULT_OK, getIntent())
            finish()

            //val intent = Intent(this, Carinfo::class.java)
            //intent.putExtra("oid", oid)
            //startActivityForResult(intent, 2)
            //Toast.makeText(this, "Please add the information of your car.", Toast.LENGTH_SHORT).show()
        }
        else Toast.makeText(this, "Password is wrong", Toast.LENGTH_SHORT).show()
    }
    private fun pack_personfile(personInfo: JSONObject){
        var file = File(commandPath, filename)
        var filestring:String?
        var personObject=JSONObject()
        personObject.put("oid",oid.toString())
        personObject.put("email",email.toString())
        personObject.put("password",password.toString())
        personObject.put("username",personInfo.getString("username").toString())
        personObject.put("height",personInfo.getString("height").toInt())
        personObject.put("weight",personInfo.getString("weight").toInt())
        personObject.put("birth",personInfo.getString("birthyear").toInt())
        personObject.put("drink",personInfo.getString("drink").toInt())
        personObject.put("disease",JSONArray(personInfo.getString("disease")))
        personObject.put("license",JSONArray(personInfo.getString("license")))
        filestring = file.readText(Charsets.UTF_8)
        try {
            if (filestring.equals("null")) {
                Log.e("test","here")
                writeLog("[$personObject]")
            }
            else{
                var file = File(commandPath, filename)
                var filestring = file.readText(Charsets.UTF_8)
                var array= JSONArray(filestring)
                var check=false
                for(i in 0 until array.length() step 1){
                    var checkemail=array.getJSONObject(i).getString("email")
                    if(checkemail.equals(email)){
                        check=true
                        array.getJSONObject(i).put("oid",personObject.getString("oid").toString())
                        array.getJSONObject(i).put("username",personObject.getString("username").toString())
                        array.getJSONObject(i).put("height",personObject.getString("height").toInt())
                        array.getJSONObject(i).put("weight",personObject.getString("weight").toInt())
                        array.getJSONObject(i).put("birth",personObject.getString("birth").toInt())
                        array.getJSONObject(i).put("drink",personObject.getString("drink").toInt())
                        array.getJSONObject(i).put("disease",JSONArray(personObject.getString("disease")))
                        array.getJSONObject(i).put("license",JSONArray(personObject.getString("license")))
                        writeLog(array.toString())
                        break
                    }
                }
                if(!check){
                    filestring=filestring.removePrefix("[").removeSuffix("]")
                    filestring=filestring+","+personObject
                    writeLog("[$filestring]")
                }
            }
        } catch (e:IOException) {
            Log.e("Error","$e")
            e.printStackTrace();
        }
        return
    }
    private fun writeLog(input:String){
        var file= File(commandPath,filename)
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            file.writeText(input)
        } catch (e:IOException) {
            e.printStackTrace();
        }
    }
}