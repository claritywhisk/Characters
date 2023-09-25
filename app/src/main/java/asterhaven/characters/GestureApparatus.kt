package asterhaven.characters

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

object GestureApparatus {
    fun gestureDetectorFor(v : View) = GestureDetector(v.context, when(v){
        is WorldView -> object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                super.onSingleTapUp(e)
                if (e != null) {
                    val xy = v.tileAt(e.x, e.y)
                    v.walkToTile(xy)
                    v.charAt(xy)?.let {
                        logToTextView("tapped " + it, v)
                    }
                    val char = v.charAt(xy)
                    if (char != null) logToTextView(char.toString(), v)
                }
                return true //consume event
            }

            //@RequiresApi(Build.VERSION_CODES.N)
            override fun onShowPress(e: MotionEvent?) {
                super.onShowPress(e)
                if (e == null) return
                val locs = IntArray(2)
                v.getLocationOnScreen(locs)
                val selected: UnicodeCharacter =
                    v.charAt(v.tileAt(e.x, e.y)) ?: return
                v.startDragAndDrop(selected)
            }
        }
        is InventorySlot -> object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }
            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                v.confirmDelete.slotTapped()
                return true
            }
            override fun onLongPress(e: MotionEvent?) {
                super.onLongPress(e)
                v.confirmDelete.initiate()
            }
        }
        is ExaminerView -> object : GestureDetector.SimpleOnGestureListener() {
            override fun onShowPress(e: MotionEvent?) {
                super.onShowPress(e)
                v.occupant?.let { v.startDragAndDrop(it) }
            }

            override fun onLongPress(e: MotionEvent?) {
                super.onLongPress(e)
                v.occupant?.let {
                    DragListener.beingDragged = null
                    val clipboard =
                        v.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip: ClipData = ClipData.newPlainText("Unicode character", it.asString)
                    clipboard.setPrimaryClip(clip)
                    if(true){//(Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2){//todo auto feedback after 31? S_V2
                        Toast.makeText(v.context, "Copied ${it.asString} to clipboard!", Toast.LENGTH_SHORT).run {
                            setGravity(Gravity.TOP, 0, 0)
                            show()
                        }
                    }
                }
            }
        }
        else -> {
            throw UnsupportedOperationException()
        }
    })
}