package asterhaven.characters

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking
open class Tile(open val character : UnicodeCharacter? = null)
class DeferredTile(private val c : Deferred<UnicodeCharacter?>?) : Tile() {
    var cancel = false
    override val character by lazy { if(cancel) null else runBlocking { c?.await() } }
}