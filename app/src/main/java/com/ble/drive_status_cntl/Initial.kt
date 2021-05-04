package com.ble.drive_status_cntl

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONArray
import org.json.JSONObject
import org.mindrot.jbcrypt.BCrypt
import java.io.File


class Initial : AppCompatActivity() , View.OnClickListener{
    lateinit var bt_gmail:SignInButton
    lateinit var bt_Login:Button
    lateinit var bt_register:Button
    lateinit var bt_confirm:Button
    lateinit var sp_userlist:Spinner
    lateinit var userlist:ArrayList<String?>
    lateinit var resStr:String
    var user=0
    var url = "http://59.120.189.128:5000/data/biologueQuery"
    ////load file
    lateinit var commandPath : File
    val filename = "emulated/0/personalFile_4_28.txt"
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)
        commandPath = File(Environment.getStorageDirectory().absolutePath)
        findID()
        ///GMailSignIn
        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct != null) {
            bt_gmail.visibility=View.VISIBLE
            Toast.makeText(this, "${acct.displayName}", Toast.LENGTH_SHORT).show()
        }
        userlist_spin()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "Request External Storage")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                9
            )
        }
    }
    private fun findID() {
        ///userlist
        sp_userlist=findViewById(R.id.sp_userlist)
        bt_confirm=findViewById(R.id.bt_confirm)
        bt_confirm.setOnClickListener(this)
        ///gmail
        bt_gmail = findViewById(R.id.sign_in_button)
        bt_gmail.setSize(SignInButton.SIZE_STANDARD)
        bt_gmail.setOnClickListener(this)
        ///Login
        bt_Login=findViewById(R.id.bt_Login)
        bt_Login.setOnClickListener(this)
        ///Register
        bt_register=findViewById(R.id.bt_register)
        bt_register.setOnClickListener(this)

    }
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.sign_in_button -> {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build()
                val mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
                findID()
                val signInIntent = mGoogleSignInClient.signInIntent
                startActivityForResult(signInIntent, 0);
            }
            R.id.bt_Login -> {
                if (CheckConnectStatus()) {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivityForResult(intent, 2)
                    Toast.makeText(this, "Please Login.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please check connect status", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.bt_register -> {
                if (CheckConnectStatus()){
                    val intent = Intent(this, Register::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivityForResult(intent, 2)
                    Toast.makeText(this, "Please Login.", Toast.LENGTH_SHORT).show()
                }
                else{
                    val intent = Intent(this, personInfo::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    intent.putExtra("status",false)
                    startActivityForResult(intent, 2)
                    Toast.makeText(this, "Please Login.", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.bt_confirm -> {
                var file = File(commandPath, filename)
                var filestring = file.readText(Charsets.UTF_8)
                if (filestring.equals("null")) Toast.makeText(this, "Please Login or Register.", Toast.LENGTH_SHORT).show()
                else {
                    if (CheckConnectStatus()){
                        val array=JSONArray(filestring)
                        val personObject:JSONObject=JSONObject(array[user].toString())
                        if(personObject.getString("username").toString()=="null"){
                            val intent = Intent(this, Register::class.java)
                            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            intent.putExtra("user",user)
                            startActivityForResult(intent, 2)
                        }else{
                            getJSON(personObject)

                        }
                    }
                    else{
                        val array=JSONArray(filestring)
                        val personObject:JSONObject=JSONObject(array[user].toString())
                        Log.e("Log","$personObject")
                    }
                }
            }
        }
    }
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount = completedTask.getResult(ApiException::class.java)!!
            bt_gmail.visibility=View.VISIBLE
            Toast.makeText(this, "${account.displayName}", Toast.LENGTH_SHORT).show()
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.e("google", "signInResult:failed code=" + e.statusCode)
            bt_gmail.visibility=View.VISIBLE
        }
    }
    override fun onBackPressed() {
        //super.onBackPressed()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==0 && resultCode ==RESULT_OK) {
            val task =GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
        else if(requestCode==2 && resultCode ==RESULT_OK){
            userlist_spin()
        }
    }
    private fun userlist_spin(){
        var file = File(commandPath, filename)
        userlist= arrayListOf()
        if (!file.exists()) {
            Log.e("test","here")
            file.createNewFile();
            file.writeText("null")
            userlist.add("NULL")
        }
        else{
            var filestring = file.readText(Charsets.UTF_8)
            if(filestring!="null"){
                var array= JSONArray(filestring)
                userlist.clear()
                for(i in 0 until array.length() step 1){
                    var nickname=array.getJSONObject(i).getString("nickname")
                    userlist.add(nickname.toString())
                }
            }else{
                userlist.clear()
                userlist.add("NULL")
            }
        }
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            userlist
        )
        sp_userlist.adapter = adapter
        sp_userlist.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.e("Spinner", "select $p2")
                user = p2
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
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
    private fun getJSON(personObject:JSONObject){
        val jsonObject=JSONObject()
        jsonObject.put("post_t", 0)/////string
        var jsonname = JSONObject()
        jsonname.put("\$regex", personObject.getString("username"))
        var jsonemail = JSONObject()
        jsonemail.put("\$regex", personObject.getString("email"))
        jsonObject.put("username", jsonname) /////string
        jsonObject.put("email", jsonemail)
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = jsonObject.toString().toRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(url)
            .post(body)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("fail : $e")
                Log.e("Error", "connect failed.")
            }

            override fun onResponse(call: Call, response: Response) {
                resStr = response.body?.string().toString()
                if (resStr.isNullOrEmpty()){
                    SystemClock.sleep(300)
                }
                resStr=resStr.removeSuffix("]").removePrefix("[")
                Log.e("Res","$resStr")
                Log.e("per","${personObject.getString("password")}")
                val resStrObject=JSONObject(resStr)
                if(BCrypt.checkpw(personObject.getString("password"), resStrObject.getString("password"))){
                    Log.e("Login","Right")
                }
                else Log.e("Login","Error")
            }
        })
    }
}