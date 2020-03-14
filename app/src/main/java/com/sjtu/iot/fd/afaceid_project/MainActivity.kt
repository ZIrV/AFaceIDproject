/**
 *@date 2018/11/22
 *@author dingfeng dingfengnju@gmail.com
 */
package com.sjtu.iot.fd.afaceid_project

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import java.lang.Exception
import java.nio.ByteBuffer
import java.text.MessageFormat
import java.util.*
import android.media.AudioManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.widget.EditText
import java.io.*
import java.net.Socket


class MainActivity : AppCompatActivity() {
    var dataRootDir: String? = null
    var ioService: IOService? = null
    val configInfo: ConfigInfo = ConfigInfo()
    private var mediaPlayer: MediaPlayer? = null
    private var audioRecord: AudioRecord? = null
    private val logTag: String = "MainActivitydddddddddd"
    private var previousRemove: Long = SystemClock.elapsedRealtime()
    private var isSeeking = false

    private val MY_WRITE_EXTERNAL_STORAGE=1;
    private val MY_READ_EXTERNAL_STORAGE=2;
    private val MY_INTERNET=4;
    private val MY_RECORD_AUDIO=3;

    private val MY_DEFAULT_REQCODE=100;


    enum class MyPermissions{
        MY_READ_EXTERNAL_STORAGE,MY_WRITE_EXTERNAL_STORAGE
    }

    companion object {
        var staticIOService: IOService? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(logTag, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //请求文件权限
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,MY_WRITE_EXTERNAL_STORAGE);
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,MY_READ_EXTERNAL_STORAGE);
        requestPermission(Manifest.permission.INTERNET,MY_INTERNET);
        //初始化多媒体
        //warning:如果一开始没有允许权限，后面操作时就会因为没有初始化而闪退
        if (requestPermission(Manifest.permission.RECORD_AUDIO,MY_RECORD_AUDIO)==1) {
            setupVideoView();
        }


        try {
            this.dataRootDir = "/data/data/" + packageName + "/temp/"
            ioService = IOService(this.dataRootDir as String)
            staticIOService = ioService
            loadTexts()
            showTexts()

            show_list_button.setOnClickListener{
                var intent = Intent(this@MainActivity, SavedFileListActivity::class.java)
                startActivity(intent)
            }


            next_button.setOnClickListener{
                updateTexts()
                var dirName = configInfo.prefix + "/" + configInfo.medium
                ioService!!.mkdir(dirName)
                configInfo.count += 1
                showTexts()
                saveTexts()
            }

            start_button.setOnClickListener{
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
                    updateTexts()
                    val filepath =
                        dataRootDir + "/" + configInfo.prefix + "/" + configInfo.medium + "/" + configInfo.count + ".pcm"
                    ioService!!.mkdir(configInfo.prefix + "/" + configInfo.medium + "/")
                    Thread{
                        //start media player after recorder starts
                        audioRecord!!.startRecording()
                        writeData(filepath)
                    }.start()
                    Thread{
                        //                        Thread.sleep(500)
                        mediaPlayer!!.seekTo(0)
                        //show  message
                        mediaPlayer!!.start()
                    }.start()
                    Log.v(logTag, Date().time.toString())
                    chronometer.base = SystemClock.elapsedRealtime()
                    chronometer.start()
//                Toast.makeText(this,"playing",Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT)
                }
            }

