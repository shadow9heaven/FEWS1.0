package com.ble.drive_status_cntl

import android.Manifest
import android.bluetooth.*
import android.bluetooth.BluetoothProfile.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock.sleep
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import com.ble.drive_status_cntl.bluetooth as bluetooth

class bluetooth : AppCompatActivity() {
    var device_connect = false
    var refreshing= false

    val SERVICE_UUID_UART = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
    val CHARACTERISTIC_UUID_UART_TX = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E" //TX

    val CHARACTERISTIC_UUID_UART_RX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e" //RX

    val DESCRIPTOR_UUID_ID_TX_UART = "00002902-0000-1000-8000-00805f9b34fb"

    var Service_UART: BluetoothGattService? = null
    var CHARACTERISTIC_UART_TX: BluetoothGattCharacteristic? = null
    var CHARACTERISTIC_UART_RX: BluetoothGattCharacteristic? = null

    private val REQUEST_CODE_ENABLE_BT: Int = 1
    private lateinit var bluetoothManager : BluetoothManager
    private lateinit var bluetoothAdapter : BluetoothAdapter
    private val bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().bluetoothLeScanner

    ////////////save list
    private var BLE_DeviceList = HashMap<String, ScanResult>()
    private var devicename: List<HashMap<String, String>> = ArrayList()
    private val listlabel = arrayOf("name", "misc")
    private val listid = intArrayOf(android.R.id.text1, android.R.id.text2)
    ////////////
    lateinit var tx_srh : TextView

    lateinit var mgatt: BluetoothGatt

    var bthHandler: Handler? = Handler()

    private lateinit var listView: ListView
    private var adapter: SimpleAdapter? = null

    private val STATE_DISCONNECTED = 0
    private val STATE_CONNECTING = 1
    private val STATE_CONNECTED = 2
    val ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
    val ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
    val ACTION_GATT_SERVICES_DISCOVERED =
        "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
    val ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    val EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA"

