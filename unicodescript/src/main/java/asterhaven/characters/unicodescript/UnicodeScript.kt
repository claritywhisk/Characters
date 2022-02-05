package asterhaven.characters.unicodescript

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.encodeToJsonElement

fun decodeAllUS(str : String) : List<UnicodeScript> = Json.decodeFromString(str)
fun encodeAllUS(list : List<UnicodeScript>) : String = Json.encodeToString(list)

@Serializable
data class UnicodeScript(val name : String, private val ranges : String){
    @Transient val size : Int = {
        var s = 0
        rangeIterate { _, _, c -> s += c }
        s
    }.invoke()

    fun charAt(pos : Int) : String {
        require(pos >= 0)
        require(pos < size)
        var codepoint = pos
        rangeIterate { first, _, c ->
            if(codepoint + 1 > c) codepoint -= c
            else {
                codepoint += first
                return@rangeIterate
            }
        }
        return String(intArrayOf(codepoint), 0, 1)
    }

    private fun rangeIterate(f : (Int, Int, Int) -> Unit) {
        val iterator = ranges.codePoints().iterator()
        while(iterator.hasNext()) {
            val first = iterator.nextInt()
            val second = iterator.nextInt()
            if(63736 in first..second) println("*****\n$this") //TODO FULL SWEEP
            val count = second - first + 1
            f(first, second, count)
        }
    }

    override fun toString(): String {
        var str = "$name ($size characters)\n"
        for(i in 0 until size) str += charAt(i)
        return str + "\n"
    }
}