package asterhaven.characters.findglyphs

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.icu.lang.UScript
import android.icu.text.UnicodeSet
import android.os.Environment
import androidx.core.app.ActivityCompat
import asterhaven.characters.typeface.FontFallback.Font.*
import asterhaven.characters.typeface.FontFallback.Static.hasGlyph
import asterhaven.characters.typeface.FontFallback.Static.loadTypeface
import asterhaven.characters.unicodescript.UnicodeScript
import asterhaven.characters.unicodescript.encodeAllUS
import java.io.File
import kotlin.reflect.full.staticProperties
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

private const val expected = 199

/*
    Unicode codepoints examined: 1115101
    Unifont13 has 80219 Glyphs
    Default Paint has 78155 (0 unique)
 */

class GlyphActivity : AppCompatActivity() {
    private fun glyph(file: File){
        val fast = false //Turn on to skip the Unknown script (87% of codepoints, < 1/6 of glyphs)
        var uSize = 0
        var gCount = 0
        var fellback = 0
        var strings = false
        val list = ArrayList<UnicodeScript>()
        for(properTea in UScript::class.staticProperties) {
            if(properTea.name == "INVALID_CODE") continue
            if(fast && properTea.name == "UNKNOWN") continue
            val scriptProperty = UScript.getShortName(properTea.get() as Int)
            val script = UnicodeSet("[:${scriptProperty}:]")
            script.freeze()
            uSize += script.size()
            var gly = 0
            var fel = 0
            val usableScript = UnicodeSet()
            script.forEach {
                if(hasGlyph(GNU_UNIFONT, it)){
                    usableScript.add(it.codePointAt(0))
                    gly++
                }
                else if(hasGlyph(SYSTEM_SEVERAL, it)) fel++ //if there were any, should add them too
            }
            usableScript.compact()
            if(!usableScript.isEmpty)
                list.add(createUnicodeScript(usableScript, UScript.getName(properTea.get() as Int)))
            gCount += gly
            //fellback += fel
            val coverage = gly.toDouble() / script.size()
            println(UScript.getName(properTea.get() as Int))
            println("\t${script.size()} chars")
            println(
                "\t${
                    when (coverage) {
                        1.0 -> "full"
                        0.0 -> "zero"
                        else -> "%.3f".format(coverage)
                    }
                }"
            )
            if(fel > 0) println("*!*$fel found by vanillaPaint not added!")
            if(!script.strings().isEmpty()) {
                println("Unexpected script.strings")
                strings = true
                script.strings().forEach(System.out::println)
            }
            if(!usableScript.strings().isEmpty()) {
                println("Unexpected usableScript.strings")
                strings = true
                usableScript.strings().forEach(System.out::println)
            }
        }
        print("${list.size} scripts... ")
        val t = measureTimeMillis {
            val json = encodeAllUS(list)
            file.createNewFile()
            file.writeText(json)
        }
        println(" to JSON in ${"%.3f".format(t.toDouble() / 1000)} s")
        println("Unicode codepoints examined: ${uSize}")
        println("Unifont14 has ${gCount} Glyphs")
        if(fast) println("skipped Unknown")
        println("Default Paint caught $fellback characters")
        val size = UScript::class.staticProperties.size
        if(size != expected)
            println("ERROR: ANDROID.ICU.LANG.USCRIPT CHANGED \n $size from $expected")
        if(strings) println("UNEXPECTED SCRIPT.STRINGS")
    }

    private fun createUnicodeScript(uSet: UnicodeSet, name: String) : UnicodeScript {
        val pairs = ArrayList<Pair<Int, Int>>()
        for(range in uSet.ranges()) pairs.add(Pair(range.codepoint, range.codepointEnd))
        val made = UnicodeScript(name, pairs)
        //println(made)
        return made
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadTypeface(applicationContext)

        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            66
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        require(requestCode == 66)
        println("ON REQUEST PERMISSIONS RESULT")
        thread {
            val dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, "scripts_json.txt")
            val t = measureTimeMillis {
                glyph(file)
            }
            println("completed in ${"%.3f".format(t.toDouble() / 1000)} s")
        }
    }
}