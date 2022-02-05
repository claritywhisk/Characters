package asterhaven.characters

import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.unicodescript.UnicodeScript
import kotlin.random.Random

data class UnicodeCharacter private constructor(
    val asString : String,
    val script : UnicodeScript,
    val indexInScript : Int
    ){
    companion object Factory {
        fun create(scriptI : Int) : UnicodeCharacter {
            val script = Universe.allScripts[scriptI]
            val randCharI = Random.nextInt(script.size)
            return create(script, randCharI)
        }
        fun create(scriptI : Int, charI : Int) = create(Universe.allScripts[scriptI], charI)
        private fun create(s : UnicodeScript, i : Int) = UnicodeCharacter(s.charAt(i), s, i)
    }
    override fun toString(): String {
        return "${script.name} $asString ${hex()}"
    }
    fun hex() = asString.codePointAt(0).toString(16).toUpperCase()
    fun scriptIndex() = Universe.indexOfScript[script]!!
    val fontIndex by lazy {
        FontFallback.Static.fontIndexForCharacter(asString)
    }
}