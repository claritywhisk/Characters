package asterhaven.characters

import asterhaven.characters.Universe.allScripts
import kotlinx.coroutines.*
import java.util.*
import kotlin.random.Random
import java.io.File

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

    @Synchronized fun see(c: UnicodeCharacter) {
        val scriptI = c.scriptIndex()
        val i = scriptStartI[scriptI] + c.indexInScript
        if (!seen[i]) {
            seen[i] = true
            //println("Saw $i " + card())
            seenInScript[scriptI]++
            if (seenInScript[scriptI] == allScripts[scriptI].size)
                repeat(20){ println("Completed ${allScripts[scriptI].name}!") }
            //logToTextView("Completed ${allScripts[scriptI].name}!", iprobablyamascreen)
        }
    }

    @Synchronized fun randUnseenInScript(si: Int) : UnicodeCharacter {
        val unseen = allScripts[si].size - seenInScript[si]
        var r = Random.nextInt(unseen)
        //todo with Skip List; test on Han https://en.wikipedia.org/wiki/Skip_list
        var i = scriptStartI[si]
        while (seen[i]) i++
        while (r-- > 0) {
            i++
            while(seen[i]) i++
        }
        return UnicodeCharacter.create(si, i - scriptStartI[si]) //TODO index out of bounds
    }

    companion object Save {
        fun load(file: File) = CoroutineScope(Dispatchers.IO).async {
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
                if(nextScript < p.scriptStartI.size && p.scriptStartI[nextScript] == i) {
                    if(seenScript) p.seenScript[script] = true
                    seenScript = true
                    script = nextScript
                }
            }
            p
        }
        fun saveJob(file: File, progress : Progress): Job {
            val bytes: ByteArray
            synchronized(progress) {
                bytes = ByteArray(progress.seen.size()) {
                    i -> if(progress.seen[i]) 1 else 0
                }
            }
            return CoroutineScope(Dispatchers.IO).launch { file.writeBytes(bytes) }
        }
    }
}
