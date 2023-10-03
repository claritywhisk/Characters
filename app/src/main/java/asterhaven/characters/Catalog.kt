package asterhaven.characters

import android.transition.TransitionManager
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import asterhaven.characters.databinding.ActivityMainBinding

object Catalog {
    var viewsRemoved : List<View> = listOf()
    var prevPanelParams : LayoutParams = LayoutParams(0,0)
    var didFirstAppear = false
    fun activate(binding: ActivityMainBinding, cat : View){
        viewsRemoved = listOf(cat)
        cat.layoutParams = LayoutParams(0,0)
        cat.updateLayoutParams<LayoutParams> {
            width = LayoutParams.MATCH_PARENT
            height = 0//binding.root.height - mp.height.also { println("height $it")} //TODO hack, also wrong
            horizontalWeight = 1f
            verticalWeight = 1f
            startToStart = binding.root.id
            topToTop = binding.root.id
            endToEnd = binding.root.id
            bottomToTop = binding.mainPanel.root.id
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
}