package asterhaven.characters

import android.view.View
import kotlin.system.measureTimeMillis

const val MUTE = true

const val CHAOS : Double = .0005 //upper bound of test random

//worldview speed
const val A = 1.3f//1f //(tiles/second)/second
const val V_MAX = 2.2f//1.75f //tiles/second
//worldview size
const val SIDE_LENGTH = 7
//worldview tap highlight
const val SELECTED_TIME = 3500L
//general glyph border
const val SCALE_TEXT2TILE = .75f

//magic walking factors
const val BLANK_QUO_CAP = 0.95 //todo
const val PROPORTION_TILE = 0.35
const val PROPORTION_INTENT = 0.01
const val PROPORTION_UNIVERSE = .64

//@RequiresApi(Build.VERSION_CODES.N)
fun randomBreathableCoordinateForCenterTEST() =
    Coordinate.create(DoubleArray(Universe.allScripts.size) {
        _ -> kotlin.random.Random.nextDouble(0.0, CHAOS)
    })

enum class Terrain {
    CLOUD
}

fun logToTextView(line : String, view : View){
    (view.context as MainActivity).logToTextView(" $line")
}

private fun timeStr(msg : String, op : () -> Unit) = "$msg ${measureTimeMillis(op)} ms"
fun time(msg : String, op : () -> Unit) = println(timeStr(msg, op))
fun timeTV(msg : String, view : View, op : () -> Unit) {
    val str = timeStr(msg, op)
    println(str)
    logToTextView(str, view)
}

var t = 0L
fun beginMark() {
    t = System.currentTimeMillis()
}
fun mark(s : String){
    println(s + " " + (System.currentTimeMillis() - t))
}

// Todo Glyphs above the Unicode Basic Multilingual Plane: unifont_upper-14.0.01.ttf (2 Mbytes)