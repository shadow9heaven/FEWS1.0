package com.ble.drive_status_cntl

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothProfile.GATT
import android.bluetooth.BluetoothProfile.STATE_CONNECTING
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.SoundPool
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bluetooth.bth_k2.DataPoint
import com.bluetooth.bth_k2.GraphView
import java.io.File
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

import com.google.gson.GsonBuilder
import okhttp3.internal.and
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.Math.random
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import org.mindrot.jbcrypt.BCrypt
import kotlin.experimental.and

class health_panel : AppCompatActivity() {

    val BCG_PACKET_SIZE = 6
    val BCG_SIZE = 1280
    val ECG_SIZE = 2560
    val HR_SIZE = 1800
    val MINUTE = 60
    var signal_level =4  ///// BCG signal level
    lateinit var tv_signalLV :TextView ///
    var delta = 0/////check bcg signal level
    var wavetune  = 1.0f


    lateinit var ib_vib :ImageButton

    var vibmode = false
    var viblevel = 0
    lateinit var bt_startdatacollect :Button

    val UPLOAD_TIME = 300

    lateinit var bt_autoup :Button
    var autoup = false

    var jsonObject = JSONObject()
    lateinit var tv_time :TextView
    var device_add = ""

    lateinit var device :BluetoothDevice

    var GV: GraphView? = null

    lateinit var bt_waveform :Button

    var BCG_pc : Int =0
    var BCG_th: Int = 320

    var BCG_pp : List<DataPoint> = ArrayList()

    var hr_gv : List<DataPoint> = ArrayList()
    var re_gv : List<DataPoint> = ArrayList()
    var ftg_gv : List<DataPoint> = ArrayList()

    var hrdraw_count = 1
    //var draw_mode  = 0

    var wavemode   = 0

    var paklost    = true
    var conncount  = 0
    var reconn = 0
    val RECON_DURA = 3

    var recbool = false

    var uploadcount = 0
    var heartrate_count =0


    lateinit var plot_thread :Thread
    lateinit var write_thread :Thread

    lateinit var sp_fatigue : SoundPool
    var dataclt = false

    var mgatt: BluetoothGatt? = null

    var ECG_DATA_DIRECTORY = "ECG_DATA"
    var user = "guest"

    val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
    val url = "http://59.120.189.128:5000/data/biologueData"


    private lateinit var bluetoothManager : BluetoothManager
    private lateinit var bluetoothAdapter : BluetoothAdapter

    var hrlist  = arrayListOf<Int>()
    var ecgdatalog = arrayListOf<String>()
    var bcgdatalog = arrayListOf<String>()

    val UUID_SERIVCE =      "1234E001-FFFF-1234-FFFF-111122223333"
    val UUID_CHAR_ECG =     "1234E002-FFFF-1234-FFFF-111122223333"

    //val UUID_CHAR_TIME =    "1234E005-FFFF-1234-FFFF-111122223333"
    val UUID_CHAR_COMMAND = "1234E004-FFFF-1234-FFFF-111122223333"
    val UUID_CHAR_BCG =     "1234E006-FFFF-1234-FFFF-111122223333"

    lateinit var biologue_service: BluetoothGattService
    lateinit var biologue_char_ecg: BluetoothGattCharacteristic
    //lateinit var biologue_char_time: BluetoothGattCharacteristic
    lateinit var biologue_char_command: BluetoothGattCharacteristic
    lateinit var biologue_char_bcg: BluetoothGattCharacteristic

    private var storagePath: File? = null


    private lateinit var DefaultFileName: String
    private lateinit var DefaultBCGName: String

    var FinalECGname : String = ""
    var FinalBCGname : String = ""

