package com.gadi.contactgrid

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/** App icon entry point: jumps to the editor of the widget on the home screen. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mgr = AppWidgetManager.getInstance(this)
        val provider = ComponentName(this, ContactGridWidget::class.java)
        val ids = mgr.getAppWidgetIds(provider)

        when {
            ids.size == 1 -> {
                openConfig(ids[0])
                finish()
            }
            ids.size > 1 -> {
                val labels = ids.mapIndexed { i, id ->
                    val names = SlotStore.load(this, id).filterNotNull().take(3).joinToString(", ") { it.name }
                    getString(R.string.widget_n, i + 1) + if (names.isNotEmpty()) " – $names" else ""
                }.toTypedArray()
                AlertDialog.Builder(this)
                    .setTitle(R.string.choose_widget)
                    .setItems(labels) { _, which -> openConfig(ids[which]) }
                    .setOnDismissListener { finish() }
                    .show()
            }
            else -> showNoWidget(mgr, provider)
        }
    }

    private fun openConfig(id: Int) {
        startActivity(
            Intent(this, ConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        )
    }

    private fun showNoWidget(mgr: AppWidgetManager, provider: ComponentName) {
        val pad = (24 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(pad, pad, pad, pad)
        }
        root.addView(TextView(this).apply {
            setText(R.string.no_widgets)
            textSize = 16f
            gravity = Gravity.CENTER
        })
        if (mgr.isRequestPinAppWidgetSupported) {
            root.addView(
                MaterialButton(this).apply {
                    setText(R.string.pin_widget)
                    setOnClickListener {
                        mgr.requestPinAppWidget(provider, null, null)
                        finish()
                    }
                },
                LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply { topMargin = pad }
            )
        }
        setContentView(root)
    }
}
