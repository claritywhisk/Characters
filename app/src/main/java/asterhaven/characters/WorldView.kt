package asterhaven.characters

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import asterhaven.characters.typeface.FontFallback
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.system.measureTimeMillis

const val EXTENDED_MAP_SIZE = SIDE_LENGTH + 4 //todo?
const val SIDE_LENGTH_EXTENDED = SIDE_LENGTH + 2
private val LOCAL_RANGE = 1..SIDE_LENGTH_EXTENDED
private val VISIBLE_RANGE = 2..(SIDE_LENGTH + 1)

class WorldView(context: Context?, attrs: AttributeSet?) : View(context, attrs), DragStarter {
    companion object { init { require(SIDE_LENGTH % 2 == 1) } }//todo

    private val walk = Walk(this) //contains px offsets from current center
    private val movement by Movement

    override val dragShadowSize by lazy {
        ENLARGE_FACTOR * SCALE_TEXT2TILE * tileWidthPx
    }

    private val paints by lazy {
        val p = Paint()
        p.color = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)
        p.textAlign = Paint.Align.CENTER
        FontFallback.paints(p)
    }
    private val selectedLocs = LinkedList<Tile>()
    private val selectedLocColor = LinkedList<ValueAnimator>()

    lateinit var computedMap : Array<Array<Tile>>

    private var updateJob : Job? = null

    //cross into a tile
    fun adjustCenter(dx : Int, dy : Int){
        if(updateJob?.isActive == true){
            val t : Long
            runBlocking {
                t = measureTimeMillis { updateJob?.join() }
            }
            if(BuildConfig.DEBUG) logToTextView("debug: behind by $t ms", this@WorldView)
        }
        val progress = (context as MainActivity).progress
        //shift map. use temp in (theoretical) case it's still computing its edges
        val range = 0 until EXTENDED_MAP_SIZE
        val mapTemp = Array(EXTENDED_MAP_SIZE) { i ->
            Array<Tile>(EXTENDED_MAP_SIZE) { j ->
                val x = i + dx
                val y = j + dy
                when (x in range && y in range) {
                    true -> computedMap[x][y].also { it ->
                        it.character?.let { progress.doNotUnspawn(it) }
                    }
                    false -> {
                        movement.requisition(i, j) //Movement will set this coordinate
                        Tile() //dummy return value
                    }
                }
            }
        }
        progress.unspawnRemaining()
        //with the new map, work can begin on the new coordinates
        computedMap = mapTemp
        movement.startUpdate()
    }

    fun outerComputedMapCoords() = Array<Pair<Int, Int>>(4 * (EXTENDED_MAP_SIZE - 1)){ i ->
        val s = EXTENDED_MAP_SIZE
        when {
            i < s     -> Pair(i, 0)
            i < s * 2 -> Pair(s - 1, i - s)
            i < s * 3 -> Pair(s - 1 - i % s, s - 1)
            else      -> Pair(0, s - 1 - i % s)
        }
    }

    private var tileWidthPx = 0f
        set(v) {
            halfTileWidthPx = v / 2f
            field = v
        }
    private var halfTileWidthPx = 0f
    private var offsetPx = 0f //gap side of screen
    //private var scaleOffsetPx = 0f //gap from shrunk centered text

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if(!::computedMap.isInitialized) return
        //tidy any finished selections
        while(selectedLocColor.peek()?.animatedFraction == 1f){
            selectedLocs.poll()
            selectedLocColor.poll()
        }
        //some drawing offscreen or partial glyphs
        var x = -walk.xOffset * tileWidthPx - halfTileWidthPx + offsetPx
        var y : Float
        for (i in LOCAL_RANGE){
            y = -walk.yOffset * tileWidthPx - halfTileWidthPx
            for(j in LOCAL_RANGE){
                val loc = computedMap[i][j]
                if(loc in selectedLocs){//note: uses equals(), overridden
                    val shade = selectedLocColor.elementAt(selectedLocs.indexOf(loc)).animatedValue as Int
                    drawTileColor(canvas, x, y, shade)
                }
                loc.character?.let { c ->
                    val paint = paints[c.fontIndex]
                    canvas?.let { can ->
                        drawCharacter(c, paint, can, x, y)
                        if (i in VISIBLE_RANGE && j in VISIBLE_RANGE)
                                (context as MainActivity).run { progress.see(c, this) }
                    }
                }
                y += tileWidthPx
            }
            x += tileWidthPx
        }
    }
    private val dtcPaint = Paint()
    private fun drawTileColor(canvas: Canvas?, x : Float, y : Float, color : Int){
        dtcPaint.color = color
        canvas?.drawRect(x - halfTileWidthPx, y - halfTileWidthPx,
                        x + halfTileWidthPx, y + halfTileWidthPx, dtcPaint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if(w != h && BuildConfig.DEBUG) logToTextView("debug: unexpected wv size $w $h", this)
        tileWidthPx = w * 1f / SIDE_LENGTH
        offsetPx = (w % tileWidthPx) / 2f
        //scaleOffsetPx = tileWidthPx * (1f - SCALE_TEXT2TILE) / 2f
        paints.forEach {
            it.textSize = (tileWidthPx * SCALE_TEXT2TILE) //.roundToInt().toFloat()
        }
    }
    private val gestureDetector = GestureApparatus.gestureDetectorFor(this)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        //super.onTouchEvent(event) //todo verify ok
        gestureDetector.onTouchEvent(event)
        if((context as MainActivity).inventoryDeleteConfirmation != null) return false
        return true //https://stackoverflow.com/a/23725322/2563422
    }

    fun tileAt(pxX : Float, pxY : Float): IntArray {
        //convert screen position
        val tileX = 2 + ((pxX - offsetPx - walk.xOffset)/tileWidthPx).toInt() //todo verify
        val tileY = 2 + ((pxY - offsetPx - walk.yOffset)/tileWidthPx).toInt()
        return intArrayOf(tileX, tileY)
    }
    fun charAt(pair: IntArray) = computedMap[pair[0]][pair[1]].character

    //@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun walkToTile(pair : IntArray){
        val tileX = pair[0]
        val tileY = pair[1]
        val touchAnimation = ValueAnimator.ofArgb(R.color.selected_map_tile, Color.TRANSPARENT)
        val touched = computedMap[tileX][tileY]
        if(!isAlreadyGoingAnim(touched,touchAnimation)){
            selectedLocs.add(touched)
            selectedLocColor.add(touchAnimation)
        }
        val redrawListener = ValueAnimator.AnimatorUpdateListener { invalidate() }
        touchAnimation.addUpdateListener(redrawListener)
        touchAnimation.duration = SELECTED_TIME
        touchAnimation.start()
        val dTilesX = tileX - EXTENDED_MAP_SIZE/2
        val dTilesY = tileY - EXTENDED_MAP_SIZE/2
        walk.to(dTilesX, dTilesY)
    }
    private fun isAlreadyGoingAnim(tile : Tile, freshAnim : ValueAnimator): Boolean {
        val i = selectedLocs.indexOf(tile)
        if(i == -1) return false
        selectedLocColor[i] = freshAnim
        return true
    }

    fun doInit(progress : Progress){
        Movement.init(this, walk) //like most lines, a must!
        computedMap = Array(EXTENDED_MAP_SIZE) { i ->
            Array(EXTENDED_MAP_SIZE) { j ->
                when {
                    i in LOCAL_RANGE && j in LOCAL_RANGE -> {
                        //random initial tile
                        var maxsi = -1
                        var max = 0.0
                        for(si in 0 until Universe.allScripts.size) {
                            rRandom.nextDouble().let {
                                if(it < CHAOS) if(it >= max) {
                                    maxsi = si
                                    max = it
                                }
                            }
                        }
                        if(maxsi == -1) Tile() else Tile(progress.spawnRandUnspawnedInScript(maxsi))
                    }
                    else -> {
                        movement.requisition(i, j)
                        Tile()
                    }
                }
            }
        }
        invalidate()
        movement.startUpdate()
    }
}