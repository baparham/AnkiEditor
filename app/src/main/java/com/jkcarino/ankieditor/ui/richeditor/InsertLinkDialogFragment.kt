/*
 * Copyright (C) 2018 Jhon Kenneth Cariño
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jkcarino.ankieditor.ui.richeditor

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.view.LayoutInflater
import android.view.WindowManager
import com.jkcarino.ankieditor.R
import kotlinx.android.synthetic.main.dialog_insert_link.view.*

class InsertLinkDialogFragment : AppCompatDialogFragment() {

    var onInsertClickListener: OnInsertClickListener? = null

    override fun show(manager: FragmentManager, tag: String) {
        if (manager.findFragmentByTag(tag) == null) {
            super.show(manager, tag)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_insert_link, null)

        return AlertDialog.Builder(activity!!).apply {
            setTitle(R.string.title_insert_link)
            setView(view)
            setPositiveButton(R.string.insert) { _, _ ->
                val title = view.text_to_display.text.toString().trim()
                val url = view.link_to.text.toString().trim()

                onInsertClickListener?.onInsertClick(title, url)
            }
            setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
        }.create()
    }

    interface OnInsertClickListener {
        fun onInsertClick(title: String, url: String)
    }

    companion object {
        fun newInstance() = InsertLinkDialogFragment()
    }
}
