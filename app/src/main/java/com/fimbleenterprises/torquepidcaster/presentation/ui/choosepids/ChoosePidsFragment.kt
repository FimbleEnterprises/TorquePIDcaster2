package com.fimbleenterprises.torquepidcaster.presentation.ui.choosepids

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fimbleenterprises.torquepidcaster.PluginActivity
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.databinding.FragmentChoosePidsBinding
import com.fimbleenterprises.torquepidcaster.presentation.adapter.PIDsAdapter
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import javax.inject.Inject

@AndroidEntryPoint
class ChoosePidsFragment : Fragment() {

    private var _binding: FragmentChoosePidsBinding? = null
    private lateinit var viewmodel: MainViewModel
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    @Inject lateinit var adapter: PIDsAdapter
    private var isLoading = false
    private var isScrolling = false

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChoosePidsBinding.inflate(inflater, container, false)
        viewmodel = (activity as PluginActivity).viewmodel
        mFirebaseAnalytics = (activity as PluginActivity).mFirebaseAnalytics
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = Bundle()
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "dd")
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "test")
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "image")
        mFirebaseAnalytics!!.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
        initRecyclerView()
        startObservingLiveData()
    }

    private fun initRecyclerView() {

        adapter.setOnItemClickListener { clickedPid, pos ->
            if (clickedPid.isMonitored) {
                CoroutineScope(Main).launch {
                    viewmodel.deletePid(clickedPid, pos)
                }
                adapter.differ.currentList[pos].isMonitored = false
                adapter.notifyItemChanged(pos)
            } else {
                clickedPid.threshold = .2
                clickedPid.operator = FullPid.AlarmOperator.GREATER_THAN
                clickedPid.broadcastAction = "fuck"
                CoroutineScope(Main).launch {
                    viewmodel.monitorPid(clickedPid, pos)
                    adapter.differ.currentList[pos].isMonitored = true
                    adapter.notifyItemChanged(pos)
                }
            }
        }

        // Remember scroll position throughout lifecycle
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@ChoosePidsFragment.onScrollListener)
            adapter = this@ChoosePidsFragment.adapter
        }
    }

    private fun startObservingLiveData() {
        /**
         * Watch for changes in the viewmodel's list of PIDs and submit that list to the adapter
         * using DiffUtil which runs on a bg thread and (I believe) does not nuke the whole list
         * when submitted but instead only updates values that have changed.
         */
        viewmodel.allPids.observe(viewLifecycleOwner) {
            adapter.differ.submitList(it)
        }
    }

    /**
     * Not implemented but could be handy if we ever needed to introduce pagination
     */
    private val onScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                isScrolling = true
            }
        }

        @Suppress("UNUSED_VARIABLE")
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val layoutManager = binding.recyclerView.layoutManager as LinearLayoutManager
            val sizeOfTheCurrentList = layoutManager.itemCount
            val visibleItems = layoutManager.childCount
            val topPosition = layoutManager.findFirstVisibleItemPosition()
            val hasReachedEnd = topPosition+visibleItems >= sizeOfTheCurrentList
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    init { Log.i(TAG, "Initialized:ChoosePidsFragment") }
    companion object { private const val TAG = "FIMTOWN|ChoosePidsFragment" }
}