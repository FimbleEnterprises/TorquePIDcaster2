package com.fimbleenterprises.torquepidcaster.presentation

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.Window
import android.widget.*
import androidx.core.content.ContextCompat.getSystemService
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.data.model.FullPid


class AddAlarmDialog constructor(
    context: Context,
    private val clickedPid: FullPid
): Dialog(context) {

    private var listener: AlarmAddRemoveListener? = null

    fun setListener(alarmAddRemoveListener: AlarmAddRemoveListener) {
        this.listener = alarmAddRemoveListener
    }

    override fun create() {
        super.create()
        this.requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.setCancelable(true)
        this.setContentView(R.layout.add_new_pid)

        val pidname = this.findViewById(R.id.txtTitle) as TextView
        pidname.text = clickedPid.id

        val operators = arrayOf(
            FullPid.AlarmOperator.GREATER_THAN.name,
            FullPid.AlarmOperator.LESS_THAN.name,
            FullPid.AlarmOperator.EQUALS.name,
            FullPid.AlarmOperator.NOT_EQUALS.name,
            FullPid.AlarmOperator.SEND_ALWAYS.name
        )

        val operator = this.findViewById(R.id.spinner) as Spinner
        val arrayAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, operators)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        operator.adapter = arrayAdapter

        when (clickedPid.operator) {
            FullPid.AlarmOperator.GREATER_THAN ->
                operator.setSelection(0)
            FullPid.AlarmOperator.LESS_THAN ->
                operator.setSelection(1)
            FullPid.AlarmOperator.EQUALS ->
                operator.setSelection(2)
            FullPid.AlarmOperator.NOT_EQUALS ->
                operator.setSelection(3)
            FullPid.AlarmOperator.SEND_ALWAYS ->
                operator.setSelection(4)
            else -> {}
        }

        val threshold = this.findViewById(R.id.etxtThreshold) as EditText
        threshold.setText(clickedPid.threshold.toString())

        val broadcastExample = this.findViewById(R.id.txtFullAction) as TextView
        if (clickedPid.getBroadcastAction().isNullOrEmpty()) {
            broadcastExample.text = context.getString(R.string.fully_qualified_broadcast, clickedPid.shortName?.replace(" ","_")?.uppercase())
        } else {
            broadcastExample.text = context.getString(R.string.fully_qualified_broadcast, clickedPid.getBroadcastAction())
        }

        val copyButton = this.findViewById(R.id.imgCopy) as ImageButton
        copyButton.setOnClickListener {
            Context.CLIPBOARD_SERVICE
            val clipboard: ClipboardManager? = getSystemService(context, ClipboardManager::class.java)
            val clip = ClipData.newPlainText("", broadcastExample.text)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
        }

        val broadcastAction = this.findViewById(R.id.etxtAction) as EditText
        if (clickedPid.getBroadcastAction().isNullOrEmpty()) {
            broadcastAction.setText(clickedPid.shortName?.replace(" ","_")?.uppercase())
        } else {
            broadcastAction.setText(clickedPid.getBroadcastAction()?.uppercase())
        }
        broadcastAction.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {}
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                broadcastExample.text = context.getString(R.string.fully_qualified_broadcast, s)
                clickedPid.setBroadcastAction(s.toString())
                broadcastExample.text = context.getString(R.string.fully_qualified_broadcast, clickedPid.getBroadcastAction())
            }
        })

        val yesBtn = this.findViewById(R.id.btnOkay) as Button
        yesBtn.setOnClickListener {
            val value = threshold.text.toString().toDouble()
            if (value >= clickedPid.min && value <= clickedPid.max) {
                clickedPid.threshold = value
                when (operator.selectedItem) {
                    FullPid.AlarmOperator.GREATER_THAN.name ->
                        clickedPid.operator = FullPid.AlarmOperator.GREATER_THAN
                    FullPid.AlarmOperator.LESS_THAN.name ->
                        clickedPid.operator = FullPid.AlarmOperator.LESS_THAN
                    FullPid.AlarmOperator.EQUALS.name ->
                        clickedPid.operator = FullPid.AlarmOperator.EQUALS
                    FullPid.AlarmOperator.NOT_EQUALS.name ->
                        clickedPid.operator = FullPid.AlarmOperator.NOT_EQUALS
                    FullPid.AlarmOperator.SEND_ALWAYS.name ->
                        clickedPid.operator = FullPid.AlarmOperator.SEND_ALWAYS
                }
            }

            listener?.onAlarmAdded(clickedPid)
            this.cancel()
        }

        val removeButton = this.findViewById(R.id.btnRemoveAlarm) as Button
        removeButton.setOnClickListener {
            listener?.onAlarmRemoved(clickedPid)
            this.cancel()
        }
    }

    interface AlarmAddRemoveListener {
        fun onAlarmAdded(pid: FullPid)
        fun onAlarmRemoved(pid: FullPid)
    }

}