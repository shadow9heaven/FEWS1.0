package com.ble.drive_status_cntl

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bluetooth.bth_k2.GraphView
import java.io.File
import java.util.*

class health_panel : AppCompatActivity() {



    var GV: GraphView? = null
    var GV2: GraphView? = null


    var mgatt: BluetoothGatt? = null

    private val ECG_DATA_DIRECTORY = "ECG_DATA"

    private lateinit var bluetoothManager : BluetoothManager
    private lateinit var bluetoothAdapter : BluetoothAdapter


    val UUID_SERIVCE =      "1234E001-FFFF-1234-FFFF-111122223333"
    val UUID_CHAR_ECG =     "1234E002-FFFF-1234-FFFF-111122223333"

    val UUID_CHAR_TIME =    "1234E005-FFFF-1234-FFFF-111122223333"
    val UUID_CHAR_COMMAND = "1234E004-FFFF-1234-FFFF-111122223333"
    val UUID_CHAR_BCG =     "1234E006-FFFF-1234-FFFF-111122223333"

    lateinit var biologue_service: BluetoothGattService
    lateinit var biologue_char_ecg: BluetoothGattCharacteristic
    lateinit var biologue_char_time: BluetoothGattCharacteristic
    lateinit var biologue_char_command: BluetoothGattCharacteristic
    lateinit var biologue_char_bcg: BluetoothGattCharacteristic

    private var storagePath: File? = null


    lateinit var bt_bluetooth : ImageButton
    var blecnt  = false

    var descript_write = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_panel)
        bt_bluetooth = findViewById(R.id.IB_bluetooth)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "Request External Storage")
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    9
            )
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


                // iv_ble.setImageResource(R.drawable.bt_off)
                // iv_ble.setImageResource(R.drawable.bt_on)
            }
            //if() {
            //    Bt_BCG?.text = "connect"
            //    bcg_collect_stage = false
            //}
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
            biologue_char_ecg = biologue_service.getCharacteristic(UUID.fromString(UUID_CHAR_ECG))
            biologue_char_time = biologue_service.getCharacteristic(UUID.fromString(UUID_CHAR_TIME))
            biologue_char_command = biologue_service.getCharacteristic(UUID.fromString(UUID_CHAR_COMMAND))

            biologue_char_bcg = biologue_service.getCharacteristic(UUID.fromString(UUID_CHAR_BCG))

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
                        //Thread.sleep(1000)
                        //send_start()
                        //while(!descript_write){ PERMISSION_REQUEST_STORAGE = PERMISSION_REQUEST_STORAGE }
                    }
                }


                /*
                val dff = SimpleDateFormat("yyyy-MM-dd-HH-mm")
                dff.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))

                var extFile = File(storagePath, "$ECG_DATA_DIRECTORY/$DefaultFileName")
                extFile.appendText("app_version: 1.1.4.5"+"\n"+"Start_Time: " + dff.format(Date()) + "\n")

                extFile = File(storagePath, "$ECG_DATA_DIRECTORY/$DefaultBCGName")
                extFile.appendText("app_version: 1.1.4.5"+"\n"+ "Start_Time: " + dff.format(Date()) + "\n")

                 */
            }
            descript_write = true
        }


        override fun onCharacteristicChanged(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?
        ){

        }

        override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int){

            Log.e("GATT", "READ")

            var recData: ByteArray = characteristic!!.value
            //if(recData.size == 6) {
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

            //}
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
                val add = data!!.getStringExtra("device")
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Connect to " +  add + " successful!")
                builder.show()
                bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
                bluetoothAdapter = bluetoothManager?.adapter


                bt_bluetooth.setImageResource(R.drawable.bt_on)
                blecnt =true


            }

    }

    fun clickble(view: View) {
        if(!blecnt) {
            val intent = Intent(this, bluetooth::class.java)
            startActivityForResult(intent, 1)

        }
        else{

            blecnt = false
            bt_bluetooth.setImageResource(R.drawable.bt_off)

        }
    }

}