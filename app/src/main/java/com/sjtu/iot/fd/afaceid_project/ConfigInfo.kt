/**
 *@date 2018/11/22
 *@author dingfeng dingfengnju@gmail.com
 */
package com.sjtu.iot.fd.afaceid_project

import android.media.AudioFormat
import android.media.MediaRecorder

class ConfigInfo {


    companion object {
        const val audioSource: Int = MediaRecorder.AudioSource.MIC
        const val sampleRateInHz: Int = 48000
        const val channelConfig = AudioFormat.CHANNEL_IN_MONO
        const val audioFormat = AudioFormat.ENCODING_PCM_FLOAT
         var bufferSize: Int = 48000*2*2
    }

    var prefixKey: String = "prefix"
    var prefix: String = "distance"
    var mediumKey: String = "medium"
    var medium: String = "name"
    var countKey = "count"
    var count: Int = 1
    var ipAddressKey = "ipAddress"
    var ipAddress: String = "192.168.11.104"
    var portKey = "port"
    var port: Int = 9999
    var descriptionKey = "description"
    var description: String? = ""

    constructor(prefix: String, medium: String, count: Int, ipAddress: String, port: Int, description: String) {
        this.prefix = prefix
        this.medium = medium
        this.count = count
        this.ipAddress = ipAddress
        this.port = port
        this.description = description
    }

    constructor()
}