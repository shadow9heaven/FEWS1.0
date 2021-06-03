package com.ble.drive_status_cntl

import android.app.AlertDialog
import android.content.DialogInterface
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
    lateinit var message:String
    lateinit var _id:String
    var timestamp:Long=0L
    var jsonObject = JSONObject()
    var numberOfReq=0
    var url = "http://59.120.189.128:8082/users"
    var isreg=false
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
                SystemClock.sleep(500)
                if(!isreg)ifRegistered(resStr)
                else checkPassword(foundLast(resStr).getString("password"))

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
        var getdata=url+"/email="+email
        val client = OkHttpClient()
        val request: Request = Request.Builder()
                .url(getdata)
                .get()
                .build()
        numberOfReq++;
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("fail : $e")
                Log.e("Error", "connect failed.")
            }
            override fun onResponse(call: Call, response: Response) {
                numberOfReq--
                resStr = response.body?.string().toString()
                if(!JSONObject(resStr).isNull("data")){
                    isreg=true
                }
            }
        })
    }
    private fun foundLast(resStr:String):JSONObject{
        var array=JSONArray(JSONObject(resStr).getString("data"))
        var max=0
        var LastTime :Long =0L
        for(i in 0 until array.length() step 1){
            var time=array.getJSONObject(i).getString("timestamp").toLong()
            if(time>LastTime){
                LastTime=time
                max=i
            }
        }
        return array.getJSONObject(max)
    }
    private fun checkPassword(resStr_password: String){
        if(BCrypt.checkpw(password, resStr_password)){
            pack_personfile(foundLast(resStr))
            Toast.makeText(this, "Login", Toast.LENGTH_SHORT).show()
            username=foundLast(resStr).getString("username").toString()
            getIntent().putExtra("user", username)
            setResult(RESULT_OK, getIntent())
            finish()

        }
        else Toast.makeText(this, "Password is wrong", Toast.LENGTH_SHORT).show()
    }
    private fun pack_personfile(personInfo: JSONObject){
        var file = File(commandPath, filename)
        var filestring:String?
        var personObject=JSONObject()
        personObject.put("_id",personInfo.getString("_id"))
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
                        array.getJSONObject(i).put("_id",personObject.getString("_id").toString())
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
    private fun ifRegistered(resStr:String){
        AlertDialog.Builder(this)
                .setTitle("Suggest")
                .setMessage("this email is never registered.")
                .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
                    Log.e("ResStr","${resStr}")
                })
                .setCancelable(false)
                .show()
    }
}