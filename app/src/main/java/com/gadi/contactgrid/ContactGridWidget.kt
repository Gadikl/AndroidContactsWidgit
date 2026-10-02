package com.gadi.contactgrid

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class ContactGridWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (id in appWidgetIds) update(context, appWidgetManager, id)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) SlotStore.delete(context, id)
    }

    companion object {
        /** Bitmap size per cell. 9 x 320x320 ARGB ≈ 3.7 MB – well under the RemoteViews limit. */
        private const val IMG_PX = 320

        private val CELL = intArrayOf(
            R.id.cell0, R.id.cell1, R.id.cell2, R.id.cell3, R.id.cell4,
            R.id.cell5, R.id.cell6, R.id.cell7, R.id.cell8,
        )
        private val IMG = intArrayOf(
            R.id.img0, R.id.img1, R.id.img2, R.id.img3, R.id.img4,
            R.id.img5, R.id.img6, R.id.img7, R.id.img8,
        )
        private val NAME = intArrayOf(
            R.id.name0, R.id.name1, R.id.name2, R.id.name3, R.id.name4,
            R.id.name5, R.id.name6, R.id.name7, R.id.name8,
        )

        fun update(ctx: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(ctx.packageName, R.layout.widget_grid)
            val slots = SlotStore.load(ctx, widgetId)

            for (i in 0 until SlotStore.SLOT_COUNT) {
                val slot = slots[i]
                if (slot == null) {
                    views.setImageViewBitmap(IMG[i], ContactImages.empty(IMG_PX))
                    views.setTextViewText(NAME[i], "")
                    views.setOnClickPendingIntent(CELL[i], configIntent(ctx, widgetId, i))
                } else {
                    val bmp = ContactImages.photo(ctx, slot, IMG_PX)
                        ?: ContactImages.initials(slot.name, IMG_PX)
                    views.setImageViewBitmap(IMG[i], bmp)
                    views.setTextViewText(NAME[i], slot.name)
                    views.setContentDescription(CELL[i], slot.name)
                    views.setOnClickPendingIntent(CELL[i], callIntent(ctx, widgetId, i, slot.number))
                }
            }
            mgr.updateAppWidget(widgetId, views)
        }

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, ContactGridWidget::class.java))
            for (id in ids) update(ctx, mgr, id)
        }

        private const val PI_FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        private fun callIntent(ctx: Context, widgetId: Int, slot: Int, number: String): PendingIntent {
            val intent = Intent(ctx, CallActivity::class.java)
                .setData(Uri.parse("contactgrid://call/$widgetId/$slot"))
                .putExtra(CallActivity.EXTRA_NUMBER, number)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return PendingIntent.getActivity(ctx, widgetId * 16 + slot, intent, PI_FLAGS)
        }

        private fun configIntent(ctx: Context, widgetId: Int, slot: Int): PendingIntent {
            val intent = Intent(ctx, ConfigActivity::class.java)
                .setData(Uri.parse("contactgrid://config/$widgetId/$slot"))
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            return PendingIntent.getActivity(ctx, widgetId * 16 + slot, intent, PI_FLAGS)
        }
    }
}
