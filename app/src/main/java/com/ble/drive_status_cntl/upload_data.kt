package com.ble.drive_status_cntl

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File


class upload_data : AppCompatActivity() {

    lateinit var uri : Uri
    lateinit var path :String

    lateinit var ecgspin :Spinner
    lateinit var bcgspin :Spinner

    lateinit var ecgdatalog : ArrayList<String>
    lateinit var bcgdatalog : ArrayList<String>


    lateinit var datafilepath :File
    var ecgindex = 0
    var bcgindex = 0

    lateinit var tv_ecgfile :TextView
    lateinit var tv_bcgfile :TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_data)

        datafilepath = this.getExternalFilesDir(null)!!


        tv_ecgfile = findViewById(R.id.tv_ecgfile)
        tv_bcgfile = findViewById(R.id.tv_bcgfile)

        try{
            if(!haveInternet()){
                Toast.makeText(baseContext, "didn't connect to the internet", Toast.LENGTH_LONG).show()
                finish()
            }



            ecgdatalog = intent?.getStringArrayListExtra("ecgdatalog")!!
            bcgdatalog = intent?.getStringArrayListExtra("bcgdatalog")!!

            ecgspin = findViewById(R.id.ecgspin)
            bcgspin = findViewById(R.id.bcgspin)

            val ecgadapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, ecgdatalog)
            ecgspin.adapter = ecgadapter
            ecgspin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    Log.e("Spinner", "select $p2")
                    ecgindex = p2
                    tv_ecgfile.text = ecgdatalog[ecgindex]
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    Log.e("Spinner", "Nothing select")
                }
            }


            val bcgadapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, bcgdatalog)
            bcgspin.adapter = bcgadapter
            bcgspin.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                    Log.e("Spinner", "select $p2")
                    bcgindex = p2
                    tv_bcgfile.text = bcgdatalog[bcgindex]
                }

                override fun onNothingSelected(p0: AdapterView<*>?) {
                    Log.e("Spinner", "Nothing select")
                }
            }

        }
        catch (e: Exception){

        }


    }

    private fun haveInternet(): Boolean {
        var result = false
        val connManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = connManager.activeNetworkInfo
        result = if (info == null || !info.isConnected) {
            false
        } else {
            if (!info.isAvailable) {
                false
            } else {
                true
            }
        }

        return result
    }

    fun parceECG():Boolean{
        Thread {
            var localFile = File(datafilepath , "/ECG_DATA/" + ecgdatalog[ecgindex])
            try{
                var ecg_linecount=0
                var ecg_ =1
                var version = "app_version:"
                var starttime = "Start_Time:"
                localFile.forEachLine {

                   val xx =  it.split(" ")
                    var ts = xx[0]
                   var data  = xx[1]
                    if(ts == version){
                        version = data
                    }
                    else if(ts == starttime){
                        starttime = data
                    }
                    else{

                    }


                }

            }
            catch(e :java.lang.Exception){}
        }.start()
            return true
    }

    fun parceBCG():Boolean{
        Thread {
            var localFile = File(datafilepath , "/ECG_DATA/" + bcgdatalog[bcgindex])
            try{
                var bcg_linecount=0
                var bcg_ =1
                var version = "app_version:"
                var starttime = "Start_Time:"
                localFile.forEachLine {

                    val xx =  it.split(" ")
                    var ts = xx[0]
                    var data  = xx[1]
                    if(ts == version){
                        version = data
                    }
                    else if(ts == starttime){
                        starttime = data
                    }
                    else{

                    }


                }

            }
            catch(e :java.lang.Exception){}
        }.start()
        return true
    }


    fun clickback(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent)
    }



    fun fabChoose_Click(view: View) {
        val intent= Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        val dest = Intent.createChooser(intent, "Select")
        startActivityForResult(dest, 42)

    }//////choose file



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 42 && resultCode == RESULT_OK && data !=null) {
            uri = data?.data!!
            var u2f = File(uri!!.getPath()).absolutePath.split(":")
            path = "/" + u2f.get(1)
            //var filename =
            //ECGfilename =

            Log.e("fileload path", path)
            //Log.e("fileload canpath",u2f.canonicalPath)

            //in_strm = getContentResolver().openInputStream(uri);

            /*
            val iv = findViewById<TextView>(R.id.textView4)
            if (uri != null){
                iv.text = ECGfilename
                fchoosed = true
            }

             */

            Toast.makeText(this, "load file : " , Toast.LENGTH_SHORT).show()

            //data_Dir
        }
    }
}