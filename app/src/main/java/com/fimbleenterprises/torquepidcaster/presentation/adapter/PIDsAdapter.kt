package com.fimbleenterprises.torquepidcaster.presentation.adapter

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
import com.fimbleenterprises.torquepidcaster.databinding.ListItemBinding

class PIDsAdapter(private val context: Context) :RecyclerView.Adapter<PIDsAdapter.PidsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PidsViewHolder {
        val binding = ListItemBinding
            .inflate(LayoutInflater.from(parent.context),parent,false)
        return PidsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PidsViewHolder, position: Int) {
        val fullPid = differ.currentList[position]
        holder.bind(fullPid)
    }

     /**
     * This a required callback object needed to leverage the differ val below.  This setup is
     * designed to replace the old .notifyDatasetChanged method that was deemed overly inefficient.
     */
    private val callback = object : DiffUtil.ItemCallback<FullPid>() {

        /*
        Compares a single item using an arbitrary definition of sameness (in this case we chose
        the item's url property because they are always unique to the item.

        Called to check whether two objects represent the same item.
        For example, if your items have unique ids, this method should check their id equality.

        Note: null items in the list are assumed to be the same as another null item and are
        assumed to not be the same as a non-null item. This callback will not be invoked for
        either of those cases.

        */
        override fun areItemsTheSame(oldItem: FullPid, newItem: FullPid): Boolean {
            return oldItem.fullName == newItem.fullName
        }

        /*
        Called to check whether two items have the same data.
        This information is used to detect if the contents of an item have changed.
        */
        override fun areContentsTheSame(oldItem: FullPid, newItem: FullPid): Boolean {
            val result = ((oldItem.getValue() == newItem.getValue()) && (oldItem.isMonitored == newItem.isMonitored))
            if (!result) {
                // Log.v(TAG, "-=PIDsAdapter:areContentsTheSame NOPE! ${oldItem.shortName} has changed ${oldItem.value} -> ${newItem.value} =-")
            }
            return result
            }

    } // callback

    /**
     * This leverages the AsyncListDiffer class from the DiffUtil library.  It is effectively the
     * bucket that holds our listview items.  It's cool but still new and kinda scary to me.
     */
    val differ = AsyncListDiffer(this, callback)

    /* This is a tricksy way of creating an onItemClick listener that makes for an article to
       somehow magically be implied (it == Team) by the caller where it is implemented.
       I do not understand how this works. */
    private var onItemClickListener: ((FullPid, Int)->Unit)? = null
    private var onItemLongClickListener: ((FullPid, Int)->Unit)? = null

    /* Sets the onItemClickListener that magically returns an Team object as "it".
    Still no understand!  Caller will implement it as such:
        adapter.setOnItemClickListener {
            // it == Team
            it.title
            it.context
            etc.
        }
    */
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

    inner class PidsViewHolder(val binding:ListItemBinding):
        RecyclerView.ViewHolder(binding.root){
           fun bind(pid: FullPid){

               binding.tvTitle.text = pid.fullName
               binding.tvDescription.text = pid.getValue().toString()
               binding.tvbottom1.text = pid.commaDelimitedPidInfo
               binding.tvbottom2.text = context.getString(R.string.last_updated_at
                   , pid.getLastUpdatedInSecs().toString())
               if (pid.isMonitored) {
                   binding.ivMainImage.visibility = View.VISIBLE
               } else {
                   binding.ivMainImage.visibility = View.GONE
               }

               /*Glide.with(binding.ivMainImage.context).
               load(pid.strTeamLogo).
               into(binding.ivMainImage)*/

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









