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
    }
    init {
        if(script.charAt(indexInScript) != asString) check(false)
        if(FontFallback.Font.values().none { FontFallback.hasGlyph(it, asString)})
            throw RuntimeException("Bad character: $asString ${
                asString.codePointAt(0).toString(16).uppercase()
            }")
    }
    override fun toString(): String {
        return "${script.name} $asString ${hex()}"
    }
    fun hex() = asString.codePointAt(0).toString(16).uppercase()
    fun scriptIndex() = Universe.indexOfScript[script]!!
    val fontIndex by lazy {
        FontFallback.fontIndexForCharacter(asString)
    }
}