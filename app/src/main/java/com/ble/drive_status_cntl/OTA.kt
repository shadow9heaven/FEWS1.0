package com.ble.drive_status_cntl

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.kotlin_ota.BLEManager
import com.example.kotlin_ota.global
import java.io.FileInputStream
import java.lang.String
import java.util.*

class OTA : AppCompatActivity() {
    lateinit var BLEM: BLEManager


    lateinit var TV_Out: TextView
    lateinit var TV_Status: TextView
    lateinit var TV_path: TextView
    lateinit var TV_verify: TextView
    lateinit var B_Disconnect: Button
    lateinit var B_Upgrade: Button
    lateinit var B_Loadfile: Button
    lateinit var B_Start_cmd: Button
    lateinit var B_Verify_cmd: Button
    lateinit var B_Clear_cmd: Button
    lateinit var fs: FileInputStream
    lateinit var ota_binary_data:ByteArray
    lateinit var pg_bar: ProgressBar
    lateinit var pg_tv: TextView
    lateinit var pg_bar4: ProgressBar
    lateinit var pg_tv4: TextView
    lateinit var Timestamp_text: TextView
    var isfs=false
    var hdlr: Handler? = Handler()
    var timerHandler: Handler?= Handler()

    var timerRunnable: Runnable = object : Runnable {
        override fun run() {
            runOnUiThread { Update_UI() }
            hdlr!!.post {
                update_progress4(BLEM.ota_progress_count.toInt())
                val ver_str =
                        Integer.toString(BLEM.ver_major.toInt()) + "_" + Integer.toString(BLEM.ver_minor.toInt()) + "_" + Integer.toString(
                                BLEM.ver_patch.toInt()
                        )
                B_Start_cmd.text = "VERSION:$ver_str"
                Log.e("progess", BLEM.ota_progress_count.toString());
            }
            timerHandler?.postDelayed(this, 100)
        }
    }


    var major=0
    var minor=0
    var patch=0
    var ota_times=0
    var TAG="MAIN"
    var progress_count:Int=0
    var PERMISSION_REQUEST_STORAGE = 1000
    var READ_REQUEST_CODE=42
    var filesize :Long=0L
    var sha256key = byteArrayOf(
            0x49.toByte(), 0x10.toByte(), 0x25.toByte(), 0x4a.toByte(), 0x2a.toByte(), 0x6e.toByte(),
            0xe0.toByte(), 0xe3.toByte(), 0xd5.toByte(), 0x6d.toByte(), 0x75.toByte(), 0x95.toByte(),
            0xb8.toByte(), 0x5d.toByte(), 0x85.toByte(), 0xfc.toByte(), 0xee.toByte(), 0x7.toByte(),
            0x7b.toByte(), 0xd5.toByte(), 0xcc.toByte(), 0x5f.toByte(), 0x4.toByte(), 0xc7.toByte(),
            0x9e.toByte(), 0x66.toByte(), 0x5a.toByte(), 0x49.toByte(), 0x3.toByte(), 0x21.toByte(),
            0x14.toByte(), 0x42.toByte(), 0x99.toByte(), 0x83.toByte(), 0x1c.toByte(), 0x81.toByte(),
            0x97.toByte(), 0x45.toByte(), 0xb2.toByte(), 0xd8.toByte(), 0x6c.toByte(), 0x31.toByte(),
            0xc5.toByte(), 0x70.toByte(), 0x62.toByte(), 0x35.toByte(), 0xff.toByte(), 0xb2.toByte(),
            0x78.toByte(), 0xae.toByte(), 0xe2.toByte(), 0x49.toByte(), 0xd9.toByte(), 0x93.toByte(),
            0x3c.toByte(), 0x54.toByte(), 0xd9.toByte(), 0x47.toByte(), 0xd0.toByte(), 0xe2.toByte(),
            0x16.toByte(), 0x17.toByte(), 0x12.toByte(), 0x3d.toByte(), 0x8.toByte(), 0xa3.toByte()
    )

