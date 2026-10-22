package com.conght.facemakerapp.feature.intro

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.FragmentIntroBinding

class IntroFragment : BaseFragment<FragmentIntroBinding>(FragmentIntroBinding::inflate) {

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) = updateIndicator(position)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setLightStatusBar(true)

        val adapter = IntroPageAdapter()
        binding.pager.adapter = adapter
        createIndicator(adapter.itemCount)
        binding.pager.registerOnPageChangeCallback(pageCallback)

        binding.tvNext.setOnSingleClickListener {
            val current = binding.pager.currentItem
            if (current < adapter.itemCount - 1) {
                binding.pager.currentItem = current + 1
            } else {
                findNavController().navigate(R.id.action_intro_to_permission)
            }
        }
    }

    override fun onDestroyView() {
        binding.pager.unregisterOnPageChangeCallback(pageCallback)
        super.onDestroyView()
    }

    private fun createIndicator(count: Int) {
        val size = resources.getDimensionPixelSize(R.dimen.indicator_dot)
        val gap = resources.getDimensionPixelSize(R.dimen.indicator_gap)
        repeat(count) { index ->
            val dot = View(requireContext())
            binding.indicator.addView(dot, LinearLayout.LayoutParams(size, size).apply {
                if (index > 0) marginStart = gap
            })
        }
        updateIndicator(binding.pager.currentItem)
    }

    /** Chấm đang chọn là thanh 16×8 màu cam, còn lại chấm trắng 8×8. */
    private fun updateIndicator(position: Int) {
        val dot = resources.getDimensionPixelSize(R.dimen.indicator_dot)
        val active = resources.getDimensionPixelSize(R.dimen.indicator_active_w)
        binding.indicator.children.forEachIndexed { index, view ->
            val selected = index == position
            view.setBackgroundResource(if (selected) R.drawable.bg_indicator_active else R.drawable.bg_indicator_dot)
            view.updateLayoutParams { width = if (selected) active else dot }
        }
    }
}
