package tech.relaycorp.gateway.ui.settings

import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertNotDisplayed
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.gateway.BuildConfig
import tech.relaycorp.gateway.R
import tech.relaycorp.gateway.data.database.StoredParcelDao
import tech.relaycorp.gateway.data.model.RecipientLocation
import tech.relaycorp.gateway.test.AppTestProvider.component
import tech.relaycorp.gateway.test.AppTestProvider.context
import tech.relaycorp.gateway.test.BaseActivityTestRule
import tech.relaycorp.gateway.test.WaitAssertions.waitFor
import tech.relaycorp.gateway.test.factory.StoredParcelFactory
import tech.relaycorp.gateway.ui.common.format
import javax.inject.Inject

class SettingsActivityTest {

    @Rule
    @JvmField
    val testRule = BaseActivityTestRule(SettingsActivity::class, false)

    @Inject
    lateinit var storedParcelDao: StoredParcelDao

    @Before
    fun setUp() {
        component.inject(this)
    }

    @Test
    fun displaysVersion() {
        testRule.start()
        assertContains(BuildConfig.VERSION_CODE.toString())
        assertContains(BuildConfig.VERSION_NAME)
    }

    @Test
    fun doesNotDisplayOutgoingData_ifEmpty() {
        testRule.start()
        waitFor {
            assertNotDisplayed(R.string.settings_data)
        }
    }

    @Test
    fun displaysOutgoingData_ifAny() {
        val parcel = StoredParcelFactory.build()
            .copy(recipientLocation = RecipientLocation.ExternalGateway)
        runBlocking { storedParcelDao.insert(parcel) }
        testRule.start()
        waitFor {
            assertDisplayed(R.string.settings_data)
            assertDisplayed(parcel.size.format(context))
        }
    }
}
