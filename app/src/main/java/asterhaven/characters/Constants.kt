package asterhaven.characters

import android.view.View
import kotlin.system.measureTimeMillis

const val MUTE = true

const val DEBUG_SMALL_SCRIPTS = true
const val DEBUG_MAX_SCRIPT_SIZE = 45
const val DEBUG_GLYPH_BOUNDS = false
const val DEBUG_RESET_PROGRESS = false
val CHAOS : Double = if(DEBUG_SMALL_SCRIPTS) .02 else .0003 //upper bound of test random

//worldview speed
const val A = 1.3f//1f //(tiles/second)/second
const val V_MAX = 2.05f//1.75f 2.2f //tiles/second
//worldview size
const val SIDE_LENGTH = 7

const val SELECTED_TIME = 500L //worldview tap highlight
const val INV_DUP_FLASH_DURATION = 300L
const val INV_HIGHLIGHT_DURATION = 200L
//general glyph border
const val SCALE_TEXT2TILE = .75f

const val SAVE_EVERY = 5000L
const val SLEEP_DELAY = 1800L

const val WIDTH_TEXT_SIZE_RATIO = 2.5f

const val ODDS_BASE_CHARACTER_PREVALENCE = 0.15
const val ODDS_PARAMETER_SAME_SCRIPT_NEAR_CHARACTER = 0.04
const val ODDS_LINEAR_DIMINISH_BY_DISTANCE = 0.007
private const val ODDS_INV_1 = 0.1
private const val ODDS_INV_2 = 0.2
private const val ODDS_INV_3 = 0.3
private const val ODDS_INV_4 = 1.0
val ODDS_INVENTORY = doubleArrayOf(0.0, ODDS_INV_1, ODDS_INV_2, ODDS_INV_3, ODDS_INV_4)

const val PROGRESS_RECENT_SIZE = 40

//Drag Shadow size things
const val ENLARGE_FACTOR = 2.2f
const val SHRINK_FACTOR = .8f

const val CATALOG_COLUMN_STARTING_WIDTH_PX = 90
const val CATALOG_SECTIONS_RV_SCROLL_DAMP_MS = 150

const val EXAMINER_MARQUEE_DELAY = 1500

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