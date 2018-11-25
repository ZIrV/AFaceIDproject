/**
 *@date 2018/11/22
 *@author dingfeng dingfengnju@gmail.com
 */
package com.sjtu.iot.fd.afaceid_project

import android.content.Context
import android.content.Intent
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.*
import android.support.v7.app.AppCompatActivity;
import android.text.Editable
import android.util.Log
import android.widget.MediaController
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.OutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.MessageFormat
import java.util.*
import android.media.AudioManager



class MainActivity : AppCompatActivity() {
    var dataRootDir: String? = null
    var ioService: IOService? = null
    val configInfo: ConfigInfo = ConfigInfo()
    var mediaPlayer: MediaPlayer? = null
    var audioRecord: AudioRecord? = null
    val logTag: String = "MainActivitydddddddddd"
    private var isSeeking = false


    companion object {
        var staticIOService: IOService? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(logTag, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        try {
            this.dataRootDir = "/data/data/" + packageName + "/temp/"
            ioService = IOService(this.dataRootDir as String)
            staticIOService = ioService
            loadTexts()
            showTexts()

            show_list_button.setOnClickListener({
                var intent = Intent(this@MainActivity, SavedFileListActivity::class.java)
                startActivity(intent)
            }
            )

            next_button.setOnClickListener({
                updateTexts()
                var dirName = configInfo.prefix + "/" + configInfo.medium
                ioService!!.mkdir(dirName)
                configInfo.count += 1
                showTexts()
                saveTexts()
            })

            start_button.setOnClickListener({
                //                Toast.makeText(this@MainActivity, "start_button", Toast.LENGTH_SHORT).show()
                try {
                    if (mediaPlayer!!.isPlaying) {
                        Toast.makeText(this@MainActivity, "playing", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
//                    ConfigInfo.bufferSize = AudioRecord.getMinBufferSize(
//                        ConfigInfo.sampleRateInHz,
//                        ConfigInfo.channelConfig,
//                        ConfigInfo.audioFormat
//                    )
                    audioRecord = AudioRecord(
                        ConfigInfo.audioSource,
                        ConfigInfo.sampleRateInHz,
                        ConfigInfo.channelConfig,
                        ConfigInfo.audioFormat,
                        ConfigInfo.bufferSize!!
                    )
                    updateTexts()
                    val filepath =
                        dataRootDir + "/" + configInfo.prefix + "/" + configInfo.medium + "/" + configInfo.count + ".pcm"
                    ioService!!.mkdir(configInfo.prefix + "/" + configInfo.medium + "/")
                    Thread({
                        audioRecord!!.startRecording()
                        writeData(filepath)
                    }).start()
                    Log.v(logTag, Date().time.toString())
                    chronometer.base = SystemClock.elapsedRealtime()
                    chronometer.start()
//                Toast.makeText(this,"playing",Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT)
                }
            })

            stop_button.setOnClickListener({
                if (!mediaPlayer!!.isPlaying) {
                    Toast.makeText(this, "stopping", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                audioRecord!!.stop()
                mediaPlayer!!.pause()
                chronometer.stop()
//                Toast.makeText(this,"stopping",Toast.LENGTH_SHORT).show()

            })

            send_button.setOnClickListener({
                NetworkTask().execute()
            })

            remove_button.setOnClickListener({
                var message: String?
                try {
                    this.ioService!!.removeAll()
                } catch (e: Exception) {
                    message = e.toString()
                }
                message = "success"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            })
            setupVideoView()
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        updateTexts()
        saveTexts()
        mediaPlayer?.release()
        audioRecord?.release()
    }



    override fun onResume() {
        super.onResume()
        val mAudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, , 0)
        val musicVolume=mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        previousMusicVolume=mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.v(logTag,"max volume "+musicVolume+" currentMusic Volume "+previousMusicVolume)
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 3, 0)
    }

    var previousMusicVolume:Int?=null
    override fun onPause() {
        super.onPause()
        val mAudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
//        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, , 0)
        val musicVolume=mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousMusicVolume!!, 0)

    }


    fun loadTexts() {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        this.configInfo.port = sharedPref.getInt(this.configInfo.portKey, this.configInfo.port)
        this.configInfo.ipAddress = sharedPref.getString(this.configInfo.ipAddressKey, this.configInfo.ipAddress)
        this.configInfo.count = sharedPref.getInt(this.configInfo.countKey, this.configInfo.count)
        this.configInfo.medium = sharedPref.getString(this.configInfo.mediumKey, this.configInfo.medium)
        this.configInfo.prefix = sharedPref.getString(this.configInfo.prefixKey, this.configInfo.prefix)
        this.configInfo.description =
                ioService!!.getContent(configInfo.prefix + "/" + configInfo.medium + "/description.txt")
    }

    fun showTexts() {
        deleteAll(filename_prefix_edit_text.text)
        deleteAll(filename_medium_edit_text.text)
        deleteAll(filename_count_edit_text.text)
        deleteAll(ip_address_edit_text.text)
        deleteAll(description_edit_text.text)
        deleteAll(ip_port_edit_text.text)
        filename_prefix_edit_text.text.insert(0, this.configInfo.prefix)
        filename_medium_edit_text.text.insert(0, this.configInfo.medium)
        filename_count_edit_text.text.insert(0, this.configInfo.count.toString())
        ip_address_edit_text.text.insert(0, this.configInfo.ipAddress)
        description_edit_text.text.insert(0, this.configInfo.description)
        ip_port_edit_text.text.insert(0, this.configInfo.port.toString())
    }

    fun deleteAll(text: Editable) {
        text.delete(0, text.length)
    }

    fun updateTexts() {
        this.configInfo.prefix = filename_prefix_edit_text.text.toString()
        this.configInfo.medium = filename_medium_edit_text.text.toString()
        this.configInfo.count = filename_count_edit_text.text.toString().toInt()
        this.configInfo.ipAddress = ip_address_edit_text.text.toString()
        this.configInfo.description = description_edit_text.text.toString()
        this.configInfo.port = ip_port_edit_text.text.toString().toInt()
    }

    fun saveTexts() {
        val sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        var editor = sharedPref.edit()
        editor.putString(this.configInfo.prefixKey, this.configInfo.prefix)
        editor.putString(this.configInfo.mediumKey, this.configInfo.medium)
        editor.putInt(this.configInfo.countKey, this.configInfo.count)
        editor.putString(this.configInfo.ipAddressKey, this.configInfo.ipAddress)
        editor.putInt(this.configInfo.portKey, this.configInfo.port)
        editor.commit()
        this.ioService!!.write(
            this.configInfo.prefix + "/" + this.configInfo.medium + "/description.txt",
            this.configInfo.description!!
        )
    }

    inner class NetworkTask : AsyncTask<Void, String, String>() {
        override fun onPreExecute() {
            super.onPreExecute()
        }

        override fun doInBackground(vararg p0: Void?): String {
            try {
                ioService!!.send(configInfo.ipAddress, configInfo.port)
            } catch (e: Exception) {
                return "connection error %s".format(e.toString())
            }
            return "succeed to send"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Toast.makeText(this@MainActivity, result, Toast.LENGTH_SHORT).show()
        }
    }

    fun setupVideoView() {
//        video_view.setVideoURI())
        mediaPlayer = MediaPlayer.create(this, R.raw.sound)
        progressSeekBar.max = mediaPlayer!!.duration
        val thread = Thread(Runnable {
            while (true) {
                Thread.sleep(500)
                progressSeekBar.progress = mediaPlayer!!.currentPosition
            }
        })
        thread.start()
    }


    private var handler: Handler = object : Handler() {
        override fun handleMessage(msg: Message?) {
            super.handleMessage(msg)
            if (msg?.what == 0) {
                Toast.makeText(this@MainActivity, msg?.obj.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    fun writeData(filepath: String) {
        val file: File = File(filepath)
        val outputStream: OutputStream = file.outputStream()
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(ConfigInfo.bufferSize!! / 3)
        val byteArray: ByteArray = ByteArray(ConfigInfo.bufferSize!! / 3)
        var len: Int = 0

        //start media player after recorder starts
        mediaPlayer!!.seekTo(0)
        //show  message
        mediaPlayer!!.start()
        sendMessage("start record")
        var previousTime=SystemClock.elapsedRealtimeNanos()
        try {
            while (audioRecord!!.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            }
            do {
                len = audioRecord!!.read(buffer, buffer.capacity())
                var currentTime=SystemClock.elapsedRealtimeNanos()
                Log.v(logTag,"read interval "+(currentTime-previousTime).toString())
                previousTime=currentTime
                buffer.rewind()
                if (len > 0) {
                    buffer.get(byteArray, 0, len)
                    buffer.clear()
                    outputStream.write(byteArray, 0, len)
                }
            } while (len > 0 || audioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING)
        } finally {
            outputStream.flush()
            outputStream.close()
        }
        //show message
        sendMessage("stop record")
    }

    fun sendMessage(str: String) {
        Message.obtain(handler, 0, str).sendToTarget()
    }
}
