package tech.relaycorp.gateway.ui.onboarding

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.data.preference.AppPreferences
import tech.relaycorp.gateway.databinding.ActivityOnboardingBinding
import tech.relaycorp.gateway.ui.BaseActivity
import tech.relaycorp.gateway.ui.main.MainActivity
import javax.inject.Inject

class OnboardingActivity : BaseActivity() {

    @Inject
    lateinit var appPreferences: AppPreferences

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pager.adapter = Adapter()
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.back.isVisible = position > 0
                binding.next.isVisible = position < ONBOARDING_PAGES.size - 1
            }
        })
        TabLayoutMediator(binding.indicators, binding.pager) { _, _ -> }.attach()

        binding.back.setOnClickListener {
            binding.pager.currentItem =
                (binding.pager.currentItem - 1).coerceAtLeast(0)
        }
        binding.next.setOnClickListener {
            binding.pager.currentItem =
                (binding.pager.currentItem + 1).coerceAtMost(ONBOARDING_PAGES.size - 1)
        }
    }

    internal fun getStarted() {
        lifecycleScope.launch {
            appPreferences.setOnboardingDone(true)
            startActivity(MainActivity.getIntent(this@OnboardingActivity))
            finish()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<OnboardingPageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            OnboardingPageViewHolder(
                LayoutInflater.from(this@OnboardingActivity)
                    .inflate(R.layout.item_onboarding, parent, false),
            )

        override fun onBindViewHolder(holder: OnboardingPageViewHolder, position: Int) {
            val page = ONBOARDING_PAGES[position]
            with(holder.itemView) {
                findViewById<TextView>(R.id.title).setText(page.title)
                findViewById<TextView>(R.id.text).setText(page.text)
                findViewById<ImageView>(R.id.image).setImageResource(page.image)
                findViewById<TextView>(R.id.getStarted).run {
                    isInvisible = !page.getStarted
                    setOnClickListener { getStarted() }
                }
            }
        }

        override fun getItemCount() = ONBOARDING_PAGES.size
    }

    private class OnboardingPageViewHolder(view: View) : RecyclerView.ViewHolder(view)

    internal data class OnboardingPage(
        @StringRes val title: Int,
        @StringRes val text: Int,
        @DrawableRes val image: Int,
        val getStarted: Boolean = false,
    )

    companion object {
        internal val ONBOARDING_PAGES = listOf(
            OnboardingPage(
                R.string.onboarding_title_1,
                R.string.onboarding_text_1,
                R.drawable.onboarding_image_1,
            ),
            OnboardingPage(
                R.string.onboarding_title_2,
                R.string.onboarding_text_2,
                R.drawable.onboarding_image_2,
            ),
            OnboardingPage(
                R.string.onboarding_title_3,
                R.string.onboarding_text_3,
                R.drawable.onboarding_image_3,
                true,
            ),
        )

        fun getIntent(context: Context) = Intent(context, OnboardingActivity::class.java)
    }
}
