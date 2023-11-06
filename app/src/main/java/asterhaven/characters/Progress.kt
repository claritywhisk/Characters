package asterhaven.characters

import android.view.Gravity
import android.widget.Toast
import androidx.collection.CircularArray
import asterhaven.characters.Universe.allScripts
import asterhaven.characters.unicodescript.UnicodeScript
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random
import java.io.File
import java.lang.Integer.min
import kotlin.reflect.KProperty

//Todo versioning smooth transition when game updates
class Progress(fresh : Boolean) {
    //times seen before in world view
    val seenChar = IntArray(UnicodeCharacter.n)
    //flags for script completion (seen once)
    val seenScript = BooleanArray(allScripts.size)
    //counts for script completion (seen once)
    val countInScript = IntArray(allScripts.size)

    private val allCharsInScript : Array<SeenUnseenInScript> = Array(allScripts.size) { si ->
        SeenUnseenInScript(allScripts[si])
    }
    class SeenUnseenInScript(private val script : UnicodeScript){
        //array of indices in script [seen... undefined (size of number currently spawned) ...unseen]
        private val list = ArrayList<Int>(script.size)
        private var iFirstNewCatalog = 0
        private var iFirstAfterSeen = 0 //index of middle, if any
        private var iUnseen = 0
        fun initAllUnseen(){
            for(ci in 0 until script.size) list.add(ci)
        }
        fun see(ci : Int) {
            list[iFirstAfterSeen] = ci
            iFirstAfterSeen++
            if(BuildConfig.DEBUG) check(iUnseen >= iFirstAfterSeen)
        }
        fun spawnRandomUnseen() : UnicodeCharacter? {
            if(iUnseen == list.size) return null
            val i = iUnseen + Random.nextInt(list.size - iUnseen)
            list[iUnseen] = list[i].also { list[i] = list[iUnseen] }
            return UnicodeCharacter.get(script, list[iUnseen++])
        }
        fun chooseKUniqueForCatalogPreview(k : Int) : ArrayList<UnicodeCharacter> {
            val ret = ArrayList<UnicodeCharacter>()
            val n = iFirstAfterSeen
            if(n == 0) return ret
            fun fisherYates(endPoint : Int){
                val i = iFirstNewCatalog + Random.nextInt(endPoint - iFirstNewCatalog)
                list[iFirstNewCatalog] = list[i].also { list[i] = list[iFirstNewCatalog] }
                ret.add(UnicodeCharacter.get(script, list[iFirstNewCatalog]))
                iFirstNewCatalog++
            }
            val f = iFirstNewCatalog
            repeat(min(k, n - f)){
                fisherYates(n)
            }
            if(iFirstNewCatalog == n) iFirstNewCatalog = 0
            repeat(min(f, k - ret.size)){
                fisherYates(f)
            }
            return ret
        }
    }

    //history of recently seen in world view and sleep
    private val history = CircularArray<UnicodeCharacter>(PROGRESS_RECENT_SIZE) //todo
    //indices of characters hiding around edges of world view
    private val spawned = HashSet<Int>()
    private val keepSpawned = HashSet<Int>()

    init {
        if(fresh) allCharsInScript.forEach { it.initAllUnseen() }
    }

    @Synchronized fun see(c: UnicodeCharacter, ma: MainActivity) {
        if (spawned.contains(c.i)) {
            seenChar[c.i]++
            allCharsInScript[c.scriptIndex()].see(c.i)
            spawned.remove(c.i)
            if(seenChar[c.i] == 1) {
                val scriptI = c.scriptIndex()
                val x = ++countInScript[scriptI]
                val sought = c.script == ma.matched4
                if (sought) ma.progressBar?.setProgress(x, true)
                if (x == allScripts[scriptI].size) {
                    seenScript[scriptI] = true
                    val toast = Toast.makeText(
                        ma,
                        "Completed ${allScripts[scriptI].name}!",
                        Toast.LENGTH_LONG
                    )
                    toast.setGravity(Gravity.TOP, 0, 0)
                    toast.show()
                    ma.logToTextView("Completed ${allScripts[scriptI].name}!")
                    if (sought) ma.finishedWithScript()
                }
            }
        }
    }

    @Synchronized fun spawnRandUnspawnedInScript(si: Int): UnicodeCharacter? {
        return allCharsInScript[si].spawnRandomUnseen()
    }
    fun doNotUnspawn(c : UnicodeCharacter) = keepSpawned.add(c.i)
    fun unspawnRemaining(){
        spawned.removeAll { it !in keepSpawned }
        keepSpawned.clear()
    }
    fun seen(s : UnicodeScript, i : Int) = seenChar[UnicodeCharacter.scriptStartI[Universe.indexOfScript[s]!!] + i] > 0

    fun recent(i : Int) = history.get(i)
    fun numRecent() = history.size()

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
                thisRef.logToTextView("Started with ${progress.seenChar.count{it > 0}} chars")
            lazyLoadedFlag = true
            return progress
        }
        private fun loadAsync(file: File) = CoroutineScope(Dispatchers.IO).async {
            val bytes = file.readBytes()
            val p = Progress(false) //todo assure proper init
            var script = 0
            var seenScript = true
            val seenDraft = ArrayList<ArrayList<Int>>()
            for(i in 0 until p.seenChar.size) {
                val x = bytes[i].toInt()
                while(seenDraft.lastIndex < x) seenDraft.add(ArrayList<Int>())
                seenDraft[x].add(i)
                when(x) {
                    0 -> seenScript = false
                    else -> {
                        p.seenChar[i] = x.toInt()
                        p.countInScript[script]++
                    }
                }
                val nextScript = script + 1
                if(nextScript < UnicodeCharacter.scriptStartI.size && UnicodeCharacter.scriptStartI[nextScript] == i + 1) {
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
                    bytes = ByteArray(progress.seenChar.size) {
                            i -> progress.seenChar[i].coerceAtMost(Byte.MAX_VALUE.toInt()).toByte()
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
            progressAsync = CoroutineScope(Dispatchers.Main).async{ Progress(true) }
            lazyLoadedFlag = false
        }
    }
}
