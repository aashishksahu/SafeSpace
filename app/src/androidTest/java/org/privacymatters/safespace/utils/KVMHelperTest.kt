package org.privacymatters.safespace.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.privacymatters.safespace.auth.AuthCrypto

@RunWith(AndroidJUnit4::class)
class KVMHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    @Test
    fun randomAlphaNumericGenerator() = runTest {
        val result1 = AuthCrypto.randomAlphanumeric(5)
        println(result1)
        val result2 = AuthCrypto.randomAlphanumeric(10)
        println(result2)
        val result3 = AuthCrypto.randomAlphanumeric(15)
        println(result3)
        val result4 = AuthCrypto.randomAlphanumeric(20)
        println(result4)

        assertEquals(5, result1.length)
        assertEquals(10, result2.length)
        assertEquals(15, result3.length)
        assertEquals(20, result4.length)

    }

    @Test
    fun setValue_and_getValue() = runTest {
        val resultArray: MutableList<KVM> = mutableListOf()

        for (i in 0..10) {
            KVMHelper.setValue(context, "key${i}", "value${i}")
            val result = KVMHelper.getValue(context, "key${i}")
            resultArray += KVM(instance = mapOf("key${i}" to result))

        }

        for (i in 0..10) {
            assertEquals("value${i}", resultArray[i].instance["key${i}"])
        }
    }

    @Test
    fun setValueEncrypted_and_getValueEncrypted() = runTest {
        val resultArray: MutableList<KVM> = mutableListOf()

        for (i in 0..10) {
            KVMHelper.setValueEncrypted(context, "key${i}", "enc_value${i}")
            val result = KVMHelper.getValueEncrypted(context, "key${i}") ?: ""
            resultArray += KVM(instance = mapOf("key${i}" to result))
        }

        for (i in 0..10) {
            assertEquals("enc_value${i}", resultArray[i].instance["key${i}"])
        }

    }
}
