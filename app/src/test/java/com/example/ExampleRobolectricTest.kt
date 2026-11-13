package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.CounterpartyEntity
import com.example.data.EntityCategory
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import com.example.data.SeedDataEngine
import com.example.data.TarazinoDatabase
import com.example.export.ReportExportEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name, verify hierarchical seed tree in Room and generate A4 PDF report`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ترازینو", appName)

        // Verify hierarchical seed counterparties (SnappPay & DigiPay under BNPL_PLATFORM) in Room
        val db = Room.inMemoryDatabaseBuilder(context, TarazinoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val dao = db.tarazinoDao()
        SeedDataEngine.populateRealisticIranianSeedData(dao)

        val counterparties = dao.observeAllCounterparties().first()
        val obligations = dao.observeAllObligations().first()
        val installments = dao.observeAllInstallments().first()

        val bnplProviders = counterparties.filter { it.categoryEnum == EntityCategory.BNPL_PLATFORM }
        assertTrue(bnplProviders.any { it.name.contains("اسنپ‌پی") })
        assertTrue(bnplProviders.any { it.name.contains("دیجی‌پی") })
        assertTrue(obligations.size >= 8)
        assertTrue(installments.isNotEmpty())
        db.close()

        val cp = CounterpartyEntity(id = 1L, name = "اسنپ‌پی (SnappPay)", category = EntityCategory.BNPL_PLATFORM.name)
        val ob = ObligationEntity(
            id = 1L,
            counterpartyId = 1L,
            title = "خرید اقساطی لپ‌تاپ ایسوس",
            principalAmountToman = 16_000_000L,
            firstDueDateJalali = "1405/08/22"
        )
        val inst = InstallmentEntity(
            id = 1L,
            obligationId = 1L,
            installmentNumber = 1,
            title = "قسط ۱ اسنپ‌پی",
            dueDateJalali = "1405/08/22",
            amountToman = 4_000_000L,
            paidAmountToman = 4_000_000L,
            status = InstallmentStatus.PAID.name
        )
        val payload = ReportExportEngine.buildReportPayload(
            reportTitleFa = "گزارش تست ترازینو",
            subtitleFa = "تست رندر A4",
            obligations = listOf(ob),
            installments = listOf(inst),
            counterparties = listOf(cp)
        )
        val pdfFile = ReportExportEngine.generateStandardA4PdfFile(context, payload)
        assertTrue(pdfFile.exists() && pdfFile.length() > 0L)
    }
}
