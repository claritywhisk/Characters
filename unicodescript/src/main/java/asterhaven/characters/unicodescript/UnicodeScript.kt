package asterhaven.characters.unicodescript

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

fun decodeAllUS(str : String) : List<UnicodeScript> = Json.decodeFromString(str)
fun encodeAllUS(list : List<UnicodeScript>) : String = Json.encodeToString(list)

@Serializable
data class UnicodeScript(val name : String, private val ranges : List<Pair<Int, Int>>){
    val size : Int = ranges.sumOf { it.qownt() }
    fun codePointOfMemberAt(pos : Int) : Int {
        require(pos >= 0)
        require(pos < size)
        var p = pos
        val iterator = ranges.listIterator()
        while(true){
            val range = iterator.next()
            val c = range.qownt()
            if(p + 1 > c) p -= c
            else return range.first + p
        }
    }
    private fun Pair<Int,Int>.qownt() = second - first + 1

    override fun toString(): String {
        var str = "$name ($size characters)\n"
        for(i in 0 until size) str += String(intArrayOf(codePointOfMemberAt(i)), 0, 1)
        return str + "\n"
    }
}