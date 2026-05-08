package com.firstapp.nixin_music

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import com.firstapp.nixin_music.databinding.ActivitySplashScreenBinding

class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding

    // How long the splash stays visible (ms)
    private val SPLASH_DELAY = 2200L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Match full-screen glass effect used in MainActivity / PlayerActivity
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        animateViews()

        // Navigate to MainActivity after delay
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            // Slide transition: new screen slides in from right, splash exits left
            overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.slide_out_right
            )
            finish()
        }, SPLASH_DELAY)
    }

    // ── ENTRY ANIMATIONS ──────────────────────────────────────────────────────
    private fun animateViews() {
        // Icon: fade + scale up from centre
        binding.splashIcon.alpha = 0f
        binding.splashIcon.scaleX = 0.6f
        binding.splashIcon.scaleY = 0.6f
        binding.splashIcon.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setStartDelay(100)
            .start()

        // Title: fade in + slide up slightly
        binding.splashTitle.alpha = 0f
        binding.splashTitle.translationY = 30f
        binding.splashTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(350)
            .start()

        // Tagline: fade in after title
        binding.splashTagline.alpha = 0f
        binding.splashTagline.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(650)
            .start()
    }

    // Disable back-press on splash so user can't skip straight to MainActivity
    // before the service / permission check is ready
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // intentionally empty — prevent back skip
    }
}