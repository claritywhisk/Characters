package asterhaven.characters

import android.view.View
import asterhaven.characters.Universe.allScripts
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random
import java.io.File
import kotlin.concurrent.fixedRateTimer
import kotlin.reflect.KProperty

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
private annotation class Saved

//Todo versioning smooth transition when game updates
class Progress {
    private val scriptStartI: IntArray
    private val seenInScript: IntArray
    val seenScript : BooleanArray
    @Saved val seen: BitSet

    constructor(){
        var n = 0
        scriptStartI = IntArray(allScripts.size) { i ->
            val start = n
            n += allScripts[i].size
            start
        }
        seenInScript = IntArray(allScripts.size)
        seenScript = BooleanArray(allScripts.size)
        seen = BitSet(n)
    }

    fun card() = "${seen.cardinality()} chars seen"

    @Synchronized fun see(c: UnicodeCharacter, v : View) {
        val scriptI = c.scriptIndex()
        val i = scriptStartI[scriptI] + c.indexInScript
        if (!seen[i]) {
            seen[i] = true
            seenInScript[scriptI]++
            if (seenInScript[scriptI] == allScripts[scriptI].size) {
                println("Completed ${allScripts[scriptI].name}!")
                logToTextView("Completed ${allScripts[scriptI].name}!", v) //todo
            }
        }
    }

    @Synchronized fun randUnseenInScript(si: Int) : UnicodeCharacter? {
        println("${ allScripts[si].size} ${seenInScript[si]}")
        val unseen = allScripts[si].size - seenInScript[si]
        if(unseen <= 0) return null
        var r = Random.nextInt(unseen)
        //todo with Skip List; test on Han https://en.wikipedia.org/wiki/Skip_list
        var i = scriptStartI[si]
        println(i)
        while (seen[i]) i++
        println(i)
        while (r > 0) {
            r--
            i++
            while(seen[i]) i++
        }
        println(i)
        println("uns $unseen $r $i ${i - scriptStartI[si]} ${seenInScript[si]}");
        return UnicodeCharacter.create(si, i - scriptStartI[si]) //TODO index out of bounds
    }

    companion object {
        lateinit var saveFile : File
        private var saveJob : Job? = null
        private lateinit var progress : Progress
        private lateinit var progressAsync : Deferred<Progress>
        private var lazyLoadedFlag = false
        operator fun getValue(mainActivity: MainActivity, property: KProperty<*>): Progress {
            if(lazyLoadedFlag) return progress
            progress = runBlocking { progressAsync.await() }
            if(BuildConfig.DEBUG) mainActivity.logToTextView("Started with ${progress.seen.cardinality()} chars")
            lazyLoadedFlag = true
            return progress
        }
        private fun loadAsync(file: File) = CoroutineScope(Dispatchers.IO).async {
            val bytes = file.readBytes()
            val p = Progress()
            var script = 0
            var seenScript = true
            for(i in 0 until p.seen.size()) {
                when(bytes[i]) {
                    1.toByte() -> {
                        p.seen[i] = true
                        p.seenInScript[script]++
                    }
                    else -> seenScript = false
                }
                val nextScript = script + 1
                if(nextScript < p.scriptStartI.size && p.scriptStartI[nextScript] == i + 1) {
                    if(seenScript) p.seenScript[script] = true
                    seenScript = true
                    script = nextScript
                }
            }
            p
        }

        fun save(progress : Progress) {
            if(::saveFile.isInitialized && saveJob?.isCompleted != false){
                val bytes: ByteArray
                synchronized(progress) {
                    bytes = ByteArray(progress.seen.size()) {
                            i -> if(progress.seen[i]) 1 else 0
                    }
                }
                saveJob = CoroutineScope(Dispatchers.IO).launch { saveFile.writeBytes(bytes) }
            }
        }

        fun beginWithSaveFile(f : File){
            saveFile = f
            check(lazyLoadedFlag == false)
            if(saveFile.exists()) CoroutineScope(Dispatchers.IO).launch {
                progressAsync = loadAsync(saveFile)
            }
            else clearProgress()
        }

        fun clearProgress() {
            progressAsync = CoroutineScope(Dispatchers.Main).async{ Progress() }
            lazyLoadedFlag = false
        }
    }
}

