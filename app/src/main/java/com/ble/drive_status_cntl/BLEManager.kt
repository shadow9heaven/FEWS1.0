package com.example.kotlin_ota

import android.R
import android.app.Activity
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context.BLUETOOTH_SERVICE
import android.os.Handler
import android.util.Log
import android.widget.SimpleAdapter
import java.math.BigInteger
import java.util.*

class BLEManager(activity: Activity?) {
    var TAG = "BLEManager"
    var SERVER_NAME = "UART Service"
    private val PARAM_UART_INPUT_BUFFER_MAX_LENGTH = 1024
    val SERVICE_UUID_UART = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    val CHARACTERISTIC_UUID_UART_TX = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") //TX

    val CHARACTERISTIC_UUID_UART_RX = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e") //RX

    val DESCRIPTOR_UUID_ID_TX_UART = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    var bluetoothAdapter: BluetoothAdapter? = null
    var bluetoothLeScanner: BluetoothLeScanner? = null
    var bluetoothManager: BluetoothManager? = null
    var GattClient: BluetoothGatt? = null
    var Service_UART: BluetoothGattService? = null
    var CHARACTERISTIC_UART_TX: BluetoothGattCharacteristic? = null
    var CHARACTERISTIC_UART_RX: BluetoothGattCharacteristic? = null
    var bthHandler: Handler? = Handler()
    var get_ack_from_ble = false
    var verify_pass_count = 0
    var verify_fail_count = 0

    var clear_pass_count = 0
    var clear_fail_count = 0
    var is_ble_ack = false
    var ota_rssi = 0
    var ota_progress_count = 0.toByte()
    var ver_major = 0.toByte()
    var ver_minor = 0.toByte()
    var ver_patch = 0.toByte()
    var UART_INPUT_BUFFER = StringBuilder()



    fun isBluetoothEnabled(): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        // Bluetooth is not enabled :)
        // Bluetooth is enabled
        return mBluetoothAdapter?.isEnabled ?: // Device does not support Bluetooth
        false
    }

    fun Connect(activity: Activity) {
        Log.e(TAG, "Connect() to $SERVER_NAME")
        bluetoothManager = activity.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager!!.adapter
        startScan()
    }

    fun Connect(activity: Activity, ServerName: String) {
        SERVER_NAME = ServerName
        Connect(activity)
    }

    fun Disconnect() {
        if (isConnected()) GattClient!!.disconnect()
    }
    fun isConnected(): Boolean {
        return GattClient != null
    }


/*
    fun ReadValue_String(characteristic: BluetoothGattCharacteristic): String? {
        Log.e(TAG, "Read Value")
        val value_string = characteristic.getStringValue(0)
        if (value_string == null) Log.e(TAG, "onServicesDiscovered() Value=null!") else Log.e(
            TAG,
            "onServicesDiscovered() Value=\"$value_string\""
        )
        return value_string
    }

    fun ReadValue_ByteArray(characteristic: BluetoothGattCharacteristic): ByteArray? {
        Log.e(TAG, "Read Value")
        val value_bytes = characteristic.value
        if (value_bytes == null) Log.e(TAG, "onServicesDiscovered() Value=null!") else {
            val output = BigInteger(1, value_bytes).toString(16)
            Log.e(TAG, "onServicesDiscovered() Value=$output")
        }
        return value_bytes
    }

    fun RQST_ReadValue(characteristic: BluetoothGattCharacteristic?) {
        Log.i(TAG, "RQST Read Value")
        GattClient!!.readCharacteristic(characteristic)
    }
    fun UART_Write(msg: String?) {
        WriteValue(CHARACTERISTIC_UART_RX, msg)
    }


    fun WriteValue(characteristic: BluetoothGattCharacteristic?, value_string: String?) {
        /*
        Log.i(TAG, "Read Value");

        if(value_string.length() > 0) {
            Log.i(TAG, "onServicesDiscovered() setting value");
            characteristic.setValue(value_string);

            Log.i(TAG, "onServicesDiscovered() sending characteristic");
            GattClient.writeCharacteristic(characteristic);
        }

         */
    }
