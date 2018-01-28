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

package com.jkcarino.ankieditor.ui.editor

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import com.ichi2.anki.api.AddContentApi
import com.jkcarino.ankieditor.R
import com.jkcarino.ankieditor.extensions.showSnackBar
import com.jkcarino.ankieditor.ui.richeditor.RichEditorActivity
import com.jkcarino.ankieditor.util.AnkiDroidHelper
import com.jkcarino.ankieditor.util.PlayStoreUtils
import kotlinx.android.synthetic.main.fragment_editor.*
import kotlinx.android.synthetic.main.view_request_permission.*
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions

class EditorFragment : Fragment(), EditorContract.View, EasyPermissions.PermissionCallbacks {

    private lateinit var presenter: EditorContract.Presenter

    private var requestPermissionView: View? = null

    /** ID of the selected note type  */
    private var noteTypeId: Long = 0

    /** ID of the selected deck  */
    private var deckId: Long = 0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkAnkiDroidAvailability()

        add_note_button.setOnClickListener {
            if (!AnkiDroidHelper.isAnkiDroidInstalled(activity!!)) {
                AnkiDroidHelper.showNoAnkiInstalledDialog(activity!!)
            } else {
                if (AnkiDroidHelper.isApiAvailable(activity!!)) {
                    presenter.addNote(noteTypeId, deckId, note_fields_container.fieldsText)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        note_fields_container.onFieldOptionsClickListener = null
    }

    override fun setPresenter(presenter: EditorContract.Presenter) {
        this.presenter = presenter
    }

    private fun checkAnkiDroidAvailability() {
        if (!AnkiDroidHelper.isAnkiDroidInstalled(activity!!)) {
            AnkiDroidHelper.showNoAnkiInstalledDialog(activity!!)
        } else {
            if (AnkiDroidHelper.isApiAvailable(activity!!)) {
                requestAnkiDroidPermissionIfNecessary()
            }
        }
    }

    private fun loadAnkiEditor() {
        presenter.populateNoteTypes()
        presenter.populateNoteDecks()

        note_fields_container.onFieldOptionsClickListener = onFieldOptionsClickListener

        requestPermissionView?.visibility = View.GONE
        editor_layout.visibility = View.VISIBLE
    }

    private val onFieldOptionsClickListener =
            object : NoteTypeFieldsContainer.OnFieldOptionsClickListener {

                override fun onClozeDeletionClick(
                        index: Int,
                        text: String,
                        selectionStart: Int,
                        selectionEnd: Int
                ) {
                    presenter.insertClozeAround(index, text, selectionStart, selectionEnd)
                }

                override fun onAdvancedEditorClick(
                        index: Int,
                        fieldName: String,
                        text: String
                ) {
                    val intent = RichEditorActivity.newIntent(activity!!, index, fieldName, text)
                    startActivityForResult(intent, RC_FIELD_EDIT)
                }
            }

    private fun setupRequestPermissionLayout() {
        // Permission not yet granted, hide the main editor
        editor_layout.visibility = View.GONE

        // Show request permission layout
        requestPermissionView = request_permission_stub.inflate()

        allow_button.setOnClickListener {
            EasyPermissions.requestPermissions(
                    this@EditorFragment,
                    getString(R.string.rationale_ad_api_permission_ask_again),
                    RC_AD_READ_WRITE_PERM,
                    AddContentApi.READ_WRITE_PERMISSION
            )
        }
    }

    @AfterPermissionGranted(RC_AD_READ_WRITE_PERM)
    private fun requestAnkiDroidPermissionIfNecessary() {
        if (EasyPermissions.hasPermissions(
                        activity?.applicationContext,
                        AddContentApi.READ_WRITE_PERMISSION)) {
            loadAnkiEditor()
        } else {
            setupRequestPermissionLayout()
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) = Unit

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            view?.showSnackBar(R.string.sb_permission_denied, Snackbar.LENGTH_LONG)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            PlayStoreUtils.RC_OPEN_PLAY_STORE -> {
                checkAnkiDroidAvailability()
            }
            AppSettingsDialog.DEFAULT_SETTINGS_REQ_CODE -> {
                if (EasyPermissions.hasPermissions(activity!!.applicationContext,
                                AddContentApi.READ_WRITE_PERMISSION)) {
                    loadAnkiEditor()
                }
            }
            EditorFragment.RC_FIELD_EDIT -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.extras?.let {
                        val index = it.getInt(RichEditorActivity.EXTRA_FIELD_INDEX)
                        val text = it.getString(RichEditorActivity.EXTRA_FIELD_TEXT, "")

                        note_fields_container.setFieldText(index, text)
                    }
                }
            }
        }
    }

    override fun showNoteTypes(ids: List<Long>, noteTypes: List<String>) {
        // Update the note types spinner
        val noteTypesAdapter =
                ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, noteTypes)
        note_type_spinner.adapter = noteTypesAdapter
        note_type_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                noteTypeId = ids[pos]
                presenter.populateNoteTypeFields(noteTypeId)
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }

    override fun showNoteDecks(ids: List<Long>, noteDecks: List<String>) {
        // Update the note decks spinner
        val noteDecksAdapter =
                ArrayAdapter(activity, android.R.layout.simple_spinner_dropdown_item, noteDecks)
        deck_spinner.adapter = noteDecksAdapter
        deck_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                deckId = ids[pos]
            }

            override fun onNothingSelected(parent: AdapterView<*>) = Unit
        }
    }

    override fun showNoteTypeFields(fields: Array<String>) {
        note_fields_container.addFields(fields)
    }

    override fun setInsertedClozeText(index: Int, text: String) {
        note_fields_container.setFieldText(index, text)
    }

    override fun setAddNoteSuccess() {
        note_fields_container.clearFields()
        view?.showSnackBar(R.string.sb_add_note_success, Snackbar.LENGTH_SHORT)
    }

    override fun setAddNoteFailure() {
        view?.showSnackBar(R.string.sb_add_note_failure, Snackbar.LENGTH_LONG)
    }

    companion object {
        private const val RC_AD_READ_WRITE_PERM = 0x01
        private const val RC_FIELD_EDIT = 0x02

        fun newInstance() = EditorFragment()
    }
}