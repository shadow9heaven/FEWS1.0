package com.example.kotlin_ota

import android.app.Activity
import android.content.Context
import android.os.Looper
import android.widget.Toast

class global {
    var context: Context? = null
    var BLEMan: BLEManager? = null
    fun init(activity: Activity?) {
        context = activity
        BLEMan = BLEManager(activity)
    }
    var DEBUG_LOG = ""
    companion object {
        val instance = global()

        fun Log(TAG: String, MSG: String) {
            android.util.Log.i(TAG, MSG)
            Log("$TAG: $MSG\n")
        }

        fun getR_String(id: Int): String {
            return instance.context!!.resources.getString(id)
        }

        fun DisplayToast(Text: String?) {
            try {
                if (Looper.getMainLooper().thread === Thread.currentThread()) {
                    Toast.makeText(instance.context, Text, Toast.LENGTH_LONG).show()
                } else {
                    android.util.Log.e(
                        "global",
                        String.format("Toast could not be displayed - not on UI Thread\n'%s'", Text)
                    )
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        fun Log(MSG: String) {
            instance.DEBUG_LOG += MSG
        }
    }
}