package com.conght.facemakerapp.feature.splash

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.conght.facemakerapp.R
import com.conght.facemakerapp.core.ui.applyStatusBarPadding
import com.conght.facemakerapp.core.ui.base.BaseFragment
import com.conght.facemakerapp.core.ui.setLightStatusBar
import com.conght.facemakerapp.databinding.FragmentSplashBinding
import com.conght.facemakerapp.feature.intro.IntroFragment
import com.conght.facemakerapp.feature.language.LanguageFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@AndroidEntryPoint
class SplashFragment : BaseFragment<FragmentSplashBinding>(FragmentSplashBinding::inflate) {

    private val viewModel: SplashViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.content.applyStatusBarPadding()
        setLightStatusBar(false)

        viewLifecycleOwner.lifecycleScope.launch {
            runProgress()
            val nav = findNavController()
            // Intro hiện mỗi lần mở app; Language + Permission chỉ ở lần đầu
            if (viewModel.isOnboardingDone()) {
                nav.navigate(
                    R.id.action_splash_to_intro,
                    Bundle().apply { putBoolean(IntroFragment.ARG_ONBOARDING_DONE, true) },
                )
            } else {
                nav.navigate(
                    R.id.action_splash_to_language,
                    Bundle().apply { putBoolean(LanguageFragment.ARG_FROM_SETTING, false) },
                )
            }
        }
    }

    /** Chạy progress 0 → 100 trong [PROGRESS_MS]; huỷ animator nếu coroutine bị huỷ. */
    private suspend fun runProgress() = suspendCancellableCoroutine { cont ->
        val animator = ValueAnimator.ofInt(0, binding.progress.max).apply {
            duration = PROGRESS_MS
            interpolator = LinearInterpolator()
            addUpdateListener { binding.progress.progress = it.animatedValue as Int }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (cont.isActive) cont.resume(Unit)
                }
            })
        }
        cont.invokeOnCancellation { animator.cancel() }
        animator.start()
    }

    private companion object {
        const val PROGRESS_MS = 2500L
    }
}
