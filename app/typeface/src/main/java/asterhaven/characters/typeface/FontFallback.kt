package asterhaven.characters.typeface

import android.content.Context
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import java.lang.IllegalStateException

object FontFallback {
    enum class Font(val typefaceResource : Int?){
        SYSTEM_SEVERAL(null),
        GNU_UNIFONT(R.font.unifont14),
        GUN_UNIFONT_UPPER(R.font.unifont_upper14)
    }

    private lateinit var paints : Array<Paint>
    fun loadTypefaces(context : Context){
        paints = Array(Font.values().size) { i ->
            val p = Paint()
            val f = Font.values()[i]
            f.typefaceResource?.let {
                p.typeface = ResourcesCompat.getFont(context, it)!!
            }
            p
        }
    }

    fun paints(style : Paint) : Array<Paint> = Array(Font.values().size){ i ->
        val p = Paint(style) //copy settings provided
        p.typeface = paints[i].typeface
        p
    }

    fun hasGlyph(f : Font, c : String) = paints[f.ordinal].hasGlyph(c)

    fun fontIndexForCharacter(c : String) : Int {
        //prefer earlier listed in our enum (such as system default) fonts
        for(f in Font.values()) if(hasGlyph(f, c)) return f.ordinal
        //-------------------------------------------------
        val msg = "Cannot display: \n$c ${c.codePointAt(0).toString(16).uppercase()}"
        System.err.println(msg)
        if(BuildConfig.DEBUG) throw IllegalStateException(msg)
        return Font.SYSTEM_SEVERAL.ordinal
    }
}