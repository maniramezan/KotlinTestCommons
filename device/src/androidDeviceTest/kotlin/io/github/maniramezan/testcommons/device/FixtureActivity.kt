package io.github.maniramezan.testcommons.device

import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class FixtureActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val delayed = TextView(this).apply {
            text = "Loaded"
            visibility = View.GONE
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                Button(this@FixtureActivity).apply {
                    id = android.R.id.button1
                    text = "Save"
                },
            )
            addView(
                EditText(this@FixtureActivity).apply {
                    id = android.R.id.edit
                    hint = "Name"
                },
            )
            addView(
                EditText(this@FixtureActivity).apply {
                    id = android.R.id.input
                    hint = "Password"
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                },
            )
            addView(delayed)
            repeat(60) { index ->
                addView(
                    TextView(this@FixtureActivity).apply {
                        text = "Item $index"
                        textSize = 24f
                    },
                )
            }
        }
        setContentView(ScrollView(this).apply { addView(column) })
        Handler(Looper.getMainLooper()).postDelayed({ delayed.visibility = View.VISIBLE }, 500)
    }
}
