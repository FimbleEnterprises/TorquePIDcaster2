package com.fimbleenterprises.torquepidcaster.presentation

    import android.app.Dialog
    import android.content.Context
    import android.widget.Button
    import android.widget.TextView
    import com.fimbleenterprises.torquepidcaster.R

class MyYesNoDialog {

        private var message = "PUT A MSG HERE, DUMMY"
        private var btn1Text = "Button 1"
        private var btn2Text = "Button 2"
        private var dialog: Dialog? = null

        interface YesNoListener {
            fun onBtn1Pressed()
            fun onBtn2Pressed()
        }

        private fun build(context: Context, listener: YesNoListener) {
            dialog = Dialog(context)
            dialog!!.setContentView(R.layout.yes_no_dialog)
            val txtMain: TextView = dialog!!.findViewById(R.id.txtMain)
            txtMain.text = message
            val btn1: Button = dialog!!.findViewById(R.id.btnNo)
            btn1.text = btn1Text
            btn1.setOnClickListener {
                listener.onBtn1Pressed()
                dialog!!.dismiss()
            }
            val btn2: Button = dialog!!.findViewById(R.id.btnYes)
            btn2.text = btn2Text
            btn2.setOnClickListener {
                listener.onBtn2Pressed()
                dialog!!.dismiss()
            }
            dialog!!.setCancelable(true)
            dialog!!.show()
        }

        fun show(
            context: Context,
            button1Text: String,
            button2Text: String,
            msg: String,
            listener: YesNoListener
        ) {
            message = msg
            btn1Text = button1Text
            btn2Text = button2Text
            build(context, listener)
            dialog!!.show()
        }

    }