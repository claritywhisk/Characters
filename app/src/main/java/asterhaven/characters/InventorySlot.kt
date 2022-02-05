package asterhaven.characters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import asterhaven.characters.typeface.FontFallback
import kotlin.math.roundToInt

//@RequiresApi(Build.VERSION_CODES.N)
open class InventorySlot(context: Context?, attrs: AttributeSet?) : CharactersView(context, attrs), DragListener, DragStarter {
    companion object {
        private val inventory = ArrayList<UnicodeCharacter>()
        val allSlots = ArrayList<InventorySlot>()
        val nextOpenSlot : InventorySlot?
            get() {
                return allSlots.firstOrNull { slot -> slot.occupant == null }
            }
        var invSlotDragShadowSize = 0f
        var invSlotTextSize : Float? = null
        val invSlotPaints by lazy {
            val dsp = Paint()
            dsp.textAlign = Paint.Align.CENTER
            if(invSlotTextSize == null) {
                if (BuildConfig.DEBUG) require(false)
            }
            else dsp.textSize = invSlotTextSize as Float
            FontFallback.Static.paints(dsp)
        }
    }
    init {
        allSlots.add(this)
        setOnDragListener()
    }

    override fun dragShadowSize() = invSlotDragShadowSize
    override fun dragPaints() = invSlotPaints
    override var formerOccupantSentToDrag: UnicodeCharacter? = null
    override var occupant : UnicodeCharacter? = null
        set(s) {
            when(s) {
                null -> inventory.remove(field)
                else -> {
                    if(inventory.contains(s)) return
                    else {
                        inventory.add(s)
                        logToTextView(s.toString(), this)
                    }
                }
            }
            field = s
            invalidate()
        }
    override var destination : DragListener? = this
        get() {
            if(occupant == null) return this
            else return nextOpenSlot
        }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val occ = occupant ?: return //capture occupant (for thread safety)
        val p = dragPaints()[occ.fontIndex]
        canvas?.let { drawCharacter(occ, p, canvas) }
        //canvas?.drawText(occ.asString, scaleOffsetPx,canvas.height - p.descent() - scaleOffsetPx, p)
    }

    var scaleOffsetPx = 0f
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scaleOffsetPx = w * (1f - SCALE_TEXT2TILE)/2f
        invSlotTextSize = (w * SCALE_TEXT2TILE).roundToInt().toFloat()
        invSlotDragShadowSize = w * DragStarter.SHRINK_FACTOR * SCALE_TEXT2TILE
    }

    val gestureDetector = GestureApparatus.forInventorySlot(getContext(),this)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true //https://stackoverflow.com/a/23725322/2563422
    }
}