    var hrhandle = Handler()
    private val timerRunnable2: Runnable = object : Runnable {
        override fun run() {
            runOnUiThread {
                if(blecnt && dataclt){
                    if (!hrlist.isEmpty()  ) {
                        //if( heartrate_count >MINUTE) {
                            var finalhr = hrlist.average()

                            hrlist.clear()
                            if (hrdraw_count < HR_SIZE) hrdraw_count++
                            else {
                                hr_gv = hr_gv.drop(1)
                                for (r in hr_gv) r.xVal -= 1
                            }
                            tv_heartrate.text = finalhr.toInt().toString()
                            var datatmp = DataPoint(hrdraw_count, finalhr.toInt())

                            hr_gv = hr_gv.plus(datatmp)

                        //}
                      //else heartrate_count ++
                    } else {

                    }

                    val bcg_sd = c_SD(BCG_pp)

                    if(bcg_sd<1000){
                        signal_level = 1
                        tv_signalLV.text = "1"
                    }
                    else if(bcg_sd<10000){
                        signal_level = 2
                        tv_signalLV.text = "2"
                    }
                    else if(bcg_sd<100000){
                        signal_level = 3
                        tv_signalLV.text = "3"
                    }
                    else {
                        signal_level = 4
                        tv_signalLV.text = "4"
                    }

                    when(wavemode){
                        0->{
                            var ori_gv = BCG_pp.reversed()
                            GV?.setData(ori_gv)
                        }///////origin wave
                        1-> {
                            GV?.setData(hr_gv)
                        }/////heartrate
                        2->{
                            //GV?.setData(re_gv)
                        }////respritory
                        3->{
                            GV?.setData(ftg_gv)
                        }
                    }/////draw graph

                    if(paklost) {
                        if (conncount < 5) {
                            Toast.makeText(this@health_panel, conncount.toString() + "packet loss!!", Toast.LENGTH_LONG).show()

                            conncount++
                        } else {

                            //var reconn = 0
                            //val RECON_DURA

                            if(reconn < RECON_DURA && !recbool){

                                Toast.makeText(this@health_panel, reconn.toString() + "connection retry!!", Toast.LENGTH_LONG).show()

                                recbool = true
                                Thread {
                                    try {
                                        reconn++
                                        device = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)[0]
                                        //if(device == null){}
                                        mgatt = device.connectGatt(this@health_panel, false, gattCB)
                                        Thread.sleep(1000)
                                        //send_start()
                                        if(bluetoothManager.getConnectionState(device, GATT) == 1 ||
                                            bluetoothManager.getConnectionState(device, GATT) == 2){
                                            Toast.makeText(this@health_panel, "connection rebuild!!", Toast.LENGTH_LONG).show()
                                            conncount = 0
                                            reconn = 0
                                        }////connection build
                                        else{}

                                    }
                                    catch(e : Exception){
                                    }
                                    recbool = false
                                }.start()

                            }////trying to reconnect
                            else if(reconn > RECON_DURA-1 && !recbool) {

                                Toast.makeText(this@health_panel, "disconnected!!", Toast.LENGTH_LONG).show()
                                mgatt?.disconnect()
                                broadcastUpdate(ACTION_GATT_DISCONNECTED)

                                blecnt = false
                                bt_bluetooth.setImageResource(R.drawable.bt_off)

                                reconn = 0
                                uploadcount = 0
                                conncount = 0
                            }
                            else{}
                        }
                    }////packlost
                    else{
                        if(conncount != 0) conncount = 0
                        paklost = true
                    }////packlost

                    if(uploadcount < UPLOAD_TIME){
                        uploadcount ++
                    }
                    else if(autoup){
                        //autoup = false
                        uploadcount =0

/////////////////upload data to database
                        Thread{
                        var upload_ECG = File(storagePath, "$ECG_DATA_DIRECTORY/$DefaultFileName")
                        var upload_BCG = File(storagePath, "$ECG_DATA_DIRECTORY/$DefaultBCGName")

                        write_new_file()
                        ////////unparse

                        var bcgcount =0
                        var bcgbuffer = arrayListOf<Int>()
                        var bcg_timestampST =0
                        var bcg_timestampEND =0

                        var ecgcount =0
                        var ecgbuffer = arrayListOf<Int>()
                        var ecg_timestampST =0
                        var ecg_timestampEND =0


                        upload_BCG.forEachLine {
                            var tmp = it.split(" ")
                                when(tmp[0]){
                                    "app_version:"->{}
                                    "Start_Time:"->{}
                                    else->{
                                        if(bcgcount==0){
                                            bcg_timestampST = tmp[0].toInt()
                                        }
                                        else if (bcgcount == BCG_SIZE){
                                            bcg_timestampEND = tmp[0].toInt()
                                        }

                                        bcgbuffer.add(tmp[1].toInt())

                                        if(bcgcount == BCG_SIZE){
                                               var bcgObject = JSONObject()
                                                bcgObject.put("post_t", 3)
                                                bcgObject.put("bcg",bcgbuffer)
                                                bcgObject.put("acc",1)
                                            //////acc x or y or z?
                                                //jsonObject.put("acc",tmp[])
                                            /////acc x or y or z
                                                //jsonObject.put("id",BCG_Algo_ID)

                                                bcgObject.put("id_len",2)
                                                bcgObject.put("timestamp",Date().time)

                                                bcgObject.put("device_t",device_add)
                                                bcgObject.put("BCGID",1)
                                                bcgObject.put("data_len",BCG_SIZE)
                                                //jsonObject.put("id")
                                                bcgObject.put("timestampST",bcg_timestampST)
                                                bcgObject.put("timestampEND",bcg_timestampEND)

                                            fetchJSON()

                                                bcgbuffer.clear()
                                                bcgcount = 0
                                                bcg_timestampST = 0
                                                bcg_timestampEND =0
                                        }////upload the packet
                                        else bcgcount++
                                    }
                                }
                            }

                        upload_ECG.forEachLine {
                            var tmp = it.split(" ")
                            when(tmp[0]){
                                "app_version:"->{}
                                "Start_Time:"->{}
                                else->{
                                    if(ecgcount == 0) {
                                        ecg_timestampST = tmp[0].toInt()
                                    }
                                    else if(ecgcount == ECG_SIZE){
                                        ecg_timestampEND = tmp[0].toInt()
                                    }
                                    ecgbuffer.add(tmp[1].toInt())
                                    if(ecgcount == ECG_SIZE){
                                        var ecgObject = JSONObject()
                                        ecgObject.put("post_t", 4)/////string
                                        ecgObject.put("ecg",ecgbuffer)
                                        ecgObject.put("data_len",ECG_SIZE)
                                        //ecgObject.put("id",ECG_Algo_ID)
                                        ecgObject.put("id_len",2)
                                        ecgObject.put("timestampST",ecg_timestampST)
                                        ecgObject.put("timestampEND",ecg_timestampEND)
                                        ecgObject.put("timestamp",Date().time)

                                        fetchJSON()

                                        ecgbuffer.clear()
                                        ecgcount = 0
                                        ecg_timestampST = 0
                                        ecg_timestampEND =0

                                    }


                                }
                            }
                        }
                        ///////unparse
/////////////////upload data to database

                        }.start()
                    }//////upload
                    else{

                    }

                }
                val dff = SimpleDateFormat("yyyyMMdd_HH:mm:ss")
                dff.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
                tv_time.setText(dff.format(Date()))
            }
            hrhandle!!.postDelayed(this, 1000)
        }
    }


    lateinit var bt_bluetooth : ImageButton
    lateinit var tv_heartrate :TextView


    var blecnt  = false

    var descript_write = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_panel)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        try{
            user  = intent?.getStringExtra("user")!!
            //ECG_DATA_DIRECTORY = user
        }
        catch (e :Exception){

        }
        val intent = Intent(this, bluetooth::class.java)
        startActivityForResult(intent, 1)

        ib_vib = findViewById(R.id.ib_showvib)

        hrhandle?.postDelayed(timerRunnable2, 0)

        bt_bluetooth = findViewById(R.id.IB_bluetooth)
        bt_waveform  = findViewById(R.id.bt_waveform)
        bt_autoup    = findViewById(R.id.bt_autoup)
        bt_startdatacollect = findViewById(R.id.bt_startdatacollect)

        tv_heartrate = findViewById(R.id.tv_heartrate)
        tv_time      = findViewById(R.id.tv_time)
        tv_signalLV  = findViewById(R.id.tv_signalLV)
        GV           = findViewById(R.id.graph_view_BCG)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "Request External Storage")
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    9
            )
        }
        else{


        }
        storagePath = this.getExternalFilesDir(null)

        create_saving_directory()

    }
    fun create_saving_directory() {

        var dataDir = File(storagePath, ECG_DATA_DIRECTORY)
        if(dataDir.mkdirs())Log.e("mkdir", dataDir.toString())

    }


    private val gattCB = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
        ) {
            Log.e("onConnectStateChange", newState.toString())

            if(newState == 0 || newState == 3){

                Toast.makeText(baseContext, "disconnected!!", Toast.LENGTH_LONG).show()

                blecnt = false
                bt_bluetooth.setImageResource(R.drawable.bt_off)


                //    Bt_BCG?.text = "connect"
                //    bcg_collect_stage = false
            }

            if(newState ==1 || newState == 2){
                gatt?.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val intentAction: String
            Log.e("GATT", "onServicesDiscovered")

            biologue_service = gatt!!.getService(UUID.fromString(UUID_SERIVCE))

            //Log.e("Biologue", "Found service")
            // Get Characteristic
            biologue_char_ecg     = biologue_service.getCharacteristic(UUID.fromString(UUID_CHAR_ECG))
            biologue_char_command = biologue_service.getCharacteristic(UUID.fromString(UUID_CHAR_COMMAND))
            biologue_char_bcg     = biologue_service.getCharacteristic(UUID.fromString(UUID_CHAR_BCG))

            Log.e("serviceDiscovered", biologue_char_command.toString())

            // Enable Notify ECG
            var notify_success = gatt!!.setCharacteristicNotification(biologue_char_ecg, true)
            if(notify_success) Log.e("Biologue", "Enable notify 1")
            else Log.e("Biologue", "Fail to enable notify 1")


            notify_success = gatt!!.setCharacteristicNotification(biologue_char_bcg, true)
            if(notify_success) Log.e("Biologue", "Enable notify 2")
            else Log.e("Biologue", "Fail to enable notify 2")


            for (dp in biologue_char_ecg.getDescriptors()) {
                Log.e("gattdevice-ecg", "dp:" + dp.toString())
                if (dp != null) {
                    if(biologue_char_ecg.getProperties() != 0 && BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0){
                        dp.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    }
                    else if (biologue_char_ecg.getProperties() != 0 && BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 ) {
                        dp.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    }
                    //descript_write = false
                    var tmp = gatt.writeDescriptor(dp)
                    Log.e("ECG", tmp.toString())
                }
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.e("DW", gatt.toString())
            if(!descript_write) {
                for (dp in biologue_char_bcg.getDescriptors()) {
                    Log.e("gattdevice-bcg", "dp:" + dp.toString())
                    if (dp != null) {
                        if (biologue_char_bcg.getProperties() != 0 && BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                            dp.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        } else if (biologue_char_bcg.getProperties() != 0 && BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
                            dp.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        }
                        //descript_write = false
                        var tmp = mgatt!!.writeDescriptor(dp)
                        Log.e("BCG", tmp.toString())
                        //while(!descript_write){ PERMISSION_REQUEST_STORAGE = PERMISSION_REQUEST_STORAGE }
                    }
                }


            }
            descript_write = true
        }


        override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
        ){

            paklost = false
            var ecgData = characteristic!!.value
            if (characteristic.uuid == UUID.fromString(UUID_CHAR_ECG)){
                var extFile = File(storagePath, "$ECG_DATA_DIRECTORY/$DefaultFileName")
                var outdata = LongArray(65)

                outdata.set(0, ((ecgData.get(0).toLong() and 0xFFL)
                        or (ecgData.get(1).toLong() and 0xFFL shl 8)
                        or (ecgData.get(2).toLong() and 0xFFL shl 16)
                        or (ecgData.get(3).toLong() and 0xFFL shl 24))
                )////////timestamp
/*
                if(ECG_pc == ECG_th){
                    ECG_pp = ECG_pp.drop(64)
                    ECG_pc -= 64
                }
*/

                for (i in 0..63) {
                    //Log.e("ii for", i.toString())
                    var nIndex = (i*2)+4
                    //var nIndex2 = nIndex+1
                    //i+=1

                    var tmp1 = ( ecgData.get(nIndex).toLong() and 0xFFL ) or (ecgData.get(nIndex + 1).toLong() and 0xFFL shl 8 )

                    /*
                    if(tmp1 >10000L ) tmp1 = ECG_pp.get(ECG_th - ECG_pc - 1).yVal.toLong()

                    if(ECG_pc<ECG_th){
                        ECG_pp += DataPoint((ECG_th - ECG_pc), tmp1.toInt())
                        ECG_pc++
                    }
                    */
                    outdata.set(i+1, tmp1)/////ecg
                }

                for(i in 1..64) {
                    extFile.appendText((outdata.get(0)+i-1).toString() + "\t"+ outdata.get(i).toString() +"\n")
                }

            }
            else if(characteristic.uuid == UUID.fromString(UUID_CHAR_BCG)){

                var bcgFile = File(storagePath, "$ECG_DATA_DIRECTORY/$DefaultBCGName")
                var j = 0
                var hrtotal = 0
                var hrcnt =0


                if(BCG_pc == BCG_th){

                    BCG_pp = BCG_pp.drop(BCG_PACKET_SIZE)
                    for(r in BCG_pp) r.xVal += BCG_PACKET_SIZE
                    BCG_pc -= BCG_PACKET_SIZE

                }

                while(j <BCG_PACKET_SIZE) {

                    var outdata = LongArray(11)

                    var nowIndex = j* 18
                    var nowIndex2 :Int
                    var nowIndex3 :Int


                    outdata.set(0,0L)
                    outdata.set(1,0L)
                    nowIndex += 2
                    nowIndex2 = nowIndex +1
                    nowIndex3 = nowIndex +2

///////////////////////////////PreADC
                    //(ecgData[nowIndex3].toLong() and 0xFFL shl 16)
                    var unsigned_check =  ( (ecgData[nowIndex].toLong() and 0xFFL) or(ecgData[nowIndex2].toLong()and 0xFFL shl 8) or (ecgData[nowIndex3].toLong()and 0xFFL shl 16  ) )
                    ///if(unsigned_check>8388608) unsigned_check -= 16777215
                    //outdata.set(2, unsigned_check)
                    if(BCG_pc<BCG_th ) {
                        BCG_pp += DataPoint((BCG_th - BCG_pc), (unsigned_check).toInt())
                        BCG_pc++
                    }
                    //else if(BCG_pc == BCG_th && ECG_draw>0){
                    // BCG_pp.drop(1)
                    // BCG_pp { it.xVal -=1}
                    // BCG_pp += DataPoint((BCG_th - BCG_pc), (unsigned_check+2000000).toInt())

                    //}
                    //outdata.set(2, (ecgData.get(nowIndex).toUByte().toLong() or (ecgData.get(nowIndex2).toUByte().toLong().shl(8)) or (ecgData.get(nowIndex3)).toUByte().toLong().shl(16)))
                    nowIndex +=3
                    outdata.set(3, ecgData[nowIndex].toLong() )///heart rate
                    nowIndex +=1
                    outdata.set(4, ecgData[nowIndex].toLong() )//////Respiratory rate
                    nowIndex +=1
                    outdata.set(5, ecgData[nowIndex].toLong() )//////////signal_status and curve_status
                    nowIndex +=1
                    nowIndex2 = nowIndex +1


                    val hrtmp = outdata.get(3).toInt()

                    if(hrtmp >40 && hrtmp <200){
                            hrcnt++
                            hrtotal +=hrtmp
                    }

                    //var signal_status = outdata.get(5)/16

                    var minus_test = (ecgData[nowIndex].toLong() or (ecgData[nowIndex2].toLong() shl 8))
                    //if(minus_test > 32767 )minus_test -= 65536
                    outdata.set(6, minus_test)//////////ACC_x
                    //outdata.get(6)

                    nowIndex += 2
                    nowIndex2 = nowIndex +1

                    minus_test = (ecgData[nowIndex].toLong() or (ecgData[nowIndex2].toLong() shl 8))
                    //if(minus_test > 32767 )minus_test -= 65536
                    outdata.set(7, minus_test)//////////ACC_y

                    nowIndex += 2
                    nowIndex2 = nowIndex +1

                    minus_test = (ecgData[nowIndex].toLong() or (ecgData[nowIndex2].toLong() shl 8))
                    //if(minus_test > 32767 )minus_test -= 65536
                    outdata.set(8, minus_test)//////////ACC_z

                    nowIndex += 2
                    nowIndex2 = nowIndex +1
                    nowIndex3 = nowIndex +2

////////////////////////////////TimeStamp
                    /* Save data */
                    var BCGtstp = (ecgData[nowIndex].toLong() and 0xFFL or ( ecgData[nowIndex2].toLong() and 0xFFL shl 8) or (ecgData[nowIndex3].toLong() and 0xFFL shl 16))

                    var originalText = BCGtstp.toString() + "," + unsigned_check
                            .toString() + "," + outdata.get(6).toString() + ","
                    originalText += outdata.get(7).toString() + "," + outdata.get(8)
                            .toString() + ","
                    originalText += outdata.get(3).toString() + "," + outdata.get(4)
                            .toString() + "," + outdata.get(5).toString() + "\n"

                    bcgFile.appendText(originalText)
                    //prev_ts = BCGtstp
                    j+=1
                }///////write bcg parced data
                if(hrcnt>0){
                    val hrtmp = (hrtotal/hrcnt)
                    hrlist.add(hrtmp)
                }
                else {

                }
            }
        }


        override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int){

            Log.e("GATT", "READ")

            var recData: ByteArray = characteristic!!.value
            var unixTIme: ULong = recData[3].toUByte().toULong().shl(24) + recData[2].toUByte().toULong().shl(16) +
                    recData[1].toUByte().toULong().shl(8) + recData[0].toUByte().toULong()
            Log.e(
                    "data",
                    recData[0].toUByte().toString() + "," + recData[1].toUByte()
                            .toString() + "," + recData[2].toUByte()
                            .toString() + "," + recData[3].toUByte().toString() + "," +
                            recData[4].toUByte().toString() + "," + recData[5].toUByte()
                            .toString() + ","
            )

        }
        override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int){
            Log.e("GATT", "WRITE " + characteristic.toString() + status.toString())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
            if(requestCode==1 && resultCode == RESULT_OK){

                device_add = data!!.getStringExtra("device").toString()
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Connect to " +  device_add + " successful!")
                builder.show()


                bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothAdapter = bluetoothManager?.adapter


                bt_bluetooth.setImageResource(R.drawable.bt_on)
                blecnt =true

            }
        else if(requestCode==1){
            finish()
        }
        else if(requestCode == 5 && resultCode == RESULT_OK){
            user = data!!.getStringExtra("user")!!
                val builder = AlertDialog.Builder(this)
                builder.setMessage("User changed to " + user)
                builder.show()
        }

    }
    fun c_SD(numArray: List<DataPoint>): Double {
        var sum = 0.0
        var standardDeviation = 0.0

        for (num in numArray) {
            sum += num.yVal
        }

        val mean = sum / numArray.size

        for (num in numArray) {
            standardDeviation += Math.pow(num.yVal - mean, 2.0)
        }

        return Math.sqrt(standardDeviation / 10)
    }


    fun write_new_file(){

        val dff = SimpleDateFormat("yyyy-MM-dd-HH-mm")
        dff.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))

        DefaultFileName = dff.format(Date()) + "_ECG.log"
        DefaultBCGName = dff.format(Date()) + ".log"
        Log.e("File", DefaultFileName)
        Log.e("File", DefaultBCGName)
        //mgatt = d.connectGatt()

        if(FinalBCGname == DefaultBCGName){
        }
        else {
            var extFile = File(storagePath, "$ECG_DATA_DIRECTORY/$DefaultFileName")
            var bcgFile = File(storagePath, "$ECG_DATA_DIRECTORY/$DefaultBCGName")

            extFile.appendText("app_version: " + "0.1.0" + "\nStart_Time: " + dff.format(Date()) + "\n")
            bcgFile.appendText("app_version: " + "0.1.0" + "\nStart_Time: " + dff.format(Date()) + "\n")
        }
        FinalECGname = DefaultFileName
        FinalBCGname = DefaultBCGName
    }

    fun clickble(view: View) {
        if(!blecnt) {

            val intent = Intent(this, bluetooth::class.java)
            startActivityForResult(intent, 1)

        }
        else{

            if (mgatt?.device != null) {

                mgatt?.disconnect()
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
            //if(plot_thread.isAlive)plot_thread.interrupt()
            blecnt = false
            bt_bluetooth.setImageResource(R.drawable.bt_off)

        }
    }
    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }


    fun clickback(view: View) {

        if(!blecnt){
            if(!ecgdatalog.isEmpty() || !bcgdatalog.isEmpty() ) {
                getIntent().putStringArrayListExtra("ecgdatalog", ecgdatalog)
                getIntent().putStringArrayListExtra("bcgdatalog", bcgdatalog)

                setResult(RESULT_OK, getIntent())
                finish()

            }
            else{
                setResult(RESULT_CANCELED, getIntent())
                finish()
            }
        }
        else {

            val intent = Intent(this, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }
    }

    fun clickwaveform(view: View) {
        when(wavemode){
            0->{

                bt_waveform.text = "心跳波型"
                wavemode = 1
            }//////////origin wave to heartbeat
            1->{

                bt_waveform.text = "呼吸波型"
                wavemode = 2
            }//////////heartbeat wave to hushi
            2->{
                bt_waveform.text = "疲勞波型"
                wavemode = 3
            }//////////hushi wave to fatigue
            3->{
                bt_waveform.text = "原始波型"
                wavemode = 0

            }//////////fatigue to origin
        }

    }

    fun clickautoup(view: View) {
        if(autoup){

            bt_autoup.text = "開啟數據上傳"
            autoup = false
        }
        else{

            bt_autoup.text = "關閉數據上傳"
            autoup = true
        }
    }

    fun clickwaveset(view: View) {

    }

    fun fetchJSON(){
        Thread{
            try {
                ///////private String url1 = "http://api.openweathermap.org/data/2.5/weather?q=";
                //val url = "http://my-json-feed"
                //car_plate = et_input?.text.toString()
                //timestamp = "1614787600"
                // heart_rate = 85
                //var realurl = url + "=" + car_plate
                val response = StringBuilder()

                var realurl = url

                var testurl = ""

                Log.e("url", realurl)
                //tv_timestamp.text = testurl
                var sess= URL(realurl);
                var conn = sess.openConnection() as HttpURLConnection;

                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestMethod("POST");

                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);

                conn.connect();

                val os = conn.outputStream
                val writer = DataOutputStream(os)
                val jsonString: String = jsonObject.toString()
                //writer.writeBytes(jsonString)
                Log.e("jsonString", jsonString)

                writer.writeBytes(jsonString);

                writer.flush()
                writer.close()

                val `is` = conn.inputStream

                val reader = BufferedReader(InputStreamReader(`is`))

                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    //tmp += line
                    response.append(line)
                    response.append('\r')
                }
                //tv_name.text = response.toString()
                reader.close()
                //thdone = true
                //var reader = conn.getInputStream();

                //var str_data = convertStreamToString(reader);
                //readAndParseJSON(str_data);

                //stream.close();
                os.close()

            }catch (e: Exception) {
                e.printStackTrace();
            }
        }.start()

    }

    fun clickvib(view: View) {
        if(vibmode){
            ib_vib.setImageResource(R.drawable.vib_off)
            vibmode =false
        }
        else{
            ib_vib.setImageResource(R.drawable.vib_on)
            vibmode = true
        }
    }

    fun clickchangeuser(view: View) {
        val intent = Intent(this, changeuser::class.java)
        startActivityForResult(intent, 5)

    }
    fun clickdata(view: View) {
        if(!dataclt) {
            Thread {
                //var dev_ind =0
                /*
                for (r in bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)) {
                    if (r.address == device_add) {
                        device = r
                    }
                }
                */
                device = bluetoothManager.getConnectedDevices(GATT)[0]
                //device = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT)[0]
                mgatt = device.connectGatt(this, false, gattCB)
            }.start()
            write_new_file()
            dataclt = true
            bt_startdatacollect.text = "停止收集"
        }
        else{
            if (mgatt?.device != null) mgatt?.disconnect()
            dataclt = false
            bt_startdatacollect.text = "收集資料"
        }
    }
}