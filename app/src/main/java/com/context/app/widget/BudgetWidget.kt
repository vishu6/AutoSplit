package com.context.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.context.app.MainActivity
import com.context.app.R
import com.context.data.Expense
import com.context.data.ExpenseDatabase
import com.context.utils.BudgetUtils
import com.context.utils.CurrencyMasker
import com.context.utils.DateFilterUtils
import com.context.utils.SecurityUtils
import com.context.utils.TimeRange
import java.util.Calendar

class BudgetWidget : GlanceAppWidget() {

    companion object {
        val KEY_WIDGET_ACTION = ActionParameters.Key<String>("widget_action")
        const val ACTION_ADD_EXPENSE = "add_expense"
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val budget = BudgetUtils.getMonthlyBudget(context)
        val db = ExpenseDatabase.getDatabase(context)
        val isPrivacyMode = SecurityUtils.isPrivacyModeEnabled(context)
        
        val (monthStart, monthEnd) = DateFilterUtils.getTimeRange(TimeRange.MONTH, Calendar.getInstance())
        
        val dbExpenses = db.expenseDao().getExpensesSince(monthStart)
        val currentMonthExpenses = dbExpenses.filter { 
            it.timestamp <= monthEnd && it.category != "Settlement" 
        }
        
        val totalSpent = currentMonthExpenses.sumOf { it.amount }
        val recent = currentMonthExpenses.sortedByDescending { it.timestamp }.take(2)
        
        provideContent {
            GlanceTheme(colors = CleaveWidgetTheme.colors) {
                BudgetWidgetContent(context, budget, totalSpent, recent, isPrivacyMode)
            }
        }
    }

    @Composable
    private fun BudgetWidgetContent(
        context: Context, 
        budget: Double, 
        spent: Double,
        recent: List<Expense> = emptyList(),
        isPrivacyMode: Boolean = false
    ) {
        val remaining = (budget - spent).coerceAtLeast(0.0)
        val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.background)
        ) {
            // Main content column - Clickable to open app safely (R8 safe)
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.Start,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remaining",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    
                    // FIXED: Reliable Refresh Button with Tint
                    Box(
                        modifier = GlanceModifier
                            .size(28.dp)
                            .padding(4.dp)
                            .clickable(actionRunCallback<RefreshCallback>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_refresh),
                            contentDescription = "Refresh",
                            modifier = GlanceModifier.size(18.dp),
                            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary)
                        )
                    }

                    Spacer(modifier = GlanceModifier.size(32.dp))
                }

                val remainingText = CurrencyMasker.formatAmount(remaining, isPrivacyMode)
                Text(
                    text = remainingText,
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    )
                )

                Spacer(modifier = GlanceModifier.size(4.dp))

                LinearProgressIndicator(
                    progress = progress,
                    modifier = GlanceModifier.fillMaxWidth(),
                    color = GlanceTheme.colors.primary,
                    backgroundColor = GlanceTheme.colors.secondaryContainer
                )

                Spacer(modifier = GlanceModifier.size(4.dp))

                Row(modifier = GlanceModifier.fillMaxWidth()) {
                    val spentText = CurrencyMasker.formatSmallAmount(spent, isPrivacyMode)
                    Text(
                        text = "Spent $spentText",
                        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = TextStyle(fontSize = 10.sp, color = GlanceTheme.colors.onSurfaceVariant)
                    )
                }

                if (recent.isNotEmpty()) {
                    Spacer(modifier = GlanceModifier.size(10.dp))
                    Text(
                        text = "RECENT",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.primary
                        )
                    )
                    Spacer(modifier = GlanceModifier.size(4.dp))
                    
                    recent.forEach { expense ->
                        Row(
                            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 1.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = expense.merchant,
                                style = TextStyle(
                                    fontSize = 11.sp, 
                                    color = GlanceTheme.colors.onSurface
                                ),
                                maxLines = 1
                            )
                            Spacer(modifier = GlanceModifier.defaultWeight())
                            val expenseAmountText = CurrencyMasker.formatSmallAmount(expense.amount, isPrivacyMode)
                            Text(
                                text = expenseAmountText,
                                style = TextStyle(
                                    fontSize = 11.sp, 
                                    fontWeight = FontWeight.Medium,
                                    color = GlanceTheme.colors.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // Quick Add Button
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(30.dp)
                        .background(GlanceTheme.colors.primary)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(KEY_WIDGET_ACTION to ACTION_ADD_EXPENSE)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        style = TextStyle(
                            color = GlanceTheme.colors.onPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}

class RefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        BudgetWidget().updateAll(context)
    }
}
