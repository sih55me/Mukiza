package com.wiwolf.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.graphics.BitmapFactory
import android.media.MediaMetadata
import android.media.MediaMetadataRetriever
import android.media.PlaybackParams
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewAnimator
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import java.io.File
import java.util.concurrent.TimeUnit


class MusicActivity : Activity() {
    private lateinit var listview : ListView
    private var items = mutableListOf<ContentValues>()
    private val array by lazy{ArrayAdapter(this, R.layout.playlist_item, mutableListOf<String>())}

    private var musicService: MuService? = null


    private var playIntent: Intent? = null

    private var musicBound = false

    private var paused: Boolean = false

    private var playbackPaused: Boolean = false

    private val select
        get() = musicService?.songPosition ?:-1


    private var changeIcon : (Int) -> Unit = {}

    private var showPlay : (Boolean) -> Unit = {}






    override fun onStart() {

        super.onStart()



    }


    override fun onResume() {
        super.onResume()
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
            musicService!!

            musicService!!.setList(items)

            musicService!!.runningActivity = this@MusicActivity

            musicService!!.mediaPlayer.setOnTimedTextListener { mp, text ->
                Toast.makeText(this@MusicActivity, text.text, Toast.LENGTH_SHORT).show()
            }
            musicService!!.runningActivity = this@MusicActivity
            if(musicService?.songPosition != -1){
                Handler(mainLooper).postDelayed({
                    // 1. Set the selection programmatically (e.g., default to the first item)
                    runOnUiThread {
                        listview.setItemChecked(select, true)


                        // 2. Optional: Scroll the list smoothly to the selected item if it is off-screen
                        listview.smoothScrollToPosition(select)
                    }
                },1500L)

            }

            musicBound = true

        }

