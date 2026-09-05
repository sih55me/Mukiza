package com.wiwolf.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.media.PlaybackParams
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import java.io.File
import java.util.concurrent.TimeUnit


class MusicActivity : Activity() {
    private lateinit var listview : ListView
    private var items = mutableListOf<ContentValues>()
    private val array by lazy{ArrayAdapter(this, android.R.layout.simple_list_item_checked, mutableListOf<String>())}

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







        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        listview.onItemClickListener = AdapterView.OnItemClickListener{_,a,position,_->
            musicService?.setList(items)
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



                override fun show() {
                    musicService?.d = this
                    setContentView(R.layout.now_playing)
                    musicService?.initGUI(null)
                    setOnDismissListener {
                        removeDialog(id)
                    }
                    super.show()
                    actionBar?.setDisplayHomeAsUpEnabled(true)
                    actionBar?.elevation=0F
                    musicService?.isShow = isShowing
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

                override fun onCreateOptionsMenu(menu: Menu): Boolean {
                    val sped = menu.addSubMenu(R.string.speed).setIcon(R.drawable.speed)
                    sped.item.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
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





                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                }

                override fun onSaveInstanceState(): Bundle {
                    val o = super.onSaveInstanceState()
                    return o
                }

                override fun onNavigateUp() {
                    onBackPressed()
                }



                override fun onDetachedFromWindow() {
                    super.onDetachedFromWindow()

                }


                override fun dismiss() {
                    musicService!!.mHandler.removeCallbacks(musicService!!.onUpdateGUI)
                    musicService!!.isShow = false
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
        }.start()

    }



    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Thread{
            while (musicService==null){}
            runOnUiThread{
                musicService?.songPosition = savedInstanceState.getInt("select")
            }
        }.start()
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