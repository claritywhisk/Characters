package asterhaven.characters.typeface

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat

class FontFallback private constructor(applicationContext : Context) {
    enum class Font {
        SYSTEM_SEVERAL,
        GNU_UNIFONT,
        GUN_UNIFONT_UPPER
    }

    private val paintVanilla = Paint()
    private val paintUnifont = Paint()
    private val paintUUpper = Paint()
    private val typefaceUnifont = ResourcesCompat.getFont(applicationContext, R.font.unifont14)!!
    private val typefaceUpper = ResourcesCompat.getFont(applicationContext, R.font.unifont_upper14)
    init {
        paintUnifont.typeface = typefaceUnifont
        paintUUpper.typeface = typefaceUpper
    }

    object Static {
        lateinit var instance: FontFallback //angry pattern
        fun loadTypefaces(context : Context){
            instance = FontFallback(context)
        }

        //workaround for custom font fallback where we *prefer* default paint behavior
        fun paints(style : Paint) : Array<Paint> = Array(Font.values().size){
            val p = Paint(style) //copy settings provided
            when(it){
                Font.GNU_UNIFONT.ordinal -> p.typeface = instance.typefaceUnifont
            }
            p
        }

        fun hasGlyph(f : Font, c : String) = when(f){
            Font.SYSTEM_SEVERAL -> instance.paintVanilla
            Font.GNU_UNIFONT -> instance.paintUnifont
            Font.GUN_UNIFONT_UPPER -> instance.paintUUpper
        }.hasGlyph(c)


        fun fontIndexForCharacter(c : String) = font(c).ordinal
        private fun font(c : String) : Font {
            for(f in Font.values()) if(hasGlyph(f, c)) return f
            //-------------------------------------------------
            System.err.println("Cannot display: \n$c ${c.codePointAt(0).toString(16).toUpperCase()}")
            if(BuildConfig.DEBUG) check(false)
            return Font.SYSTEM_SEVERAL
        }
    }
}