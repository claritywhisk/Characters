package asterhaven.characters

import android.content.res.Configuration
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.databinding.ActivityMainBinding
import asterhaven.characters.databinding.InventoryBinding
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var invBinding : InventoryBinding //included layout

    private lateinit var mediaPlayer : MediaPlayer

    //@RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Characters)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        invBinding = binding.inventory
        setContentView(binding.root)
        
        CoroutineScope(Dispatchers.IO).launch {
            FontFallback.Static.loadTypeface(applicationContext)
            Universe.readAllUS(resources)
            CoroutineScope(Dispatchers.Main).launch {
                binding.worldView.doInit()
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
}