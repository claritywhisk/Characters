package asterhaven.characters

import android.content.res.Resources
import asterhaven.characters.unicodescript.UnicodeScript
import asterhaven.characters.unicodescript.decodeAllUS

object Universe {
    lateinit var allScripts : Array<UnicodeScript>
    lateinit var indexOfScript : Map<UnicodeScript, Int>
    fun readAllUS(resources : Resources) {
        resources.openRawResource(R.raw.scripts).bufferedReader().use {
            allScripts = decodeAllUS(it.readText()).filter { s ->
                if(DEBUG_SMALL_SCRIPTS) s.size <= DEBUG_MAX_SCRIPT_SIZE else true
            }.toTypedArray()
        }
        val ios = HashMap<UnicodeScript,Int>(allScripts.size)
        java.util.Arrays.sort(allScripts, compareBy { it.name })
        for(i in allScripts.indices) ios[allScripts[i]] = i
        indexOfScript = ios
        if(BuildConfig.DEBUG){
            println("${allScripts.size} scripts")
            println("${allScripts.sumOf {it.size}} characters")
            println("${allScripts.count {it.size == 0}} empty scripts")
        }
    }
}