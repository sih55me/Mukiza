package com.wiwolf.music

import android.R
import android.app.ActionBar
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.view.WindowManager

/**
 * [Paper] is a [android.app.Activity]-like [android.app.Dialog] with a bit modify
 *
 * Feature :
 * * Can Make Menu
 * * Can Show and add listener for Back Button
 * * Can customize the toolbar using [getActionBar]
 */
open class Paper @JvmOverloads constructor(context: Context, showActionBar : Boolean = true): Dialog(context, context.getThemeId()), AutoCloseable {

    var useBlur = false


    var winAttr set(value) {
        window?.attributes = value
    } get() = window?.attributes

    var winType set(value) {
        if (value != null) {
            winAttr?.type = value
        }
    } get() = winAttr?.type


    val decorView get() = window?.decorView


    init {
        window?.requestFeature(Window.FEATURE_OPTIONS_PANEL)
        if(!showActionBar){
            unShowToolbar()
        }
    }

    @JvmField
    var windowAnimation = R.style.Animation_InputMethod


    var currentActionMode : ActionMode? = null
        private set

    val windowManager get()= window?.windowManager ?: context.getSystemService(Activity.WINDOW_SERVICE) as WindowManager



    final fun getString(resId: Int): String{
        return context.getString(resId)
    }

    override fun onKeyShortcut(keyCode: Int, event: KeyEvent): Boolean {
        if(event.isCtrlPressed == true){
            if (keyCode == KeyEvent.KEYCODE_W) {
                dismiss()
                return true
            }
        }
        return super.onKeyShortcut(keyCode, event)
    }


    override fun show() {
        winAttr?.let{
            it.windowAnimations = this@Paper.windowAnimation
        }

        super.show()
        if(window?.hasFeature(Window.FEATURE_ACTION_BAR) == true){
            actionBar?.let { setupActionBar(it) }
        }
    }

    @JvmOverloads
    fun createNLoad(s: Bundle? = null){
        onCreate(s)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        return super.onCreateOptionsMenu(menu)
    }


    open fun onNavigateUp(){
        dismiss()
    }

    /**
     * Make it work when onOptionsItemSelected
     */
    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        when(featureId){
            Window.FEATURE_OPTIONS_PANEL ->{
                if (item.itemId == R.id.home){
                    onNavigateUp()
                    return true;
                }
                if (onOptionsItemSelected(item)) {
                    return true;
                }
            }
        }
        return super.onMenuItemSelected(featureId, item)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }



    /**
     *  calling after Paper [show] up and [android.app.ActionBar] is set (except if set [Window.FEATURE_NO_TITLE] in [Window]'s flag)
     */
    open fun setupActionBar(actionBar: ActionBar){}




    fun destroyActionMode(){
        currentActionMode?.finish()
    }

    override fun onBackPressed() {
        if(currentActionMode != null){
            destroyActionMode()
            return
        }
        super.onBackPressed()
    }
    /**
     *  make the [Paper] dim behind
     *
     *  Importance : must call before calling [show]
     */



    /**
     *  make [android.widget.Toolbar] or [ActionBar] gone in [Paper]
     *
     *  Importance : must call before calling [show] or [onCreate]
     */
    fun unShowToolbar(){
        requestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return true
    }


    override fun onActionModeFinished(mode: ActionMode?) {
        currentActionMode = null
        super.onActionModeFinished(mode)
    }

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        currentActionMode = mode

    }

    override fun close() {
        dismiss()
    }
}