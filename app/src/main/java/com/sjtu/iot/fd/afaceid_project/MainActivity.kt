/**
 *@date 2018/11/22
 *@author dingfeng dingfengnju@gmail.com
 */
package com.sjtu.iot.fd.afaceid_project

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.util.Log

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.view.View
import android.widget.*
import java.io.*
import java.net.Socket

private const val READ_REQUEST_CODE: Int = 42
const val DEBUG_MESSAGE1 = "com.sjtu.iot.fd.afaceid_project.debug1"

class MainActivity : AppCompatActivity() {
    private var dataRootDir: String? = null
    var ioService: IOService? = null
    val configInfo: ConfigInfo = ConfigInfo()
    var socket: Socket? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioRecord: AudioRecord? = null
    private val logTag: String = "afaceid_MainActivity"
    private var previousRemove: Long = SystemClock.elapsedRealtime()

    private var audioSourceFile:String?="sound"

    private val MY_WRITE_EXTERNAL_STORAGE=1;
    private val MY_READ_EXTERNAL_STORAGE=2;
    private val MY_INTERNET=4;
    private val MY_RECORD_AUDIO=3;


    var RES_PREFIX:String = ""

    var debuginfo_micinfo:String=""

    /*
    enum class MyPermissions{
        MY_READ_EXTERNAL_STORAGE,MY_WRITE_EXTERNAL_STORAGE
    }*/


    companion object {
        var staticIOService: IOService? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.v(logTag, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //设置文件存储根目录
        RES_PREFIX= "android.resource://$packageName/"
        //初始化界面资源
        val audioSpinner:Spinner=findViewById(R.id.audiosource_spinner)
        //数据采集根目录
        this.dataRootDir= Environment.getExternalStorageDirectory().absolutePath + "/AFaceIDproject"+ "/collectdata/"

        //this.dataRootDir = "/data/data/" + packageName + "/temp/"
        ioService = IOService(this.dataRootDir as String)
        staticIOService = ioService
        //读取app输入文本历史
        loadTexts()
        showTexts()


        //请求文件权限
        requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,MY_WRITE_EXTERNAL_STORAGE)
        requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,MY_READ_EXTERNAL_STORAGE)
        requestPermission(Manifest.permission.INTERNET,MY_INTERNET)
        //请求音频权限并初始化多媒体
        //warning:如果一开始没有允许权限，后面操作时就会因为没有初始化而闪退
        if (requestPermission(Manifest.permission.RECORD_AUDIO,MY_RECORD_AUDIO)==1) {
            setupVideoView()
        }

        /** 界面设置部分
         * 设置组件的基本外观，以及一些不涉及到主要逻辑的美化*/

        /** 按钮反馈函数设置
         * */
        try {
            show_list_button.setOnClickListener{
                val intent = Intent(this@MainActivity, SavedFileListActivity::class.java)
                startActivity(intent)
            }
            next_button.setOnClickListener{
                updateTexts()
                val dirName = configInfo.prefix + "/" + configInfo.medium
                ioService!!.mkdir(dirName)
                configInfo.count += 1
                showTexts()
                saveTexts()
            }

            start_button.setOnClickListener{
                //                Toast.makeText(this@MainActivity, "start_button", Toast.LENGTH_SHORT).show()
                try {
                    /*if (mediaPlayer!!.isPlaying) {
                        Toast.makeText(this@MainActivity, "playing", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }*/
//                    ConfigInfo.bufferSize = AudioRecord.getMinBufferSize(
//                        ConfigInfo.sampleRateInHz,
//                        ConfigInfo.channelConfig,
//                        ConfigInfo.audioFormat
//                    )
                    if (!mediaPlayer!!.isPlaying) {
                        updateTexts()
                        val filepath =
                            dataRootDir + "/" + configInfo.prefix + "/" + configInfo.medium + "/" + configInfo.count + ".pcm"
                        ioService!!.mkdir(configInfo.prefix + "/" + configInfo.medium + "/")
                        Thread {
                            //start media player after recorder starts
                            audioRecord!!.startRecording()
                            NetworkTask().execute()
                            writeData(filepath)
                        }.start()
                        Thread {
                            //                        Thread.sleep(500)
                            mediaPlayer!!.seekTo(0)
                            //show  message
                            mediaPlayer!!.start()
                            //print out  current device used
                            /*var deviceInfo: AudioDeviceInfo?=mediaPlayer.getSelectedTrack()
                            var deviceinfoString= "Now using device:"+deviceInfo.getProductName()+",samplerate:"+deviceInfo.getSampleRates()[0]
                            Log.v(logTag,"")*/
                        }.start()
                        Log.v(logTag, Date().time.toString())
                        chronometer.base = SystemClock.elapsedRealtime()
                        chronometer.start()
                    }
                    else{
                        audioRecord!!.stop()
                        mediaPlayer!!.pause()
                        chronometer.stop()
//                Toast.makeText(this,"stopping",Toast.LENGTH_SHORT).show()
                        next_button.callOnClick()
                    }
//                Toast.makeText(this,"playing",Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT).show()
                }
            }

            /*合并按钮之后这一块代码并不执行*/
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

            remove_button.setOnClickListener{
                var message: String?
                message = "success"
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
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, e.toString(), Toast.LENGTH_SHORT).show()
        }
        connect_button.setOnClickListener{
            updateTexts()
            connect()
        }
        send_button.setOnClickListener{
            //NetworkTask().execute()
        }
        debug_info_button.setOnClickListener{
            val intent=Intent(this,DebugInfoActivity::class.java).apply {
                putExtra(DEBUG_MESSAGE1,debuginfo_micinfo)
            }
            startActivity(intent)
        }


