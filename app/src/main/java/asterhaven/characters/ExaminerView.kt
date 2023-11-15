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
class ExaminerView(context: Context, attrs: AttributeSet?) : androidx.constraintlayout.widget.ConstraintLayout(context, attrs), DragListener, DragStarter {
    private val ex : TextView by lazy { findViewById(R.id.ex) }
    private val desc : TextView by lazy { findViewById(R.id.description) }
    private val hex : TextView by lazy { findViewById(R.id.hex) }
    override var occupant : UnicodeCharacter? = null
        set(value) {
            if(value != null){
                (context as MainActivity).logToTextView(value.toString())
                ex.typeface = FontFallback.Font.values()[value.fontIndex].getTypeface()
            }
            ex.text = value?.asString ?: ""
            desc.isSelected = false
            desc.postDelayed({
                desc.isSelected = true
            }, EXAMINER_MARQUEE_DELAY.toLong())
            desc.text = value?.description() ?: ""
            hex.text = value?.hex() ?: ""
            field = value
        }
    override val dragShadowSize by lazy {
        ex.textSize * SHRINK_FACTOR
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