        override fun onServiceDisconnected(name: ComponentName) {

            musicBound = false

        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.apply {
            add(0,R.id.coverSong,0,"NOW").setEnabled(false).setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
            add(R.string.set)
            add("Exit")
        }
        return super.onCreateOptionsMenu(menu)
    }


    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.coverSong)?.isEnabled =(musicService?.songPosition?:-1) != -1
        return super.onPrepareOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.title == "Exit"){
            finish()
        }
        if(item.title=="NOW"){
            showDialog(1)
        }
        return super.onOptionsItemSelected(item)
    }







    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_playlist)
        actionBar?.apply{
            elevation = 0F
        }



        listview = findViewById<ListView>(R.id.list)

        listview.choiceMode = AbsListView.CHOICE_MODE_SINGLE
        listview = findViewById(R.id.list)
        listview.adapter = array

        requestPermissions(
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.POST_NOTIFICATIONS
            ),10
        )







        //requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        listview.onItemClickListener = AdapterView.OnItemClickListener{_,a,position,_->
            if(position!=select){
                musicService?.setSong(position)
                musicService?.playSong()
            }
            invalidateOptionsMenu()
            musicService?.runningActivity = this
            showDialog(1)
        }

        listview.setOnScrollListener(object : AbsListView.OnScrollListener{
            override fun onScroll(
                view: AbsListView?, firstVisibleItem: Int,
                visibleItemCount: Int, totalItemCount: Int) {
                if(view ==null)
                    return
                if ((firstVisibleItem > 0) or (view!!.getChildCount() > 0) and
                    ((view!!.getChildAt(0)?.getTop()?:0) < 0)) {

                    actionBar?.setElevation(8f);
                } else {
                    actionBar?.setElevation(0f);
                }
            }

            override fun onScrollStateChanged(p0: AbsListView?, p1: Int) {

            }
        })

    }


    fun saveDataForRecreate(b:Bundle){
        if(musicService?.songs.isNullOrEmpty().not()){
            b.putParcelable("currentItem", musicService?.currentItem)
            b.putInt("currentIndex", musicService?.songPosition ?: -1)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray
    ) {
        if(requestCode==10){
            if(grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showSong()
                }
                if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                }
            }
        }
    }


    override fun onCreateDialog(id: Int): Dialog? {
        if(id==1){
            return object : Paper(this@MusicActivity) {


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

                    override fun onRewind() {
                        super.onRewind()
                    }

                    override fun onFastForward() {
                        super.onFastForward()
                    }


                    override fun onSkipToPrevious() {
                        musicService?.apply{
                            if (songPosition > 1) {
                                songPosition -= 1
                            }
                            playSong()
                        }
                        initMTI()
                    }

                    override fun onSkipToNext() {
                        musicService?.apply{
                            if (songPosition < songs.size - 1) {
                                songPosition += 1
                            }
                            playSong()
                        }
                        initMTI()
                    }
                }

                init{
                    musicService?.med?.setCallback(meca)

                }
                private fun showNotif(mCurrentPosition: Int, max: Int) {
                    if(musicService==null)return
                    val currentItem = musicService!!.currentItem
                    val b = Notification.Builder(musicService!!, "c").apply {
                        setSmallIcon(R.drawable.play)
                        setContentTitle(currentItem.getAsString(MediaStore.Audio.Media.TITLE))
                        setContentText(currentItem.getAsString(MediaStore.Audio.Media.ARTIST))
                        setPriority(Notification.PRIORITY_LOW)
                        setProgress(max, mCurrentPosition, false)
                        setOngoing(true)
                        setStyle(Notification.MediaStyle().setShowActionsInCompactView(0,1,2).setMediaSession(musicService!!.med.sessionToken))
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
                private fun timeFormat(int:Int) = java.lang.String.format(
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

                private val seekbar get()= findViewById<SeekBar>(R.id.slider)
                private val play get()= findViewById<ImageButton>(R.id.play)

                //wait until item ready
                fun waitUntilPlaylist(){
                    while(
                        (musicService?.songs?.isNullOrEmpty() == true) or
                        (musicService?.songPosition == -1)
                    ){}//wait
                }

                val onUpdateGUI = Thread() {
                    while (true){
                        val dd = this
                        musicService?.apply {
                            waitUntilPlaylist()
                            try {
                                val mCurrentPosition: Int =
                                    mediaPlayer.currentPosition / 1000
                                val max: Int = mediaPlayer.duration / 1000
                                runOnUiThread {
                                    seekbar?.setProgress(mCurrentPosition)
                                    seekbar?.max = max
                                    currentItem.getAsString(MediaStore.Audio.Media.TITLE)
                                        ?.let { name ->
                                            dd?.findViewById<TextView>(R.id.song)?.text =
                                                name
                                        }
                                    dd?.findViewById<TextView>(R.id.artist)
                                        ?.setText(currentItem.getAsString(MediaStore.Audio.Media.ARTIST))
                                    dd?.findViewById<TextView>(R.id.dur)?.text =
                                        timeFormat(mediaPlayer.duration)
                                    dd?.findViewById<TextView>(R.id.pos)?.text =
                                        timeFormat(mediaPlayer.currentPosition)
                                    if (!mediaPlayer.isPlaying) {

                                        play?.setImageResource(R.drawable.play)
                                        play?.tooltipText = getString(R.string.pause)

                                    } else {
                                        play?.setImageResource(R.drawable.pause)
                                        play?.tooltipText = getString(R.string.play)
                                    }
                                }

                                med.setPlaybackState(getPlayBackState())
                                med.setMetadata(
                                    MediaMetadata.Builder()
                                        .putLong(
                                            MediaMetadata.METADATA_KEY_DURATION,
                                            max.toLong() * 1000
                                        )
                                        .build()
                                )

                                showNotif(mCurrentPosition, max)
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    }
                }



                fun initGUI(){
                    Thread{
                        while(musicService==null){}//wait
                        val mediaPlayer = musicService!!.mediaPlayer
                        runOnUiThread {
                            play?.setOnClickListener {
                                if (mediaPlayer.isPlaying) {
                                    mediaPlayer.pause()
                                } else {
                                    mediaPlayer.start()
                                }
                            }

                            findViewById<View>(R.id.prev)?.setOnClickListener {
                                meca.onSkipToPrevious()
                            }
                            findViewById<View>(R.id.next)?.setOnClickListener {
                                meca.onSkipToNext()
                            }

                            seekbar?.setOnSeekBarChangeListener(object :
                                SeekBar.OnSeekBarChangeListener {
                                override fun onStopTrackingTouch(seekBar: SeekBar) {
                                }

                                override fun onStartTrackingTouch(seekBar: SeekBar) {
                                }

                                override fun onProgressChanged(
                                    seekBar: SeekBar,
                                    progress: Int,
                                    fromUser: Boolean
                                ) {
                                    if (fromUser && (musicService?.songPosition != -1)) {
                                        mediaPlayer.seekTo(progress * 1000)
                                    }
                                }
                            })

                        }
                        if (musicService != null) {
                            waitUntilPlaylist()
                            loadAlbumArtFromMediaStore(
                                musicService?.currentItem?.getAsLong(MediaStore.Audio.Media._ID)
                                    ?: 0L
                            )
                            runOnUiThread{
                                initMTI()
                            }
                        }
                    }.start()
                    onUpdateGUI.start()
                }


                private fun initMTI(){
                    val ity = musicService?.currentItem
                    if(ity==null){
                        return
                    }
                    val mtia = ArrayAdapter(context, android.R.layout.simple_list_item_1, mutableListOf<kotlin.String>(""))
                    mtia.addAll(ity.keySet().toMutableList())
                    findViewById<ListView>(R.id.mti)?.let {
                        if(it.adapter is ArrayAdapter<*>){
                            (it.adapter as ArrayAdapter<*>).clear()
                        }
                        it.adapter = mtia
                        it.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
                            AlertDialog.Builder(context)
                                .setTitle(mtia.getItem(i))
                                .setMessage(ity.getAsString(mtia.getItem(i)))
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                    }
                }



                override fun show() {
                    setOnDismissListener {
                        removeDialog(id)
                    }
                    setContentView(R.layout.now_playing)
                    super.show()
                    initGUI()
                    actionBar?.setDisplayShowCustomEnabled(true)
                    actionBar?.setDisplayShowTitleEnabled(false)
                    actionBar?.setCustomView(R.layout.music_title)
                    actionBar?.setDisplayHomeAsUpEnabled(true)
                    actionBar?.elevation=0F
                    val infoT = layoutInflater.inflate(R.layout.info_mtdt,null)
                    val infoPopup = PopupWindow(
                        infoT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        true
                    )
                    val m = Markwon.builder(context)
                        .usePlugin(CorePlugin.create())
                        .usePlugin(StrikethroughPlugin.create())
                        .build()
                    infoPopup.isFocusable = true
                    infoPopup.isTouchModal = false
                    infoT.findViewById<View>(R.id.more)?.setOnClickListener {
                        infoPopup.dismiss()
                        findViewById<ViewAnimator>(R.id.root)?.displayedChild = 1
                    }
                    infoT.findViewById<View>(R.id.close)?.setOnClickListener {
                        infoPopup.dismiss()
                    }
                    actionBar?.customView?.setOnClickListener {
                        if(!infoPopup.isShowing){
                            val msg = SpannableStringBuilder("")
                            infoT.findViewById<TextView>(R.id.title)?.setText(musicService?.currentItem?.getAsString("title"))
                            msg.append("- Album: ${musicService?.currentItem?.getAsString("album")}\n")
                            msg.append("- Artist: ${musicService?.currentItem?.getAsString("artist")}\n")
                            msg.append("- Author: ${musicService?.currentItem?.getAsString("author")}\n")
                            msg.append("- Where?: ${musicService?.currentItem?.getAsString("_data")}\n")
                            m.setMarkdown(infoT.findViewById<TextView>(R.id.info), msg.toString())
                            infoPopup.showAsDropDown(it)
                        }else{
                            infoPopup.dismiss()
                        }
                    }
                }



                override fun onAttachedToWindow() {

                    super.onAttachedToWindow()

                }


                //for change pitch
                fun cP(r:Float){
                    val f = musicService?.mediaPlayer?.playbackParams ?: PlaybackParams()
                    f?.pitch = r
                    musicService?.mediaPlayer?.playbackParams = f
                }

                //for change speed
                fun cs(r:Float){
                    val f = musicService?.mediaPlayer?.playbackParams ?: PlaybackParams()
                    f?.speed = r
                    musicService?.mediaPlayer?.playbackParams = f
                }

                private fun loadAlbumArtFromMediaStore(id: Long) {
                    // Generate the standard content URI for the specific album ID
                    Thread{


                        val imgcov = findViewById<ImageView>(R.id.coverSong)
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

                override fun onCreateOptionsMenu(menu: Menu): Boolean {
                    val sped = menu.addSubMenu(R.string.speed).setIcon(R.drawable.speed)
                    sped.item
                    sped.add("0.25").setOnMenuItemClickListener {
                        cs(0.25F)
                        true
                    }
                    sped.add("0.5").setOnMenuItemClickListener {
                        cs(0.5F)
                        true
                    }
                    sped.add("1").setOnMenuItemClickListener {
                        cs(1F)
                        true
                    }
                    sped.add("1.25").setOnMenuItemClickListener {
                        cs(1.25F)
                        true
                    }
                    sped.add("1.5").setOnMenuItemClickListener {
                        cs(1.5F)
                        true
                    }
                    sped.add("2").setOnMenuItemClickListener {
                        cs(2F)
                        true
                    }
                    val p = sped.addSubMenu("Pitch")

                    p.add("0.25").setOnMenuItemClickListener {
                        cP(0.25F)
                        true
                    }
                    p.add("0.5").setOnMenuItemClickListener {
                        cP(0.5F)
                        true
                    }
                    p.add("1").setOnMenuItemClickListener {
                        cP(1F)
                        true
                    }
                    p.add("1.25").setOnMenuItemClickListener {
                        cP(1.25F)
                        true
                    }
                    p.add("1.5").setOnMenuItemClickListener {
                        cP(1.5F)
                        true
                    }
                    p.add("2").setOnMenuItemClickListener {
                        cP(2F)
                        true
                    }
                    return super.onCreateOptionsMenu(menu)
                }






                override fun onSaveInstanceState(): Bundle {
                    val o = super.onSaveInstanceState()
                    saveDataForRecreate(o)
                    return o
                }

                override fun onNavigateUp() {
                    onBackPressed()
                }

                override fun onBackPressed() {
                    if(currentActionMode != null){
                        destroyActionMode()
                        return
                    }
                    val r = findViewById<ViewAnimator>(R.id.root)
                    if(r.displayedChild > 0){
                        r.showPrevious()
                        return
                    }
                    dismiss()
                }




                override fun dismiss() {
                    onUpdateGUI.interrupt()
                    super.dismiss()
                }
            }
        }
        return super.onCreateDialog(id)
    }





    private fun showSong() {

        fun ContentValues.name():String{
            return getAsString(MediaStore.Audio.Media.TITLE).orEmpty()
        }
        Thread{
            getSongList()
            items.sortWith { a, b -> a.name().compareTo(b.name()) }
            if (!array.isEmpty) {
                array.clear()
            }
            runOnUiThread {
                array.addAll(items.map { it.name() })
            }
            musicService?.setList(items)
        }.start()

    }



    private fun getPlayBackState(): PlaybackState? {
        val mediaPlayer = musicService?.mediaPlayer
        return PlaybackState.Builder()
            .setState(
                if (mediaPlayer?.isPlaying == true) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                (mediaPlayer?.currentPosition?.toLong() ?: 0L), 0F
            )
            .setActions(PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_SKIP_TO_NEXT or PlaybackState.ACTION_SKIP_TO_PREVIOUS or PlaybackState.ACTION_PLAY_PAUSE)
            .build()
    }



    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Thread{
            while (musicService==null){}
            runOnUiThread{
                musicService?.songPosition = savedInstanceState.getInt("currentIndex")
            }
        }.start()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saveDataForRecreate(outState)
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

            do {
                val item = ContentValues()
                DatabaseUtils.cursorRowToContentValues(musicCursor, item)
                items.add(item)
                ContentValues()



            } while (musicCursor.moveToNext())

            musicCursor.close()

        } else {

            Log.d("MyTag", "The song list is empty")

        }

    }


    override fun onDestroy() {
        unbindService(musicConnection)
        if(isFinishing){
            stopService(playIntent)
        }

        musicService = null



        super.onDestroy()

    }




}