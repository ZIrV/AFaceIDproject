/**
 *@date 2018/11/22
 *@author dingfeng dingfengnju@gmail.com
 */
package com.sjtu.iot.fd.afaceid_project

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileReader
import java.io.PrintWriter
import java.net.Socket
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.experimental.suspendCoroutine

class IOService(rootDir: String) {
    val rootDir: String = rootDir

    init {
        var rootDirFile = File(rootDir)
        if (!rootDirFile.exists()) {
            rootDirFile.mkdir()
        }
    }

    fun createDir(dir: String) {
        val dirPath = rootDir + "/" + dir
        val file = File(dirPath)
        if (!file.exists()) {
            file.mkdir()
        }
    }

    fun removeAll() {
        deleteDir(File(rootDir))
        createDir("")
    }

    fun allFiles(): Array<String> {
        var dirFile = File(rootDir)
        return dirFile.list()
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory()) {
            val children = dir.list()
            for (child in children) {
                val success = deleteDir(File(dir, child))
                if (!success) {
                    return false
                }
            }
        }
        return dir.delete()
    }

    fun send(ipAddress: String, port: Int) {

//        var byteOutputStream = ByteArrayOutputStream()
        var socket = Socket(ipAddress, port)
        ZipUtils.toZip(rootDir, socket.getOutputStream(), true)
//        var byteArray = byteOutputStream.toByteArray()
//        var socket = Socket(ipAddress, port)
//        socket.getOutputStream().write(byteArray)
        socket.close()
    }

    fun mkdir(dirName: String) {
        var file = File(rootDir + '/' + dirName)
        if (!file.exists()) {
            file.mkdirs()
        }
    }

    fun write(filepath: String, content: String) {
        var filepath = rootDir + "/" + filepath
        val file = File(filepath).parentFile
        if (!file.exists()) {
            file.mkdirs()
        }
        var pw: PrintWriter? = null
        try {
            pw = PrintWriter(filepath)
            pw.write(content)
        } finally {
            pw?.close()
        }

    }

    fun selector(f: File): Long = -f.lastModified()

    fun fileList(): ArrayList<String> {
        val fileList: ArrayList<File> = ArrayList()
        val queue: Queue<File> = LinkedList<File>()
        queue.add(File(rootDir))
        while (!queue.isEmpty()) {
            val file = queue.poll()
            for (fileTmp in file.listFiles()) {
                if (fileTmp.isDirectory()) {
                    queue.offer(fileTmp)
                } else {
                    fileList.add(fileTmp)
                }
            }
        }
        val result: ArrayList<String> = ArrayList()
        fileList.sortBy({ selector(it) })
        val simpleDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        for (file in fileList) {
            val date = Date(file.lastModified())
            val dateStr = simpleDateFormatter.format(date)
            val fileAbsolutePath = file.absolutePath
            result.add(dateStr + "   " + fileAbsolutePath.substringAfter("temp/"))
        }
        return result
    }

    fun getContent(filepath: String): String {
        val filepath = rootDir + "/" + filepath
        val file = File(filepath)
        if (file.exists() && file.isFile()) {
            return file.readText()
        }
        return ""
    }

}