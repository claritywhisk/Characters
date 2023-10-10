package asterhaven.characters

import android.animation.TimeAnimator
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class Walk (private val wView : WorldView) : TimeAnimator.TimeListener {
    companion object {
        private const val o = 0f
        private const val T_CRUISE_BEGIN = V_MAX/A
        private val FULLSTOPSTARTDIST = constAccDist(T_CRUISE_BEGIN, A)
        private fun constAccDist(t : Float, a : Float, v_0 : Float = 0f) = v_0*t + a*t*t/2f
    }
    var stopped = true
    //displacement from wView.centerCoordinate, up to 1/2 Tile
    var xOffset = o
    var yOffset = o
    private lateinit var currentAnimation : TimeAnimator
    private var xCenter = 0
    private var yCenter = 0
    //universe coordinate (with fraction) of location when animation starts
    private var departX = 0
    private var departY = 0
    private var departXOffset = o
    private var departYOffset = o
    //for reneging on a previous movement
    private var vXStopping = o
    private var vYStopping = o
    private var t = o
    private var aStoppingX = o
    private var aStoppingY = o
    //main acceleration (forward) and used opposite for end deceleration
    private var aForwardX = o
    private var aForwardY = o
    //time acceleration ceases
    private var tAStop = o
    //time of transition to end deceleration
    private var tSlow = o
    //potential times ending at rest
    private var tNormalStop = o //normal stopping motion completion
    private var tStoppingStop = o //stopping motion component from velocity previous to this dest

    fun to(dTilesX : Int, dTilesY : Int) {//world xy
        //compute (update) velocity from any interrupted movement
        vXStopping += aStoppingX * tStopping() + aForwardX * (tAcc() - tDec())
        vYStopping += aStoppingY * tStopping() + aForwardY * (tAcc() - tDec())
        //stopping motion
        val lengthV = sqrt(vXStopping * vXStopping + vYStopping * vYStopping)
        val dxStopping : Float
        val dyStopping : Float
        when(lengthV){
            o -> {
                dxStopping = o
                dyStopping = o
                tStoppingStop = o
            }
            else -> {
                val cosThetaOne = vXStopping / lengthV
                val sinThetaOne = vYStopping / lengthV
                aStoppingX = -A * cosThetaOne
                aStoppingY = -A * sinThetaOne
                tStoppingStop = lengthV / A
                dxStopping = constAccDist(tStoppingStop, aStoppingX, vXStopping)
                dyStopping = constAccDist(tStoppingStop, aStoppingY, vYStopping)
            }
        }
        //main motion
        val dx = dTilesX - xOffset - dxStopping
        val dy = dTilesY - yOffset - dyStopping
        val d = sqrt(dx * dx + dy * dy) + Float.MIN_VALUE
        val cosThetaTwo = dx / d
        val sinThetaTwo = dy / d
        aForwardX = A * cosThetaTwo
        aForwardY = A * sinThetaTwo
        val dCruise = d - 2f * FULLSTOPSTARTDIST
        when (dCruise > 0f) {
            true -> {
                val cruiseTime = dCruise / V_MAX
                tAStop = T_CRUISE_BEGIN
                tSlow = T_CRUISE_BEGIN + cruiseTime
                tNormalStop = 2 * T_CRUISE_BEGIN + cruiseTime
            }
            false -> {
                tSlow = sqrt(d / A ) // Att/2 = d/2
                tAStop = tSlow
                tNormalStop = 2 * tAStop
            }
        }
        departX = xCenter
        departY = yCenter
        departXOffset = xOffset
        departYOffset = yOffset
        currentAnimation = TimeAnimator()
        currentAnimation.setTimeListener(this)
        currentAnimation.start()
        stopped = false
    }

    ///@RequiresApi(Build.VERSION_CODES.O)
    override fun onTimeUpdate(animation: TimeAnimator?, totalTime: Long, deltaTime: Long) {
        if(animation != currentAnimation){
            animation?.end()
            return
        }
        //manage time
        val end = max(tNormalStop, tStoppingStop)
        t = min(end, totalTime/1000f)
        if(t == end) {
            animation.end()
            stopped = true
        }
        //integrate accelerations twice
        val dStoppingX = constAccDist(tStopping(), aStoppingX, vXStopping)
        val dStoppingY = constAccDist(tStopping(), aStoppingY, vYStopping)
        val dAccX = constAccDist(tAcc(), aForwardX)
        val dAccY = constAccDist(tAcc(), aForwardY)
        val topSpeedX = tAcc() * aForwardX
        val topSpeedY = tAcc() * aForwardY
        val cruiseT = min(tSlow - tAStop, max(t - tAStop, o))
        val dCruiseX = topSpeedX * cruiseT
        val dCruiseY = topSpeedY * cruiseT
        //travel during deceleration including initial velocity
        val dDecX = constAccDist(tDec(), -1f * aForwardX, topSpeedX)
        val dDecY = constAccDist(tDec(), -1f * aForwardY, topSpeedY)
        //distance from departure center
        val x = departXOffset + dStoppingX + dAccX + dCruiseX + dDecX
        val y = departYOffset + dStoppingY + dAccY + dCruiseY + dDecY
        var xTiles = (x.absoluteValue + .5f).toInt()
        if(x < 0) xTiles *= -1
        var yTiles = (y.absoluteValue + .5f).toInt()
        if(y < 0) yTiles *= -1
        xOffset = x - xTiles
        yOffset = y - yTiles
        var dCenterXTiles = xTiles - (xCenter - departX)
        var dCenterYTiles = yTiles - (yCenter - departY)
        if(dCenterXTiles != 0 || dCenterYTiles != 0) {
            //check, but expect only one tile at a time
            if(dCenterXTiles.absoluteValue > 1) dCenterXTiles /= dCenterXTiles.absoluteValue
            if(dCenterYTiles.absoluteValue > 1) dCenterYTiles /= dCenterYTiles.absoluteValue
            //last place we track x and y
            xCenter += dCenterXTiles
            yCenter += dCenterYTiles
            //notify crossing into a new tile
            wView.adjustCenter(dCenterXTiles, dCenterYTiles)
        }
        wView.invalidate()
    }
    fun tAcc() = min(tAStop, t)
    fun tDec() = max(t - tSlow, o)
    fun tStopping() = min(tStoppingStop, t)
}