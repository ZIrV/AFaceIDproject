package com.sjtu.iot.fd.afaceid_project

interface WaveGenerator {
    fun getNext(): FloatArray
}