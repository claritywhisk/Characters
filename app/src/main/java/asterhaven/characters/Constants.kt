package asterhaven.characters

import android.view.View
import kotlin.system.measureTimeMillis

const val FRESH_PROGRESS = false
const val MUTE = true

const val CHAOS : Double = .0003 //upper bound of test random

//worldview speed
const val A = 1.3f//1f //(tiles/second)/second
const val V_MAX = 2.2f//1.75f //tiles/second
//worldview size
const val SIDE_LENGTH = 7
//worldview tap highlight
const val SELECTED_TIME = 3500L
//general glyph border
const val SCALE_TEXT2TILE = .75f

const val SAVE_EVERY = 15000L
const val SLEEP_DELAY = 1800L

const val WIDTH_TEXT_SIZE_RATIO = 2.5f

const val ODDS_PARAMETER_SAME_SCRIPT_NEAR_CHARACTER = 0.05
const val ODDS_LINEAR_DIMINISH_BY_DISTANCE = 0.01

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