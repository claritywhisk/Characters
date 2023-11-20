package asterhaven.characters

import android.view.Gravity
import android.widget.Toast
import androidx.collection.CircularArray
import asterhaven.characters.Universe.allScripts
import asterhaven.characters.unicodescript.UnicodeScript
import kotlinx.coroutines.*
import java.util.*
import java.io.File
import java.lang.Integer.min
import kotlin.NoSuchElementException
import kotlin.collections.ArrayList
import kotlin.reflect.KProperty

//Todo versioning smooth transition when game updates
class Progress {
    //times seen before in world view
    val seenScriptChar = Array(allScripts.size){ si ->
        IntArray(allScripts[si].size)
    }
    //flags for script completion (seen once)
    val seenScript = BooleanArray(allScripts.size)
    //counts for script completion (seen once)
    val countFoundInScript = IntArray(allScripts.size)

    private val allCharsInScript : Array<SeenUnseenInScript> = Array(allScripts.size) { si ->
        SeenUnseenInScript(allScripts[si])
    }
    class SeenUnseenInScript(private val script : UnicodeScript){
        //array of indices in script [seen... undefined (size of number currently spawned) ...unseen]
        private val list = IntArray(script.size){ ci -> ci } //initialize all unseen
        private var iFirstNewCatalog = 0
        private var iFirstAfterSeen = 0 //index of middle, if any
        private var iUnseen = 0

        fun initializationSeeOperation(ci: Int){
            swap(iUnseen, ci)
            iUnseen++
            iFirstAfterSeen++
        }
        fun see(ci : Int) { //see a spawned char for the first time
            indexOfSpawned(ci)?.let {
                swap(iFirstAfterSeen, it)
                iFirstAfterSeen++
                if (BuildConfig.DEBUG) check(iUnseen >= iFirstAfterSeen)
            }
        }
        fun spawnRandomUnseen() : UnicodeCharacter? {
            if(iUnseen == list.size) return null
            val i = iUnseen + rRandom.nextInt(list.size - iUnseen)
            swap(iUnseen, i)
            return UnicodeCharacter.get(script, list[iUnseen++])
        }
        fun chooseKUniqueForCatalogPreview(k : Int) : ArrayList<UnicodeCharacter> {
            val ret = ArrayList<UnicodeCharacter>()
            val n = iFirstAfterSeen
            if(n == 0) return ret
            fun fisherYates(endPoint : Int){
                val i = iFirstNewCatalog + rRandom.nextInt(endPoint - iFirstNewCatalog)
                swap(iFirstNewCatalog, i)
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
        fun unspawn(ci : Int){
            indexOfSpawned(ci)?.let {
                iUnseen--
                swap(iUnseen, it)
            }
        }
        private fun swap(i : Int, j : Int){
            list[i] = list[j].also { list[j] = list[i] }
        }
        private fun indexOfSpawned(ci : Int) : Int? {
            val i = (iFirstAfterSeen until iUnseen).firstOrNull {i ->
                list[i] == ci
            }
            if(BuildConfig.DEBUG) check(i != null)
            return i
        }
    }
    //history of recently seen in world view and sleep
    private val history = CircularArray<UnicodeCharacter>(PROGRESS_RECENT_SIZE)
    //indices of characters hiding around edges of world view
    private val spawned = HashSet<UnicodeCharacter>()
    private val keepSpawned = HashSet<UnicodeCharacter>()

    @Synchronized fun see(c: UnicodeCharacter, ma: MainActivity) {
        if (spawned.contains(c)) {
            val si = c.scriptIndex()
            seenScriptChar[si][c.i]++
            allCharsInScript[si].see(c.i)
            spawned.remove(c)
            if(history.size() == PROGRESS_RECENT_SIZE) history.removeFromEnd(1)
            history.addFirst(c)
            if(seenScriptChar[si][c.i] == 1) {
                val x = ++countFoundInScript[si]
                val sought = c.script == ma.matched4
                if (sought) ma.progressBar?.setProgress(x, true)
                if (x == allScripts[si].size) {
                    seenScript[si] = true
                    val msg = ma.resources.getString(R.string.progress_script_finished, allScripts[si].name)
                    val toast = Toast.makeText(ma, msg, Toast.LENGTH_LONG)
                    toast.setGravity(Gravity.TOP, 0, 0)
                    toast.show()
                    ma.logToTextView(msg)
                    if (sought) ma.finishedWithScript()
                }
            }
        }
    }

    @Synchronized fun spawnRandUnspawnedInScript(si: Int) = allCharsInScript[si].spawnRandomUnseen().also {
        if(it != null) spawned.add(it)
    }
    @Synchronized fun doNotUnspawn(c : UnicodeCharacter) = keepSpawned.add(c)
    @Synchronized fun unspawnRemaining(){
        spawned.filter{ it !in keepSpawned }.forEach {
            spawned.remove(it)
            allCharsInScript[it.scriptIndex()].unspawn(it.i)
        }
        keepSpawned.clear()
    }
    fun seen(s : UnicodeScript, i : Int) = seenScriptChar[Universe.indexOfScript[s]!!][i] > 0

    fun recent(i : Int) : UnicodeCharacter = history.get(i)
    fun kRecent(k : Int) = history.size().coerceAtMost(k).let {
        ArrayList<UnicodeCharacter>(it).apply {
            for (i in 0 until it) add(recent(i))
        }
    }
    fun numRecent() = history.size()

    fun kUniqueInScriptForCatalogPreview(script: UnicodeScript, k: Int) =
        allCharsInScript[Universe.indexOfScript[script]!!].chooseKUniqueForCatalogPreview(k)


    companion object {
        private lateinit var saveFile : File
        private var saveJob : Job? = null
        private lateinit var progress : Progress
        private lateinit var progressAsync : Deferred<Progress>
        private var lazyLoadedFlag = false
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Progress {
            if(lazyLoadedFlag) return progress
            progress = runBlocking { progressAsync.await() }
            if(BuildConfig.DEBUG && thisRef is MainActivity) {
                thisRef.logToTextView("Started with ${progress.countFoundInScript.sum()} chars")
            }
            lazyLoadedFlag = true
            return progress
        }
        private fun loadAsync(file: File) = CoroutineScope(Dispatchers.IO).async {
            val bytes = file.readBytes()
            var j = 0
            val p = Progress()
            for(si in allScripts.indices) {
                var seenScript = true
                for(ci in 0 until allScripts[si].size){
                    when(val x = bytes[j++].toInt()) {
                        0 -> seenScript = false
                        else -> {
                            p.seenScriptChar[si][ci] = x
                            p.countFoundInScript[si]++
                            p.allCharsInScript[si].initializationSeeOperation(ci)
                        }
                    }
                }
                p.seenScript[si] = seenScript
            }
            p
        }

        fun save(progress : Progress) {
            if(::saveFile.isInitialized && saveJob?.isCompleted != false){
                val bytes: ByteArray
                synchronized(progress) {
                    var si = 0
                    var ci = 0
                    bytes = ByteArray(progress.seenScriptChar.sumOf { it.size }) { _ ->
                        while(ci == allScripts[si].size){
                            ci = 0
                            si++
                        }
                        progress.seenScriptChar[si][ci++].coerceAtMost(Byte.MAX_VALUE.toInt()).toByte()
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
