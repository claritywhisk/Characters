package asterhaven.characters

import android.content.res.Resources
import asterhaven.characters.unicodescript.UnicodeScript
import asterhaven.characters.unicodescript.decodeAllUS
import java.util.*

object Universe {
    lateinit var allScripts : Array<UnicodeScript>
    lateinit var indexOfScript : MutableMap<UnicodeScript, Int>
    fun readAllUS(resources : Resources) {
        resources.openRawResource(R.raw.scripts_json).bufferedReader().use {
            allScripts = decodeAllUS(it.readText()).toTypedArray()
        }
        indexOfScript = HashMap<UnicodeScript,Int>()
        Arrays.sort(allScripts, compareBy { it.name })
        for(i in allScripts.indices) indexOfScript[allScripts[i]] = i
        println(allScripts.size)
    }
}