package asterhaven.characters

import asterhaven.characters.Universe.allScripts
import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.unicodescript.UnicodeScript

data class UnicodeCharacter private constructor(
    val script : UnicodeScript,
    val asString: String,
    val i : Int //index in all
){
    companion object {
        val n by lazy { allScripts.sumOf { it.size } }
        val scriptStartI by lazy {
            var i = 0
            IntArray(allScripts.size){ si ->
                i.also { i += allScripts[si].size }
            }
        }
        //all 100000+ character-describing objects
        val all by lazy {
            var si = 0
            var charIter = allScripts[si].charIterator()
            Array(n){ ci ->
                if(si + 1 < allScripts.size && scriptStartI[si + 1] == ci){
                    si++
                    charIter = allScripts[si].charIterator()
                }
                if(BuildConfig.DEBUG) check(charIter.hasNext())
                UnicodeCharacter(allScripts[si], charIter.next(), ci)
            }
        }
        fun getFor(script: UnicodeScript, pos : Int) = all[scriptStartI[Universe.indexOfScript[script]!!] + pos]
    }

    init {
        if(BuildConfig.DEBUG && FontFallback.Font.values().none { FontFallback.hasGlyph(it, asString)})
            throw RuntimeException("Bad character: $asString ${
                asString.codePointAt(0).toString(16).uppercase()
            }")
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