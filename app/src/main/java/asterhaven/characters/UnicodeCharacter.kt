package asterhaven.characters

import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.unicodescript.UnicodeScript

data class UnicodeCharacter(val asString : String, val script : UnicodeScript){
    override fun toString(): String {
        return "${script.name} $asString ${hex()}"
    }
    fun hex() = asString.codePointAt(0).toString(16).toUpperCase()
    fun scriptIndex() = Universe.indexOfScript[script]!!
    val fontIndex by lazy {
        FontFallback.Static.fontIndexForCharacter(asString)
    }
}