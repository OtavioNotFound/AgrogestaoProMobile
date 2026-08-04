package com.agrogestao.pro.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.agrogestao.pro.data.local.AgroDatabase
import com.agrogestao.pro.data.repository.AgroRepository
import com.agrogestao.pro.data.repository.SignUpOutcome
import com.agrogestao.pro.data.security.SecureSessionStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveSupabaseAuthTest {

    @Test
    fun signupKeepsProfileUntilEmailConfirmation() = runBlocking {
        val arguments = InstrumentationRegistry.getArguments()
        val email = arguments.getString("liveSupabaseEmail").orEmpty()
        val password = arguments.getString("liveSupabasePassword").orEmpty()
        assumeTrue("Credenciais temporárias não fornecidas", email.isNotBlank() && password.isNotBlank())

        val context = ApplicationProvider.getApplicationContext<Context>()
        val sessionStore = SecureSessionStore(context).also { it.clear() }
        val database = Room.inMemoryDatabaseBuilder(context, AgroDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = AgroRepository(
                backupDao = database.backupDao(),
                cropDao = database.cropDao(),
                taskDao = database.taskDao(),
                financialDao = database.financialDao(),
                producerDao = database.producerDao(),
                reportHistoryDao = database.reportHistoryDao(),
                reportConsentDao = database.reportConsentDao(),
                secureSessionStore = sessionStore
            )

            val result = repository.signUp(
                nome = "Produtor Confirmação",
                email = email,
                password = password,
                propriedade = "Sítio Teste",
                municipio = "Petrolina - PE",
                caf = "CAF-TESTE",
                area = 7.5
            )

            assertTrue(result.exceptionOrNull()?.message, result.isSuccess)
            assertEquals(SignUpOutcome.EMAIL_CONFIRMATION_REQUIRED, result.getOrNull())
            val profile = database.producerDao().getProducerProfileOnce()
            assertEquals("Produtor Confirmação", profile?.nomeProdutor)
            assertEquals("Sítio Teste", profile?.nomePropriedade)
            assertEquals(email.lowercase(), profile?.email)
            assertFalse(profile?.isLoggedIn ?: true)
        } finally {
            database.close()
            sessionStore.clear()
        }
    }
}
