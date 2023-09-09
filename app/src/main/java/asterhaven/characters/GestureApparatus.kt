package asterhaven.characters

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.FrameLayout
import android.widget.ImageView

object GestureApparatus {
    fun forWV(app: Context, worldView: WorldView): GestureDetector {
        return GestureDetector(app, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                super.onSingleTapUp(e)
                if (e != null){
                    val xy = worldView.tileAt(e.x, e.y)
                    worldView.walkToTile(xy)
                    logToTextView("tapped " + worldView.charAt(xy), worldView)
                    val char = worldView.charAt(xy)
                    if(char != null) logToTextView(char.toString(), worldView)
                }
                return true //consume event
            }

            //@RequiresApi(Build.VERSION_CODES.N)
            override fun onShowPress(e: MotionEvent?) {
                super.onShowPress(e)
                if (e == null) return
                val locs = IntArray(2)
                worldView.getLocationOnScreen(locs)
                val selected: UnicodeCharacter = worldView.charAt(worldView.tileAt(e.x, e.y)) ?: return
                worldView.startDragAndDrop(selected)
                //println("Picked up "+ selected + " ( "+Integer.toHexString(selected.asString.codePoints().toArray()[0])+" )")
            }
            //override fun onDown
            //override fun onLongPress
            //override fun onScroll
            //override fun onFling
            //override fun onSingleTapConfirmed
            //override fun onDoubleTap
            //override fun onDoubleTapEvent
            //override fun onContextClick
        })
    }

    fun forInventorySlot(app: Context, inventorySlot: InventorySlot): GestureDetector {
        return GestureDetector(app, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                inventorySlot.confirmDelete.slotTapped()
                return true
            }
            override fun onLongPress(e: MotionEvent?) {
                super.onLongPress(e)
                inventorySlot.confirmDelete.initiate()
            }
        })
    }
}