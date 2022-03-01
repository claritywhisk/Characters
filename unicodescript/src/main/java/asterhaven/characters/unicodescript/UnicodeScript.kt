package asterhaven.characters.unicodescript

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.IllegalStateException

fun decodeAllUS(str : String) : List<UnicodeScript> = Json.decodeFromString(str)
fun encodeAllUS(list : List<UnicodeScript>) : String = Json.encodeToString(list)

var testValues = intArrayOf(0x11265, 0x16B5A, 0x16ACA, 0x10EAA, 0xDF2A4, 0x61)

@Serializable
data class UnicodeScript(val name : String, private val ranges : String){
    @Transient val size : Int = run {
        var s = 0
        rangeIterate { _, count -> s += count }
        s
    }

    fun charAt(pos : Int) : String {
        if(pos < 0 || pos >= size) {
            throw IllegalStateException("pos $pos for $name with $size chars")
        }
        var remaining = pos + 1
        rangeIterate { first, count ->
            if(remaining > count) remaining -= count
            else return String(intArrayOf(first + remaining - 1), 0, 1)
        }
        throw IllegalStateException("UnicodeScript.charAt error")
        //return ranges[0].toString()
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
        var str = "$name ($size characters)\n"
        for(i in 0 until size) str += charAt(i)
        return str + "\n"
    }
}