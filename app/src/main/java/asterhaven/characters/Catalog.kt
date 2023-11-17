package asterhaven.characters

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.transition.TransitionManager
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import asterhaven.characters.databinding.ActivityMainBinding
import asterhaven.characters.typeface.FontFallback
import asterhaven.characters.unicodescript.UnicodeScript

class Catalog(binding: ActivityMainBinding, activity: MainActivity) {
    private var viewsRemoved : List<View> = listOf()
    private var prevPanelParams : LayoutParams = LayoutParams(0,0)
    private var didFirstAppear = false
    private var itemSize = CATALOG_COLUMN_STARTING_WIDTH_PX
    private var openFullScriptI : Int = -1
    private val cat : ViewGroup
    private val previewSections : RecyclerView
    private val seenBackground : Drawable
    private val unseenBackground : Drawable
    private val progress by Progress
    init {
        cat = activity.layoutInflater.inflate(R.layout.catalog, null) as ConstraintLayout
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
        //get background drawables
        seenBackground = ResourcesCompat.getDrawable(activity.resources, R.drawable.catalog_entry, activity.theme)!!
        val gd = seenBackground.constantState?.newDrawable(activity.resources)?.mutate() as GradientDrawable?
        val typedValue = TypedValue()
        activity.theme.resolveAttribute(R.attr.colorCatalogUnseen, typedValue, true)
        gd?.setColor(typedValue.data)
        unseenBackground = gd ?: seenBackground
        //set up outer recyclerview
        previewSections = cat.findViewById(R.id.catalogSections)
        previewSections.post {
            previewSections.apply {
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
                            post{ ada.loadSection() }
                            postDelayed(runner, CATALOG_SECTIONS_RV_SCROLL_DAMP_MS.toLong())
                        }
                    }
                })
                fun fillItems() : Any = post {
                    if (ada.loadSection()) {
                        val contentsHeight = (0 until previewSections.childCount).sumOf {
                            previewSections.getChildAt(it)?.height ?: 0
                        }
                        if (contentsHeight < previewSections.height) fillItems()
                    }
                }
                fillItems()
            }
        }
    }
    @Synchronized fun toggle(binding: ActivityMainBinding){
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
        }
        val restoringViews = viewsRemoved
        viewsRemoved = binding.root.children.filter { it != mp }.toList()
        viewsRemoved.forEach { binding.root.removeView(it) }
        restoringViews.forEach {
            binding.root.addView(it)
            if(didFirstAppear && it.id == R.id.catRoot) {
                if(previewSections.isVisible) (previewSections.adapter as SectionAdapter).updatePreviews()
                else updateFullScript(it.findViewById(R.id.fullScript))
            }
        }
        didFirstAppear = true
    }
    @Synchronized fun openFullScript(script : UnicodeScript){
        openFullScriptI = Universe.indexOfScript[script]!!
        TransitionManager.beginDelayedTransition(cat)
        cat.findViewById<LinearLayout>(R.id.fullScript).apply {
            visibility = VISIBLE
            findViewById<TextView>(R.id.sectionTitle).text = script.name
            findViewById<ImageButton>(R.id.btnCatRightLeft).apply {
                setImageResource(R.drawable.baseline_arrow_back_24)
                setOnClickListener {
                    backToPreviews()
                }
            }
            findViewById<RecyclerView>(R.id.sectionRecyclerView).apply {
                gridRVInit { columnsAvail ->
                    columnsAvail.coerceAtMost(script.size)
                }
                adapter = CharacterGridAdapter(script, false)
            }
            updateFullScript(this)
        }
        previewSections.visibility = GONE
    }
    @Synchronized fun backToPreviews(){
        TransitionManager.beginDelayedTransition(cat)
        cat.findViewById<LinearLayout>(R.id.fullScript).visibility = GONE
        (previewSections.adapter as SectionAdapter).updatePreviews()
        previewSections.visibility = VISIBLE
    }
    private fun updateFullScript(loneSection : LinearLayout){
        loneSection.findViewById<TextView>(R.id.sectionProgress).text = numString(openFullScriptI)
        loneSection.findViewById<RecyclerView>(R.id.sectionRecyclerView).adapter.apply {
            this as CharacterGridAdapter
            notifyItemRangeChanged(0, itemCount) //note optional 'payload' parameter in case of performance wish
        }
    }
    private fun numString(si : Int) = "${progress.countFoundInScript[si]}/${Universe.allScripts[si].size}"
    private fun RecyclerView.gridRVInit(columns : (Int) -> Int) {
        post {
            val columnsAvail = (parent as FrameLayout).measuredWidth / itemSize
            val cols = columns(columnsAvail).coerceAtLeast(1)
            layoutManager = GridLayoutManager(context, cols)
        }
    }
    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.sectionTitle)
        val second: TextView = itemView.findViewById(R.id.sectionProgress)
        val btn: ImageButton = itemView.findViewById(R.id.btnCatRightLeft)
        val rv: RecyclerView = itemView.findViewById(R.id.sectionRecyclerView)
    }
    inner class SectionAdapter(private val context : Context) : RecyclerView.Adapter<SectionViewHolder>() {
        private var normalScriptsLoaded = 0
        private val strRecent = context.resources.getString(R.string.cat_recent_chars_title)
        private val strUnknownScript = context.resources.getString(R.string.cat_undiscovered_script_title)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SectionViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.catalog_section, parent, false))
        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            holder.rv.gridRVInit { columnsAvail ->
                (holder.rv.adapter as CharacterGridAdapter).preview(columnsAvail)
            }
            if(position == 0) {
                holder.title.text = strRecent
                holder.second.text = ""
                holder.btn.visibility = View.INVISIBLE
                holder.rv.adapter = CharacterGridAdapter(null, true)
                //todo decide recent behavior and if "full" recent on click
            }
            else {
                val si = position - 1
                val script = Universe.allScripts[si]
                val met = progress.countFoundInScript[si] > 0
                holder.title.text = if(met) script.name else strUnknownScript
                holder.second.text = if(met) numString(si) else ""
                if(met) holder.btn.setOnClickListener{ openFullScript(script) }
                else holder.btn.visibility = View.INVISIBLE //todo update when found script
                holder.rv.adapter = CharacterGridAdapter(script, true)
                if(met) View.OnClickListener {
                    openFullScript(script)
                }.let {
                    holder.title.setOnClickListener(it)
                    holder.rv.setOnClickListener(it)
                }
                Toast.makeText(context, "Bind section (script) $normalScriptsLoaded", LENGTH_SHORT).show() //todo paginate, test, and remove
            }
        }
        override fun getItemCount(): Int = 1 + normalScriptsLoaded //one section for Recent
        fun loadSection() : Boolean {
            if(normalScriptsLoaded < Universe.allScripts.size){
                normalScriptsLoaded++
                notifyItemInserted(normalScriptsLoaded)
                return true
            }
            return false
        }
        fun updatePreviews(){
            notifyItemRangeChanged(0, itemCount)
        }
    }
    inner class CharacterGridHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tv: CatalogTileView = itemView.findViewById(R.id.catTileTV)
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
    inner class CharacterGridAdapter(
        private val script: UnicodeScript?,
        private val isPreview : Boolean
    ) : RecyclerView.Adapter<CharacterGridHolder>() {
        private var previewChars : ArrayList<UnicodeCharacter>? = null
        fun preview(rowSize : Int) : Int {
            previewChars = if(script == null) progress.kRecent(rowSize)
            else progress.kUniqueInScriptForCatalogPreview(script, rowSize)
            notifyItemRangeInserted(0, previewChars!!.size)
            return previewChars!!.size
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CharacterGridHolder {
            val lI = LayoutInflater.from(parent.context)
            val v = lI.inflate(R.layout.catalog_character_tile, parent, false)
            return CharacterGridHolder(v)
        }
        override fun onBindViewHolder(holder: CharacterGridHolder, position: Int) {
            holder.size()
            holder.character(
                if(isPreview) previewChars!![position]
                else if(script == null) progress.recent(position)
                else if(progress.seen(script, position)) UnicodeCharacter.get(script, position)
                else null
            )
        }
        override fun getItemCount(): Int {
            return if(isPreview) previewChars?.size ?: 0 else script?.size ?: progress.numRecent()
        }
    }
}