    fun Update_UI() {
        TV_Status.text =
            if (BLEM.isConnected()) String.format("CONNECTED to %s", BLEM.SERVER_NAME) else "DISCONNECTED"
        TV_Status.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (BLEM.isConnected()) R.color.color_temp_low else R.color.light_gray
            )
        )
        B_Disconnect.text = if (BLEM.isConnected()) "DISCONNECT" else "CONNECT"
        B_Disconnect.setBackgroundColor(
            ContextCompat.getColor(
                this,
                if (BLEM.isConnected()) R.color.light_gray else R.color.color_temp_low
            )
        )
        if (BLEM.isConnected()) {
            B_Start_cmd.isEnabled = true
            B_Verify_cmd.setEnabled(true)
            B_Clear_cmd.isEnabled = true
            B_Loadfile.isEnabled = true
            if (isfs!=false) {
                B_Upgrade.isEnabled = true
            } else {
                B_Upgrade.isEnabled = false
            }
        } else {
            B_Start_cmd.isEnabled = false
            B_Verify_cmd.setEnabled(false)
            B_Clear_cmd.isEnabled = false
            B_Loadfile.isEnabled = false
            B_Upgrade.isEnabled = false
        }
    }
    fun update_progress(count: Int) {
        pg_tv.setText(Integer.toString(count) + "%")
        pg_bar.setProgress(count)
    }

    fun update_progress4(count: Int) {
        pg_tv4.setText(Integer.toString(count) + "%")
        pg_bar4.setProgress(count)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ota)

        B_Disconnect = findViewById(R.id.B_Disconnect)
        B_Upgrade = findViewById(R.id.B_upgrade_cmd)
        B_Loadfile = findViewById(R.id.B_loadfile)
        B_Start_cmd = findViewById(R.id.B_start_cmd)

        TV_path = findViewById(R.id.TV_path)
        pg_bar = findViewById(R.id.progressBar3)
        pg_tv = findViewById(R.id.progress_text)

        pg_bar4 = findViewById(R.id.progressBar4)
        pg_tv4 = findViewById(R.id.progress4_text)

        global.instance.init(this)
        val ota_device_name = "SMART_SEAT1.3"
        BLEM = global.instance.BLEMan!!

        if (!BLEM.isBluetoothEnabled()) {
            global.DisplayToast("Bluetooth disabled")
        } else {
            BLEM.Connect(this, ota_device_name)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //Start UI Update Timer
        //Start UI Update Timer
        timerHandler!!.postDelayed(timerRunnable, 0)
        pg_bar.progress = progress_count
        pg_bar4.progress = 0
        B_Upgrade.isEnabled = false
    }

    fun B_Disconnect_onClick(v: View?) {
        if (BLEM.isConnected()) BLEM.Disconnect() else BLEM.Connect(this)
    }
    fun ota_key_cmd() {
        //byte[] data = sha256key;
        BLEM.UART_Writebytes(sha256key)
    }


    fun ota_start_cmd(addr: Int, len: Int) {
        val pack = ByteArray(10)
        pack[0] = 0x50.toByte()
        pack[1] = 0x08.toByte()
        pack[2] = (len shr 0).toByte()
        pack[3] = (len shr 8).toByte()
        pack[4] = (len shr 16).toByte()
        pack[5] = (len shr 24).toByte()
        pack[6] = (addr shr 0).toByte()
        pack[7] = (addr shr 8).toByte()
        pack[8] = (addr shr 16).toByte()
        pack[9] = (addr shr 24).toByte()
        BLEM.UART_Writebytes(pack)
    }

    fun ota_write_cmd(pdata: ByteArray) {
        val pack = ByteArray(pdata.size + 2)
        pack[0] = 0x51
        pack[1] = (pdata.size and 0xff).toByte()
        for (i in 2 until pack.size) {
            pack[i] = pdata[i - 2]
        }
        BLEM.UART_Writebytes(pack)
    }


    fun ota_verify_cmd(check: Int) {


        //byte[] data={(byte)0x52,(byte)0x01,(byte)0x43,(byte)0x12, (byte) 0xab, (byte) 0xcd};
        val data = ByteArray(6)
        data[0] = 0x52.toByte()
        data[1] = 0x01.toByte()
        data[2] = (check shr 0).toByte()
        data[3] = (check shr 8).toByte()
        data[4] = (check shr 16).toByte()
        data[5] = (check shr 24).toByte()
        BLEM.UART_Writebytes(data)
    }

    fun ota_clearrom_cmd(start_addr: Int, end_addr: Int) {
//        byte[] data={(byte)0x53,(byte)0x08,(byte)0x00,(byte)0x00, (byte) 0x05, (byte) 0x00
//                ,(byte)0x00,(byte)0x00, (byte) 0x07, (byte) 0x00};
        val data = ByteArray(10)
        data[0] = 0x53
        data[1] = 0x08
        data[2] = (start_addr shr 0).toByte()
        data[3] = (start_addr shr 8).toByte()
        data[4] = (start_addr shr 16).toByte()
        data[5] = (start_addr shr 24).toByte()
        data[6] = (end_addr shr 0).toByte()
        data[7] = (end_addr shr 8).toByte()
        data[8] = (end_addr shr 16).toByte()
        data[9] = (end_addr shr 24).toByte()
        BLEM.UART_Writebytes(data)
    }

    fun ota_flash_cmd() {
        val data = byteArrayOf(
                0x54.toByte(),
                0x01.toByte(), 0x43.toByte(), 0x12.toByte(), 0xab.toByte(), 0xcd.toByte()
        )
        BLEM.UART_Writebytes(data)
    }

    fun ota_check_version() {
        val data = ByteArray(2)
        data[0] = 0x55.toByte()
        data[1] = 0x01.toByte()
        BLEM.UART_Writebytes(data)
    }

    fun ota_write_label(addr: Int, major: Int, minor: Int, patch: Int) {
        val data = ByteArray(8)
        data[0] = 0x56.toByte()
        data[1] = (addr shr 0).toByte()
        data[2] = (addr shr 8).toByte()
        data[3] = (addr shr 16).toByte()
        data[4] = (addr shr 24).toByte()
        data[5] = major.toByte()
        data[6] = minor.toByte()
        data[7] = patch.toByte()
        BLEM.UART_Writebytes(data)
    }

    fun ota_burn(start_addr: Int, length: Int) {
//        byte[] data = new byte[10];
//        data[0] = (byte)0x57;
//        data[1] = (byte)0x01;
//        data[2] = (byte)0x00;
        val data = ByteArray(10)
        data[0] = 0x57
        data[1] = 0x08
        data[2] = (start_addr shr 0).toByte()
        data[3] = (start_addr shr 8).toByte()
        data[4] = (start_addr shr 16).toByte()
        data[5] = (start_addr shr 24).toByte()
        data[6] = (length shr 0).toByte()
        data[7] = (length shr 8).toByte()
        data[8] = (length shr 16).toByte()
        data[9] = (length shr 24).toByte()
        BLEM.UART_Writebytes(data)
    }


    fun byte_sum(vararg bytes: Byte): Byte {
        var total: Byte = 0
        for (b in bytes) {
            total= (total+b).toByte()
        }
        return total
    }

    fun ota_write_page(address: Int, page: ByteArray): Int {
        var remain_size = page.size
        var count = 0
        val pack_size = 128 //default 128
        val err_ret = 0
        ota_start_cmd(address, page.size) //cmd1
        while (remain_size > 0) {
            if (remain_size >= pack_size) {
                ota_write_cmd(Arrays.copyOfRange(page, count, count + pack_size)) //cmd2
                count += pack_size
                remain_size -= pack_size
            } else {
                ota_write_cmd(Arrays.copyOfRange(page, count, count + remain_size)) //cmd2
                count += remain_size
                remain_size = 0
            }
        }
        ota_flash_cmd() //cmd3
        val checksum = byte_sum(*page)
        android.util.Log.e("checksum", "checksum = " + java.lang.Byte.toString(checksum))
        ota_verify_cmd(checksum.toInt()) //cmd4
        return err_ret
    }

    fun checksum_error(data: ByteArray): Int {
        var sum = 0
        for (i in data.indices) {
            sum += data[i]
        }
        sum = sum and 0xffff
        return sum
    }

    fun ota_upgrade() {
        var address = 0x50000
        var remain_size = ota_binary_data.size
        var count = 0
        val page_size = 4096 //default 4096

        // if ecdsa_verify failed restart ota_key_cmd until pass verification.
        //if the ble device always return noack then try to restart ota upgrade function.
        // do while
        do {
            ota_key_cmd()
        } while (BLEM.is_ble_ack === false)

        //ota_write_label(0x6000,major,minor,patch);
        do {
            ota_clearrom_cmd(0x50000, 0x70000)
        } while (BLEM.is_ble_ack === false)
        ota_times = ota_times + 1 //39   //31
        while (remain_size > 0) {
            if (remain_size >= page_size) {
                val tmp = Arrays.copyOfRange(ota_binary_data, count, count + page_size)
                ota_write_page(address, tmp)
                address += page_size
                count = count + page_size
                remain_size -= page_size
            } else {
                val tmp = Arrays.copyOfRange(ota_binary_data, count, count + remain_size)
                ota_write_page(address, tmp)
                address += remain_size
                count = count + remain_size
                remain_size = 0
            }
            progress_count = 100 - remain_size * 100 / ota_binary_data.size
            hdlr!!.post {
                TV_verify.text = "Times: " + Integer.toString(ota_times) +
                        "  verify f" + Integer.toString(BLEM.verify_fail_count) +
                        "  clear f:" + Integer.toString(BLEM.clear_fail_count)
                //update_progress(progress_count)
                // update_progress4(BLEM.ota_progress_count);
                // Log.e("progess", Integer.toString(BLEM.ota_progress_count));
            }
        }
    }


    fun B_Start_cmd_onclick(v: View?) {
        ota_check_version()
    }

    fun B_Verify_cmd_onclick(v: View?) {
        ota_write_label(0x6000, major, minor, patch)
    }


    fun B_loadfile_onClick(view: View?) {
        android.util.Log.e(TAG, "load file from external flash")
        performFilesearch()
    }

    fun B_upgrade_cmd_onclick(view: View?) {
        Thread { // a potentially time consuming task
            val startTime = System.nanoTime()
            progress_count = 0
            hdlr!!.post {
                //update_progress(0)
                //update_progress4(0)
                BLEM.ota_progress_count = 0
            }
            do {
                ota_write_label(0x6000, major, minor, patch)
            } while (BLEM.is_ble_ack === false)
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            ota_upgrade()
            do {
                // ota_clearrom_cmd(0x50000,0x70000);
                ota_burn(0x6000, ota_binary_data.size)
            } while (BLEM.is_ble_ack === false)
            android.util.Log.e(
                    "Measure",
                    "TASK took : " + java.lang.Long.toString((System.nanoTime() - startTime) / 1000000) + "ms"
            )
            Timestamp_text.setText("Time duration:" + (System.nanoTime() - startTime) / 1000000 + "ms")
            try {
                Thread.sleep(500)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }.start()
//        new Thread(new Runnable() {
//            public void run() {
//
//
//
//                long startTime =   System.nanoTime();
//                ota_upgrade();
//                Log.e("Measure", "TASK took : " +Long.toString((System.nanoTime()-startTime)/1000000)+"ms");
//
//                Timestamp_text.setText("Time duration:" + ((System.nanoTime()-startTime)/1000000)+"ms");
//
//
//
//            }
//        }).start();
    }

    fun B_clear_cmd_onclick(view: View?) {
        ota_clearrom_cmd(0x50000, 0x51000)
    }

    fun B_close(view: View?) {
        BLEM.Disconnect()
        finish()
        System.exit(0)
    }


    fun B_download_bin(view: View?) {
//        String downloadUrl="";
//        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
//
//        request.allowScanningByMediaScanner();
//        request.setNotificationVisibility(DownloadManager.
//                Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
//        request.setDestinationInExternalPublicDir("/Biologue_OTA", "binary.bin");
//        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
//        dm.enqueue(request);
        Toast.makeText(applicationContext, "Downloading File", Toast.LENGTH_LONG).show()
    }

    fun B_key_cmd_onclick(view: View?) {
        ota_key_cmd()
    }
    private fun performFilesearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    fun clickback(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent)

    }
}