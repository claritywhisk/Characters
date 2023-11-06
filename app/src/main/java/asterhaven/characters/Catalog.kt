package asterhaven.characters

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import asterhaven.characters.databinding.ActivityMainBinding
import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.unicodescript.UnicodeScript

object Catalog {
    private var viewsRemoved : List<View> = listOf()
    private var prevPanelParams : LayoutParams = LayoutParams(0,0)
    private var didFirstAppear = false
    private var itemSize = CATALOG_COLUMN_STARTING_WIDTH_PX
    fun initIfUninitialized(binding: ActivityMainBinding, activity: MainActivity) {
        if(didFirstAppear) return
        val cat = activity.layoutInflater.inflate(R.layout.catalog, null)
        viewsRemoved = listOf(cat)
        cat.layoutParams = LayoutParams(0,0)
        cat.updateLayoutParams<LayoutParams> {
            width = LayoutParams.MATCH_PARENT
            height = binding.root.height - binding.mainPanel.root.height
            startToStart = binding.root.id
            topToTop = binding.root.id
            endToEnd = binding.root.id
            bottomToTop = binding.mainPanel.root.id
        }
        CharacterGridHolder.getBackgroundDrawables(activity)
        val mainRV = cat.findViewById<RecyclerView>(R.id.catalogSections )
        mainRV.post {
            mainRV.apply {
                val layMan = LinearLayoutManager(activity)
                val ada = SectionAdapter(activity)
                layoutManager = layMan
                adapter = ada
                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    var loading = false
                    val runner = Runnable { loading = false }
                    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(rv, dx, dy)
                        if(!loading && layMan.findLastVisibleItemPosition() == ada.itemCount - 1) {
                            removeCallbacks(runner)
                            loading = true
                            ada.loadSection()
                            postDelayed(runner, CATALOG_SECTIONS_RV_SCROLL_DAMP_MS.toLong())
                        }
                    }
                })
                ada.loadSection() //todo initial fill
            }
        }
    }
    fun toggle(binding: ActivityMainBinding){
        TransitionManager.beginDelayedTransition(binding.root)
        val mp = binding.mainPanel.root
        prevPanelParams = (mp.layoutParams as LayoutParams).also { mp.layoutParams = prevPanelParams }
        if(!didFirstAppear){
            val cat = viewsRemoved.first()
            mp.updateLayoutParams<LayoutParams> {
                width = LayoutParams.MATCH_PARENT
                height = LayoutParams.WRAP_CONTENT
                startToStart = binding.root.id
                topToBottom = cat.id
                endToEnd = binding.root.id
                bottomToBottom = binding.root.id
            }
            didFirstAppear = true
        }
        val restoringViews = viewsRemoved
        viewsRemoved = binding.root.children.filter { it != mp }.toList()
        viewsRemoved.forEach { binding.root.removeView(it) }
        restoringViews.forEach { binding.root.addView(it) }
    }

    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.sectionTitle)
        val rv: RecyclerView = itemView.findViewById(R.id.recyclerView)
    }
    class SectionAdapter(private val context : Context) : RecyclerView.Adapter<SectionViewHolder>() {
        private var normalScriptsLoaded = 0
        private val strRecent = context.resources.getString(R.string.cat_recent_chars_title)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SectionViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.catalog_section, parent, false))
        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            holder.rv.layoutManager = GridLayoutManager(context, 1)
            holder.rv.post {
                val columns = holder.rv.measuredWidth / itemSize
                (holder.rv.layoutManager as GridLayoutManager).spanCount = columns
            }
            if(position == 0) {
                holder.title.text = strRecent
                holder.rv.adapter = CharacterGridAdapter(null)
            }
            else {
                val script = Universe.allScripts[position - 1]
                holder.title.text = script.name
                holder.rv.adapter = CharacterGridAdapter(script)
                Toast.makeText(context, "Bind section (script) $normalScriptsLoaded", LENGTH_SHORT).show()
            }
        }
        override fun getItemCount(): Int = 1 + normalScriptsLoaded //one section for Recent
        fun loadSection(){
            if(normalScriptsLoaded < Universe.allScripts.size){
                normalScriptsLoaded++
                notifyItemInserted(normalScriptsLoaded)
            }
        }
    }
    class CharacterGridHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: CatalogTileView = itemView.findViewById(R.id.catTileTV)
        companion object {
            lateinit var seenBackground : Drawable
            lateinit var unseenBackground : Drawable
            fun getBackgroundDrawables(activity: MainActivity){
                seenBackground = ResourcesCompat.getDrawable(activity.resources, R.drawable.catalog_entry, activity.theme)!!
                val gd = seenBackground.constantState?.newDrawable(activity.resources)?.mutate() as GradientDrawable?
                val typedValue = TypedValue()
                activity.theme.resolveAttribute(R.attr.colorCatalogUnseen, typedValue, true)
                gd?.setColor(typedValue.data)
                unseenBackground = gd ?: seenBackground
            }
        }
        fun size() = tv.updateLayoutParams {
            width = itemSize
            height = itemSize
        }
        fun character(c : UnicodeCharacter?) {
            if (c != null) {
                tv.background = seenBackground
                tv.typeface = FontFallback.Font.values()[c.fontIndex].getTypeface()
                tv.text = c.asString
                tv.occupant = c
            }
            else {
                tv.background = unseenBackground
                tv.text = ""
                tv.occupant = null
            }
        }
    }
    class CharacterGridAdapter(private val script: UnicodeScript?) : RecyclerView.Adapter<CharacterGridHolder>() {
        companion object {
            val progress by Progress
        }
        private val scriptI by lazy { Universe.indexOfScript[script]!! }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterGridHolder {
            val lI = LayoutInflater.from(parent.context)
            val v = lI.inflate(R.layout.catalog_character_tile, parent, false)
            return CharacterGridHolder(v)
        }
        override fun onBindViewHolder(holder: CharacterGridHolder, position: Int) {
            holder.size()
            holder.character(
                if(script == null) progress.recent(position)
                else if(progress.seen(script, position)) UnicodeCharacter.get(script, position)
                else null
            )
        }
        override fun getItemCount(): Int {
            return script?.size ?: progress.numRecent()
        }
    }
}