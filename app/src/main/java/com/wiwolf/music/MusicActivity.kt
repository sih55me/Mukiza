package com.wiwolf.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import android.widget.Toast
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.single.PermissionListener
import com.wiwolf.music.data.MuService
import com.wiwolf.music.data.Mukis
import java.io.File
import java.util.concurrent.TimeUnit


class MusicActivity : Activity() {
    private lateinit var listview : ListView
    private var items = mutableListOf<Mukis>()
    private val array by lazy{ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf<String>())}

    private var musicService: MuService? = null

    private var playIntent: Intent? = null

    private var musicBound = false

    private var paused: Boolean = false

    private var playbackPaused: Boolean = false

    private var select = -1

    private var changeIcon : (Int) -> Unit = {}

    private var showPlay : (Boolean) -> Unit = {}

    val meca = object : MediaSession.Callback() {
        val m get() = musicService?.mediaPlayer
        override fun onPlay() {
            m?.start()
        }

        override fun onPause() {
            m?.pause()
        }

        override fun onStop() {
            m?.stop()
        }

        override fun onSeekTo(pos: Long) {
            m?.seekTo(pos.toInt())
        }
    }

    private val med by lazy {
        MediaSession(this, "Music").apply {
            isActive = true
            setCallback(meca)
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)

        }
    }




    override fun onStart() {

        super.onStart()

        if (playIntent == null) {

            playIntent = Intent(this, MuService::class.java)

            bindService(playIntent!!, musicConnection, BIND_AUTO_CREATE)

            startService(playIntent)



        }

    }


    private val musicConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {

            val binder = service as MuBin



            musicService = binder.getService

            musicService!!.setList(items)

            musicService!!.mediaPlayer.setOnTimedTextListener { mp, text ->
                Toast.makeText(this@MusicActivity, text.text, Toast.LENGTH_SHORT).show()
            }

            musicBound = true

        }

        override fun onServiceDisconnected(name: ComponentName) {

            musicBound = false

        }

    }





    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)
        actionBar?.apply{
            elevation = 0F
        }



        val play = findViewById<ImageButton>(R.id.play)
        play.setOnClickListener {
            if(musicService!!.mediaPlayer.isPlaying){
                musicService!!.mediaPlayer.pause()
            }else{
                musicService!!.mediaPlayer.start()
            }
        }
        val seekbar = findViewById<SeekBar>(R.id.slider)
        findViewById<View>(R.id.close).setOnClickListener {
            finishAndRemoveTask()
        }
        listview = findViewById(R.id.list)
        listview.adapter = array
        val peli = object : PermissionListener{
            override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                showSong()
            }

            override fun onPermissionDenied(p0: PermissionDeniedResponse?) {

            }

            override fun onPermissionRationaleShouldBeShown(
                p0: PermissionRequest?,
                p1: PermissionToken?,
            ) {
                p1?.continuePermissionRequest()
            }

        }
        val mpl = object : MultiplePermissionsListener {
            override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {
                showSong()
            }

            override fun onPermissionRationaleShouldBeShown(
                p0: MutableList<PermissionRequest>?,
                p1: PermissionToken?,
            ) {
                p1?.continuePermissionRequest()
            }

        }
        Dexter.withContext(this).withPermissions(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_EXTERNAL_STORAGE).withListener(mpl).check()




        listview.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            musicService?.setSong(position)
            select = position
            musicService?.playSong()

            findViewById<TextView>(R.id.name).setText(items[select].name)
        }



        val mHandler: Handler = Handler()

