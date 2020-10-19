package com.fantasmaplasma.catfacts

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_tutorial.*

class TutorialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        setUpViews()
    }

    private fun setUpViews() {
        editTextTutorial
            .addTextChangedListener(object : TextWatcher {
                var charIncrease = 0
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    charIncrease = s?.length ?: 0
                }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    charIncrease = s?.length?.minus(charIncrease) ?: 0
                    if(charIncrease > 1) {
                        step_tv.text = getString(R.string.step_two)
                        send_btn.visibility = View.VISIBLE
                        send_btn.isEnabled = true
                        editTextTutorial.removeTextChangedListener(this)
                    }
                }
            })
        send_btn
            .setOnClickListener {
                step_tv.text = getString(R.string.step_three)
                editTextTutorial.text.clear()
            }
        back_btn
            .setOnClickListener {
                finish()
            }
    }
}