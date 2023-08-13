package asterhaven.characters

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import asterhaven.characters.typeface.FontFallback
import java.lang.RuntimeException
import kotlin.properties.Delegates
import kotlin.random.Random

class SleepView(context: Context?, attrs: AttributeSet?) : CharactersView(context, attrs) {
    private lateinit var loc : DoubleArray
    private var sigmadims by Delegates.notNull<Double>()

    private var glyphOld : UnicodeCharacter? = null
    private var glyphNew : UnicodeCharacter? = null

    private val oldAlpha = ValueAnimator.ofInt(255, 0)

    private val progress : Progress
        get() = (context as MainActivity).progress

    override val dragShadowSize = 0f

    init {
        oldAlpha.startDelay = SLEEP_DELAY
        oldAlpha.doOnStart {
            glyphOld = glyphNew
            glyphNew = character() ?: glyphOld //todo if completed
            logToTextView(glyphNew.toString(), this)
        }
        oldAlpha.doOnEnd {
            oldAlpha.start()
        }
        oldAlpha.addUpdateListener {
            invalidate()
        }
    }

    private val paints by lazy {
        val p = Paint()
        p.textAlign = Paint.Align.CENTER
        FontFallback.paints(p)
    }

    fun sleep() {
        oldAlpha.start()
    }

    fun wake() {
        oldAlpha.pause()
        glyphOld = null
        glyphNew = null
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val o = oldAlpha.animatedValue as Int
        val n = -o + 255
        canvas?.let { can ->
            glyphOld?.let { it.draw(can, o)}
            glyphNew?.let { it.draw(can, n)
                if(n > 192) see(it)
            }
        }
    }

    private fun UnicodeCharacter.draw(canvas : Canvas, alpha : Int){
        val paint = paints[this.fontIndex]
        paint.alpha = alpha
        drawCharacter(this, paint, canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if(w != h && BuildConfig.DEBUG) logToTextView("debug: unexpected sv size $w $h", this)
        paints.forEach {
            it.textSize = w / WIDTH_TEXT_SIZE_RATIO
        }
    }

    fun setLocation(scriptDims : DoubleArray) {
        loc = scriptDims
        sigmadims = 0.0
        for(si in loc.indices) {
            if(progress.seenScript[si]) loc[si] = 0.0
            else sigmadims += loc[si]
        }
    }

    //TODO call from Progress or always wakes up
    fun onScriptCompleted(si : Int) {
        sigmadims -= loc[si]
        loc[si] = 0.0
    }

    private fun character() = progress.randUnseenInScript(randScriptI())

    private fun randScriptI() : Int {
        var r4Script = Random.nextDouble() * sigmadims
        for (i in loc.indices) {
            if (loc[i] > r4Script) return i
            else r4Script -= loc[i]
        }
        if(BuildConfig.DEBUG) throw RuntimeException("Couldn't math random script")
        return 0
    }
}