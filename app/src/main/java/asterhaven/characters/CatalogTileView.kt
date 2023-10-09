package asterhaven.characters

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

class CatalogTileView(context: Context, attrs: AttributeSet?) : androidx.appcompat.widget.AppCompatTextView(context, attrs), DragStarter {
    var occupant : UnicodeCharacter? = null
    override val dragShadowSize by lazy {
        textSize
    }
    private val gestureDetector = GestureApparatus.gestureDetectorFor(this)
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        if ((context as MainActivity).inventoryDeleteConfirmation != null) return false
        return true
    }
}