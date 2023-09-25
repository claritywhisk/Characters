package asterhaven.characters

import android.graphics.Color
import android.view.DragEvent
import android.view.View
import androidx.core.content.ContextCompat

interface DragListener {
    companion object {
        var beingDragged : UnicodeCharacter? = null //a little suspicious but fine
    }

    //abstract properties
    var occupant : UnicodeCharacter?
    fun destination() : DragListener?
    //var formerOccupantSentToDrag : UnicodeCharacter?
    //@RequiresApi(Build.VERSION_CODES.N)
    fun setOnDragListener() {
        (this as View).setOnDragListener { v, event ->
            if (BuildConfig.DEBUG && v != this) {
                error("Assertion failed")
            }
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    //register for more events
                    true
                }
                DragEvent.ACTION_DROP -> {
                    var consume = true
                    val dropped = beingDragged//event.clipData.getItemAt(0).text.toString()
                    beingDragged = null
                    if (this.occupant == null) this.occupant = dropped
                    else {
                        val n = destination()
                        if (n == null) {
                            println("Inventory Full")
                            consume = false
                        } else {
                            n.occupant = dropped
                        }
                    }
                    consume
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    /*if(formerOccupantSentToDrag != null) {
                        if (event.result) formerOccupantSentToDrag = null
                        else occupant = formerOccupantSentToDrag
                    }*/
                    true
                }
                else -> {
                    false
                }
            }
        }
    }
}