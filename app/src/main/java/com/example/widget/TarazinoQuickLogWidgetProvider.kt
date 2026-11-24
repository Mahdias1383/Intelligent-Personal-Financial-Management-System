package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.example.R
import com.example.core.JalaliCalendarEngine
import com.example.core.PersianCurrencyEngine
import com.example.data.TarazinoDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android Home Screen Widget provider (`Tarazino • ثبت سریع`) enabling users to:
 * 1. View their live remaining debt total and most urgent next due installment.
 * 2. Settle the next due installment in **1 tap directly on the home screen** without opening the app.
 * 3. Launch a lightweight floating quick-log overlay over the home screen to log a new expense
 *    or a custom/partial debt payment without launching the full application.
 */
class TarazinoQuickLogWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgetsFromDatabase(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_QUICK_PAY_NEXT_INSTALLMENT -> {
                val targetInstallmentId = intent.getLongExtra(EXTRA_INSTALLMENT_ID, -1L)
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = TarazinoDatabase.getInstance(context)
                        val dao = db.tarazinoDao()
                        val allInsts = dao.getAllInstallmentsOnce()
                        val target = allInsts.firstOrNull { it.id == targetInstallmentId }
                        if (target != null && target.remainingToman > 0L) {
                            val updated = QuickLogWidgetEngine.applyQuickDebtPayment(
                                installment = target,
                                paymentAmountToman = target.remainingToman,
                                todayJalali = JalaliCalendarEngine.todayJalali()
                            )
                            dao.updateInstallment(updated)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "«${target.title}» با موفقیت پرداخت و ثبت شد.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        val manager = AppWidgetManager.getInstance(context)
                        val ids = manager.getAppWidgetIds(
                            ComponentName(context, TarazinoQuickLogWidgetProvider::class.java)
                        )
                        if (ids.isNotEmpty()) {
                            updateWidgetsFromDatabase(context, manager, ids)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_REFRESH_WIDGET -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val manager = AppWidgetManager.getInstance(context)
                        val ids = manager.getAppWidgetIds(
                            ComponentName(context, TarazinoQuickLogWidgetProvider::class.java)
                        )
                        if (ids.isNotEmpty()) {
                            updateWidgetsFromDatabase(context, manager, ids)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_QUICK_PAY_NEXT_INSTALLMENT =
            "com.example.widget.ACTION_QUICK_PAY_NEXT_INSTALLMENT"
        const val ACTION_REFRESH_WIDGET =
            "com.example.widget.ACTION_REFRESH_WIDGET"
        const val EXTRA_INSTALLMENT_ID = "extra_installment_id"
        const val EXTRA_QUICK_LOG_MODE = "extra_quick_log_mode"

        /**
         * Refreshes all placed instances of the Tarazino Quick-Log Home Screen Widget.
         */
        fun requestRefreshAll(context: Context) {
            val appCtx = context.applicationContext
            val manager = AppWidgetManager.getInstance(appCtx)
            val ids = manager.getAppWidgetIds(
                ComponentName(appCtx, TarazinoQuickLogWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    updateWidgetsFromDatabase(appCtx, manager, ids)
                }
            }
        }

        /**
         * Requests the Android launcher (API 26+) to pin the Quick-Log Widget onto the user's home screen.
         * Returns `true` if the pin request dialog was triggered, `false` if unsupported by launcher.
         */
        fun requestPinToHomeScreen(context: Context): Boolean {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return false
            val provider = ComponentName(context, TarazinoQuickLogWidgetProvider::class.java)
            return manager.requestPinAppWidget(provider, null, null)
        }

        private suspend fun updateWidgetsFromDatabase(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val dao = TarazinoDatabase.getInstance(context).tarazinoDao()
            val obligations = dao.getAllObligationsOnce()
            val installments = dao.getAllInstallmentsOnce()
            val snapshot = QuickLogWidgetEngine.buildSnapshot(
                obligations = obligations,
                installments = installments
            )

            appWidgetIds.forEach { appWidgetId ->
                val views = buildRemoteViews(context, snapshot)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            snapshot: QuickLogWidgetSnapshot
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_tarazino_quick_log)
            views.setTextViewText(R.id.widget_total_debt_summary, snapshot.summaryHeaderFa)

            if (snapshot.nextDueInstallmentId != null) {
                views.setTextViewText(
                    R.id.widget_next_due_title,
                    snapshot.nextDueObligationTitleFa
                )
                val formattedAmount = PersianCurrencyEngine.formatAmount(snapshot.nextDueRemainingToman)
                views.setTextViewText(
                    R.id.widget_next_due_subtitle,
                    "$formattedAmount • ${snapshot.nextDueRelativeLabelFa}"
                )
                views.setViewVisibility(R.id.widget_btn_one_tap_pay, View.VISIBLE)

                val payIntent = Intent(context, TarazinoQuickLogWidgetProvider::class.java).apply {
                    action = ACTION_QUICK_PAY_NEXT_INSTALLMENT
                    putExtra(EXTRA_INSTALLMENT_ID, snapshot.nextDueInstallmentId)
                }
                val payPending = PendingIntent.getBroadcast(
                    context,
                    1001,
                    payIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_one_tap_pay, payPending)
            } else {
                views.setTextViewText(
                    R.id.widget_next_due_title,
                    context.getString(R.string.widget_no_pending_debt)
                )
                views.setTextViewText(
                    R.id.widget_next_due_subtitle,
                    "برای ثبت هزینه جدید روی «+ ثبت هزینه» بزنید"
                )
                views.setViewVisibility(R.id.widget_btn_one_tap_pay, View.GONE)
            }

            // Refresh button
            val refreshIntent = Intent(context, TarazinoQuickLogWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                1002,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPending)

            // Quick Log Expense button -> opens floating translucent dialog overlay (not full app)
            val expenseIntent = Intent(context, QuickLogWidgetActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_QUICK_LOG_MODE, WidgetQuickLogMode.EXPENSE.name)
            }
            val expensePending = PendingIntent.getActivity(
                context,
                2001,
                expenseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_log_expense, expensePending)

            // Quick Log Debt Payment button -> opens floating translucent dialog overlay
            val paymentIntent = Intent(context, QuickLogWidgetActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_QUICK_LOG_MODE, WidgetQuickLogMode.DEBT_PAYMENT.name)
            }
            val paymentPending = PendingIntent.getActivity(
                context,
                2002,
                paymentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_log_payment, paymentPending)

            return views
        }
    }
}
