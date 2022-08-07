package com.fimbleenterprises.torquepidcaster.presentation

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.*
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.util.Helpers


class ShowBatteryOptDialog constructor(
    private val activity: Activity
): Dialog(activity) {

    override fun create() {
        super.create()
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setCancelable(true)
        this.setContentView(R.layout.check_batt_dialog)

        val yesBtn = this.findViewById(R.id.btnOkay) as Button
        yesBtn.setOnClickListener {
            Helpers.Application.sendToAppSettings(activity)
            this.cancel()
        }
        this.show()
    }

}