*/



    fun UART_Writebytes(data: ByteArray) {
        val startTime = System.nanoTime()
        var interval: Long = 0

        //Long.toString((System.nanoTime()-startTime)/1000000)
        //Timestamp_text.setText("Time duration:" + ((System.nanoTime()-startTime)/1000000)+"ms");
        get_ack_from_ble = false
        UART_Writebyte(CHARACTERISTIC_UART_RX, data)
        // waiting for ack from ble device
        while (get_ack_from_ble != true && interval < 3000) {      //timeout 3s
            interval = (System.nanoTime() - startTime) / 1000000
        }
        if (interval >= 3000) {
            is_ble_ack = false
            Log.e("OTA", "device  nack")
        } else {
            is_ble_ack = true
            // Log.e("OTA","device  ack");
        }
    }

    fun UART_Writebyte(characteristic: BluetoothGattCharacteristic?, data: ByteArray) {
        if (data.size > 0) {
            characteristic!!.value = data
            GattClient!!.writeCharacteristic(characteristic)
        }
    }

    private fun ResetConnection() {
        GattClient = null
        Service_UART = null
        CHARACTERISTIC_UART_RX = null
        CHARACTERISTIC_UART_TX = null
        if (false) //TODO true here if try to reconnect after disconnect
            startScan()
    }

    private fun startScan() {
        Log.e(TAG, "startScan()")
        if (isConnected()) {
            Log.e(TAG, "startScan(): already connected")
            return
        }
        if (bluetoothLeScanner != null) {
            Log.e(TAG, "startScan(): already scanning")
            return
        }
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Log.e(TAG, "startScan(): bluetoothLeScanner == null")
            return
        }
        bluetoothLeScanner!!.startScan(leScanCallback) //callback -> onScanResult(int callbackType, ScanResult result)
        bthHandler?.postDelayed(Runnable {
            bluetoothLeScanner!!.stopScan(leScanCallback)
            Log.i(TAG, "onScanResult(): stopping scan")
            bluetoothLeScanner = null
        }, 3000)
    }

    //System Calls this when BLE is ready to subscribe to Services
    private fun InitCharacteristics() {
        InitCharacteristics_Service_UART() //Service for RX and TX UART
    }

    private fun InitCharacteristics_Service_UART() {
        Log.i(TAG, "onServicesDiscovered() getting service UART")
        Service_UART = GattClient!!.getService(SERVICE_UUID_UART)
        if (Service_UART == null) {
            Log.e(TAG, "onServicesDiscovered() Service_UART not found")
            return
        }
        Log.i(TAG, "onServicesDiscovered() getting characteristic UART")
        CHARACTERISTIC_UART_TX = Service_UART!!.getCharacteristic(CHARACTERISTIC_UUID_UART_TX)
        CHARACTERISTIC_UART_RX = Service_UART!!.getCharacteristic(CHARACTERISTIC_UUID_UART_RX)
        Log.i(TAG, "onServicesDiscovered() enable Notification on characteristic UART")
        GattClient!!.setCharacteristicNotification(CHARACTERISTIC_UART_TX, true)

        //Enable Notification for UART TX
        val _descriptor = CHARACTERISTIC_UART_TX?.getDescriptor(DESCRIPTOR_UUID_ID_TX_UART)
        if (_descriptor != null) {
            Log.i(TAG, "onServicesDiscovered() Write to Descriptor ENABLE_NOTIFICATION_VALUE")
            _descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            //_descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            GattClient!!.writeDescriptor(_descriptor)
        } else Log.i(TAG, "onServicesDiscovered() descriptor == null")
    }

    private fun connectDevice(device: BluetoothDevice?) {
        Log.i(TAG, "connectDevice()")
        if (device == null) Log.e(TAG, "connectDevice(): Device is null") else {
            Log.i(TAG, "connectDevice(): connecting to Gatt")
            if (GattClient == null) {
                GattClient =
                    device.connectGatt(global.instance.context, false, gattCallback)
            } else {
                Log.e(TAG, "connectDevice(): Gatt Client already created -> Stopping")
            }
        }
    }
    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            Log.i(TAG, "onScanResult()")
            if (result?.device?.name != null) {
                if (result?.device.name == SERVER_NAME && result?.device.toString()=="E2:5F:AF:1D:20:09") {
                    Log.e(TAG,result.device.name)
                    Log.e(TAG,result.device.toString())
                    Log.e(TAG, "onScanResult(): Found BLE Device")
                    ota_rssi = result.rssi
                    connectDevice(result.device)
                }
            }
        }
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "ErrorCode: $errorCode")
        }
    }
    // BLE Scan Callbacks
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.i(TAG, "onConnectionStateChange()")
            if (status == BluetoothGatt.GATT_FAILURE) {
                Log.e(TAG, "onConnectionStateChange(): GATT FAILURE")
                return
            }
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onConnectionStateChange(): status != GATT_SUCCESS")
                //Connection lost to BLE-Server <- u sure?
                //ResetConnection();
                //return;
            }
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onConnectionStateChange(): status == GATT_SUCCESS")
            }
            Log.i(TAG, "onConnectionStateChange(): New State: $newState")
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChange CONNECTED")


                //Here connection to Gatt was successful
                global.Log("BluetoothGattCallback", "CONNECTED")
                Log.i(TAG, "onConnectionStateChange(): start discover Services")
                gatt.discoverServices()
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //Connection lost to BLE-Server
                Log.i(TAG, "onConnectionStateChange DISCONNECTED")
                global.Log("BluetoothGattCallback", "DISCONNECTED")
                ResetConnection()
            }
        }
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "onMtuChanged()")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.i(TAG, "onServicesDiscovered()")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "onServicesDiscovered() status != BluetoothGatt.GATT_SUCCESS")
                return
            }
            Log.i(TAG, "Connected to $SERVER_NAME")
            Log.i(TAG, "onServicesDiscovered() status == BluetoothGatt.GATT_SUCCESS")
            InitCharacteristics()
        }

        /*Callback reporting the result of a characteristic read operation.*/  override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.i(TAG, "onCharacteristicRead()")
            Log.i(TAG, "onServicesDiscovered() reading Value")
            val value_bytes = characteristic.value

            //read as bytes
            if (value_bytes == null) Log.e(TAG, "onServicesDiscovered() Value=null!") else {
                val output = BigInteger(1, value_bytes).toString(16)
                Log.e(TAG, "onServicesDiscovered() Value=$output")
            }

            //read as String
            val value_string = characteristic.getStringValue(0)
            if (value_string == null) Log.e(TAG, "onServicesDiscovered() Value=null!") else Log.i(
                TAG,
                "onServicesDiscovered() Value=\"$value_string\""
            )
        }

        /*Callback indicating the result of a characteristic write operation.*/  override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            Log.i(TAG, "onCharacteristicWrite() with Status=$status")
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            if (characteristic == CHARACTERISTIC_UART_TX) {
                val ack = characteristic.value
                /*
                key_state = 0x49,
                start_state = 0x50,
                write_state =0x51 ,
                verify_state = 0x52,
                clearrom_state =0x53,
                flash_state =0x54,
                check_version_state=0x55,
                label_state=0x56
                version state = 0x65

                progress_state = 0x70
                 */get_ack_from_ble = true
                when (ack[0]) {
                    0x49.toByte() -> {
                    }
                    0x50.toByte()  -> if (ack[1] == 14.toByte()) {
                        clear_fail_count = clear_fail_count + 1
                    }
                    0x51.toByte()  -> {
                    }
                    0x52.toByte()  -> if (ack[1] == 10.toByte() ) {
                        verify_pass_count = verify_pass_count + 1
                    } else if (ack[1] == 11.toByte() ) {
                        verify_fail_count = verify_fail_count + 1
                    }
                    0x53.toByte()  -> if (ack[1] == 12.toByte() ) {
                        clear_pass_count = clear_pass_count + 1
                    } else if (ack[1] == 13.toByte() ) {
                        clear_fail_count = clear_fail_count + 1
                    }
                    0x55.toByte()  -> {
                        ver_major = ack[1]
                        ver_minor = ack[2]
                        ver_patch = ack[3]
                    }
                    0x70.toByte() -> ota_progress_count = ack[1]
                }
            }
        }

        override fun onDescriptorRead(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorRead(gatt, descriptor, status)
            Log.i(TAG, "onDescriptorRead()")
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.i(TAG, "onDescriptorWrite()")
        }
    }
}