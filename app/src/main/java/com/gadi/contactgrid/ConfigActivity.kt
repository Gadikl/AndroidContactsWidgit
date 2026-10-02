package com.gadi.contactgrid

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

/**
 * Edit screen for one widget: pick which contact sits in each of the 9 squares.
 * Opened when the widget is placed, from the launcher's "reconfigure" option,
 * by tapping an empty square, or from the app icon.
 */
class ConfigActivity : AppCompatActivity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var slots = arrayOfNulls<Slot>(SlotStore.SLOT_COUNT)
    private var pickingSlot = -1

    private val tileImages = ArrayList<ImageView>()
    private val tileNames = ArrayList<TextView>()
    private lateinit var callWarning: TextView
    private lateinit var contactsWarning: TextView

    private val pickContact =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
            val uri = res.data?.data
            val idx = pickingSlot
            pickingSlot = -1
            if (res.resultCode == RESULT_OK && uri != null && idx in slots.indices) {
                val picked = readPicked(uri)
                if (picked != null) {
                    slots[idx] = picked
                    render()
                } else {
                    Toast.makeText(this, R.string.no_number, Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val requestPerms =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            render()
            ContactGridWidget.updateAll(this) // photos appear once READ_CONTACTS is granted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // If the user backs out while placing the widget, the launcher removes it.
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        slots = if (savedInstanceState != null) {
            pickingSlot = savedInstanceState.getInt(STATE_PICKING, -1)
            SlotStore.fromJson(savedInstanceState.getString(STATE_SLOTS))
        } else {
            SlotStore.load(this, widgetId)
        }

        setContentView(buildUi())

        val missing = listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE)
            .filter { !granted(it) }
        if (missing.isNotEmpty() && savedInstanceState == null) {
            requestPerms.launch(missing.toTypedArray())
        }
        render()
    }

    override fun onResume() {
        super.onResume()
        if (::callWarning.isInitialized) render() // permissions may have changed in Settings
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SLOTS, SlotStore.toJson(slots))
        outState.putInt(STATE_PICKING, pickingSlot)
    }

    // ---------------------------------------------------------------- UI

    private fun dp(v: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(16))
        }

        root.addView(TextView(this).apply {
            setText(R.string.config_hint)
            textSize = 15f
            setPadding(0, 0, 0, dp(8))
        })

        callWarning = warningView(R.string.call_perm_off)
        contactsWarning = warningView(R.string.contacts_perm_off)
        root.addView(contactsWarning)
        root.addView(callWarning)

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(grid, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))

        for (r in 0 until 3) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            grid.addView(row, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
            for (c in 0 until 3) {
                val index = r * 3 + c
                val tile = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(6), dp(6), dp(6), dp(6))
                    isClickable = true
                    isFocusable = true
                    val tv = TypedValue()
                    theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
                    setBackgroundResource(tv.resourceId)
                    setOnClickListener { pick(index) }
                    setOnLongClickListener {
                        slots[index] = null
                        render()
                        true
                    }
                }
                val img = ImageView(this).apply { scaleType = ImageView.ScaleType.FIT_CENTER }
                val name = TextView(this).apply {
                    gravity = Gravity.CENTER
                    isSingleLine = true
                    ellipsize = TextUtils.TruncateAt.END
                    textSize = 13f
                }
                tile.addView(img, LinearLayout.LayoutParams(MATCH_PARENT, 0, 1f))
                tile.addView(name, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
                row.addView(tile, LinearLayout.LayoutParams(0, MATCH_PARENT, 1f))
                tileImages.add(img)
                tileNames.add(name)
            }
        }

        root.addView(
            MaterialButton(this).apply {
                setText(R.string.save)
                setOnClickListener { save() }
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { topMargin = dp(12) }
        )
        return root
    }

    private fun warningView(textRes: Int) = TextView(this).apply {
        setText(textRes)
        textSize = 13f
        setTextColor(0xFFD1242F.toInt())
        setPadding(0, dp(4), 0, dp(4))
        setOnClickListener {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
            )
        }
    }

    private fun render() {
        val px = dp(96)
        for (i in 0 until SlotStore.SLOT_COUNT) {
            val s = slots[i]
            if (s == null) {
                tileImages[i].setImageBitmap(ContactImages.empty(px))
                tileNames[i].setText(R.string.add)
            } else {
                tileImages[i].setImageBitmap(ContactImages.photo(this, s, px) ?: ContactImages.initials(s.name, px))
                tileNames[i].text = s.name
            }
        }
        callWarning.visibility = if (granted(Manifest.permission.CALL_PHONE)) View.GONE else View.VISIBLE
        contactsWarning.visibility = if (granted(Manifest.permission.READ_CONTACTS)) View.GONE else View.VISIBLE
    }

    // ---------------------------------------------------------------- logic

    private fun granted(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    /** Opens the system contact picker, filtered to entries that have a phone number. */
    private fun pick(index: Int) {
        pickingSlot = index
        try {
            pickContact.launch(Intent(Intent.ACTION_PICK, Phone.CONTENT_URI))
        } catch (e: Exception) {
            pickingSlot = -1
            Toast.makeText(this, e.localizedMessage ?: "No contacts app", Toast.LENGTH_SHORT).show()
        }
    }

    /** Reads the picked phone row. The picker grants temporary read access to this URI. */
    private fun readPicked(uri: Uri): Slot? = try {
        contentResolver.query(
            uri,
            arrayOf(Phone.CONTACT_ID, Phone.LOOKUP_KEY, Phone.DISPLAY_NAME, Phone.NUMBER),
            null, null, null
        )?.use { c ->
            if (!c.moveToFirst()) {
                null
            } else {
                val number = c.getString(3)
                if (number.isNullOrBlank()) {
                    null
                } else {
                    Slot(
                        contactId = c.getLong(0),
                        lookupKey = c.getString(1),
                        name = c.getString(2) ?: number,
                        number = number,
                    )
                }
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun save() {
        SlotStore.save(this, widgetId, slots)
        ContactGridWidget.update(this, AppWidgetManager.getInstance(this), widgetId)
        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }

    companion object {
        private const val STATE_SLOTS = "slots"
        private const val STATE_PICKING = "picking"
    }
}
