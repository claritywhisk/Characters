package asterhaven.characters

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.view.View

interface DragStarter {
    companion object {
        const val ENLARGE_FACTOR = 2.2f
        const val SHRINK_FACTOR = .8f
        var beingDragged : UnicodeCharacter? = null //a little suspicious but fine
    }
    fun dragShadowSize() : Float
    fun dragPaints() : Array<Paint>

    //@RequiresApi(Build.VERSION_CODES.N)
    fun startDragAndDrop(selected : UnicodeCharacter){
        beingDragged = selected
        (this as View).startDragAndDrop(null,//ClipData.newPlainText("", selected.asString)
            object : View.DragShadowBuilder(){
                override fun onDrawShadow(canvas: Canvas?) {
                    val paint = dragPaints()[selected.fontIndex]
                    canvas?.let { (this as CharactersView).drawCharacter(selected, paint, canvas) }
                    //canvas?.drawText(selected.asString,0f, dragShadowSize(), paint)
                }
                override fun onProvideShadowMetrics(
                    outShadowSize: Point?,
                    outShadowTouchPoint: Point?
                ) {
                    outShadowSize?.x = dragShadowSize().toInt()
                    outShadowSize?.y = dragShadowSize().toInt()
                    outShadowTouchPoint?.x = (dragShadowSize() / 2).toInt()
                    outShadowTouchPoint?.y = (dragShadowSize() / 2).toInt()
                }
            },
            null,
            0
        )
    }

}