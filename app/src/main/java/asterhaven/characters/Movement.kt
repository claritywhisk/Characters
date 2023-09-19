package asterhaven.characters

import kotlinx.coroutines.*
import kotlin.math.sqrt
import kotlin.random.Random

class Movement(private val wv : WorldView, private val progress : Progress) {
    val SZ = Universe.allScripts.size
    //val LOCAL_MAP_TILES = SIDE_LENGTH_EXTENDED * SIDE_LENGTH_EXTENDED
    val center = SIDE_LENGTH_EXTENDED / 2

    private val next: MutableList<Pair<Int, Int>> = ArrayList(EXTENDED_MAP_SIZE * 2)

    fun requisition(i : Int, j : Int) = next.add(Pair(i, j))

    //call async for each new coordinate
    fun startUpdate() = CoroutineScope(Dispatchers.Default).launch {
        for ((i, j) in next) wv.computedMap[i][j] = DeferredTile( async { computeCharacter(i, j) } )
        next.clear()
    }

    private fun computeCharacter(i: Int, j: Int): UnicodeCharacter? {
        val dims : DoubleArray = tileOdds(i, j)
        var roll = Random.nextDouble()
        for(si in 0 until SZ) {
            if(roll < dims[si]) return progress.randUnseenInScript(si)
            else roll -= dims[si]
        }
        return null
    }

    private fun tileOdds(newX : Int, newY : Int) : DoubleArray {
        val baseRate = ODDS_BASE_CHARACTER_PREVALENCE / Universe.allScripts.size
        val odds = DoubleArray(SZ) { i ->
            baseRate + ODDS_INVENTORY[InventorySlot.scriptCount[i]]
        }
        forLocalMap { mapX, mapY, c ->
            if(c != null) {
                val dx = newX - mapX
                val dy = newY - mapY
                val d = sqrt(0.0 + dx * dx + dy * dy)
                odds[c.scriptIndex()] +=
                    ODDS_PARAMETER_SAME_SCRIPT_NEAR_CHARACTER - d * ODDS_LINEAR_DIMINISH_BY_DISTANCE
            }
        }
        odds.sum().also { if(it > 1.0) for(i in odds.indices) odds[i] /= it }
        return odds
    }

    private val extendedRange = 1..SIDE_LENGTH_EXTENDED
    private inline fun forLocalMap(f : (Int, Int, UnicodeCharacter?) -> Unit){
        for(i in extendedRange) for(j in extendedRange) f(i, j, wv.computedMap[i][j].character)
    }

    fun sleepScriptDims() : DoubleArray = tileOdds(EXTENDED_MAP_SIZE/2, EXTENDED_MAP_SIZE/2)
}