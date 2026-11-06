package com.fantasy.oc.maker.customize.feature.dialog

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.children
import androidx.core.widget.ImageViewCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.fantasy.oc.maker.customize.R
import com.fantasy.oc.maker.customize.core.common.AppConfig
import com.fantasy.oc.maker.customize.core.ui.base.BaseDialogFragment
import com.fantasy.oc.maker.customize.core.ui.collectWhenStarted
import com.fantasy.oc.maker.customize.core.ui.setOnSingleClickListener
import com.fantasy.oc.maker.customize.databinding.DialogRateUsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

/**
 * Rate us (Figma 69:339), mở từ Setting hoặc khi Back ở Home.
 * 0 sao: nút Rate disable; 1–3 sao: mở email góp ý; 4–5 sao: chỉ đóng dialog.
 * Sau khi rate, báo [KEY_RATED] cho màn cha để lưu đã rate (Setting ẩn dòng Rate us).
 * Bấm Exit hoặc Back/chạm ngoài dialog thì báo [KEY_EXIT] (Home dùng để thoát app).
 */
@AndroidEntryPoint
class RateUsDialogFragment : BaseDialogFragment<DialogRateUsBinding>(DialogRateUsBinding::inflate) {

    private val viewModel: RateUsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        createStars()
        binding.btnRate.setOnSingleClickListener { rate() }
        binding.btnExit.setOnSingleClickListener {
            parentFragmentManager.setFragmentResult(KEY_EXIT, Bundle.EMPTY)
            dismiss()
        }
        collectWhenStarted(viewModel.stars, ::render)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        parentFragmentManager.setFragmentResult(KEY_EXIT, Bundle.EMPTY)
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
        setupStarSwipe()
    }

    /**
     * Chạm hoặc lướt trên hàng sao thì chọn sao gần ngón tay nhất.
     * Gắn cả vào từng sao vì sao clickable sẽ giữ cử chỉ, hàng sao chỉ nhận phần chạm vào khe giữa các sao.
     * onClick của từng sao vẫn giữ cho TalkBack.
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupStarSwipe() {
        val listener = View.OnTouchListener { touched, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_MOVE) {
                val x = if (touched === binding.stars) event.x else touched.left + event.x
                viewModel.setStars(starCountAt(x))
            }
            true
        }
        binding.stars.setOnTouchListener(listener)
        binding.stars.children.forEach { it.setOnTouchListener(listener) }
    }

    /** [x] tính theo toạ độ của hàng sao. Dùng vị trí thật của từng sao nên đúng cả khi layout RTL. */
    private fun starCountAt(x: Float): Int =
        binding.stars.children.withIndex()
            .minBy { (_, star) -> abs((star.left + star.right) / 2f - x) }
            .index + 1

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
        if (stars <= LOW_RATING_MAX) openFeedbackEmail()
        parentFragmentManager.setFragmentResult(KEY_RATED, Bundle.EMPTY)
        dismiss()
    }

    private fun openFeedbackEmail() {
        val intent = Intent(Intent.ACTION_SENDTO, "mailto:${AppConfig.FEEDBACK_EMAIL}".toUri()).apply {
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.rate_feedback_subject, getString(R.string.app_name)))
        }
        startSafely(intent)
    }

    private fun startSafely(intent: Intent) {
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // Máy không có app email: vẫn đóng dialog như bình thường.
        }
    }

    companion object {
        /** Fragment result báo người dùng đã bấm Rate. */
        const val KEY_RATED = "rate_us_rated"

        /** Fragment result báo người dùng đóng dialog mà không rate. */
        const val KEY_EXIT = "rate_us_exit"

        private const val TAG = "RateUsDialog"
        private const val LOW_RATING_MAX = 3
        private const val DISABLED_ALPHA = 0.5f

        /** Hiện dialog, bỏ qua nếu đang mở. */
        fun showOnce(manager: FragmentManager) {
            if (manager.findFragmentByTag(TAG) == null) RateUsDialogFragment().show(manager, TAG)
        }
    }
}
