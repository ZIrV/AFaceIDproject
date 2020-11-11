package com.sjtu.iot.fd.afaceid_project

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView

class DebugInfoActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.debug_info_page)

        val message=intent.getStringExtra(DEBUG_MESSAGE1)
        val textView=findViewById<TextView>(R.id.textViewmicinfo).apply {
            text=message
        }

    }

}