package asterhaven.characters

import android.view.Gravity
import android.widget.Toast
import asterhaven.characters.Universe.allScripts
import asterhaven.characters.unicodescript.UnicodeScript
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random
import java.io.File
import kotlin.collections.ArrayDeque
import kotlin.reflect.KProperty

//Todo versioning smooth transition when game updates
class Progress() {
    class Marks(n : Int) {
        val char = BitSet(n)
        val countInScript = IntArray(allScripts.size)
    }
    val seen: Marks
    val spawnedOrSeen : Marks
    val seenScript = BooleanArray(allScripts.size) //flags for script completion
    val recentQueue = ArrayDeque<UnicodeCharacter>(PROGRESS_RECENT_SIZE)

    private val scriptStartI: IntArray
    init {
        var n = 0
        scriptStartI = IntArray(allScripts.size) { i ->
            val start = n
            n += allScripts[i].size
            start
        }
        seen = Marks(n)
        spawnedOrSeen = Marks(n)
    }
    @Synchronized fun see(c: UnicodeCharacter, ma: MainActivity) {
        val scriptI = c.scriptIndex()
        if (!seen.char[c.i]) {
            seen.char[c.i] = true
            recentQueue.apply{
                if(size == PROGRESS_RECENT_SIZE) removeLast()
                addFirst(c)
            }
            val x = ++seen.countInScript[scriptI]
            val sought = c.script == ma.matched4
            if(sought) ma.progressBar?.setProgress(x, true)
            if (x == allScripts[scriptI].size) {
                seenScript[scriptI] = true
                val toast = Toast.makeText(ma, "Completed ${allScripts[scriptI].name}!", Toast.LENGTH_LONG)
                toast.setGravity(Gravity.TOP, 0, 0)
                toast.show()
                ma.logToTextView("Completed ${allScripts[scriptI].name}!")
                if(sought) ma.finishedWithScript()
            }
        }
    }

    @Synchronized fun spawnRandUnspawnedInScript(si: Int): UnicodeCharacter? = spawnRandUnmarked(spawnedOrSeen, si)
    @Synchronized fun spawnRandUnseenInScript(si: Int): UnicodeCharacter? = spawnRandUnmarked(seen, si)
    @Synchronized private fun spawnRandUnmarked(marks: Marks, si: Int): UnicodeCharacter? {
        val unmarked = allScripts[si].size - marks.countInScript[si]
        if(unmarked <= 0) return null
        var r = Random.nextInt(unmarked)
        //todo with Skip List; test on Han https://en.wikipedia.org/wiki/Skip_list
        var i = scriptStartI[si]
        while (marks.char[i]) i++
        while (r > 0) {
            r--
            i++
            while(marks.char[i]) i++
        }
        if(BuildConfig.DEBUG) check(i < scriptStartI[si] + allScripts[si].size)
        spawnedOrSeen.char[i] = true
        spawnedOrSeen.countInScript[si]++
        return UnicodeCharacter.create(si, i - scriptStartI[si])
    }
    fun mayUnspawn(c : UnicodeCharacter) = c.i.let {
        if(spawnedOrSeen.char[it] && !seen.char[it]){
            spawnedOrSeen.char[it] = false
            spawnedOrSeen.countInScript[c.scriptIndex()]--
        }
    }
    fun didNotUnspawn(c : UnicodeCharacter) = c.i.let {
        if(!spawnedOrSeen.char[it]){
            spawnedOrSeen.char[it] = true
            spawnedOrSeen.countInScript[c.scriptIndex()]++
        }
    }
    fun seen(s : UnicodeScript, i : Int) = seen.char[scriptStartI[Universe.indexOfScript[s]!!] + i]
    private val UnicodeCharacter.i get() = scriptStartI[this.scriptIndex()] + this.indexInScript

    companion object {
        private lateinit var saveFile : File
        private var saveJob : Job? = null
        private lateinit var progress : Progress
        private lateinit var progressAsync : Deferred<Progress>
        private var lazyLoadedFlag = false
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Progress {
            if(lazyLoadedFlag) return progress
            progress = runBlocking { progressAsync.await() }
            if(BuildConfig.DEBUG && thisRef is MainActivity)
                thisRef.logToTextView("Started with ${progress.seen.char.cardinality()} chars")
            lazyLoadedFlag = true
            return progress
        }
        private fun loadAsync(file: File) = CoroutineScope(Dispatchers.IO).async {
            val bytes = file.readBytes()
            val p = Progress()
            var script = 0
            var seenScript = true
            for(i in 0 until p.seen.char.size()) {
                when(bytes[i]) {
                    1.toByte() -> {
                        p.seen.char[i] = true
                        p.seen.countInScript[script]++
                        p.spawnedOrSeen.char[i] = true
                        p.spawnedOrSeen.countInScript[script]++
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
                    bytes = ByteArray(progress.seen.char.size()) {
                            i -> if(progress.seen.char[i]) 1 else 0
                    }
                }
                saveJob = CoroutineScope(Dispatchers.IO).launch { saveFile.writeBytes(bytes) }
            }
        }

        fun beginWithSaveFile(f : File){
            saveFile = f
            check(!lazyLoadedFlag)
            if(saveFile.exists() && !DEBUG_RESET_PROGRESS) CoroutineScope(Dispatchers.IO).launch {
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

