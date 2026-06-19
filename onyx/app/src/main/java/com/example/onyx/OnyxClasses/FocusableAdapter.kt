package com.example.onyx.OnyxClasses

import android.view.View

interface FocusableAdapter<T> {
    var onItemFocused: ((View, T) -> Unit)?
    var onItemFocusLost: (() -> Unit)?
    fun getItem(position: Int): T?
}
