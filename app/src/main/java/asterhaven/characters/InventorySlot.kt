package asterhaven.characters

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.ContextCompat

class InventorySlot(context: Context?, attrs: AttributeSet?) : CharactersView(context, attrs), DragListener {
    companion object Inventory {
        val scriptCount by lazy { IntArray(Universe.allScripts.size) }
        private val inventory = ArrayList<UnicodeCharacter>()
        private val allSlots = ArrayList<InventorySlot>()
        private val nextOpenSlot : InventorySlot?
            get() = allSlots.firstOrNull { slot -> slot.occupant == null }
        lateinit var trashCanDrawable: Drawable
        lateinit var matchColors: List<Int>
        fun clearAll() = allSlots.forEach { it.occupant = null }
        fun init(c : Context){
            trashCanDrawable = ContextCompat.getDrawable(c, android.R.drawable.ic_menu_delete)!!
            matchColors = listOf(
                ContextCompat.getColor(c, R.color.inv_highlight_coral),
                ContextCompat.getColor(c, R.color.inv_highlight_goldenrod),
                ContextCompat.getColor(c, R.color.inv_highlight_magenta),
                ContextCompat.getColor(c, R.color.inv_highlight_turquoise)
            )
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
            if(s == null) {
                field?.let {
                    inventory.remove(it)
                    scriptCount[it.scriptIndex()]--
                }
                field = s
                invalidate()
                return
            }
            else if(inventory.contains(s)) {
                allSlots.first { it.occupant == s }.duplicate_flash.start()
                return
            }
            else {
                logToTextView(s.toString(), this)
                field = s
                invalidate()
                inventory.add(s)
                val x = ++scriptCount[s.scriptIndex()]
                if(inventory.size == allSlots.size){
                    if(x == allSlots.size) {
                        (context as MainActivity).inventoryMatched(s.script)
                        highlight.apply {
                            setIntValues(highlight.animatedValue as Int, Color.TRANSPARENT)
                            start()
                        }
                    }
                    else {
                        val colors = matchColors.shuffled()
                        val scripts = allSlots.map { it.occupant!!.script }.distinct()
                        allSlots.forEach {
                            val color = colors[scripts.indexOf(it.occupant!!.script)]
                            it.highlight.apply {
                                setIntValues(Color.TRANSPARENT, color)
                                start()
                            }
                        }
                    }
                }
            }
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
                canvas.drawColor(highlight.animatedValue as Int)
                canvas.drawColor(duplicate_flash.animatedValue as Int)
                drawCharacter(occ, p, canvas)
            }
        }
    }

    private val gestureDetector = GestureApparatus.forInventorySlot(getContext(),this)
    override fun onTouchEvent(event: MotionEvent): Boolean {//todo verify ok
        gestureDetector.onTouchEvent(event)
        if((context as MainActivity).inventoryDeleteConfirmation != null && !confirmDelete.isOpen()) return false
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

    private val duplicate_flash =
        ValueAnimator.ofArgb(Color.TRANSPARENT, R.color.inv_duplicate_flash, Color.TRANSPARENT).apply {
            addUpdateListener { invalidate() }
            duration = INV_DUP_FLASH_DURATION
        }
    private var highlight = ValueAnimator.ofArgb(Color.TRANSPARENT).apply {
        addUpdateListener { invalidate() }
        duration = INV_HIGHLIGHT_DURATION
    }
}