        /**spinner 下拉列表设置
         * 对应切换音源的功能*/
        val list:MutableList<String>
        list=listRaw()
        list.add(getString(R.string.more_files))
        val arrayAdapter:ArrayAdapter<String>?=ArrayAdapter(this,android.R.layout.simple_spinner_item,list)
        audioSpinner.adapter=arrayAdapter
        audioSpinner.setSelection(arrayAdapter!!.getPosition(audioSourceFile))
        audioSpinner.onItemSelectedListener= object:AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position==arrayAdapter.count-1){
                    //选择最后一个选项，表示从手机读取音频
                    performFileSearch("audio/*",this@MainActivity)
                    //see the result processing in on activityresult
                }
                else{
                    //选择app自带的音频
                    if (audioSourceFile==arrayAdapter.getItem(position)){
                        //选项已经被选中，不用切换
                        //warning:未处理的小bug，如果在播放时候换音频，应用会阻止这种行为，必须停下来再换。然后由于此处逻辑，停下后需要选择其它的音频再选回来。
                    }
                    else {
                        audioSourceFile = arrayAdapter.getItem(position)
                        //找到名字对应的资源文件的url
                        val sourceurl = Uri.parse(
                            RES_PREFIX + resources.getIdentifier(
                                audioSourceFile,
                                "raw",
                                packageName
                            )
                        )
                        Log.v(logTag, sourceurl.toString())
                        changeAudioSource(sourceurl)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }


    //封装的请求权限函数，permissionString为请求类型，requestCode为请求返回结果的代码，通过result函数分别处理不同的返回结果
    private fun requestPermission(permissionString:String, requestCode: Int):Int{
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
                return 1
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,arrayOf(permissionString),
                    requestCode)
                return 1
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        }
        else{
            println(permissionString+"got")
            return 1
        }

    }

    //请求权限结果，根据requestCode调用不同的函数
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted
                    Log.v(logTag,permissions[0]+"got")
                } else {
                    // permission denied
                    Log.v(logTag,permissions[0]+"denied")
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

    //处理弹出其它activity界面后返回的结果
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            resultData?.data?.also { uri ->
                Log.v(logTag, "audioUri: $uri")
                changeAudioSource(uri)
            }
        }
    }

    //连接数据传输服务器的函数
    private fun connect() {
        //println("77777777777777777777777777-7777777777777777777777777777")
        println("==connecting "+configInfo.ipAddress+":"+configInfo.port)
        Thread()
        {
            //updateTexts()
            //println("123123")
            try {
                socket = Socket(configInfo.ipAddress, configInfo.port) //通过socket连接服务器,参数ip为服务端ip地址，port为服务端监听端口
                println("==connecting "+configInfo.ipAddress+":"+configInfo.port)
                val pw = PrintWriter(socket!!.getOutputStream())
                val br = BufferedReader(InputStreamReader(socket!!.getInputStream()))

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
                //socket!!.close()
            } catch (e: Exception) {
                println(e)
                println("connect fault")
            }
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


    private fun loadTexts() {
        val sharedPref = this.getSharedPreferences(resources.getString(R.string.preference_file_key),Context.MODE_PRIVATE)
        this.configInfo.port = sharedPref.getInt(this.configInfo.portKey, this.configInfo.port)
        this.configInfo.ipAddress = sharedPref.getString(this.configInfo.ipAddressKey, this.configInfo.ipAddress)!!
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
        val sharedPref = this.getSharedPreferences(resources.getString(R.string.preference_file_key),Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
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
                //socket?.let { ioService!!.send_to(it) }
                val outputStream: OutputStream? = socket?.getOutputStream()
                val buffer: ByteBuffer = ByteBuffer.allocateDirect(ConfigInfo.bufferSize / 3)
                val byteArray: ByteArray = ByteArray(ConfigInfo.bufferSize / 3)
                var len: Int = 0

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
                            outputStream?.write(byteArray, 0, len)
                            //println(len)
                        }
                    } while (len > 0 || audioRecord!!.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                } finally {
                    outputStream?.flush()
                    outputStream?.close()


                }
                //show message
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
        //根据手机是否支持，设置录音未经处理的音源
        val audioManager=getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioManager.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED)!=null){
            ConfigInfo.audioSource=MediaRecorder.AudioSource.UNPROCESSED
            debuginfo_micinfo+="audio source: unprocessed"
        }
        else{
            ConfigInfo.audioSource=MediaRecorder.AudioSource.VOICE_RECOGNITION
            debuginfo_micinfo+="audio source: voice_recognition"
        }

        //初始化audiorecord类
            audioRecord = AudioRecord(
            ConfigInfo.audioSource,
            ConfigInfo.sampleRateInHz,
            ConfigInfo.channelConfig,
            ConfigInfo.audioFormat,
            ConfigInfo.bufferSize!!
        )

        //设置音源文件
        Log.v(logTag,resources.getIdentifier(audioSourceFile,"raw",packageName).toString())
        mediaPlayer = MediaPlayer.create(this, resources.getIdentifier(audioSourceFile,"raw",packageName))
        //mediaPlayer = MediaPlayer.create(this,R.raw.gsm44k_hamming)

        //sdk28以上可以查麦克风信息
        if (Build.VERSION.SDK_INT>=28){
            val activemics:List<MicrophoneInfo>?=audioRecord!!.activeMicrophones
            var micinfo:String=""
            for (activemic in activemics.orEmpty()){
                val micid=activemic.id
                val micchannels=activemic.channelMapping
                val micfrequency=activemic.frequencyResponse
                micinfo+="microphone id=$micid\n"+
                        "-channels:\n"
                for (channel in micchannels){
                    micinfo+="(${channel.first},${channel.first});"
                }
                micinfo+="\n-frequency response:\n"
                for (frequency in micfrequency) {
                    micinfo += "(${frequency.first},${frequency.second});"
                }
                micinfo+="\n"
            }
            debuginfo_micinfo+=micinfo
        }

        //进度条
        progressSeekBar.max = mediaPlayer!!.duration
        val thread = Thread(Runnable {
            while (true) {
                Thread.sleep(500)
                progressSeekBar.progress = mediaPlayer!!.currentPosition
            }
        })
        thread.start()
    }

    //change audiosource
    fun changeAudioSource(fileuri:Uri){
        if (mediaPlayer!!.isPlaying){
            //cannot change audiosource when playing
            Toast.makeText(this@MainActivity, "Please stop playing first", Toast.LENGTH_SHORT)
        }
        else{
            mediaPlayer!!.reset()
            mediaPlayer!!.setDataSource(this@MainActivity, fileuri)
            mediaPlayer!!.prepare()
            progressSeekBar.max = mediaPlayer!!.duration
            val audioSourceText:TextView=findViewById(R.id.audiosource_text)
            val filename:String=fileuri.toString()
            audioSourceText.text=filename.substringAfterLast('/')
        }
    }

    private val handler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
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
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(ConfigInfo.bufferSize / 3)
        val byteArray: ByteArray = ByteArray(ConfigInfo.bufferSize / 3)
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


    /*helper functions to sendmessage*/
    fun sendMessage(str: String) {
        Message.obtain(handler, 0, str).sendToTarget()
    }

    fun sendMessage(str: String, what: Int) {
        Message.obtain(handler, what, str).sendToTarget()
    }
}
