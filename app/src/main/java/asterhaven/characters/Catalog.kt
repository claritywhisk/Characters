package asterhaven.characters

import android.content.Context
import android.graphics.Color
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import asterhaven.characters.databinding.ActivityMainBinding
import asterhaven.characters.unicodescript.UnicodeScript

object Catalog {
    private var viewsRemoved : List<View> = listOf()
    private var prevPanelParams : LayoutParams = LayoutParams(0,0)
    private var didFirstAppear = false
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
        populate(cat.findViewById(R.id.catalogSections), activity)
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
    fun populate(sectionsRV : RecyclerView, activity: MainActivity){
        val layMan = LinearLayoutManager(activity)
        sectionsRV.layoutManager = layMan
        sectionsRV.adapter = SectionAdapter(activity.applicationContext)
        /*sectionsRV.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(rv : RecyclerView, dx : Int, dy : Int){
                super.onScrolled(rv, dx, dy)
                val f = layMan.findFirstVisibleItemPosition()
                val l = layMan.findLastVisibleItemPosition()

                //todo recover resources from invisible sections... no? it's done automatically
            }
        })*/
        SectionAdapter.strRecent = activity.resources.getString(R.string.cat_recent_chars_title)
    }

    class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.sectionTitle)
        val rv: RecyclerView = itemView.findViewById(R.id.recyclerView)
    }
    class SectionAdapter(private val applicationContext: Context) : RecyclerView.Adapter<SectionViewHolder>() {
        companion object {
            lateinit var strRecent : String
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SectionViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.catalog_section, parent, false))
        override fun onBindViewHolder(holder: SectionViewHolder, position: Int) {
            if(position == 0) {
                holder.title.text = strRecent
                //todo
            }
            else {
                val script = Universe.allScripts[position - 1]
                holder.title.text = script.name
                holder.rv.layoutManager = GridLayoutManager(applicationContext, 8) //todo column width
                holder.rv.adapter = CharacterGridAdapter(script)
            }
        }
        override fun getItemCount(): Int {
            return 5 //todo pagination onScroll 1 + Universe.allScripts.size
        }
    }
    class CharacterGridHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        //opt for android default drawing of chars here
        val textView: TextView = itemView.findViewById(R.id.catTileTV)
    }
    class CharacterGridAdapter(private val script: UnicodeScript?) : RecyclerView.Adapter<CharacterGridHolder>() {
        companion object {
            val progress by Progress
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = CharacterGridHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.catalog_character_tile, parent, false))
        override fun onBindViewHolder(holder: CharacterGridHolder, position: Int) {
            if(script == null) {
                //todo recent[position]
            }
            else {
                if(progress.seen(script, position)) holder.textView.text = script.charAt(position)
                else holder.textView.setBackgroundColor(Color.GRAY) //todo missing look, filter to seen
            }
        }
        override fun getItemCount(): Int {
            return script?.size ?: 0 //todo recent
        }
    }
}