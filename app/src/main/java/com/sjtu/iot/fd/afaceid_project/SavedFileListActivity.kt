/**
 *@date 2018/11/22
 *@author dingfeng dingfengnju@gmail.com
 */
package com.sjtu.iot.fd.afaceid_project

import android.app.ListActivity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_list.*

class SavedFileListActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        val fileList = MainActivity.staticIOService!!.fileList()
        val adapter =
            object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, fileList) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val view = super.getView(position, convertView, parent)
                    val text1 = view.findViewById(android.R.id.text1) as TextView
                    val text2 = view.findViewById(android.R.id.text2) as TextView
                    text1.setText(position.toString())
                    text2.setText(fileList[position])
                    return view
                }
            }
        file_list_view.adapter = adapter
    }
}