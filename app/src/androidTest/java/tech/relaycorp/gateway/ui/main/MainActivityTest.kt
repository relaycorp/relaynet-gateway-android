package tech.relaycorp.gateway.ui.main

import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotContains
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.data.preference.AppPreferences
import tech.relaycorp.gateway.test.AppTestProvider.component
import tech.relaycorp.gateway.test.BaseActivityTestRule
import javax.inject.Inject

class MainActivityTest {

    @Rule
    @JvmField
    val testRule = BaseActivityTestRule(MainActivity::class, false)

    @Inject
    lateinit var appPreferences: AppPreferences

    @Before
    fun setUp() {
        component.inject(this)
    }

    @Test
    fun opensOnboarding_ifFirstTime() {
        testRule.start()
        assertDisplayed(R.string.onboarding_title_1)
    }

    @Test
    fun doesNotOpenOnboarding_ifOnboardingIsDone() {
        runBlocking { appPreferences.setOnboardingDone(true) }
        testRule.start()
        assertNotContains(R.string.onboarding_title_1)
    }
}
