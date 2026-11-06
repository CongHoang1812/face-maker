package com.fantasy.oc.maker.customize.feature.maker.template

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.fantasy.oc.maker.customize.R
import com.fantasy.oc.maker.customize.core.common.Resource
import com.fantasy.oc.maker.customize.core.ui.GridSpacingItemDecoration
import com.fantasy.oc.maker.customize.core.ui.applyStatusBarPadding
import com.fantasy.oc.maker.customize.core.ui.base.BaseFragment
import com.fantasy.oc.maker.customize.core.ui.collectWhenStarted
import com.fantasy.oc.maker.customize.core.ui.navigateSafe
import com.fantasy.oc.maker.customize.core.ui.setLightStatusBar
import com.fantasy.oc.maker.customize.core.ui.setOnSingleClickListener
import com.fantasy.oc.maker.customize.data.model.CharacterTemplate
import com.fantasy.oc.maker.customize.databinding.FragmentTemplateSelectBinding
import com.fantasy.oc.maker.customize.feature.dialog.NoInternetDialogFragment
import com.fantasy.oc.maker.customize.feature.maker.customize.CustomizeFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Maker – chọn nhân vật gốc (Figma 3:278). Không có ô None.
 * Chờ API: hiện [PLACEHOLDER_COUNT] thẻ khung giấy thật (ô ảnh shimmer), chưa bấm được; tải xong mới sang Customize.
 */
@AndroidEntryPoint
class TemplateSelectFragment : BaseFragment<FragmentTemplateSelectBinding>(FragmentTemplateSelectBinding::inflate) {

    private val viewModel: TemplateSelectViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(false)

        binding.btnBack.setOnSingleClickListener { findNavController().popBackStack() }

        childFragmentManager.setFragmentResultListener(KEY_NO_INTERNET, viewLifecycleOwner) { _, _ ->
            viewModel.pendingTemplate?.let(::onTemplateClick)
        }

        val adapter = TemplateAdapter(::onTemplateClick)
        val columns = resources.getInteger(R.integer.template_grid_columns)
        binding.rvTemplates.layoutManager = GridLayoutManager(requireContext(), columns)
        binding.rvTemplates.addItemDecoration(
            GridSpacingItemDecoration(columns, 0, resources.getDimensionPixelSize(R.dimen.template_grid_gap_v)),
        )
        binding.rvTemplates.adapter = adapter
        binding.errorState.setOnRetryClickListener { viewModel.retry() }

        collectWhenStarted(viewModel.state) { resource ->
            binding.rvTemplates.isVisible = resource !is Resource.Error
            binding.errorState.isVisible = resource is Resource.Error
            when (resource) {
                Resource.Loading -> adapter.submitList(List(PLACEHOLDER_COUNT) { TemplateCell(it, null) })
                is Resource.Success -> adapter.submitList(resource.data.mapIndexed { i, t -> TemplateCell(i, t) })
                is Resource.Error -> binding.errorState.setError(resource.throwable)
            }
        }
    }

    /** Có mạng → mở Customize; không → dialog No Internet (Try again gọi lại hàm này). */
    private fun onTemplateClick(template: CharacterTemplate) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.isOnline()) {
                openCustomize(template)
            } else {
                viewModel.pendingTemplate = template
                NoInternetDialogFragment.showOnce(childFragmentManager, KEY_NO_INTERNET)
            }
        }
    }

    private fun openCustomize(template: CharacterTemplate) {
        navigateSafe(
            R.id.action_templateSelect_to_customize,
            Bundle().apply { putString(CustomizeFragment.ARG_TEMPLATE_ID, template.id) },
        )
    }

    private companion object {
        const val PLACEHOLDER_COUNT = 10
        const val KEY_NO_INTERNET = "template_no_internet"
    }
}
