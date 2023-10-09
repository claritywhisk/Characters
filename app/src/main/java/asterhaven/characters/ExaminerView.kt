package asterhaven.characters

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import asterhaven.characters.typeface.FontFallback

@SuppressLint("RestrictedApi")
class ExaminerView(context: Context, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatTextView(context, attrs), DragListener, DragStarter {
    val desc by lazy { (parent as ViewGroup).findViewById<TextView>(R.id.description) }
    val hex by lazy { (parent as ViewGroup).findViewById<TextView>(R.id.hex) }
    override var occupant : UnicodeCharacter? = null
        set(value) {
            if(value != null) typeface = FontFallback.Font.values()[value.fontIndex].getTypeface()
            text = value?.asString ?: ""
            desc.text = value?.description() ?: ""
            hex.text = value?.hex() ?: ""
            field = value
        }
    override val dragShadowSize by lazy {
        textSize * SHRINK_FACTOR
    }
    override fun destination() = this
    init {
        setOnDragListener()
    }
    private val gestureDetector = GestureApparatus.gestureDetectorFor(this)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if ((context as MainActivity).inventoryDeleteConfirmation != null) return false
        return true //https://stackoverflow.com/a/23725322/2563422
    }
}
    /*companion object {
        const val FRACTION_NON_LABEL = 3f / 4f
    }
    override var occupant : UnicodeCharacter? = null
    override var formerOccupantSentToDrag: UnicodeCharacter? = null
    override fun destination() = this
    init {
        setOnDragListener()
    }
    override val dragShadowSize
        get() = textSize * SHRINK_FACTOR

    var textSize = 0f
    private val hexLabelTextSize by lazy {
        SCALE_TEXT2TILE * SCALE_TEXT2TILE * height * (1f - FRACTION_NON_LABEL)
    }
    private val paints by lazy {
        val p = Paint()
        p.textAlign = Paint.Align.CENTER
        p.textSize = glyAreaHeight
        FontFallback.paints(p)
    }
    private val hexLabelPaint by lazy {
        val p = Paint()
        p.textSize = hexLabelTextSize
        p.textAlign = Paint.Align.CENTER
        p
    }
    private var midX = 0f
    private var midGlyY = 0f
    private var hexBaselineY = 0f
    private var glyAreaHeight = 0f
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        midX = w / 2f
        val midHexY = h * (FRACTION_NON_LABEL + 1f) / 2f
        hexBaselineY = midHexY - (hexLabelPaint.descent() + hexLabelPaint.ascent()) / 2
        midGlyY = h * FRACTION_NON_LABEL / 2f
        glyAreaHeight = h * FRACTION_NON_LABEL
        logToTextView("exam area $w $h", this)
    }
    private var boundsRect = Rect()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        occupant?.let { occ ->
            canvas?.let { can ->
                can.drawText(occ.hex(), midX, hexBaselineY, hexLabelPaint)
                val paint = paints[occ.fontIndex]
                paint.getTextBounds(occ.asString,0, occ.asString.length, boundsRect)
                logToTextView("$glyAreaHeight ${boundsRect.height()}", this)
                logToTextView("$width ${boundsRect.width()}", this)
                val rat = min(
                    glyAreaHeight / boundsRect.height().toFloat(),
                    width / boundsRect.width().toFloat()
                )
                paint.textSize *= rat
                paint.getTextBounds(occ.asString,0, occ.asString.length, boundsRect)
                logToTextView("${boundsRect.height()}", this)
                logToTextView("${boundsRect.width()}", this)

                textSize = paint.textSize

                drawCharacter(occ, paint, can, midX, midGlyY)
            }
        }
    }*/