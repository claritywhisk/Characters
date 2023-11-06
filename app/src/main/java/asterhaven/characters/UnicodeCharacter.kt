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
        //all 100000+ character addresses
        private val allCodepoints by lazy {
            Array(allScripts.size){si ->
                val iterator = allScripts[si].codepointIterator()
                IntArray(allScripts[si].size){
                    if(BuildConfig.DEBUG) check(iterator.hasNext())
                    iterator.next()
                }
            }
        }
        private val all by lazy {
            Array(allScripts.size) { si ->
                Array<UnicodeCharacter?>(allScripts[si].size) {
                    null
                }
            }
        }
        fun get(script : UnicodeScript, i : Int) : UnicodeCharacter {
            val si = Universe.indexOfScript[script]!!
            if(all[si][i] == null){
                val asString = String(intArrayOf(allCodepoints[si][i]), 0, 1) //todo 2 codepoint emoji?
                all[si][i] = UnicodeCharacter(allScripts[si], asString, i)
            }
            return all[si][i]!!
        }
    }

    /*init { Todo: time consuming check has apparently passed on my phone
        if(BuildConfig.DEBUG && FontFallback.Font.values().none { FontFallback.hasGlyph(it, asString)})
            throw RuntimeException("Bad character: $asString ${
                asString.codePointAt(0).toString(16).uppercase()
            }")
    }*/

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