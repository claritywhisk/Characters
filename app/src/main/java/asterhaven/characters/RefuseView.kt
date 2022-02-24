package asterhaven.characters

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView

class RefuseView(context: Context?, attrs: AttributeSet?) : AppCompatImageView(context!!, attrs), DragListener {
    override var occupant : UnicodeCharacter? = null
        set(_) {} //infinitely empty recycle bin
    override var formerOccupantSentToDrag: UnicodeCharacter? = null //DragListener had bad design
    override fun destination() = this
    init {
        this.setOnDragListener()
    }
}