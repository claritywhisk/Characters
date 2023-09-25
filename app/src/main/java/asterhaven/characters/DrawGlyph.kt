package asterhaven.characters

import android.graphics.*

private val rect = Rect()
val paintTest by lazy {
    val p = Paint()
    p.color = Color.CYAN
    p.style = Paint.Style.STROKE
    p.strokeWidth *= 2
    p
}

fun drawCharacter(c : UnicodeCharacter, paint : Paint, canvas : Canvas) =
    drawCharacter(c, paint, canvas, canvas.width / 2f, canvas.height / 2f)
fun drawCharacter(c : UnicodeCharacter, paint : Paint, canvas : Canvas, x : Float, y : Float) {
    if(BuildConfig.DEBUG) require(paint.textAlign == Paint.Align.CENTER)

    val baseline = y - (paint.descent() + paint.ascent()) / 2
    //val baseline = y - (paint.fontMetrics.bottom + paint.fontMetrics.top) / 2

    paint.getTextBounds(c.asString,0, c.asString.length, rect)

    //rect.contains(0,0)) false
    if(DEBUG_GLYPH_BOUNDS) {
        val wid = paint.measureText(c.asString)

        paintTest.color = Color.BLUE
        paintTest.strokeWidth = 4f

        rect.offset((x - wid / 2).toInt(), baseline.toInt())
        canvas.drawRect(
            rect.left.toFloat(), rect.top.toFloat(),
            rect.right.toFloat(), rect.bottom.toFloat(), paintTest
        )

        val dWid = wid / 2 - rect.width() / 2

        paintTest.color = Color.RED
        paintTest.strokeWidth = 2f

        rect.offset(dWid.toInt(), 0)
        canvas.drawRect(
            rect.left.toFloat(), rect.top.toFloat(),
            rect.right.toFloat(), rect.bottom.toFloat(), paintTest
        )
    }

    canvas.drawText(c.asString, x, baseline, paint)
}