package com.example.onyx.OnyxClasses

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import com.example.onyx.R

// Callback interface for search actions
interface OnSearchListener {
    fun EnterActionTrigger(query: String)
}

class CustomKeyboardManager(
    private val context: Context,
    private val searchEditText: EditText,
    private val keyboardLayout: LinearLayout,
    private val searchListener: OnSearchListener? = null
) {

    init {
        setupKeyboardButtons()
    }

    private fun setupKeyboardButtons() {
        // Numbers row (0-9)
        setupButton(R.id.key_0, "0")
        setupButton(R.id.key_1, "1")
        setupButton(R.id.key_2, "2")
        setupButton(R.id.key_3, "3")
        setupButton(R.id.key_4, "4")
        setupButton(R.id.key_5, "5")
        setupButton(R.id.key_6, "6")
        setupButton(R.id.key_7, "7")
        setupButton(R.id.key_8, "8")
        setupButton(R.id.key_9, "9")

        // First row (Q-P)
        setupButton(R.id.key_q, "Q")
        setupButton(R.id.key_w, "W")
        setupButton(R.id.key_e, "E")
        setupButton(R.id.key_r, "R")
        setupButton(R.id.key_t, "T")
        setupButton(R.id.key_y, "Y")
        setupButton(R.id.key_u, "U")
        setupButton(R.id.key_i, "I")
        setupButton(R.id.key_o, "O")
        setupButton(R.id.key_p, "P")

        // Second row (A-L)
        setupButton(R.id.key_a, "A")
        setupButton(R.id.key_s, "S")
        setupButton(R.id.key_d, "D")
        setupButton(R.id.key_f, "F")
        setupButton(R.id.key_g, "G")
        setupButton(R.id.key_h, "H")
        setupButton(R.id.key_j, "J")
        setupButton(R.id.key_k, "K")
        setupButton(R.id.key_l, "L")

        // Third row (Z-M + Backspace)
        setupButton(R.id.key_z, "Z")
        setupButton(R.id.key_x, "X")
        setupButton(R.id.key_c, "C")
        setupButton(R.id.key_v, "V")
        setupButton(R.id.key_b, "B")
        setupButton(R.id.key_n, "N")
        setupButton(R.id.key_m, "M")

        // Special keys
        setupBackspaceButton()
        setupSpaceButton()
        setupEnterButton()
        setupClearButton()
    }

    private fun setupButton(buttonId: Int, character: String) {
        val button = keyboardLayout.findViewById<View>(buttonId)
        button?.setOnClickListener {
            insertText(character)
        }
    }

    private fun setupBackspaceButton() {
        val backspaceButton = keyboardLayout.findViewById<View>(R.id.key_backspace)
        backspaceButton?.setOnClickListener {
            val currentText = searchEditText.text.toString()
            if (currentText.isNotEmpty()) {
                val newText = currentText.substring(0, currentText.length - 1)
                searchEditText.setText(newText)
                searchEditText.setSelection(newText.length)
            }
        }
    }

    private fun setupSpaceButton() {
        val spaceButton = keyboardLayout.findViewById<View>(R.id.key_space)
        spaceButton?.setOnClickListener {
            insertText(" ")
        }
    }

    private fun setupEnterButton() {
        val enterButton = keyboardLayout.findViewById<View>(R.id.key_enter)
        enterButton?.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                searchListener?.EnterActionTrigger(query)
            }
        }
    }

    private fun setupClearButton() {
        val clearButton = keyboardLayout.findViewById<View>(R.id.key_clear)
        clearButton?.setOnClickListener {
            clearText()
        }
    }

    private fun insertText(text: String) {
        val currentText = searchEditText.text.toString()
        val cursorPosition = searchEditText.selectionStart
        val newText = currentText.substring(0, cursorPosition) + text + currentText.substring(cursorPosition)
        searchEditText.setText(newText)
        searchEditText.setSelection(cursorPosition + text.length)
    }

    fun showKeyboard() {
        keyboardLayout.visibility = View.VISIBLE
        // Hide the system keyboard
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    fun hideKeyboard() {
        keyboardLayout.visibility = View.GONE
    }

    fun isKeyboardVisible(): Boolean {
        return keyboardLayout.visibility == View.VISIBLE
    }

    fun clearText() {
        searchEditText.text.clear()
    }
}

