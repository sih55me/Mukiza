package com.wiwolf.music

import android.os.Binder

class MuBin(private val service: MuService) : Binder() {

    val getService: MuService

        get() = service

}

