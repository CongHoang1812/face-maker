package com.conght.facemakerapp.feature.dialog

import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.common.AppConfig
import com.conght.facemakerapp.core.ui.base.BaseDialogFragment
import com.conght.facemakerapp.core.ui.collectWhenStarted
import com.conght.facemakerapp.core.ui.setOnSingleClickListener
import com.conght.facemakerapp.databinding.DialogRateUsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Rate us (Figma 69:339). 0 sao: nút Rate disable; 1–3 sao: mở email góp ý; 4–5 sao: mở Play Store.
 * Sau khi Rate: `setRated(true)`. Kết quả `setFragmentResult(REQUEST_KEY, { KEY_RATED: Boolean })`
 * khi dialog đóng (để màn gọi, vd. Home lúc thoát app, biết mà xử lý tiếp).
 */
@AndroidEntryPoint
class RateUsDialogFragment : BaseDialogFragment<DialogRateUsBinding>(DialogRateUsBinding::inflate) {

    private val viewModel: RateUsViewModel by viewModels()
    private var rated = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rated = savedInstanceState?.getBoolean(STATE_RATED) ?: false
        createStars()
        binding.btnRate.setOnSingleClickListener { rate() }
        binding.btnExit.setOnSingleClickListener { dismiss() }
        collectWhenStarted(viewModel.stars, ::render)
    }

    private fun createStars() {
        val size = resources.getDimensionPixelSize(R.dimen.rate_star)
        val gap = resources.getDimensionPixelSize(R.dimen.rate_star_gap)
        repeat(RateUsViewModel.MAX_STARS) { index ->
            val star = ImageView(requireContext()).apply {
                setImageResource(R.drawable.ic_star_rate)
                contentDescription = resources.getQuantityString(R.plurals.cd_rate_stars, index + 1, index + 1)
                setOnClickListener { viewModel.setStars(index + 1) }
            }
            binding.stars.addView(star, LinearLayout.LayoutParams(size, size).apply {
                if (index > 0) marginStart = gap
            })
        }
    }

    private fun render(stars: Int) {
        val (emoji, title, message) = when (stars) {
            0 -> Triple(R.drawable.img_rate_0, R.string.rate_title_default, R.string.rate_message_default)
            1 -> Triple(R.drawable.img_rate_1, R.string.rate_title_low, R.string.rate_message_low)
            2 -> Triple(R.drawable.img_rate_2, R.string.rate_title_low, R.string.rate_message_low)
            3 -> Triple(R.drawable.img_rate_3, R.string.rate_title_low, R.string.rate_message_low)
            4 -> Triple(R.drawable.img_rate_4, R.string.rate_title_high, R.string.rate_message_high)
            else -> Triple(R.drawable.img_rate_5, R.string.rate_title_high, R.string.rate_message_high)
        }
        binding.ivEmoji.setImageResource(emoji)
        binding.tvTitle.setText(title)
        binding.tvMessage.setText(message)

        val on = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.star_on))
        val off = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.star_off))
        binding.stars.children.forEachIndexed { index, view ->
            ImageViewCompat.setImageTintList(view as ImageView, if (index < stars) on else off)
        }
        binding.btnRate.isEnabled = stars > 0
        binding.btnRate.alpha = if (stars > 0) 1f else DISABLED_ALPHA
    }

    private fun rate() {
        val stars = viewModel.stars.value
        if (stars == 0) return
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.markRated()
            rated = true
            if (stars <= LOW_RATING_MAX) openFeedbackEmail() else openPlayStore()
            dismiss()
        }
    }

    private fun openFeedbackEmail() {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:${AppConfig.FEEDBACK_EMAIL}".toUri()).apply {
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.rate_feedback_subject, getString(R.string.app_name)))
        }
        startSafely(intent)
    }

    // TODO: có thể thay bằng Play In-App Review (com.google.android.play:review) khi app lên store
    private fun openPlayStore() {
        val packageName = requireContext().packageName
        val market = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri())
        if (!startSafely(market)) {
            startSafely(Intent(Intent.ACTION_VIEW, AppConfig.playStoreUrl(packageName).toUri()))
        }
    }

    private fun startSafely(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_RATED, rated)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isStateSaved) {
            setFragmentResult(REQUEST_KEY, Bundle().apply { putBoolean(KEY_RATED, rated) })
        }
    }

    companion object {
        const val TAG = "RateUsDialog"
        const val REQUEST_KEY = "rate_us"
        const val KEY_RATED = "rated"

        private const val STATE_RATED = "rated"
        private const val LOW_RATING_MAX = 3
        private const val DISABLED_ALPHA = 0.5f

        fun newInstance() = RateUsDialogFragment()
    }
}
