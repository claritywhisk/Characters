package asterhaven.characters

import kotlin.random.*

val rRandom = if(DEBUG_DETERMINISTICISH) Random(42) else Random(System.currentTimeMillis())
