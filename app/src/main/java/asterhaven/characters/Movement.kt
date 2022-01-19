package asterhaven.characters

import kotlinx.coroutines.*
import kotlin.math.min

class Movement(private val wv : WorldView) {
    val SZ = Universe.allScripts.size
    val OVER_SZ = 1.0 / SZ
    val LOCAL_MAP_TILES = SIDE_LENGTH_EXTENDED * SIDE_LENGTH_EXTENDED

    private val next: MutableList<Pair<Int, Int>> = ArrayList(EXTENDED_MAP_SIZE * 2)

    fun requisition(i : Int, j : Int) = next.add(Pair(i, j))

    //call async for each new coordinate
    fun startUpdate() = CoroutineScope(Dispatchers.Default).launch {
        findAverageCoordinate()
        for ((i, j) in next) wv.computedMap[i][j] = DeferredCo( async { computeCoordinate(i, j) })
        next.clear()
    }

    //for universe contribution //todo verify that's what I want (d...)
    private val averageCoordinate = DoubleArray(SZ)
    private fun findAverageCoordinate() {
        forScripts { si -> averageCoordinate[si] = 0.0 }
        forLocalMap { _, _, c ->
            forScripts { si -> averageCoordinate[si] += c.scriptDims[si] }
        }
        forScripts { si -> averageCoordinate[si] /= LOCAL_MAP_TILES.toDouble() }
        logToTextView("sigma " + "%.2f".format(averageCoordinate.sum()), wv)
    }

    private fun computeCoordinate(i: Int, j: Int): Coordinate {
        val dims = tileWish(i, j)
        forScripts { si ->
            dims[si] *= PROPORTION_TILE
            dims[si] += OVER_SZ * PROPORTION_INTENT
            dims[si] += averageCoordinate[si] * PROPORTION_UNIVERSE
        }
        return Coordinate.create(dims)
    }

    private fun tileWish(newX : Int, newY : Int) : DoubleArray {
        val sum = DoubleArray(SZ) //sum of dimensions before normalization
        var sum1OverDSquared = 0.0
        fun intoSum(weight : Double, f : (Int) -> Double) {
            for(i in sum.indices) sum[i] += weight * f(i)
        }
        forLocalMap { mapX, mapY, c ->
            val overDSqr = inverseDistanceSquared(newX, newY, mapX, mapY)
            sum1OverDSquared += overDSqr
            when {
                c.unicodeCharacter != null -> {
                    val iScript = c.unicodeCharacter.scriptIndex()
                    intoSum(overDSqr) { i ->
                        if (i == iScript) 1.0 else 0.0
                    }
                }
                c.terrain == null -> {
                    //blanks want zero (1 - sigmaDims)% and status quo sigmaDims%
                    val fractionKeep = min(c.sigmaDims, BLANK_QUO_CAP)
                    intoSum(overDSqr) { i ->
                        c.scriptDims[i] * fractionKeep
                    }
                }
                c.terrain == Terrain.CLOUD -> {
                    intoSum(overDSqr) { _ ->
                        OVER_SZ
                    }
                }
            }
        }
        for(i in sum.indices) sum[i] /= sum1OverDSquared
        return sum
    }

    /*//important part todo consider
    private inline fun curb(coord : Double, max : Double, wish : () -> Double) : Double {
        val w = wish()
        val d = w - coord
        return when {
            (abs(d) <= max) -> w
            else            -> coord + if(d > 0) max else -max
        }
    }*/

    private fun inverseDistanceSquared(x1 : Int, y1 : Int, x2 : Int, y2 : Int) : Double {
        val dx = x2 - x1
        val dy = y2 - y1
        val dSqr = dx * dx + dy * dy
        return 1.0 / dSqr
    }

    private inline fun forScripts(f : (si : Int) -> Unit) { for(si in 0 until SZ) f(si) }

    private val extendedRange = 1..SIDE_LENGTH_EXTENDED
    private inline fun forLocalMap(f : (Int, Int, Coordinate) -> Unit){
        for(i in extendedRange) for(j in extendedRange) f(i, j, wv.computedMap[i][j].coordinate())
    }
}