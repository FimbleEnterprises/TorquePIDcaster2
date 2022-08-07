package com.fimbleenterprises.torquepidcaster.presentation.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.databinding.PidListItemBinding
import com.fimbleenterprises.torquepidcaster.util.Helpers

class PIDsAdapter(private val context: Context) :RecyclerView.Adapter<PIDsAdapter.PidsViewHolder>() {

    private var showValues: Boolean = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PidsViewHolder {
        val binding = PidListItemBinding
            .inflate(LayoutInflater.from(parent.context),parent,false)
        return PidsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PidsViewHolder, position: Int) {
        val fullPid = differ.currentList[position]
        holder.bind(fullPid)
    }

    fun setShowValues(boolean: Boolean) {
        this.showValues = boolean
    }

     /**
     * This a required callback object needed to leverage the differ val below.  This setup is
     * designed to replace the old .notifyDatasetChanged method that was deemed overly inefficient.
     */
    private val callback = object : DiffUtil.ItemCallback<FullPid>() {

         override fun areItemsTheSame(oldItem: FullPid, newItem: FullPid): Boolean {
             return ((oldItem.getValue() == newItem.getValue()) && (oldItem.isMonitored == newItem.isMonitored))
         }

        /*
        Called to check whether two items have the same data.
        This information is used to detect if the contents of an item have changed.
        */
        override fun areContentsTheSame(oldItem: FullPid, newItem: FullPid): Boolean {
            return oldItem == newItem
        }

    } // callback

    /**
     * This leverages the AsyncListDiffer class from the DiffUtil library.  It is effectively the
     * bucket that holds our listview items.  It's cool but still new and kinda scary to me.
     */
    val differ = AsyncListDiffer(this, callback)

    private var onItemClickListener: ((FullPid, Int)->Unit)? = null

    private var onItemLongClickListener: ((FullPid, Int)->Unit)? = null

    fun setOnItemClickListener(listener : (FullPid, Int) -> Unit) {
        onItemClickListener = listener
    }

    /**
     * Same as above but for long clicks
     */
    fun setOnItemLongClickListener(listener: (FullPid, Int) -> Unit) {
        onItemLongClickListener = listener
    }

    // must be implemented.  Return your list's size
    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class PidsViewHolder(val binding:PidListItemBinding):
        RecyclerView.ViewHolder(binding.root){
           fun bind(pid: FullPid) {

               binding.tvTitle.text = pid.id
               binding.tvDescription.text = pid.getValue().toString()

               when (this@PIDsAdapter.showValues) {
                    true -> binding.tvDescription.visibility = View.VISIBLE
                   false -> binding.tvDescription.visibility = View.GONE
               }

               binding.tvbottom1.text = context.getString(
                   R.string.min_max_listview,
                   Helpers.Numbers.formatAsOneDecimalPointNumber(pid.min).toString(),
                   Helpers.Numbers.formatAsOneDecimalPointNumber(pid.max).toString()
               )

               binding.tvbottom2.text = context.getString(R.string.unit, pid.unit)

               if (pid.isMonitored) {
                   binding.ivMainImage.visibility = View.VISIBLE
               } else {
                   binding.ivMainImage.visibility = View.GONE
               }

               binding.root.setOnClickListener {
                  onItemClickListener?.run {
                        this(pid, layoutPosition)
                  }
               }

               binding.root.setOnLongClickListener {
                   onItemLongClickListener?.let {
                       it(pid, layoutPosition)
                   }
                   false
               }

           }
        }

    companion object {
        private const val TAG = "FIMTOWN|PIDsAdapter"
    }

    init {
        Log.i(TAG, "Initialized:PIDsAdapter")
    }

}









