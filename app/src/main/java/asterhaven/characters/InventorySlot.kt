package asterhaven.characters

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent

//@RequiresApi(Build.VERSION_CODES.N)
class InventorySlot(context: Context?, attrs: AttributeSet?) : CharactersView(context, attrs), DragListener {
    companion object {
        private val inventory = ArrayList<UnicodeCharacter>()
        val allSlots = ArrayList<InventorySlot>()
        val nextOpenSlot : InventorySlot?
            get() {
                return allSlots.firstOrNull { slot -> slot.occupant == null }
            }
    }
    init {
        allSlots.add(this)
        setOnDragListener()
    }

    private val textSize by lazy {
        width * SCALE_TEXT2TILE
    }

    override val dragShadowSize by lazy {
        textSize * SHRINK_FACTOR
    }
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
    override fun destination() : DragListener? =
        if(occupant == null) this
        else nextOpenSlot

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val occ = occupant ?: return //capture occupant (for thread safety)
        val p = dragPaints[occ.fontIndex]
        p.textSize = textSize
        canvas?.let { drawCharacter(occ, p, canvas) }
    }

    private val gestureDetector = GestureApparatus.forInventorySlot(getContext(),this)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return true //https://stackoverflow.com/a/23725322/2563422
    }
}