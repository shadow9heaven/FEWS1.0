package com.ble.drive_status_cntl

import android.Manifest
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okhttp3.*
import okhttp3.MediaType.Companion.get
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.wait
import okio.IOException
import org.jetbrains.annotations.Nullable
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONObject.NULL
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.lang.Exception
import java.lang.Thread.interrupted
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap
import kotlin.concurrent.thread
import kotlin.random.Random

class Carinfo : AppCompatActivity() {
    /*testData = {
        "post_t": 1,
        "vehicle_t": -1,
        "energy_t": -1,
        "brand_t": -1,
        "model_t": -1,
        "tonnage": -1,
        "subjectID": None,
        "timestamp": time.time(),
    }*/
    lateinit var resStr:String
    lateinit var message:String
    ///listview
    private lateinit var listView: ListView
    private var adapter: SimpleAdapter? = null
    private var devicename: List<HashMap<String, String>> = ArrayList()
    private val listlabel = arrayOf("name", "misc")
    private val listid = intArrayOf(android.R.id.text1, android.R.id.text2)
    private var Car_List = HashMap<String, String>()
    lateinit var swipeToRefresh: SwipeRefreshLayout
    var numberOfReq=0
    val check_url="http://59.120.189.128:5000/data/biologueQuery"
    val test_url="http://59.120.189.128:5000/data/biologueData"
    var ispost=false
    // Define the ECG and BCG sampling rate
    val ECG_FS = 128
    val BCG_FS = 64
    val ECG_PERIOD = 1.0/ECG_FS*1000
    val BCG_PERIOD = 1.0/BCG_FS*1000
    // Unit: seconds
    val Algo_SEND_TIME = 3
    val ECG_SEND_TIME = 60
    val BCG_SEND_TIME = 60
    //sendData
    lateinit var queue:MutableList<JSONObject?>
    var algomiss=0
    var bcgmiss=0
    var ecgmiss=0
    lateinit var Thread_BCG :Thread
    lateinit var Thread_ECG :Thread
    lateinit var Thread_sendDate :Thread
    //ecg
    lateinit var ECG_Algo_ID:MutableList<Any?>
    lateinit var ecg:MutableList<Any?>
    //bcg
    lateinit var BCG_Algo_ID:MutableList<Any?>
    lateinit var bcg:MutableList<Any?>
    lateinit var acc:MutableList<Any?>
    ///Data test
    var checkboolean:Boolean=true
    var wrongList: List<HashMap<JSONObject,JSONObject>> =ArrayList()
    ////write Log
    lateinit var commandPath :File
    val filename = "emulated/0/webAPI_4_23.txt"
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_carinfo)
        message= intent.getStringExtra("oid").toString()
        Log.e("oid","$message")
        findID()
        getJSON()
        SystemClock.sleep(500)
        Log.e("time","${Date().time}")
        ListCar()
        refreshApp()
        listView.setOnItemClickListener{ parent, view, position, id ->
            test(position)
        }
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
    private fun findID(){
        listView = findViewById<ListView>(R.id.car_list)
        swipeToRefresh =findViewById(R.id.swipeToRefresh)
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.carinfo_menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var id = item.getItemId();
        if(id == R.id.Add){
            add_information()
        }
        else if(id==R.id.logout){
            setResult(RESULT_OK, getIntent())
            finish()
        }
        return true;
    }
    private fun add_information(){
        val intent = Intent(this, AddCarInfo::class.java)
        intent.putExtra("message",message)
        startActivityForResult(intent, 2)
        //test_string=getRandom()
        //testinput(test_string)
        //fetchJSON()
        //SystemClock.sleep(500)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==2&&resultCode == RESULT_OK){
            Log.e("CarInfo","BACK")
            SystemClock.sleep(500)
            getJSON()
            SystemClock.sleep(500)
            for(i in devicename) devicename -= i
            ListCar()

        }
        else{
            Log.e("MainActivity","Error.")
        }
    }
    private fun ListCar(){
        val array= JSONArray(resStr)
        Log.e("CarInfo","${array.length()}")
        for(i in 0 until array.length() step 1){
            Log.e("CarInfo", "${array[i]}") // 0 2 4
            var Json:JSONObject= JSONObject(array[i].toString())
            var plate=Json.getString("car_plate").toString()
            var oid=Json.getJSONObject("_id")?.getString("\$oid").toString()
            Car_List.put(oid,plate)
        }
        if(Car_List.isNullOrEmpty()) Log.e("Car_Device", "NULL")
        else {
            Log.e("TAG","Searching")
            for (r in Car_List) {
                val hashMap: HashMap<String, String> = HashMap()
                hashMap.put("name", r.value)
                hashMap.put("misc", r.key)
                Log.e("${r.component1()}","${r.key}")
                if (!devicename.contains(hashMap) && !r.value.isNullOrEmpty())devicename += hashMap
            }
            adapter = SimpleAdapter(
                this,
                devicename,
                android.R.layout.simple_list_item_2,
                listlabel,
                listid
            );
            listView.setAdapter(adapter);
        }
    }
    private fun refreshApp(){
        swipeToRefresh.setOnRefreshListener{
            Toast.makeText(this, "Page refresh!!", Toast.LENGTH_SHORT).show()
            getJSON()
            SystemClock.sleep(500)
            for(i in devicename) devicename -= i
            ListCar()
            swipeToRefresh.isRefreshing=false
        }
    }
    private fun getJSON(){
        var getObject=JSONObject()
        getObject.put("post_t", 1)/////string
        getObject.put("subjectID",message)
        resStr=""
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = getObject.toString().toRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(check_url)
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
                Log.e("CarInfo", "GetJSON:${resStr}")
            }
        })
    }
    override fun onBackPressed() {
        //super.onBackPressed()
    }
    /////test function
    private fun test(position: Int){
        val car_id=Car_List.keys.elementAt(position)
        val user_id=message
        Thread_BCG=Thread{
            BCG(car_id)
        }
        Thread_ECG=Thread{
            ECG()
        }
        Thread_sendDate=Thread{
            var sendData_counter=0
            var Type2_cnt = 0
            var Type3_cnt=0
            var Type4_cnt=0
            var timestampST=Date().time
            var totaltime:Long=0L
            var timeout:Boolean=true
            lateinit var temp:JSONObject
            while (timeout){
                if (!queue.isNullOrEmpty()){
                    try {
                        temp= JSONObject(queue.get(0).toString())
                        resStr=""
                        val client = OkHttpClient()
                        val mediaType = "application/json".toMediaType()
                        val body = temp.toString().toRequestBody(mediaType)
                        val request: Request = Request.Builder()
                            .url(test_url)
                            .post(body)
                            .build()
                        numberOfReq++;
                        client.newCall(request).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                println("fail : $e")
                                writeLog("-Error-$sendData_counter-${e.toString()}-${Date().time}")
                                Log.e("Error","connect failed.")
                            }
                            override fun onResponse(call: Call, response: Response) {
                                numberOfReq--
                                ispost = true
                                resStr = response.body?.string().toString()
                                if(temp?.getString("post_t")?.toInt()==2){
                                    Type2_cnt++
                                    getQuery(2,resStr,temp,sendData_counter,0)
                                    BCG_Algo_ID.add(resStr)
                                    ECG_Algo_ID.add(resStr)
                                    Log.e("User_ID:$user_id,Car_ID:$car_id,sum_counter:$sendData_counter","Algo_counter:$Type2_cnt")
                                }
                                else if(temp?.getString("post_t")?.toInt()==3){
                                    Type3_cnt++
                                    getQuery(3,resStr,temp,sendData_counter,0)
                                    Log.e("User_ID:$user_id,Car_ID:$car_id,sum_counter:$sendData_counter", "BCG_counter:$Type3_cnt")
                                }
                                else if(temp?.getString("post_t")?.toInt()==4){
                                    Type4_cnt++
                                    getQuery(4,resStr,temp,sendData_counter,0)
                                    Log.e("User_ID:$user_id,Car_ID:$car_id,sum_counter:$sendData_counter", "ECG_counter:$Type4_cnt")
                                }
                                if (!queue.isNullOrEmpty()){
                                    Log.e("$sendData_counter","${queue.get(0)}")
                                    queue.removeAt(0)
                                }
                                sendData_counter++
                                if (sendData_counter>=21){
                                    totaltime=(Date().time-timestampST)/1000
                                    timeout=false
                                }
                            }
                        })

                    }
                    catch (e:Exception){
                        Log.e("Exception:","$e,${queue.size}")
                        writeLog("Exception:$e,${queue.size}")
                    }
                }
                sleep(500)
            }
            writeLog("****************************************************************************")
            writeLog("Total times:$sendData_counter,Algo_miss:$algomiss,BCG_miss:$bcgmiss,ECG_miss:$ecgmiss,time:{$totaltime}s")
            Log.e("Result","Total times:$sendData_counter,Algo_miss:$algomiss,BCG_miss:$bcgmiss,ECG_miss:$ecgmiss,time:{$totaltime}s")
        }
        queue= arrayListOf()
        BCG_Algo_ID= arrayListOf()
        ECG_Algo_ID= arrayListOf()
        bcg= arrayListOf()
        acc= arrayListOf()
        ecg= arrayListOf()
        Thread_ECG.start()
        Thread_BCG.start()
        SystemClock.sleep(100)
        Thread_sendDate.start()
    }
    private fun ECG(){
        var data_len=0
        var ecg_cnt=0
        while(true){
            ecg.add((0..2048).random())
            data_len+=1
            ecg_cnt++
            if (ecg_cnt%(ECG_FS*ECG_SEND_TIME)==0){
                var getObject=JSONObject()
                getObject.put("post_t", 4)/////string
                getObject.put("ecg",ecg)
                getObject.put("data_len",data_len)
                getObject.put("id",ECG_Algo_ID)
                getObject.put("id_len",2)
                getObject.put("timestampST",1)
                getObject.put("timestampEND",5)
                getObject.put("timestamp",Date().time)
                queue.add(getObject)
                ecg.clear()
                data_len=0
                ECG_Algo_ID.clear()
            }
            SystemClock.sleep(ECG_PERIOD.toLong())
        }
    }
    private fun BCG(car_id:String){
        var user_id=message
        var data_len = 0
        var bcg_cnt=0
        while(true){
            bcg.add((10..50000).random())
            acc.add((0..360).random())
            data_len++
            bcg_cnt++
            if(bcg_cnt%(BCG_FS*BCG_SEND_TIME)==0){
                var getObject=JSONObject()
                getObject.put("post_t", 3)/////string
                getObject.put("bcg",bcg)
                getObject.put("acc",acc)
                getObject.put("device_t",1)
                getObject.put("ECGID",NULL)
                getObject.put("data_len",data_len)
                getObject.put("id",BCG_Algo_ID)
                getObject.put("id_len",2)
                getObject.put("timestampST",0)
                getObject.put("timestampEND",0)
                getObject.put("timestamp",Date().time)
                queue.add(getObject)
                bcg.clear()
                acc.clear()
                BCG_Algo_ID.clear()
                data_len=0
            }
            else if(bcg_cnt%(BCG_FS*Algo_SEND_TIME)==0){
                var getObject=JSONObject()
                getObject.put("post_t", 2)/////string
                getObject.put("hr",(50..80).random())
                getObject.put("sensor_t",1)
                getObject.put("confidence",1)
                getObject.put("resp",(10..20).random())
                getObject.put("fatigue",(0..5).random())
                getObject.put("status",(0..3).random())
                getObject.put("subjectID",user_id)
                getObject.put("carID",car_id)
                getObject.put("timestamp",Date().time)
                getObject.put("timestampM",0)
                queue.add(getObject)
            }
            SystemClock.sleep(BCG_PERIOD.toLong())
        }
    }
    private fun getQuery(post_t:Int,id:String,temp:JSONObject,counter:Int,TryCounter:Int){
        var getObject=JSONObject()
        var tObject=JSONObject()
        getObject.put("post_t",post_t )/////string
        getObject.put("_id",id)
        tObject.put("\$lte",Date().time)   // <=
        tObject.put("\$gte",Date().time-10000)   // >=
        getObject.put("timestamp", tObject)
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = getObject.toString().toRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(check_url)
            .post(body)
            .build()
        numberOfReq++;
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("fail : $e")
                Log.e("Error", "connect failed.")
            }
            override fun onResponse(call: Call, response: Response) {
                Thread{
                    var queryStr=""
                    numberOfReq--
                    ispost = true
                    queryStr = response.body?.string().toString()
                    val array = JSONArray(queryStr)
                    if(array.length()!=1){
                        writeLog("////////////////////////////////////////////////////////////////////////")
                        writeLog("-$post_t-$counter-FailQuery")
                        writeLog("------------------------------------------------------------------------")
                        writeLog("array:\n  $array")
                        writeLog("------------------------------------------------------------------------")
                        writeLog("temp:\n  $temp")
                        writeLog("////////////////////////////////////////////////////////////////////////")
                        Log.e("-$post_t-$counter","$temp")
                        if(TryCounter<10){
                            val nextCounter=TryCounter+1
                            getQuery(post_t,id,temp,counter,nextCounter)
                        }
                    }
                    else {
                        var queryObject=JSONObject(array.get(0).toString())
                        checkObject(queryObject,temp,post_t,counter)
                        if( !checkboolean &&post_t == 2){
                            algomiss++
                            Log.e("Receive Algo","${array.length()}")
                        }
                        else if (!checkboolean && post_t == 3) {
                            bcgmiss++
                            Log.e("Receive BCG","${array.length()}")
                        }
                        else if (!checkboolean && post_t == 4){
                            ecgmiss ++
                            Log.e("Receive ECG","${array.length()}")
                        }
                    }
                }.start()
            }
        })
    }
    private fun checkObject(A:JSONObject,B:JSONObject,type:Int,counter: Int){
        checkboolean=true
        if(type==2){
            var StrA=""
            StrA=StrA+A.getString("hr")
            StrA=StrA+A.getString("sensor_t")
            StrA=StrA+A.getString("confidence")
            StrA=StrA+A.getString("resp")
            StrA=StrA+A.getString("fatigue")
            StrA=StrA+A.getString("status")
            StrA=StrA+A.getString("timestamp")
            StrA=StrA+A.getString("timestampM")
            var StrB=""
            StrB=StrB+B.getString("hr")
            StrB=StrB+B.getString("sensor_t")
            StrB=StrB+B.getString("confidence")
            StrB=StrB+B.getString("resp")
            StrB=StrB+B.getString("fatigue")
            StrB=StrB+B.getString("status")
            StrB=StrB+B.getString("timestamp")
            StrB=StrB+B.getString("timestampM")
            Log.e("ALGO/type4A","$StrA")
            Log.e("ALGO/type4B","$StrB")
            if(!StrA.equals(StrB)){
                checkboolean=false
                writeLog("////////////////////////////////////////////////////////////////////////")
                writeLog("-$type-$counter-ContentFailed-${TimeUtil.StampToDate(B.getLong("timestamp"), Locale.TAIWAN)}")
                writeLog("------------------------------------------------------------------------")
                writeLog("send:${B.toString()}")
                writeLog("------------------------------------------------------------------------")
                writeLog("Receive:${A.toString()}")
                writeLog("////////////////////////////////////////////////////////////////////////")
            }
            else{
                writeLog("-$type-$counter-succeed-${TimeUtil.StampToDate(B.getLong("timestamp"), Locale.TAIWAN)}")
            }
        }
        else if(type==3){
            var StrA=""
            StrA=StrA+A.getString("bcg")
            StrA=StrA+A.getString("acc")
            StrA=StrA+A.getString("device_t")
            StrA=StrA+A.getString("ECGID")
            StrA=StrA+A.getString("data_len")
            StrA=StrA+A.getString("id")
            StrA=StrA+A.getString("id_len")
            StrA=StrA+A.getString("timestampST")
            StrA=StrA+A.getString("timestampEND")
            StrA=StrA+A.getString("timestamp")
            var StrB=""
            StrB=StrB+B.getString("bcg")
            StrB=StrB+B.getString("acc")
            StrB=StrB+B.getString("device_t")
            StrB=StrB+B.getString("ECGID")
            StrB=StrB+B.getString("data_len")
            StrB=StrB+B.getString("id")
            StrB=StrB+B.getString("id_len")
            StrB=StrB+B.getString("timestampST")
            StrB=StrB+B.getString("timestampEND")
            StrB=StrB+B.getString("timestamp")
            Log.e("BCG/type4A","$StrA")
            Log.e("BCG/type4B","$StrB")
            if(!StrA.equals(StrB)){
                checkboolean=false
                writeLog("////////////////////////////////////////////////////////////////////////")
                writeLog("-$type-$counter-ContentFailed-${TimeUtil.StampToDate(B.getLong("timestamp"), Locale.TAIWAN)}")
                writeLog("------------------------------------------------------------------------")
                writeLog("send:${B.toString()}")
                writeLog("------------------------------------------------------------------------")
                writeLog("Receive:${A.toString()}")
                writeLog("////////////////////////////////////////////////////////////////////////")
            }
            else{
                writeLog("-$type-$counter-succeed-${TimeUtil.StampToDate(B.getLong("timestamp"), Locale.TAIWAN)}")
            }
        }
        else if(type==4){
            var StrA=""
            StrA=StrA+A.getString("ecg")
            StrA=StrA+A.getString("data_len")
            StrA=StrA+A.getString("id")
            StrA=StrA+A.getString("id_len")
            StrA=StrA+A.getString("timestampST")
            StrA=StrA+A.getString("timestampEND")
            StrA=StrA+A.getString("timestamp")
            var StrB=""
            StrB=StrB+B.getString("ecg")
            StrB=StrB+B.getString("data_len")
            StrB=StrB+B.getString("id")
            StrB=StrB+B.getString("id_len")
            StrB=StrB+B.getString("timestampST")
            StrB=StrB+B.getString("timestampEND")
            StrB=StrB+B.getString("timestamp")
            Log.e("ECG/type4A","$StrA")
            Log.e("ECG/type4B","$StrB")
            if(!StrA.equals(StrB)){
                checkboolean=false
                writeLog("////////////////////////////////////////////////////////////////////////")
                writeLog("-$type-$counter-ContentFailed-${TimeUtil.StampToDate(B.getLong("timestamp"), Locale.TAIWAN)}")
                writeLog("------------------------------------------------------------------------")
                writeLog("send:${B.toString()}")
                writeLog("------------------------------------------------------------------------")
                writeLog("Receive:${A.toString()}")
                writeLog("////////////////////////////////////////////////////////////////////////")

            }
            else{
                writeLog("-$type-$counter-succeed-${TimeUtil.StampToDate(B.getLong("timestamp"), Locale.TAIWAN)}")
            }
        }
    }
    private fun writeLog(input:String){
        var file=File(commandPath,filename)
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            file.appendText("\n")
            file.appendText(input)
        } catch (e:IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    object TimeUtil {
        @JvmStatic
        fun StampToDate(time: Long, locale: Locale): String {
            // 進來的time以秒為單位，Date輸入為毫秒為單位，要注意

            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)

            return simpleDateFormat.format(Date(time))
        }

        @JvmStatic
        fun DateToStamp(date: String, locale: Locale): Long {
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", locale)

            /// 輸出為毫秒為單位
            return simpleDateFormat.parse(date).time
        }
    }
}