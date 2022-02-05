package asterhaven.characters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.databinding.ActivityMainBinding
import asterhaven.characters.databinding.InventoryBinding
import kotlinx.coroutines.*
import java.io.File
import kotlin.concurrent.fixedRateTimer

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var invBinding : InventoryBinding //included layout

    lateinit var progress : Progress
    private lateinit var saveFile : File
    private lateinit var mediaPlayer : MediaPlayer

    private var shortAnimationDuration : Int = 0

    //@RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Characters)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        invBinding = binding.inventory
        setContentView(binding.root)

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        CoroutineScope(Dispatchers.IO).launch {
            FontFallback.Static.loadTypefaces(applicationContext)
            timeTV("readAllUS", binding.worldView) { Universe.readAllUS(resources) }
            CoroutineScope(Dispatchers.Main).launch {
                doProgressInit()
                binding.worldView.doInit()
            }
        }
        CoroutineScope(Dispatchers.Default).launch {
            mediaPlayer = MediaPlayer.create(applicationContext, R.raw.atfots)
            if(MUTE) mediaPlayer.setVolume(0f, 0f)
            mediaPlayer.start()
        }
    }

    override fun onStop() {
        super.onStop()
        println("onStop")
        if(::saveFile.isInitialized) runBlocking {
            println("please final save")
            Progress.saveJob(saveFile, progress).join()
            println("final save")
        }
    }

    private fun doProgressInit() {
        saveFile = File(filesDir, "save")
        if(FRESH_PROGRESS) {
            logToTextView("Fresh start")
            logToTextView(if(saveFile.delete()) "fresh" else " !  NOT fresh")
        }
        if(saveFile.exists()) CoroutineScope(Dispatchers.IO).launch {
            progress = Progress.load(saveFile).await()
            logToTextView("restored progress of ${progress.card()}")
        }
        else progress = Progress()
        fixedRateTimer("autosave timer", true, SAVE_EVERY, SAVE_EVERY){
            Progress.saveJob(saveFile, progress)
        }
    }

    fun progressInitialized() = ::progress.isInitialized

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.bar_menu, menu)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.actionBar?.hide()
            //TODO display menu in landscape
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.actionBar?.show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.sound -> {
            println(item.title)
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

    fun sleepButtonClick(view : View){
        when(binding.worldView.visibility){
            View.VISIBLE -> {
                if(!::progress.isInitialized) return
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
}