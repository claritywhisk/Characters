package asterhaven.characters

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import asterhaven.characters.typeface.FontFallback
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlin.system.measureTimeMillis

const val EXTENDED_MAP_SIZE = SIDE_LENGTH + 4
const val SIDE_LENGTH_EXTENDED = SIDE_LENGTH + 2
private val LOCAL_RANGE = 1..SIDE_LENGTH_EXTENDED
private val VISIBLE_RANGE = 2..(SIDE_LENGTH + 1)

class WorldView(context: Context?, attrs: AttributeSet?) : CharactersView(context, attrs) {
    companion object { init { require(SIDE_LENGTH % 2 == 1) } }//todo

    private val walk = Walk(this) //contains px offsets from current center
    lateinit var movement : Movement

    override val dragShadowSize by lazy {
        ENLARGE_FACTOR * SCALE_TEXT2TILE * tileWidthPx
    }

    private val paints by lazy {
        val p = Paint()
        p.textAlign = Paint.Align.CENTER
        FontFallback.paints(p)
    }
    private val selectedLocs = LinkedList<Coordinate>()
    private val selectedLocColor = LinkedList<ValueAnimator>()

    lateinit var computedMap : Array<Array<CoordinateWrapper>>

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
        //shift map. use temp in (theoretical) case it's still computing its edges
        val range = 0 until EXTENDED_MAP_SIZE
        val mapTemp = Array(EXTENDED_MAP_SIZE) { i ->
            Array<CoordinateWrapper>(EXTENDED_MAP_SIZE) { j ->
                val x = i + dx
                val y = j + dy
                when (x in range && y in range) {
                    true -> computedMap[x][y].coordinate()
                    false -> {
                        movement.requisition(i, j) //Movement will set this coordinate
                        Coffin(computedMap[i][j].coordinate()) //dummy return value
                    }
                }
            }
        }
        //with the new map, work can begin on the new coordinates
        computedMap = mapTemp
        updateJob = movement.startUpdate()
        val t = System.currentTimeMillis()
        updateJob!!.invokeOnCompletion {
            //logToTextView("M update "+(System.currentTimeMillis() - t), this) //todo?
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
                val loc = computedMap[i][j].coordinate()
                if(loc in selectedLocs){//note: uses equals(), overridden
                    val shade = selectedLocColor.elementAt(selectedLocs.indexOf(loc)).animatedValue as Int
                    drawTileColor(canvas, x, y, shade)
                }
                when(loc.terrain){
                    Terrain.CLOUD ->
                        drawTileColor(canvas, x, y, R.color.gray_400)
                    null -> loc.unicodeCharacter?.let { c ->
                        val paint = paints[c.fontIndex]
                        canvas?.let { can ->
                            drawCharacter(c, paint, can, x, y)
                            if (i in VISIBLE_RANGE && j in VISIBLE_RANGE) see(c)
                        }
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

    private val gestureDetector = GestureApparatus.forWV(getContext(),this)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true //https://stackoverflow.com/a/23725322/2563422
    }

    fun tileAt(pxX : Float, pxY : Float): IntArray {
        //convert screen position
        val tileX = 2 + ((pxX - offsetPx - walk.xOffset)/tileWidthPx).toInt() //todo verify
        val tileY = 2 + ((pxY - offsetPx - walk.yOffset)/tileWidthPx).toInt()
        return intArrayOf(tileX, tileY)
    }
    fun coordinateAt(pair: IntArray) = computedMap[pair[0]][pair[1]].coordinate()
    fun charAt(pair: IntArray) : UnicodeCharacter? = coordinateAt(pair).unicodeCharacter

    //@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun walkToTile(pair : IntArray){
        val tileX = pair[0]
        val tileY = pair[1]
        val touchAnimation = ValueAnimator.ofArgb(R.color.selected_map_tile, Color.TRANSPARENT)
        val touched = computedMap[tileX][tileY].coordinate()
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
    private fun isAlreadyGoingAnim(coord : Coordinate, freshAnim : ValueAnimator): Boolean {
        val i = selectedLocs.indexOf(coord)
        if(i == -1) return false
        selectedLocColor[i] = freshAnim
        return true
    }

    fun doInit(){
        movement = Movement(this) //like most lines, a must!
        val c = randomBreathableCoordinateForCenterTEST()
        computedMap = Array(EXTENDED_MAP_SIZE) { i ->
            Array(EXTENDED_MAP_SIZE) { j ->
                when {
                    i in LOCAL_RANGE && j in LOCAL_RANGE -> {
                        //println("made$i$j")
                        Coordinate.create(c.scriptDims)
                    }
                    else -> {
                        movement.requisition(i, j)
                        Coffin(c)
                    }
                }
            }
        }
        invalidate()
        movement.startUpdate()
    }
}