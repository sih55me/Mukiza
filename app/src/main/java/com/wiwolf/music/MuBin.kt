package com.wiwolf.music

import android.os.Binder
import com.wiwolf.music.data.MuService

class MuBin(private val service: MuService) : Binder() {

    val getService: MuService

        get() = service

}