//Make sure you update Seekbar on UI thread
        runOnUiThread(object : Runnable {
            @SuppressLint("DefaultLocale")
            override fun run() {
                if (musicService != null) {
                    if(select != -1) {
                        try{
                            med.controller
                            val mCurrentPosition: Int =
                                musicService!!.mediaPlayer.currentPosition / 1000
                            val max: Int = musicService!!.mediaPlayer.duration / 1000
                            seekbar.setProgress(mCurrentPosition)
                            seekbar.max = max
                            findViewById<TextView>(R.id.name).text = items[select].name
                            findViewById<TextView>(R.id.dur).text = SpannableStringBuilder(
                                "${time(musicService!!.mediaPlayer.currentPosition)} / ${
                                    time(musicService!!.mediaPlayer.duration)
                                }"
                            )
                            if (!musicService!!.mediaPlayer.isPlaying) {
                                play.setImageResource(android.R.drawable.ic_media_play)
                            } else {
                                play.setImageResource(android.R.drawable.ic_media_pause)
                            }
                            med.setPlaybackState(getPlayBackState())
                            med.setMetadata(
                                MediaMetadata.Builder()
                                    .putLong(MediaMetadata.METADATA_KEY_DURATION, max.toLong() * 1000)
                                    .build()
                            )
                            showNotif(mCurrentPosition, max)
                        }catch (e: Throwable){

                        }
                    }
                }
                mHandler.post(this)
            }

            private fun getPlayBackState(): PlaybackState? {
                return PlaybackState.Builder()
                    .setState(
                        if (musicService?.mediaPlayer?.isPlaying == true) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                        (musicService?.mediaPlayer?.currentPosition?.toLong() ?: 0L), 0F
                    )
                    .setActions(PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_PLAY_PAUSE)
                    .build()
            }

            private fun showNotif(mCurrentPosition: Int, max: Int) {
                val b = Notification.Builder(this@MusicActivity, "c").apply {
                    setSmallIcon(android.R.drawable.ic_media_play)
                    setContentTitle(items[select].name)
                    setContentText(time(musicService!!.mediaPlayer.currentPosition))
                    setPriority(Notification.PRIORITY_LOW)
                    setProgress(max, mCurrentPosition, false)
                    setOngoing(true)
                    setStyle(Notification.MediaStyle().setShowActionsInCompactView(0,1,2,3).setMediaSession(med.sessionToken))
                    setOnlyAlertOnce(true)
                    setCategory(Notification.CATEGORY_SERVICE)
                }.build()
                val n = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                n.createNotificationChannel(NotificationChannel("c", "Music", NotificationManager.IMPORTANCE_MIN))
                n.notify(1, b)
            }
        })

        seekbar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (musicService != null && fromUser && (select != -1)) {
                    musicService!!.mediaPlayer.seekTo(progress * 1000)
                }
            }
        })


    }



    private fun showSong() {
        getSongList()
        items.sortWith { a, b -> a.name.compareTo(b.name) }
        if(!array.isEmpty){
            array.clear()
        }
        array.addAll(items.map { it.name })
    }

    private fun showSongList() {
        val list = findSongList(Environment.getExternalStorageDirectory())
        items = MutableList<Mukis>(list.size){Mukis(it.toLong(), "", "")}
        for (i in 0 until list.size) {
            items[i].name = list[i].name.replace(".mp3", "").replace(".wav", "")
        }
        items.sortWith { a, b -> a.name.compareTo(b.name) }
        if(!array.isEmpty){
            array.clear()
        }
        array.addAll(items.map { it.name })
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        select = savedInstanceState.getInt("select", -1)
        super.onRestoreInstanceState(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("select", select)
        super.onSaveInstanceState(outState)
    }

    //for newest
    private fun getSongList() {

        val musicResolver = contentResolver

        val musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val selection = "${MediaStore.Audio.Media.DATA} LIKE ? AND ${MediaStore.Audio.Media.DURATION} >= ?"

        val selectionArgs = arrayOf("%/Music/%", "15000")

        val musicCursor = musicResolver.query(musicUri, null, selection, selectionArgs, null)

        if ((musicCursor != null) && musicCursor.moveToFirst()) {

            val titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)

            val idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID)

            val durationColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.DURATION)

            do {

                val thisId = musicCursor.getLong(idColumn)

                val thisTitle = musicCursor.getString(titleColumn)

                val thisDuration = musicCursor.getString(durationColumn)

                items.add(Mukis(thisId, thisTitle, thisDuration))



            } while (musicCursor.moveToNext())

            musicCursor.close()

        } else {

            Log.d("MyTag", "The song list is empty")

        }

    }

    private fun findSongList(file:File):MutableList<File>{
        val list = mutableListOf<File>()
        val files = file.listFiles()
        if(files != null) {
            for (singleFile in files) {
                if (singleFile.isDirectory && !singleFile.isHidden) {
                    list.addAll(findSongList(singleFile))
                }else {
                    if(singleFile.name.endsWith(".mp3") || singleFile.name.endsWith(".wav")){
                        list.add(singleFile)
                    }
                }

            }
        }
        return list

    }

    override fun onDestroy() {

        stopService(playIntent)

        musicService = null



        super.onDestroy()

    }

    @SuppressLint("DefaultLocale")
    fun time(int:Int) = java.lang.String.format(
        "%02d:%02d ",
        TimeUnit.MILLISECONDS.toMinutes(int.toLong()),
        TimeUnit.MILLISECONDS.toSeconds(
            int.toLong()
        ) - TimeUnit.MINUTES.toSeconds(
            TimeUnit.MILLISECONDS.toMinutes(
                int.toLong()
            )
        )
    )


}