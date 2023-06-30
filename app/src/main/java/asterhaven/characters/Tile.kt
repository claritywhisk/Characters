package asterhaven.characters

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

/*
//script/etc. components over [0, 1]
//@RequiresApi(Build.VERSION_CODES.N)
data class Coordinate private constructor(
        val scriptDims : DoubleArray,
        val sigmaDims : Double,
        val terrain : Terrain?,
        val unicodeCharacter : UnicodeCharacter?
        ) : CoordinateWrapper {
    companion object Factory {
        fun create(scriptDims : DoubleArray) : Coordinate {
            val sigmaDims = scriptDims.fold(0.0) {acc, dim -> acc + dim}
            var unicodeCharacter : UnicodeCharacter? = null
            for(si in scriptDims.indices){
                if (Random.nextDouble() < scriptDims[si]) {
                    if(unicodeCharacter != null){
                        return Coordinate(scriptDims, sigmaDims, Terrain.CLOUD, null)
                    }
                    unicodeCharacter = UnicodeCharacter.create(si)
                }
            }
            return Coordinate(scriptDims, sigmaDims, null, unicodeCharacter)
        }
    }
    override fun coordinate() = this
    override fun equals(other: Any?): Boolean = when {
        (other is Coordinate) -> other === this
        else -> false
    }
    fun semiVerbose() : String {
        //average of coordinates and script of most notable coordinate
        var str = ""
        var high = -1.0
        var highIndex = -1
        for(i in scriptDims.indices){
            val x = scriptDims[i]
            if(x > high){
                high = x
                highIndex = i
            }
        }
        val round = "%.4f" //decimal places
        val name = Universe.allScripts[highIndex].name
        val range = 0 until 4.coerceAtMost(name.length)
        str += name.substring(range) + " " + round.format(high)
        str += "\t sigma ${round.format(sigmaDims)}"
        return str
    }
}
interface CoordinateWrapper {
    fun coordinate() : Coordinate
}
class DeferredCo(val c : Deferred<Coordinate>) : CoordinateWrapper {
    override fun coordinate() = runBlocking{ c.await() }
}
class Coffin(val c : Coordinate) : CoordinateWrapper {
    override fun coordinate(): Coordinate {
        if(BuildConfig.DEBUG) check(false)
        return c
    }
} */

open class Tile(open val character : UnicodeCharacter? = null)
class DeferredTile(private val c : Deferred<UnicodeCharacter?>?) : Tile() {
    override val character by lazy { runBlocking { c?.await() } }
}