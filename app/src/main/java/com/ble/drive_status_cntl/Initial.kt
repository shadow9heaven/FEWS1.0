package com.ble.drive_status_cntl

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.*
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
import java.lang.Exception
import kotlin.system.exitProcess


class Initial : AppCompatActivity() , View.OnClickListener{

    var username :String = "guest"
    lateinit var bt_gmail:SignInButton
    lateinit var bt_Login:Button
    lateinit var bt_register:Button
    lateinit var bt_confirm:Button
    lateinit var bt_exit:Button
    lateinit var bt_guest:Button
    lateinit var sp_userlist:Spinner
    lateinit var userlist:ArrayList<String?>
    lateinit var resStr:String
    var user=0
    var url = "http://59.120.189.128:8081/data/biologueQuery"
    var timerHandler2: Handler? = Handler()
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "Request External Storage")

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                9
            )
        }

        try {
            userlist_spin()
            timerHandler2?.postDelayed(timerRunnable2, 0)
        }catch (e :Exception){
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
        //exit
        bt_exit=findViewById(R.id.bt_exit)
        bt_exit.setOnClickListener(this)
        //guest
        bt_guest=findViewById(R.id.bt_guest_mode)
        bt_guest.setOnClickListener(this)
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

                    val intent = Intent(this, loginactivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivityForResult(intent, 4)
                    Toast.makeText(this, "Please Login.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Please check connect status", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.bt_register -> {
                if (CheckConnectStatus()){
                    val intent = Intent(this, Register::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    startActivityForResult(intent, 3)
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
                val array = JSONArray(filestring)
                val personObject: JSONObject = JSONObject(array[user].toString())
                if (filestring.equals("null")) Toast.makeText(this, "Please Login or Register.", Toast.LENGTH_SHORT).show()
                else {
                    if (CheckConnectStatus()) {
                        val array = JSONArray(filestring)
                        val personObject: JSONObject = JSONObject(array[user].toString())
                        if (personObject.getString("email").toString() == "null") {
                            askRegister(personObject)
                        } else {
                            getJSON(personObject)
                            username = userlist[user]!!
                            getIntent().putExtra("user", username)
                            setResult(RESULT_OK, getIntent())
                            finish()
                        }
                    } else {
                        /////offline login
                        username = userlist[user]!!
                        getIntent().putExtra("user", username)
                        setResult(RESULT_OK, getIntent())
                        finish()
                        Log.e("Log", "$personObject")
                    }
                }
            }
            R.id.bt_guest_mode->{

                getIntent().putExtra("user", "guest")
                setResult(RESULT_OK, getIntent())
                finish()

            }
            R.id.bt_exit->{
                moveTaskToBack(true);
                exitProcess(-1)
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
/*
    override fun onBackPressed() {
        //super.onBackPressed()
    }
*/
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==0 && resultCode ==RESULT_OK) {

            val task =GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
        else if(requestCode==2 && resultCode ==RESULT_OK){
            userlist_spin()

        }
        else if(requestCode==3 && resultCode ==RESULT_OK){
            userlist_spin()

        }
        else if(requestCode == 4 && resultCode == RESULT_OK){
            userlist_spin()
            username = data?.getStringExtra("user")!!
/*
            getIntent().putExtra("user", username)
            setResult(RESULT_OK, getIntent())
            finish()*/
        }
        else{
            Log.e("check","$requestCode，$resultCode")
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
                    var username=array.getJSONObject(i).getString("username")
                    userlist.add(username.toString())
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
        var jsonemail = JSONObject()
        jsonemail.put("\$regex", personObject.getString("email"))
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
    private val timerRunnable2: Runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.M)
        override fun run() {
            runOnUiThread {
                if(CheckConnectStatus()){
                    bt_Login.isClickable=true
                    bt_Login.setTextColor(getColor(R.color.black))
                }
                else{
                    bt_Login.setTextColor(getColor(R.color.gray))
                    bt_Login.isClickable=false
                }
                if(userlist.get(0)=="NULL"){
                    bt_confirm.isClickable=false
                    bt_confirm.setTextColor(getColor(R.color.gray))
                }
                else{
                    bt_confirm.isClickable=true
                    bt_confirm.setTextColor(getColor(R.color.black))
                }
            }
            timerHandler2!!.postDelayed(this, 1000)
        }
    }
    private fun askRegister(personObject: JSONObject){
        AlertDialog.Builder(this)
            .setTitle("Suggest")
            .setMessage("Do you mind to Register a account?")
            .setPositiveButton("OK", DialogInterface.OnClickListener { dialogInterface, i ->
                val intent = Intent(this, Register::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                intent.putExtra("user", user)
                startActivityForResult(intent, 2)
            })
            .setNegativeButton("No", DialogInterface.OnClickListener { dialogInterface, i ->
                Log.e("Log", "$personObject")
            })
            .setCancelable(false)
            .show()
    }
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            9-> {
                userlist_spin()
                timerHandler2?.postDelayed(timerRunnable2, 0)
            }
            else -> {
            }
        }
    }
}