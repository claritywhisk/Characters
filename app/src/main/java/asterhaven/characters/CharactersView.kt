package asterhaven.characters

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

open class CharactersView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    fun see(c : UnicodeCharacter) {
        val x = context as MainActivity
        if(x.progressInitialized()) x.progress.see(c)
    }

    fun drawCharacter(c : UnicodeCharacter, paint : Paint, canvas : Canvas) =
        drawCharacter(c, paint, canvas, canvas.width / 2f, canvas.height / 2f)
    fun drawCharacter(c : UnicodeCharacter, paint : Paint, canvas : Canvas, x : Float, y : Float) {
        if(BuildConfig.DEBUG) require(paint.textAlign == Paint.Align.CENTER)
        val trueY = y - (paint.descent() + paint.ascent()) / 2f //todo verify
        canvas.drawText(c.asString, x, trueY, paint)
    }
}