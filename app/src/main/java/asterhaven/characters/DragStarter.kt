package asterhaven.characters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.View
import asterhaven.characters.typeface.FontFallback
import com.google.android.material.color.MaterialColors

interface DragStarter {
    val dragShadowSize : Float

    companion object{
        lateinit var dragPaints : Array<Paint>
        fun init(c : Context){
            dragPaints = FontFallback.paints(Paint().also {
                it.color = MaterialColors.getColor(c, com.google.android.material.R.attr.colorOnPrimary, Color.TRANSPARENT)
                it.textAlign = Paint.Align.CENTER
                //size is dragShadowSize of the particular view
            })
        }
    }

    fun startDragAndDrop(selected : UnicodeCharacter){
        DragListener.beingDragged = selected
        (this as View).startDragAndDrop(null,//ClipData.newPlainText("", selected.asString)
            object : View.DragShadowBuilder(){
                override fun onDrawShadow(canvas: Canvas?) {
                    val paint = dragPaints[selected.fontIndex]
                    paint.textSize = dragShadowSize
                    canvas?.let {  drawCharacter(selected, paint, canvas) }
                }
                override fun onProvideShadowMetrics(
                    outShadowSize: Point?,
                    outShadowTouchPoint: Point?
                ) {
                    outShadowSize?.x = dragShadowSize.toInt()
                    outShadowSize?.y = dragShadowSize.toInt()
                    outShadowTouchPoint?.x = (dragShadowSize / 2f).toInt()
                    outShadowTouchPoint?.y = (dragShadowSize / 2f).toInt()
                }
            },
            null,
            0
        )
    }
}