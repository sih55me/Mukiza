package com.wiwolf.music

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ComponentName
import android.content.ContentValues
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.database.DatabaseUtils
import android.os.Bundle
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

            if(musicService?.songPosition != -1){
                musicService?.show()
                // 1. Set the selection programmatically (e.g., default to the first item)
                runOnUiThread{
                    listview.setItemChecked(select, true)


                    // 2. Optional: Scroll the list smoothly to the selected item if it is off-screen
                    listview.smoothScrollToPosition(select)
                }

            }

            musicBound = true

        }

        override fun onServiceDisconnected(name: ComponentName) {

            musicBound = false

        }

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.apply {
            add("Settings")
            add("Exit")
        }
        return super.onCreateOptionsMenu(menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.title == "Exit"){
            finish()
        }
        return super.onOptionsItemSelected(item)
    }





    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
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








        listview.onItemClickListener = AdapterView.OnItemClickListener{_,_,position,_->
            musicService?.setList(items)
            if(position!=select){
                musicService?.setSong(position)
                musicService?.playSong()
            }
            musicService?.show()
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
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                showSong()
            }
            if(grantResults[2]== PackageManager.PERMISSION_GRANTED){

            }
        }
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