    /*
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
       //if (isScrollToTop()) {

            //Log.e("RefreshRecyclerView", "ScrollToTop")
            when (ev.action) {

                MotionEvent.ACTION_DOWN -> {


                }

                MotionEvent.ACTION_MOVE -> {

                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                }

            }
                    //}

        return super.dispatchTouchEvent(ev)
    }
*/
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth)

        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter

        if (bluetoothAdapter?.isEnabled == false) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, 1)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    101
            )
        }

        /*
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "Request External Storage")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                9
            )
        }
        */

        listView = findViewById<ListView>(R.id.BTHlist);
        tx_srh = findViewById(R.id.searchtext);

        Log.e("GATT", bluetoothManager.getConnectedDevices(GATT).toString())

        bluetoothLeScanner!!.startScan(leScanCallback)
        tx_srh.text = "Searching......"

        bthHandler?.postDelayed(Runnable {
            bluetoothLeScanner!!.stopScan(leScanCallback)

            for (r in  BLE_DeviceList) {
                val hashMap: HashMap<String, String> = HashMap()
                hashMap.put("name", r.value.device.name)
                hashMap.put("misc", r.key)
                devicename += hashMap
            }

            adapter = SimpleAdapter(
                    this,
                    devicename,
                    android.R.layout.simple_list_item_2,
                    listlabel,
                    listid
            );
            listView.setAdapter(adapter);

            tx_srh.text = ""
        }, 3000)

        listView.setOnItemClickListener{ parent, view, position, id ->
            clickConn(this, position)
        }

    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {

            if(result!!.device.name != null) {
                BLE_DeviceList.put(result!!.device.toString(), result)
            }

        }
        override fun onScanFailed(errorCode: Int) {
            Log.e("Scan Failed", "Error Code: $errorCode")
        }
    }



    fun clickConn(view: bluetooth, position: Int) {
        if (bluetoothAdapter?.isEnabled == false) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, REQUEST_CODE_ENABLE_BT)
        }
        Log.e("BUTTON $position", BLE_DeviceList.keys.elementAt(position))

        mgatt = BLE_DeviceList.get(BLE_DeviceList.keys.elementAt(position))?.device!!.connectGatt(
                applicationContext,
                false,
                gattCallback
        )

        sleep(1000)

        if(bluetoothManager?.getConnectionState(mgatt.device,GATT) != STATE_DISCONNECTED && bluetoothManager?.getConnectionState(mgatt.device, GATT) != STATE_DISCONNECTING){
            val builder = AlertDialog.Builder(this@bluetooth)
            //builder.setMessage("Connect to " + mgatt.device.toString() + " successful!")
            //builder.show()
            device_connect = true
            //mgatt.disconnect()

            getIntent().putExtra("device",mgatt.device.toString())
            setResult(RESULT_OK , getIntent())
            finish()

        }
    }


    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if(newState != STATE_DISCONNECTED && gatt != null) {
                gatt?.discoverServices()

            }
            else if(newState ==STATE_DISCONNECTED){

                broadcastUpdate(ACTION_GATT_DISCONNECTED)

            }

        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {

            Service_UART = gatt!!.getService(UUID.fromString(SERVICE_UUID_UART))

            Log.e("BLE_SERVICE", "onServicesDiscovered()"+gatt.services.toString())

            if(Service_UART == null)
            {
                Log.e("BLE_service", "onServicesDiscovered() Service_UART not found")
                //return
            }

            CHARACTERISTIC_UART_TX = Service_UART!!.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_UART_TX))
            CHARACTERISTIC_UART_RX = Service_UART!!.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID_UART_RX))

            gatt.setCharacteristicNotification(CHARACTERISTIC_UART_TX, true)


            //Enable Notification for UART TX
            val _descriptor = CHARACTERISTIC_UART_TX?.getDescriptor(UUID.fromString(DESCRIPTOR_UUID_ID_TX_UART))
            if (_descriptor != null) {
                Log.i("BLE_SERVICE", "onServicesDiscovered() Write to Descriptor ENABLE_NOTIFICATION_VALUE")
                _descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                //_descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(_descriptor)
            } else Log.i("BLE_SERVICE", "onServicesDiscovered() descriptor == null")

        }
    }

    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

    fun clickDisconnect(view: View) {
            Log.e("disconnect", bluetoothManager.getConnectedDevices(GATT).toString())
            // var bAdapter : BluetoothAdapter = bluetoothManager?.getAdapter()
            // var prdevice = bAdapter.getRemoteDevice()

            for(d in bluetoothManager.getConnectedDevices(GATT)  ) {
                //var gcs =  bluetoothManager?.getConnectionState(d, GATT)
                //if(gcs== STATE_CONNECTED || gcs == STATE_CONNECTING){

                mgatt = d.connectGatt(applicationContext, false, gattCallback)
                //mgatt?.disconnect()
                mgatt?.close()
                broadcastUpdate(ACTION_GATT_DISCONNECTED)

                //mgatt = null
                //d.createBond()
                //d::class.java.getMethod("refresh").invoke(d)
                //Log.e("disconnect", d.toString())
                //}
            }
            Log.e("disconnect", bluetoothManager.getConnectedDevices(GATT).toString())
        }

    fun clickUpdate(view: View) {
        if (bluetoothAdapter?.isEnabled == false) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, 1)
        }

        for(i in devicename) devicename -= i

        bluetoothLeScanner!!.startScan(leScanCallback)

        tx_srh.text = "Searching......"
        bthHandler?.postDelayed(Runnable {
            bluetoothLeScanner!!.stopScan(leScanCallback)
            for (r in BLE_DeviceList) {
                val hashMap: HashMap<String, String> = HashMap()
                hashMap.put("name", r.value.device.name)
                hashMap.put("misc", r.key)
                if (!devicename.contains(hashMap)) devicename += hashMap
            }

            adapter = SimpleAdapter(
                    this,
                    devicename,
                    android.R.layout.simple_list_item_2,
                    listlabel,
                    listid
            );
            listView.setAdapter(adapter);
            tx_srh.text = ""
        }, 3000)
    }
    fun clickback(view: View) {
        if(device_connect) {
            setResult(RESULT_OK, getIntent())
        }
        else{
            setResult(RESULT_CANCELED, getIntent())
        }

        finish()
    }

}