/**
 *@date 2018/11/24
 *@author dingfeng dingfengnju@gmail.com
 */
package com.sjtu.iot.fd.afaceid_project

import android.app.Notification
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Message
import android.util.Log

class MyAudioTrack(waveGenerator: WaveGenerator,handler: Handler) : Thread() {

    private val waveGenerator: WaveGenerator = waveGenerator
    private val handler=handler
    private val audioFormat = AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
        .setSampleRate(ConfigInfo.sampleRateInHz)
        .build()
    private val audioAttributes = AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_MUSIC).build()
    private val audioTrack: AudioTrack = AudioTrack(
        audioAttributes,
        audioFormat,
        ConfigInfo.sampleRateInHz * 4 * 2,
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    )
    private var playing: Boolean = false

    fun stopPlay() {
        playing=false
        audioTrack.stop()
    }

    fun startPlay() {
        start()
    }

    override fun run() {
        playing=true
        audioTrack.play()
        Message.obtain(handler, 0, "start play").sendToTarget()
        while (playing) {
            val floatArray = waveGenerator.getNext()
            var result=audioTrack.write(floatArray, 0, floatArray.size, AudioTrack.WRITE_BLOCKING)
            Log.v(ConfigInfo.logTag,"result = "+result.toString())
            if(result<0)
            {
                break
            }
        }
        Message.obtain(handler, 0, "stop play").sendToTarget()
    }
}