package asterhaven.characters.findglyphs

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.icu.lang.UScript
import android.icu.text.UnicodeSet
import android.os.Environment
import androidx.core.app.ActivityCompat
import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.typeface.FontFallback.hasGlyph
import asterhaven.characters.typeface.FontFallback.Font
import asterhaven.characters.unicodescript.UnicodeScript
import asterhaven.characters.unicodescript.encodeAllUS
import java.io.File
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.reflect.full.staticProperties
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

private const val expected = 199

/*
    Unicode codepoints examined: 1114112
    Glyphs found: 84932
    Scripts: 157
 */

class GlyphActivity : AppCompatActivity() {
    var testValues = intArrayOf(0x11265, 0x16B5A, 0x16ACA, 0x10EAA)
    private fun glyph(file: File){
        val fast = false //Turn on to skip the Unknown script (87% of codepoints, < 1/6 of glyphs)
        var uSize = 0
        var strings = false
        val list = ArrayList<UnicodeScript>()
        val seen = HashSet<Int>()
        for(properTea in UScript::class.staticProperties) {
            if(properTea.name == "INVALID_CODE") continue
            if(fast && properTea.name == "UNKNOWN") continue
            val code = properTea.get() as Int
            //otherwise two copies of Canadian_Aboriginal, Miao, Merotic, Khudawadi, Mandaic, others
            if(seen.contains(code)) continue
            seen.add(code)
            val scriptProperty = UScript.getShortName(code)
            val script = UnicodeSet("[:${scriptProperty}:]")
            script.freeze()
            uSize += script.size()
            var gly = 0
            val usableScript = UnicodeSet()
            var progInd = 0
            script.forEach { Ꭿ ->
                if(progInd++ % 40000 == 0) println(".")
                if(Font.values().any{ hasGlyph(it, Ꭿ) }) {
                    usableScript.add(Ꭿ.codePointAt(0))
                    gly++
                    if(Ꭿ.codePointAt(0) in testValues){
                        println("Test value ${Ꭿ.codePointAt(0).toString(16)} added")
                        for(f in Font.values()) println(f.name + " " + hasGlyph(f, Ꭿ))
                    }
                }
                if(Ꭿ.codePointAt(0) in testValues){
                    println("(Test value ${Ꭿ.codePointAt(0).toString(16)} exists)")
                }
            }
            println()
            usableScript.compact()
            if(!usableScript.isEmpty)
                list.add(createUnicodeScript(usableScript, UScript.getName(properTea.get() as Int)))
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
        println("Unicode codepoints examined: $uSize")
        println("Glyphs found: ${list.fold(0){ sum, script -> sum + script.size}}")
        println("Scripts: ${list.size}")
        if(fast) println("skipped Unknown")
        val size = UScript::class.staticProperties.size
        if(size != expected)
            println("ERROR: ANDROID.ICU.LANG.USCRIPT CHANGED \n $size from $expected")
        if(strings) println("UNEXPECTED SCRIPT.STRINGS")
    }

    private fun createUnicodeScript(uSet: UnicodeSet, name: String) : UnicodeScript {
        val pairs = StringBuilder(uSet.rangeCount * 2)
        for(range in uSet.ranges()) {
            for(tv in testValues) if(tv in range.codepoint..range.codepointEnd){
                println("Test value ${tv.toString(16)} included in $name")
            }
            pairs.append(String(intArrayOf(range.codepoint, range.codepointEnd), 0, 2))
        }
        return UnicodeScript(name.replace('_',' '), pairs.toString())
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FontFallback.loadTypefaces(applicationContext)

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
            val file = File(dir, "scripts.txt")
            val t = measureTimeMillis {
                glyph(file)
            }
            println("completed in ${"%.3f".format(t.toDouble() / 1000)} s")
        }
    }
}