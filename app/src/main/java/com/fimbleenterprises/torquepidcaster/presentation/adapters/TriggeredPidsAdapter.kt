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
import com.fimbleenterprises.torquepidcaster.data.model.TriggeredPid
import com.fimbleenterprises.torquepidcaster.databinding.TriggeredPidListItemBinding

class TriggeredPidsAdapter(private val context: Context) :RecyclerView.Adapter<TriggeredPidsAdapter.PidsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PidsViewHolder {
        val binding = TriggeredPidListItemBinding
            .inflate(LayoutInflater.from(parent.context),parent,false)
        return PidsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PidsViewHolder, position: Int) {
        val triggeredPid = differ.currentList[position]
        holder.bind(triggeredPid)
    }

     /**
     * This a required callback object needed to leverage the differ val below.  This setup is
     * designed to replace the old .notifyDatasetChanged method that was deemed overly inefficient.
     */
    private val callback = object : DiffUtil.ItemCallback<TriggeredPid>() {

         override fun getChangePayload(oldItem: TriggeredPid, newItem: TriggeredPid): Any? {
             return super.getChangePayload(oldItem, newItem)
         }

         /*
                 Compares a single item using an arbitrary definition of sameness (in this case we chose
                 the item's url property because they are always unique to the item.

                 Called to check whether two objects represent the same item.
                 For example, if your items have unique ids, this method should check their id equality.

                 Note: null items in the list are assumed to be the same as another null item and are
                 assumed to not be the same as a non-null item. This callback will not be invoked for
                 either of those cases.

                 */
        override fun areItemsTheSame(oldItem: TriggeredPid, newItem: TriggeredPid): Boolean {
            return oldItem == newItem
        }

        /*
        Called to check whether two items have the same data.
        This information is used to detect if the contents of an item have changed.
        */
        override fun areContentsTheSame(oldItem: TriggeredPid, newItem: TriggeredPid): Boolean {
            return (oldItem.triggeredOnMillis == newItem.triggeredOnMillis)
        }

    } // callback

    /**
     * This leverages the AsyncListDiffer class from the DiffUtil library.  It is effectively the
     * bucket that holds our listview items.  It's cool but still new and kinda scary to me.
     */
    val differ = AsyncListDiffer(this, callback)

    private var onItemClickListener: ((TriggeredPid, Int)->Unit)? = null

    private var onItemLongClickListener: ((TriggeredPid, Int)->Unit)? = null

    fun setOnItemClickListener(listener : (TriggeredPid, Int) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnItemLongClickListener(listener: (TriggeredPid, Int) -> Unit) {
        onItemLongClickListener = listener
    }

    // must be implemented.  Return your list's size
    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    inner class PidsViewHolder(private val binding:TriggeredPidListItemBinding):
        RecyclerView.ViewHolder(binding.root){
           fun bind(pid: TriggeredPid){

               binding.tvTitle.text = pid.getTriggeredOnAsPrettyDateTime()

               binding.tvDescription.text = context.getString(
                   R.string.trigger_log2,
                   pid.pidFullname,
                   pid.value.toString(),
                   pid.operator?.name,
                   pid.threshold.toString())

               binding.ivMainImage.visibility = View.VISIBLE

               binding.tvbottom1.text = context.getString(
                   R.string.broadcast_action,
                   pid.showFullBroadcast(context)
               )

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









