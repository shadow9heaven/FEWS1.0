package com.ble.drive_status_cntl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject

class AddCarInfo : AppCompatActivity(),View.OnClickListener {
    lateinit var sp_energy_t : Spinner
    lateinit var sp_vehicle_t : Spinner
    lateinit var bt_confirm: Button
    lateinit var ed_car_plate:EditText
    lateinit var ed_brand:EditText
    lateinit var ed_model:EditText
    lateinit var ed_tonnage:EditText
    lateinit var model_t:String
    lateinit var car_plate:String
    lateinit var brand_t:String
    lateinit var message:String
    lateinit var resStr:String
    var carinfo_url="http://59.120.189.128:8081/data/biologueData"
    var tonnage=.0.toFloat()
    var vehicle_t=0
    var energy_t=0
    var timestamp :Long =0L
    var numberOfReq=0
    var ispost=false
    var regex_plate=Regex("[A-Z]{2,3}-[0-9]{3,4}")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car_info)
        message=intent.getStringExtra("message").toString()
        Log.e("ADDCAR","$message")
        findID()
        vehicle_spin()
        energy_spin()
    }
    private fun findID(){
        sp_energy_t=findViewById(R.id.sp_energy_t)
        sp_vehicle_t=findViewById(R.id.sp_vehicle_t)
        bt_confirm=findViewById(R.id.bt_confirm)
        bt_confirm.setOnClickListener(this)
        ed_brand=findViewById(R.id.ed_brand)
        ed_car_plate=findViewById(R.id.ed_car_plate)
        ed_model=findViewById(R.id.ed_model)
        ed_tonnage=findViewById(R.id.ed_tonnage)
    }
    override fun onClick(v: View?) {
        when(v?.id){
            R.id.bt_confirm->{
                userinput()
            }
        }
    }
    private fun userinput(){
        brand_t=ed_brand.text.toString()
        if(!brand_t.isNullOrEmpty()){
            car_plate=ed_car_plate.text.toString()
            if(!car_plate.isNullOrEmpty() && regex_plate.matches(car_plate)){
                model_t=ed_model.text.toString()
                if(!model_t.isNullOrEmpty()){
                    tonnage=ed_tonnage.text.toString().toFloat()
                    if (tonnage>.0){
                        fetchJSON()
                        SystemClock.sleep(500)
                        setResult(RESULT_OK, getIntent())
                        finish()
                    }
                    else Toast.makeText(this, "Please input the weight of the Car", Toast.LENGTH_SHORT).show()
                }
                else Toast.makeText(this, "Please input the model of the Car", Toast.LENGTH_SHORT).show()
            }
            else if(!regex_plate.matches(car_plate))  Toast.makeText(this, "Car Plate is illegal.", Toast.LENGTH_SHORT).show()
            else Toast.makeText(this, "Please input the plate of the Car", Toast.LENGTH_SHORT).show()
        }
        else Toast.makeText(this, "Please input the brand of the Car", Toast.LENGTH_SHORT).show()
    }
    private fun fetchJSON(){
        var fetchObject= JSONObject()
        fetchObject.put("post_t", 1)/////string
        fetchObject.put("car_plate", car_plate)/////string
        fetchObject.put("vehicle_t", vehicle_t)
        fetchObject.put("energy_t", energy_t)
        fetchObject.put("brand_t", brand_t)
        fetchObject.put("model_t", model_t)
        fetchObject.put("tonnage",tonnage)
        fetchObject.put("subjectID",message)
        fetchObject.put("timestamp", timestamp)
        resStr=""
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val body = fetchObject.toString().toRequestBody(mediaType)
        val request: Request = Request.Builder()
            .url(carinfo_url)
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
                Log.e(" CarInfo", "Succeed  ${resStr}")
                resStr=resStr.removePrefix("[")
                resStr=resStr.removeSuffix("]")
            }
        })
    }
    private fun vehicle_spin(){
        val vehiclelist = arrayListOf(
            "car","truck"
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            vehiclelist
        )
        sp_vehicle_t.adapter = adapter
        sp_vehicle_t.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.e("Spinner", "select $p2")
                vehicle_t = p2
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }
    private fun energy_spin(){
        val energylist = arrayListOf(
            "Gasoline","Diesel","Electric","Hybrid"
        )
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            energylist
        )
        sp_energy_t.adapter = adapter
        sp_energy_t.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                Log.e("Spinner", "select $p2")
                energy_t = p2
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {
            }
        }
    }
}