package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CounterpartyEntity
import com.example.data.InstallmentEntity
import com.example.data.InstallmentStatus
import com.example.data.ObligationEntity
import com.example.export.ReportExportEngine
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
    fun `read app_name and generate A4 PDF report`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("ترازینو", appName)

        val cp = CounterpartyEntity(id = 1L, name = "بانک مهر ایران")
        val ob = ObligationEntity(
            id = 1L,
            counterpartyId = 1L,
            title = "وام ازدواج",
            principalAmountToman = 120_000_000L,
            firstDueDateJalali = "1405/08/22"
        )
        val inst = InstallmentEntity(
            id = 1L,
            obligationId = 1L,
            installmentNumber = 1,
            title = "قسط اول وام ازدواج",
            dueDateJalali = "1405/08/22",
            amountToman = 10_000_000L,
            paidAmountToman = 10_000_000L,
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