            stop_button.setOnClickListener{
                if (!mediaPlayer!!.isPlaying) {
                    Toast.makeText(this, "stopping", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                audioRecord!!.stop()
                mediaPlayer!!.pause()
                chronometer.stop()
//                Toast.makeText(this,"stopping",Toast.LENGTH_SHORT).show()
                next_button.callOnClick()
            }

            send_button.setOnClickListener{
                NetworkTask().execute()
            }

            remove_button.setOnClickListener{
                var message: String?
                val timeDiff= SystemClock.elapsedRealtime()-this.previousRemove
                if (timeDiff > 700) {
                    this.previousRemove=SystemClock.elapsedRealtime()
                    return@setOnClickListener
                }
                this.previousRemove=SystemClock.elapsedRealtime()
                try {
                    this.ioService!!.removeAll()
                } catch (e: Exception) {
                    message = e.toString()
                }
                message = "success"
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }


            connect_button.setOnClickListener{
                connect()
            }
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT).show()
        }
    }


    //封装的请求权限函数
    fun requestPermission(permissionString:String, requestCode: Int):Int{
        if (ContextCompat.checkSelfPermission(this, permissionString)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permissionString)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                ActivityCompat.requestPermissions(this,arrayOf(permissionString),
                    requestCode)
                return 1;
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,arrayOf(permissionString),
                    requestCode)
                return 1;
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }
        else{
            println(permissionString+"got");
            return 1;
        }

    }

    //请求权限结果
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    println(permissions[0]+"got");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }




    fun connect() {
        Thread()
        {
            //val ipText=findViewById<EditText>(R.id.ip_address_edit_text).text.toString();
            //val portText=findViewById<EditText>(R.id.ip_port_edit_text).text.toString();
            //val portNum=portText.toInt();
            //var socket=Socket(ipText,portNum);
            var socket = Socket(configInfo.ipAddress, configInfo.port)
            println("==connecting "+configInfo.ipAddress+":"+configInfo.port);
            val pw = PrintWriter(socket.getOutputStream())
            val br = BufferedReader(InputStreamReader(socket.getInputStream()))
            while (true) {
                val content = br.readLine()
                if (content == null) {
                    break
                }
                if (content.equals("info")) {
                    pw.println("prefix = ${configInfo.prefix} media = ${configInfo.medium} count= ${configInfo.count}")
                }
                sendMessage(content, 1)
                sendMessage("received ${content}")
                pw.println("ok!")
                pw.flush()
            }
            socket.close()

        }.start()
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
        val musicVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        previousMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        Log.v(logTag, "max volume " + musicVolume + " currentMusic Volume " + previousMusicVolume)
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 10, 0)
    }

    var previousMusicVolume: Int? = null
    override fun onPause() {
        super.onPause()
        val mAudioManager = applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, previousMusicVolume!!, 0)
    }


    fun loadTexts() {
        val sharedPref = this.getSharedPreferences(resources.getString(R.string.preference_file_key),Context.MODE_PRIVATE);
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
        val sharedPref = this.getSharedPreferences(resources.getString(R.string.preference_file_key),Context.MODE_PRIVATE);
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

    //初始化audiorecord
    fun setupVideoView() {
//        video_view.setVideoURI())
            audioRecord = AudioRecord(
            ConfigInfo.audioSource,
            ConfigInfo.sampleRateInHz,
            ConfigInfo.channelConfig,
            ConfigInfo.audioFormat,
            ConfigInfo.bufferSize!!
        )
        mediaPlayer = MediaPlayer.create(this, R.raw.sound2)
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
            } else if (msg?.what == 1) {
                handleOperation(msg?.obj.toString())
            }
        }
    }

    private fun handleOperation(content: String) {
        when (content) {
            "start" -> start_button.callOnClick()
            "stop" -> stop_button.callOnClick()
            "remove" -> remove_button.callOnClick()
            "next" -> next_button.callOnClick()
            "send" -> send_button.callOnClick()
        }
    }

    fun writeData(filepath: String) {
        val file: File = File(filepath)
        val outputStream: OutputStream = file.outputStream()
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(ConfigInfo.bufferSize!! / 3)
        val byteArray: ByteArray = ByteArray(ConfigInfo.bufferSize!! / 3)
        var len: Int = 0


        sendMessage("start record")
        var previousTime = SystemClock.elapsedRealtimeNanos()
        try {
            while (audioRecord!!.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            }
            do {
                len = audioRecord!!.read(buffer, buffer.capacity())
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

    fun sendMessage(str: String, what: Int) {
        Message.obtain(handler, what, str).sendToTarget()
    }
}
