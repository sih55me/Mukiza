package com.wiwolf.music

import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewAnimator
import java.lang.String
import java.util.concurrent.TimeUnit
import kotlin.Boolean
import kotlin.Exception
import kotlin.Int
import kotlin.Long
import kotlin.Throwable
import kotlin.apply
import kotlin.getValue
import kotlin.lazy
import kotlin.let

class MuService: Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener {

    lateinit var mediaPlayer: MediaPlayer

    val songs: MutableList<ContentValues> = mutableListOf()
    val mHandler: Handler = Handler(Looper.getMainLooper())

    var isPlayerReady = false



    var runningActivity: Activity? = null
        set(value) {
            if(value!=null){

            }
        }

    var songPosition = -1



    private val musicBinder: IBinder = MuBin(this)






    val med by lazy {
        MediaSession(this, "Music").apply {
            isActive = true
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)

        }
    }


    override fun onBind(intent: Intent?): IBinder = musicBinder

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return true
    }

    override fun onDestroy() {

        super.onDestroy()
        mediaPlayer.stop()
        isPlayerReady = false
        mediaPlayer.release()

    }

    override fun onCreate() {
        super.onCreate()
        songPosition = -1

        mediaPlayer = MediaPlayer()

        initMusicPlayer()
    }


    val currentItem get() = songs[songPosition]




    fun setList(theSongs: MutableList<ContentValues>) {
        songs.clear()
        songs.addAll(theSongs)

    }



    override fun onPrepared(mp: MediaPlayer?) {
        isPlayerReady = true
        mediaPlayer.playbackParams = PlaybackParams().allowDefaults().setPitch(1F).setSpeed(1F)
        mp?.start()
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        var reson = "kill"
        if(what == MediaPlayer.MEDIA_ERROR_SERVER_DIED){
            var ext = "dunno~~"
            if(extra == MediaPlayer.MEDIA_ERROR_IO){
                ext = "File issue"
            }else if(extra == MediaPlayer.MEDIA_ERROR_MALFORMED){
                ext = "Spec issue"
            }
            else if(extra == MediaPlayer.MEDIA_ERROR_MALFORMED){
                ext = "Spec issue"
            }
            else if(extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT){
                ext = "Delay issue"
            }
            Toast.makeText(this, "what make $reson\n$ext", Toast.LENGTH_LONG).show()
        }


        return true
    }

    override fun onCompletion(mp: MediaPlayer?) {

    }

    fun playSong() = Thread{
        mediaPlayer.stop()
        mediaPlayer.reset()

        val playSong = songs[songPosition]

        val currentSongId: Long = playSong.getAsLong(MediaStore.Audio.Media._ID)

        val trackUri = ContentUris.withAppendedId(

            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,

            currentSongId

        )

        try {

            mediaPlayer.setDataSource(applicationContext, trackUri)

        } catch (e: Exception) {

            Log.e("MUSIC SERVICE", "Error setting data source", e)

        }



        try {

            mediaPlayer.prepare()

        } catch (e: Exception) {

            Log.e("MUSIC SERVICE", "Error setting player", e)

        }


    }.start()


    var isShow = false




    fun setSong(songIndex: Int) {

        songPosition = songIndex

    }

    //PRIVATE VOID












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


}