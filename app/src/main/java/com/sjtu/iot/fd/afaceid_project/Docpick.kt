package com.sjtu.iot.fd.afaceid_project
import android.content.Intent
import androidx.core.app.ActivityCompat.startActivityForResult
import android.app.Activity


private const val READ_REQUEST_CODE: Int = 42


fun performFileSearch(fileType:String, thisActivity: Activity) {

    // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
    // browser.
    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        addCategory(Intent.CATEGORY_OPENABLE)
        type = fileType
    }
    startActivityForResult(thisActivity,intent, READ_REQUEST_CODE,null)
}


fun listRaw(): MutableList<String> {
    val list:MutableList<String> = ArrayList()
    val fields = R.raw::class.java.fields
    for (count in fields.indices) {
        list.add(fields[count].name)
    }
    return list
}

