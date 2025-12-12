package com.wiwolf.music.data

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.IBinder
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import com.wiwolf.music.MuBin
import kotlin.random.Random

class MuService: Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    lateinit var mediaPlayer: MediaPlayer

    private lateinit var songs: MutableList<Mukis>

    private var songPosition = 0


    private val musicBinder: IBinder = MuBin(this)

    override fun onBind(intent: Intent?): IBinder = musicBinder

    override fun onUnbind(intent: Intent?): Boolean {
        mediaPlayer.stop()

        mediaPlayer.release()

        return false

    }

    override fun onCreate() {

        super.onCreate()

        songPosition = 0

        mediaPlayer = MediaPlayer()

        initMusicPlayer()

        val random = Random.Default

    }
    private fun initMusicPlayer() {



        mediaPlayer.setWakeMode(

            applicationContext,

            PowerManager.PARTIAL_WAKE_LOCK)

        val audioAttributes = AudioAttributes.Builder()

            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)

            .setUsage(AudioAttributes.USAGE_MEDIA)

            .build()

        mediaPlayer.setAudioAttributes(audioAttributes)

        mediaPlayer.setOnPreparedListener(this)

        mediaPlayer.setOnCompletionListener(this)

        mediaPlayer.setOnErrorListener(this)

    }

    fun setList(theSongs: MutableList<Mukis>) {

        songs = theSongs

    }



    override fun onPrepared(mp: MediaPlayer?) {
        mp?.start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return false
    }

    override fun onCompletion(mp: MediaPlayer?) {

    }


    fun playSong() {



        mediaPlayer.reset()

        val playSong = songs[songPosition]

        val currentSongId: Long = playSong.id

        val trackUri = ContentUris.withAppendedId(

            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,

            currentSongId

        )

        try {

            mediaPlayer.setDataSource(applicationContext, trackUri)

        } catch (e: Exception) {

            Log.e("MUSIC SERVICE", "Error setting data source", e)

        }

        mediaPlayer.prepare()

    }

    fun setSong(songIndex: Int) {

        songPosition = songIndex

    }


}