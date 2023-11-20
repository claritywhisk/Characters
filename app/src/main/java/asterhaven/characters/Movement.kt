package asterhaven.characters

import kotlinx.coroutines.*
import kotlin.math.sqrt
import kotlin.reflect.KProperty

class Movement() {
    companion object {
        private lateinit var wv : WorldView
        private lateinit var walk : Walk
        private val progress by Progress
        private val movement by lazy { Movement() }
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Movement = movement
        fun init(wv : WorldView, w : Walk){
            check(!::wv.isInitialized)
            this.wv = wv
            this.walk = w
        }
    }
    val SZ = Universe.allScripts.size
    //val LOCAL_MAP_TILES = SIDE_LENGTH_EXTENDED * SIDE_LENGTH_EXTENDED
    val center = SIDE_LENGTH_EXTENDED / 2

    private val next: MutableList<Pair<Int, Int>> = ArrayList(EXTENDED_MAP_SIZE * 2)

    fun requisition(i : Int, j : Int) = next.add(Pair(i, j))

    //call async for each new coordinate
    private var updateJob : Job? = null
    fun startUpdate() {
        updateJob = CoroutineScope(Dispatchers.Default).launch {
            next.shuffle()
            for ((i, j) in next) wv.computedMap[i][j] = DeferredTile( async { computeCharacter(i, j) } ) //TODO java.util.ConcurrentModificationException - debugger??
            next.clear()
        }
    }

    private fun computeCharacter(i: Int, j: Int): UnicodeCharacter? {
        val dims : DoubleArray = tileOdds(i, j)
        var roll = rRandom.nextDouble()
        for(si in 0 until SZ) {
            if(roll < dims[si]) return progress.spawnRandUnspawnedInScript(si)
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
                val contrib = ODDS_PARAMETER_SAME_SCRIPT_NEAR_CHARACTER - d * ODDS_LINEAR_DIMINISH_BY_DISTANCE
                if(contrib > 0.0) odds[c.scriptIndex()] += contrib
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

    fun inventoryChange(){
        if(walk.stopped){
            runBlocking {
                updateJob?.join()
            }
            //initiate respawn of map edges according to inventory probability
            wv.outerComputedMapCoords().forEach {
                wv.computedMap[it.first][it.second].let { tile ->
                    if(tile is DeferredTile) tile.cancel = true
                }
                movement.requisition(it.first, it.second)
            }
            progress.unspawnRemaining()
            startUpdate()
        }
    }
}