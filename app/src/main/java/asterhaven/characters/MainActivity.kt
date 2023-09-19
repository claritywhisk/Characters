package asterhaven.characters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.databinding.ActivityMainBinding
import asterhaven.characters.unicodescript.UnicodeScript
import kotlinx.coroutines.*
import java.io.File
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    //private lateinit var invBinding : InventoryBinding //included layout
    private lateinit var mediaPlayer : MediaPlayer

    val progress by Progress
    var progressBar : ProgressBar? = null
    var matched4 : UnicodeScript? = null
    var inventoryDeleteConfirmation : InventorySlot.ConfirmDeleteStatus? = null
    private var shortAnimationDuration : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Characters)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        InventorySlot.init(this)
        setContentView(binding.root)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        CoroutineScope(Dispatchers.IO).launch {
            FontFallback.loadTypefaces(applicationContext)
            timeTV("readAllUS", binding.worldView) { Universe.readAllUS(resources) }
            Progress.beginWithSaveFile(File(filesDir, "save"))
            fixedRateTimer("autosave timer", true, SAVE_EVERY, SAVE_EVERY){
                Progress.save(progress)
            }
            CoroutineScope(Dispatchers.Main).launch {
                binding.textView.typeface = FontFallback.Font.GNU_UNIFONT.getTypeface() //todo dynamic
                binding.worldView.doInit(progress)
                /*CoroutineScope(Dispatchers.Default).launch {
                    var x = 0
                    var width = 0
                    Universe.allScripts.forEachIndexed { i, it ->
                        println(it.name)
                        println()
                        for(j in 0 until it.size){
                            x++
                            val c = UnicodeCharacter.create(i, j)

                        }
                        println()
                    }
                    println("*** Completed $x")
                    //println("*** $width ")
                }*/
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.atfots)
            if(MUTE) mediaPlayer.setVolume(0f, 0f)
            mediaPlayer.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bar_menu, menu)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.actionBar?.hide()
            //TODO display menu in landscape, or abolish landscape
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.actionBar?.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.sound -> {
            if (item.title == resources.getString(R.string.menu_sound_on)) {
                mediaPlayer.setVolume(0f, 0f)
                item.title = resources.getString(R.string.menu_sound_off)
            } else {
                mediaPlayer.setVolume(1f, 1f)
                item.title = resources.getString(R.string.menu_sound_on)
            }
            true
        }
        R.id.about -> {
            println("About World")
            //todo credit
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    fun logToTextView(line : String) =
        runOnUiThread { binding.textView.append(line + "\n") }

    fun inventoryMatched(script : UnicodeScript){
        matched4 = script
        binding.inventory.scriptName.text = script.name
        progressBar = binding.inventory.scriptProgress.also {
            it.max = script.size
            it.setProgress(progress.seenInScript[Universe.indexOfScript[script]!!], true)
        }
        crossfade(binding.inventory.invTable, binding.inventory.invMatched, false){}
    }

    private fun finishedWithScriptClick(v : View) = finishedWithScript()
    fun finishedWithScript(){
        InventorySlot.clearAll()
        //todo picture?
        crossfade(binding.inventory.invMatched, binding.inventory.invTable, false){}
        progressBar = null
        matched4 = null
    }

    fun pictureButtonClick(v : View){
        for(i in Universe.allScripts.indices){
            val s = Universe.allScripts[i]
            logToTextView("${s.name} ${progress.seenInScript[i]}/${s.size} ${progress.seenScript[i]}")
        }
    }

    fun sleepButtonClick(z : View){
        when(binding.worldView.visibility){
            View.VISIBLE -> {
                binding.sleepView.setLocation(binding.worldView.movement.sleepScriptDims())
                crossfade(binding.worldView, binding.sleepView, false){
                    binding.sleepView.sleep()
                }
            }
            View.INVISIBLE -> {
                binding.sleepView.wake()
                crossfade(binding.sleepView, binding.worldView, true){ }
            }
        }
    }

    fun catalogButtonClick(v : View){
        Progress.clearProgress()
        logToTextView("Reset progress")
    }
    fun settingsButtonClick(v : View){
        //todo
    }

    //https://developer.android.com/training/animation/reveal-or-hide-view#Crossfade
    private fun crossfade(a : View, b : View, beGone : Boolean, onComplete : () -> Unit) {
        b.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate()
                .alpha(1f)
                .setDuration(shortAnimationDuration.toLong())
                .setListener(null)
        }
        a.animate()
            .alpha(0f)
            .setDuration(shortAnimationDuration.toLong())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    a.visibility = if(beGone) View.GONE else View.INVISIBLE
                    onComplete.invoke()
                }
            })
    }
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        inventoryDeleteConfirmation?.let {
            if(event?.action == MotionEvent.ACTION_DOWN) {
                it.didRespond()
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}