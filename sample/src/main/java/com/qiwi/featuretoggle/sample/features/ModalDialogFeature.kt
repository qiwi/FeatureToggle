/**
 * Copyright (c) 2021 QIWI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.qiwi.featuretoggle.sample.features

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qiwi.featuretoggle.annotation.Factory
import com.qiwi.featuretoggle.annotation.FeatureFlag
import com.qiwi.featuretoggle.factory.SimpleFeatureFactory
import com.qiwi.featuretoggle.flag.SimpleFeatureFlag
import com.qiwi.featuretoggle.sample.R

interface ModalDialogFeature {

    fun showDialog(activity: AppCompatActivity)
}

class BottomSheetDialogModal : ModalDialogFeature {

    override fun showDialog(activity: AppCompatActivity) {
        val modalBottomSheet = ModalBottomSheet()
        modalBottomSheet.show(activity.supportFragmentManager, ModalBottomSheet.TAG)
    }
}

class AlertDialogModal : ModalDialogFeature {

    override fun showDialog(activity: AppCompatActivity) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Alert dialog")
            .setMessage("This is alert dialog")
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}

class ModalBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.modal_bottom_sheet_content, container, false)

    companion object {
        const val TAG = "ModalBottomSheet"
    }
}

@FeatureFlag("modal_dialog")
class DialogFeatureFlag : SimpleFeatureFlag()

@Factory
class DialogFeatureFactory : SimpleFeatureFactory<ModalDialogFeature, DialogFeatureFlag>() {

    override fun getEnabledFeature(): ModalDialogFeature = BottomSheetDialogModal()

    override fun getDisabledFeature(): ModalDialogFeature = AlertDialogModal()
}
