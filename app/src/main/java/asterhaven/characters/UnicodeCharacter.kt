package asterhaven.characters

import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.unicodescript.UnicodeScript
import kotlin.random.Random

data class UnicodeCharacter private constructor(val script : UnicodeScript, val indexInScript : Int){
    val asString: String = script.charAt(indexInScript)
    companion object Factory {
        fun create(scriptI : Int) : UnicodeCharacter {
            val script = Universe.allScripts[scriptI]
            val randCharI = Random.nextInt(script.size)
            return UnicodeCharacter(script, randCharI)
        }
        fun create(scriptI : Int, charI : Int) = UnicodeCharacter(Universe.allScripts[scriptI], charI)
        fun create(asString : String) : UnicodeCharacter {
            fun script(str : String) = Character.UnicodeScript.of(str.codePointAt(0))
            val s = script(asString)
            val scriptI = Universe.allScripts.indices.first {
                script(Universe.allScripts[it].charAt(0)) == s
            }
            for(charI in 0 until Universe.allScripts[scriptI].size) {
                val c = Universe.allScripts[scriptI].charAt(charI)
                if(c != asString) continue
                return create(scriptI, charI)
            }
            throw squawk(asString)
        }
        private fun squawk(asString : String) = RuntimeException("Bad character: $asString ${
            asString.codePointAt(0).toString(16).uppercase()
        }")
    }
    init {
        if(script.charAt(indexInScript) != asString) check(false)
        if(FontFallback.Font.values().none { FontFallback.hasGlyph(it, asString)})
            throw squawk(asString)
    }

    fun description() = Character.getName(asString.codePointAt(0)) //todo test on all and 2xCP emojis

    override fun toString(): String {
        return "${hex()} ${script.name} $asString"
    }
    fun hex() = asString.codePointAt(0).toString(16).uppercase()
    fun scriptIndex() = Universe.indexOfScript[script]!!
    val fontIndex by lazy {
        FontFallback.fontIndexForCharacter(asString)
    }
}