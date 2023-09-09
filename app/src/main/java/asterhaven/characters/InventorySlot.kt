package asterhaven.characters

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat

//@RequiresApi(Build.VERSION_CODES.N)
class InventorySlot(context: Context?, attrs: AttributeSet?) : CharactersView(context, attrs), DragListener {
    companion object Inventory {
        val scriptCount by lazy { IntArray(Universe.allScripts.size) }
        private val inventory = ArrayList<UnicodeCharacter>()
        private val allSlots = ArrayList<InventorySlot>()
        private val nextOpenSlot : InventorySlot?
            get() {
                return allSlots.firstOrNull { slot -> slot.occupant == null }
            }
        private var trashCanDrawable: Drawable? = null
        fun clearAll() = allSlots.forEach { it.occupant = null }
    }
    init {
        allSlots.add(this)
        setOnDragListener()
        if(trashCanDrawable == null && context != null){
            trashCanDrawable = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_delete)
        }
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
                null -> {
                    field?.let {
                        inventory.remove(it)
                        scriptCount[it.scriptIndex()]--
                    }
                }
                else -> {
                    if(inventory.contains(s)) return
                    else {
                        inventory.add(s)
                        val x = ++scriptCount[s.scriptIndex()]
                        if(inventory.size == allSlots.size){
                            if(x == allSlots.size) {
                                (context as MainActivity).inventoryMatched(s.script)
                            }
                            else {
                                logToTextView("Mixture", this)

                            }
                        }
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
        canvas?.let {
            if(confirmDelete.isOpen()){
                trashCanDrawable?.setBounds(0, 0, width, height)
                trashCanDrawable?.draw(canvas)
            }
            else {
                val occ = occupant ?: return //capture occupant (for thread safety)
                val p = dragPaints[occ.fontIndex]
                p.textSize = textSize
                drawCharacter(occ, p, canvas)
            }
        }
    }

    private val gestureDetector = GestureApparatus.forInventorySlot(getContext(),this)
    override fun onTouchEvent(event: MotionEvent): Boolean {//todo verify ok
        gestureDetector.onTouchEvent(event)
        if((context as MainActivity).inventoryDeleteConfirmation != null) return false
        return true //https://stackoverflow.com/a/23725322/2563422
    }

    inner class ConfirmDeleteStatus {
        private var open = false
        private var action = false
        fun initiate(){
            if(occupant == null) return
            open = true
            action = false
            invalidate()
            (context as MainActivity).inventoryDeleteConfirmation = this
        }
        fun didRespond() {
            action = true
            (context as MainActivity).inventoryDeleteConfirmation = null
            postInvalidateDelayed(100) //todo
        }
        fun isOpen() = !action && open
        fun slotTapped() {
            if(open) {
                occupant = null
                open = false
                action = false
                (context as MainActivity).inventoryDeleteConfirmation = null
                invalidate()
                logToTextView("deleted", this@InventorySlot)
            }
        }
    }
    val confirmDelete = ConfirmDeleteStatus()
}