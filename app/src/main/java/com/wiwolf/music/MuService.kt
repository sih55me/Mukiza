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

    private val songs: MutableList<ContentValues> = mutableListOf()
    val mHandler: Handler = Handler(Looper.getMainLooper())



    var runningActivity: Activity? = null
        set(value) {
            if(value!=null){
                makePage()
            }
        }

    var songPosition = -1



    private val musicBinder: IBinder = MuBin(this)

    var d : Paper? = null



    val meca = object : MediaSession.Callback() {
        val m get() = mediaPlayer
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

        override fun onRewind() {
            super.onRewind()
        }

        override fun onFastForward() {
            super.onFastForward()
        }


        override fun onSkipToPrevious() {
            if(songPosition > 1){
                songPosition -=1
            }
            playSong()
            initMTI(null)
        }

        override fun onSkipToNext() {
            if(songPosition < songs.size-1){
                songPosition +=1
            }
            playSong()
            initMTI(null)
        }
    }

    private val med by lazy {
        MediaSession(this, "Music").apply {
            isActive = true
            setCallback(meca)
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
        try{
            d?.dismiss()
            d = null
        }catch (_: Exception){

        }
        super.onDestroy()
        mediaPlayer.stop()

        mediaPlayer.release()

    }

    override fun onCreate() {
        setTheme(R.style.Theme_Mz)
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
        mediaPlayer.playbackParams = PlaybackParams().allowDefaults().setPitch(1F).setSpeed(1F)
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

        mediaPlayer.prepare()

    }


    var isShow = false


    fun show(tkn: IBinder?){

        if(d?.isShowing==true){
            try{
                d?.dismiss()
            }catch (_: Exception){

            }
        }
        if(runningActivity!=null){

            //d?.window!!.attributes.token = runningActivity!!.window!!.attributes.token
            //d?.window!!.attributes.token = tkn
        }
        try{
            d?.show()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun hide(){
        d?.hide()
    }

    fun setSong(songIndex: Int) {

        songPosition = songIndex

    }

    //PRIVATE VOID

    private val seekbar get()= d?.findViewById<SeekBar>(R.id.slider)
    private val play get()= d?.findViewById<ImageButton>(R.id.play)

     fun makePage() {
        d = object : Paper(runningActivity?:this) {

            init {
                windowAnimation = getResources().getIdentifier("Animation.RecentApplications", "style", "android")
            }

            override fun show() {
                super.show()
                actionBar?.setDisplayHomeAsUpEnabled(true)
                actionBar?.elevation=0F
            }

            override fun onAttachedToWindow() {
                super.onAttachedToWindow()
                isShow = isShowing
                setContentView(R.layout.now_playing)
                initGUI(null)
            }

            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
            }

            override fun onSaveInstanceState(): Bundle {
                val o = super.onSaveInstanceState()
                o.putParcelable("item", currentItem)
                return o
            }

            override fun onNavigateUp() {
                hide()
            }

            override fun onDetachedFromWindow() {
                super.onDetachedFromWindow()
                mHandler.removeCallbacks(onUpdateGUI)
                isShow = false
            }

            override fun hide() {
                super.hide()
            }

            override fun dismiss() {
                super.dismiss()
            }
        }
    }




    private fun loadAlbumArtFromMediaStore(id: Long) {
        // Generate the standard content URI for the specific album ID
        Thread{


            val imgcov = d?.findViewById<ImageView>(R.id.coverSong)
            try {
                val rt = MediaMetadataRetriever()
                val u = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                val artbita = rt.embeddedPicture

                val artwork = BitmapFactory.decodeByteArray(artbita,0,artbita?.size?:0)


                imgcov?.post{
                    imgcov?.setImageBitmap(artwork)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                imgcov?.post{
                    imgcov?.setImageResource(R.drawable.ic_launcher_foreground)
                }
            }
        }.start()
    }
    private fun getPlayBackState(): PlaybackState? {
        return PlaybackState.Builder()
            .setState(
                if (mediaPlayer?.isPlaying == true) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                (mediaPlayer?.currentPosition?.toLong() ?: 0L), 0F
            )
            .setActions(PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_PLAY_PAUSE)
            .build()
    }

    private fun showNotif(mCurrentPosition: Int, max: Int) {
        val b = Notification.Builder(this@MuService, "c").apply {
            setSmallIcon(R.drawable.play)
            setContentTitle(currentItem.getAsString(MediaStore.Audio.Media.TITLE))
            setContentText(currentItem.getAsString(MediaStore.Audio.Media.ARTIST))
            setPriority(Notification.PRIORITY_LOW)
            setProgress(max, mCurrentPosition, false)
            setOngoing(true)
            setStyle(Notification.MediaStyle().setShowActionsInCompactView(0,1,2,3).setMediaSession(med.sessionToken))
            setOnlyAlertOnce(true)
            setCategory(Notification.CATEGORY_SERVICE)
        }.build()
        val n = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        n.createNotificationChannel(
            NotificationChannel(
                "c",
                "Music",
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        n.notify(1, b)
    }
    private fun timeFormat(int:Int) = String.format(
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
    val onUpdateGUI =  object:Runnable {
        override fun run() {
            if(songPosition != -1) {
                try{
                    val mCurrentPosition: Int = mediaPlayer.currentPosition / 1000
                    val max: Int = mediaPlayer.duration / 1000
                    seekbar?.setProgress(mCurrentPosition)
                    seekbar?.max = max
                    currentItem.getAsString(MediaStore.Audio.Media.TITLE)?.let{name->
                        d?.findViewById<TextView>(R.id.song)?.text = name
                    }
                    d?.findViewById<TextView>(R.id.artist)?.setText(currentItem.getAsString(MediaStore.Audio.Media.ARTIST))
                    d?.findViewById<TextView>(R.id.dur)?.text = timeFormat(mediaPlayer.duration)
                    d?.findViewById<TextView>(R.id.pos)?.text = timeFormat(mediaPlayer.currentPosition)
                    if (!mediaPlayer.isPlaying) {

                        play?.setImageResource(R.drawable.play)
                        play?.tooltipText = getString(R.string.pause)

                    } else {
                        play?.setImageResource(R.drawable.pause)
                        play?.tooltipText = getString(R.string.play)
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
            mHandler.post(this)
        }
    }

    private fun initTabs(){

    }

    fun initGUI(savedInstanceState: Bundle?){
        play?.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
            } else {
                mediaPlayer.start()
            }
        }

        d?.findViewById<View>(R.id.prev)?.setOnClickListener {
            meca.onSkipToPrevious()
        }
        d?.findViewById<View>(R.id.next)?.setOnClickListener {
            meca.onSkipToNext()
        }

        seekbar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onProgressChanged(
                seekBar: SeekBar,
                progress: Int,
                fromUser: Boolean
            ) {
                if (fromUser && (songPosition != -1)) {
                    mediaPlayer.seekTo(progress * 1000)
                }
            }
        })
        mHandler?.post(onUpdateGUI)
        initTabs()
        loadAlbumArtFromMediaStore(currentItem.getAsLong(MediaStore.Audio.Media._ID))
        initMTI(null)
    }


    private fun initMTI(savedInstanceState: Bundle?){
        val ity = currentItem
        val mtia = ArrayAdapter(d?.context?:this, android.R.layout.simple_list_item_1, mutableListOf<kotlin.String>(""))
        mtia.addAll(ity.keySet().toMutableList())
        d?.findViewById<ListView>(R.id.mti)?.let {
            if(it.adapter is ArrayAdapter<*>){
                (it.adapter as ArrayAdapter<*>).clear()
            }
            it.adapter = mtia
            if(savedInstanceState==null){
                it.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
                    AlertDialog.Builder(this)
                        .setTitle(mtia.getItem(i))
                        .setMessage(ity.getAsString(mtia.getItem(i)))
                        .setPositiveButton(android.R.string.ok, null)
                        .create().also { md ->
                            md.window!!.attributes.token = d!!.window!!.attributes.token
                        }.show()
                }
            }
        }
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


}