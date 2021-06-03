package com.ble.drive_status_cntl


import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.graphics.rotationMatrix
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.util.*
import kotlin.collections.RandomAccess

class Register : AppCompatActivity(), View.OnClickListener{
    lateinit var bt_register : Button
    lateinit var bt_back:Button
    lateinit var ed_email: EditText
    lateinit var ed_password: EditText
    lateinit var ed_checkpassword:EditText
    lateinit var email:String
    lateinit var password:String
    lateinit var resStr :String
    lateinit var checkpassword:String
    var ifreg=true
    var user=-1
    var timestamp:Long=0L
    var numberOfReq=0
    var ispost:Boolean =false
    var url = "http://59.120.189.128:8082/users"
    val regex_email=Regex("[a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z.]{2,18}")
    val regex_password=Regex("[A-Za-z0-9_.-]{1,32}")
    ////personal file
    lateinit var commandPath : File
    val filename = "emulated/0/personalFile_4_28.txt"
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        findID()
        commandPath = File(Environment.getStorageDirectory().absolutePath)
        user=intent.getIntExtra("user",-1)
    }
    private fun findID(){
        bt_register=findViewById(R.id.bt_register)
        bt_register.setOnClickListener(this)
        ed_email=findViewById(R.id.ed_email)
        ed_password=findViewById(R.id.ed_password)
        ed_checkpassword=findViewById(R.id.ed_checkpassword)
        bt_back=findViewById(R.id.bt_back)
        bt_back.setOnClickListener(this)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==30 && resultCode == RESULT_OK){
            Log.e("TEST","request2")
            setResult(RESULT_OK, getIntent())
            finish()
        }
        else{
            Log.e("MainActivity","Error.")
        }
    }
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.bt_register -> {
                userinput()
            }
            R.id.bt_back->{
                finish()
            }
        }
    }
    private fun sendmessage(){
        val intent = Intent(this, personInfo::class.java)
        intent.putExtra("email",email)
        intent.putExtra("password",password)
        intent.putExtra("status",true)
        Log.e("SEND","request2")
        startActivityForResult(intent, 30)
    }

    private fun checkRegister(){
        resStr=""
        var getemail=url+"/email="+email
        val client = OkHttpClient()
        val request: Request = Request.Builder()
                .url(getemail)
                .get()
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
                val temp=JSONObject(resStr)
                if(temp.isNull("data")){
                    ifreg=false
                    if(user==-1)sendmessage()
                    else{
                        fetchJSON(user)
                    }
                }
            }
        })
    }
    private fun userinput(){
        timestamp= Date().time
        email=ed_email.text.toString()
        if (!email.isNullOrEmpty() && regex_email.matches(email)){
            password=ed_password.text.toString()
            if (!password.isNullOrEmpty() && regex_password.matches(password)){
                checkpassword=ed_checkpassword.text.toString()
                if (!checkpassword.isNullOrEmpty())
                    if (checkpassword==password){
                        checkRegister()
                        SystemClock.sleep(500)
                        if(ifreg)ifRegistered(resStr)
                    }
                    else Toast.makeText(this, "Check Password and Password are incorrect.", Toast.LENGTH_SHORT).show()
                else Toast.makeText(this, "Check Password can't be empty.", Toast.LENGTH_SHORT).show()
            }
            else if(!regex_password.matches(password))Toast.makeText(this, "Password is illegal.", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "Password can't be empty.", Toast.LENGTH_SHORT).show()
        }
        else if(!regex_email.matches(email))Toast.makeText(this, "Email is illegal.", Toast.LENGTH_SHORT).show()
        else Toast.makeText(this, "Email can't be empty.", Toast.LENGTH_SHORT).show()
    }
    private fun writeLog(input:String){
        var file=File(commandPath,filename)
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            file.writeText(input)
        } catch (e:IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    private fun fetchJSON(user:Int){
        var file = File(commandPath, filename)
        var filestring = file.readText(Charsets.UTF_8)
        var array= JSONArray(filestring)
        array.getJSONObject(user).put("password",password)
        array.getJSONObject(user).put("email",email)
        val personObject=array.getJSONObject(user)
        var jsonObject= JSONObject()
        Log.e("fetchJSON",personObject.toString())
        jsonObject.put("username", personObject.getString("username").toString())/////string
        jsonObject.put("email", personObject.getString("email").toString())
        jsonObject.put("password", generateHashedPass(personObject.getString("password")))
        jsonObject.put("birthyear", personObject.getString("birth").toInt())
        jsonObject.put("height", personObject.getString("height").toInt())
        jsonObject.put("weight", personObject.getString("weight").toInt())
        jsonObject.put("drink", personObject.getString("drink").toInt())
        jsonObject.put("license", JSONArray(personObject.getString("license")))
        jsonObject.put("disease", JSONArray(personObject.getString("disease")))
        jsonObject.put("timestamp", Date().time)
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
                Log.e("Error", "$e")
            }
            override fun onResponse(call: Call, response: Response) {
                numberOfReq--
                ispost = true
                resStr = response.body?.string().toString()
                Log.e("Register Succeed", "${resStr}")
                resStr=resStr.substring(1,25)
                array.getJSONObject(user).put("_id",resStr)
                Log.e("CHECK",array.getJSONObject(user).toString())
                writeLog(array.toString())
            }
        })
    }
    private fun generateHashedPass(pass: String): String {
        return BCrypt.hashpw(pass, BCrypt.gensalt())
    }
    private fun ifRegistered(resStr:String){
        AlertDialog.Builder(this)
                .setTitle("Suggest")
                .setMessage("this email is registered.")
                .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
                    Log.e("ResStr","${resStr}")
                })
                .setCancelable(false)
                .show()
    }
}