package asterhaven.characters.unicodescript

import kotlinx.serialization.*
import kotlinx.serialization.json.Json

fun decodeAllUS(str : String) : List<UnicodeScript> = Json.decodeFromString(str)
fun encodeAllUS(list : List<UnicodeScript>) : String = Json.encodeToString(list)

var testValues = intArrayOf(0x11265, 0x16B5A, 0x16ACA, 0x10EAA, 0xDF2A4, 0x61)

@Serializable
data class UnicodeScript(val name : String, private val ranges : String){
    val size : Int by lazy { run {
        var s = 0
        rangeIterate { _, count -> s += count }
        s
    }}

    fun codepointIterator() = object : Iterator<Int> {
        val rangeIterator = ranges.codePoints().iterator()
        var first = 0
        var last = -1
        var i = 0
        override fun hasNext() = first + i <= last || rangeIterator.hasNext()
        override fun next(): Int {
            if(first + i > last){
                first = rangeIterator.nextInt()
                last = rangeIterator.nextInt()
                i = 0
            }
            return first + i++
        }
    }

    private inline fun rangeIterate(f : (Int, Int) -> Unit) {
        val iterator = ranges.codePoints().iterator()
        while(iterator.hasNext()) {
            val first = iterator.nextInt()
            val second = iterator.nextInt()
            /*for(tv in testValues) if(tv in first..second) {
                println("Test value ${tv.toString(16)} spotted in $name")
            }*/
            val count = second - first + 1
            f(first, count)
        }
    }

    override fun toString(): String {
        val str = StringBuilder("$name ($size characters)\n")
        for(c in codepointIterator()) str.append(c)
        str.append("\n")
        return str.toString()
    }
}