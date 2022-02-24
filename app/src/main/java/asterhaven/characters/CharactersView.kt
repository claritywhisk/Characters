package asterhaven.characters

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import asterhaven.characters.typeface.FontFallback

abstract class CharactersView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    companion object {
        const val ENLARGE_FACTOR = 2.2f
        const val SHRINK_FACTOR = .8f
    }

    fun see(c : UnicodeCharacter) {
        val x = context as MainActivity
        if(x.progressInitialized()) x.progress.see(c)
    }

    val rect = Rect()
    val paintTest by lazy {
        val p = Paint()
        p.color = Color.CYAN
        p
    }

    fun drawCharacter(c : UnicodeCharacter, paint : Paint, canvas : Canvas) =
        drawCharacter(c, paint, canvas, canvas.width / 2f, canvas.height / 2f)
    fun drawCharacter(c : UnicodeCharacter, paint : Paint, canvas : Canvas, x : Float, y : Float) {
        if(BuildConfig.DEBUG) require(paint.textAlign == Paint.Align.CENTER)
        val trueY = y - (paint.descent() + paint.ascent()) / 2f //todo verify

        paint.getTextBounds(c.asString,0, 1, rect)
        rect.offset(x.toInt() - rect.width()/2, trueY.toInt())
        canvas.drawRect(rect, paintTest)

        canvas.drawText(c.asString, x, trueY, paint)
    }

    abstract val dragShadowSize : Float
    val dragPaints by lazy {
        val dsp = Paint()
        dsp.textAlign = Paint.Align.CENTER
        //size is dragShadowSize of the particular view
        FontFallback.paints(dsp)
    }

    fun startDragAndDrop(selected : UnicodeCharacter){
        DragListener.beingDragged = selected
        startDragAndDrop(null,//ClipData.newPlainText("", selected.asString)
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