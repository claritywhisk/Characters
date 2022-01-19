package asterhaven.characters

import android.content.Context
import android.util.AttributeSet
import android.view.View
import asterhaven.characters.unicodescript.UnicodeScript

class Scroll(context: Context?, attrs: AttributeSet?) : View(context, attrs), DragListener {
    var scriptPursued : UnicodeScript? = null
    var level = 0
    var remaining = 1
    private val chars = ArrayList<UnicodeCharacter>()
    override var occupant: UnicodeCharacter? = null
        set(value) {
            if(value == null) return
            //if(level == 9000)logToTextView("Scroll maxed")

            else if(value.script != scriptPursued && level != 0) {
                    logToTextView("Did not match", this)
                    //tod
            }
            else {
                if(chars.isEmpty()) scriptPursued = value.script
                chars.add(value)
                if(--remaining == 0){
                    remaining = ++level + 1
                    logToTextView("Scroll level $level", this)
                    //animateBackground() //tod and image
                }
            }
            field = null
        }
    override var destination: DragListener? = this
    override var formerOccupantSentToDrag: UnicodeCharacter? = null
    init{
        setOnDragListener()
    }
}