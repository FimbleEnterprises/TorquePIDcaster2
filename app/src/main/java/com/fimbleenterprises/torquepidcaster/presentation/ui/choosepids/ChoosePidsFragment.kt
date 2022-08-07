package com.fimbleenterprises.torquepidcaster.presentation.ui.choosepids

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.AbsListView
import android.widget.SearchView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fimbleenterprises.torquepidcaster.PluginActivity
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.data.model.FullPid
import com.fimbleenterprises.torquepidcaster.databinding.FragmentChoosePidsBinding
import com.fimbleenterprises.torquepidcaster.presentation.AddAlarmDialog
import com.fimbleenterprises.torquepidcaster.presentation.MyYesNoDialog
import com.fimbleenterprises.torquepidcaster.presentation.adapters.PIDsAdapter
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModel
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_choose_pids.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import javax.inject.Inject


@AndroidEntryPoint
class ChoosePidsFragment : Fragment() {

    private lateinit var binding: FragmentChoosePidsBinding
    private lateinit var viewmodel: MainViewModel
    private var mFirebaseAnalytics: FirebaseAnalytics? = null

    @Inject lateinit var adapter: PIDsAdapter
    private var isLoading = false
    private var isScrolling = false
    private var _showSavedOnly = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChoosePidsBinding.inflate(inflater, container, false)
        viewmodel = (activity as PluginActivity).viewmodel
        mFirebaseAnalytics = (activity as PluginActivity).mFirebaseAnalytics

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initRecyclerView()
        initSearchView()
        startObservingLiveData()

        binding.floatingActionButton.setOnClickListener {
            setSavedOnlyFilter(!_showSavedOnly)
        }

        // Turn off/on showing pid values in the listview.
        binding.swtchRealtimeValues.setOnCheckedChangeListener { _, isChecked ->
            viewmodel.apply {
                adapter.setShowValues(isChecked)
                viewmodel.showValuesInListView(isChecked)

                // Notify that the data has changed.
                viewmodel.allPids.value?.let { adapter.notifyItemRangeChanged(0, it.size) }
            }
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.choose_pids_frag_menu, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.action_remove_all_alarms -> {
                        MyYesNoDialog().show(
                            requireContext(),
                            getString(R.string.no),
                            getString(R.string.yes),
                            getString(R.string.are_you_sure),
                            object : MyYesNoDialog.YesNoListener {
                                override fun onBtn1Pressed() { }
                                override fun onBtn2Pressed() {
                                    CoroutineScope(IO).launch {
                                        val count = viewmodel.stopMonitoringAllPids()
                                        withContext(Main) {
                                            Toast.makeText(context, "Removed $count", Toast.LENGTH_SHORT).show()
                                            setSavedOnlyFilter(false)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        /* This resolves the following bug:
        If the user enters a search query and the list is properly filtered showing only their
        expected results.  Then the user hits the back button multiple times sufficient to be
        taken away from this fragment (to the main frag or wherever) then resumes this fragment -
        the filtered constraint results remain while the searchview shows no sign of filtering.
        No amount of hitting the X next to the searchview will then clear the constraint.

        Sending a null query as this frag is destroyed resolves this. */
        searchview.setQuery(null,true)
    }

    private fun setSavedOnlyFilter(showOnlySaved: Boolean) {
        if (viewmodel.monitoredPids.value?.size == 0 && showOnlySaved) {
            Toast.makeText(context, getString(R.string.no_saved_pids_toast), Toast.LENGTH_SHORT).show()
        } else {
            _showSavedOnly = showOnlySaved
            viewmodel.showSavedOnly(_showSavedOnly)
            if (!_showSavedOnly) {
                binding.searchview.visibility = View.VISIBLE
                binding.searchview.setQuery(null, true)
                binding.searchview.isIconified = true
                // This is the only way to update the list after turning off filtering.
                adapter.differ.submitList(viewmodel.allPids.value)
            }
            when (_showSavedOnly) {
                true -> binding.searchview.visibility = View.GONE
                false -> binding.searchview.visibility = View.VISIBLE
            }
        }
        setSavedOnlyButtonState()
    }

    private fun setSavedOnlyButtonState() {
        when (_showSavedOnly) {
            true -> {
                binding.floatingActionButton.backgroundTintList = ColorStateList.valueOf(
                    Color.rgb(100, 100, 100)
                )
            }
            false -> {
                binding.floatingActionButton.backgroundTintList = ColorStateList.valueOf(
                    Color.rgb(255, 255, 255)
                )
            }
        }
    }

    private fun initRecyclerView() {

        adapter.setOnItemClickListener { clickedPid, pos ->
            showAlarmDialog(clickedPid, pos)
        }

        adapter.setShowValues(binding.swtchRealtimeValues.isChecked)

        // Remember scroll position throughout lifecycle
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.ALLOW

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@ChoosePidsFragment.onScrollListener)
            adapter = this@ChoosePidsFragment.adapter
        }
    }

    /**
     * Sets up the search view and adds some simple null logic for searching
     */
    private fun initSearchView() {
        binding.searchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(usertext: String?): Boolean {
                if (usertext != null && usertext.length > 2) {
                    viewmodel.filterPidsByName(usertext)
                } else {
                    viewmodel.filterPidsByName(null)
                    // This is the only way to update the list after turning off filtering.
                    adapter.differ.submitList(viewmodel.allPids.value)
                }
                return false // false = Default action performed (hide keyboard in this case)
            }

            override fun onQueryTextChange(usertext: String?): Boolean {
                if (usertext.isNullOrEmpty()) {
                    viewmodel.filterPidsByName(null)
                    // This is the only way to update the list after turning off filtering.
                    adapter.differ.submitList(viewmodel.allPids.value)
                } else if (usertext.length > 1) {
                    viewmodel.filterPidsByName(usertext)
                }
                return false // false = Default action performed (show hints in this case)
            }
        })
        binding.searchview.isIconified = true
        binding.searchview.queryHint = getString(R.string.searchview_query_hint)
    }

    /**
     * This is the dialog the user will use to create an alarm and set its parameters.
     */
    private fun showAlarmDialog(clickedPid: FullPid, pos: Int) {

        val alarmDialog = AddAlarmDialog(requireContext(), clickedPid)
        alarmDialog.run {
            create()
            setListener(object : AddAlarmDialog.AlarmAddRemoveListener {
                override fun onAlarmAdded(pid: FullPid) {
                    /*if (serviceWasRunning) {
                        viewmodel.stopService()
                    }*/
                    clickedPid.isMonitored = true
                    clickedPid.threshold = pid.threshold
                    clickedPid.operator = pid.operator
                    clickedPid.setBroadcastAction(pid.getBroadcastAction())
                    CoroutineScope(IO).launch {
                        viewmodel.monitorPid(clickedPid, pos)
                        // Edit the item in the adapter directly so we don't have to wait another
                        // cycle for the adapter to realize it should show the bell icon.
                        withContext(Main) {
                            adapter.differ.currentList[pos].isMonitored = true
                            adapter.notifyItemChanged(pos)
                            Toast.makeText(context, "Added", Toast.LENGTH_SHORT).show()
                            alarmDialog.cancel()
                            alarmDialog.dismiss()
                        }
                    }
                } // added

                override fun onAlarmRemoved(pid: FullPid) {
                    // if user clicked "Remove" as a means to cancel the dialog.
                    if (!pid.isMonitored) {
                        return
                    }
                    CoroutineScope(IO).launch {
                        viewmodel.stopMonitoringPid(clickedPid, pos)
                        adapter.differ.currentList[pos].isMonitored = false
                        // Edit the item in the adapter directly so we don't have to wait another
                        // cycle for the adapter to realize it should hide the bell icon.
                        withContext(Main) {
                            adapter.notifyItemChanged(pos)
                            alarmDialog.cancel()
                            alarmDialog.dismiss()
                            Toast.makeText(context, "Removed", Toast.LENGTH_SHORT).show()

                            if (viewmodel.monitoredPids.value?.size == 0) {
                                _showSavedOnly = false
                                setSavedOnlyFilter(_showSavedOnly)
                                viewmodel.showSavedOnly(false)
                                viewmodel.filterPidsByName(null)
                                setSavedOnlyButtonState()
                                // This is the only way to update the list after turning off filtering.
                                adapter.differ.submitList(viewmodel.allPids.value)
                            }
                        } // main thread
                    } // bg thread
                } // removed
            }) // listener
            show()
        } // dialog
    }

    private fun startObservingLiveData() {

        viewmodel.forceRedraw.observe(viewLifecycleOwner) {
            adapter.differ.submitList(it)
            adapter.notifyItemRangeChanged(0, adapter.differ.currentList.size - 1)
        }

        // The listview gets janky when it updates so frequently so ideally we only want to update
        // it when its empty, or the user wants to see real-time values and it is not currently
        // being scrolled.
        viewmodel.allPids.observe(viewLifecycleOwner) {
            if (
                viewmodel.showRealtimeValues.value == true &&
                !isScrolling &&
                !viewmodel.isFiltering
            ) {
                adapter.differ.submitList(it)
            }

            if (adapter.itemCount == 0 && !viewmodel.isFiltering) {
                // adapter.setShowValues(MyApp.AppPreferences.showValuesInListView)
                adapter.differ.submitList(it)
            }

            if (adapter.differ.currentList.size != it.size && !viewmodel.isFiltering &&
                !isScrolling) {
                adapter.differ.submitList(it)
            }
        }

        // Should only update when user sends a query to viewmodel.filterPidsByName(query)
        viewmodel.filteredPids.observe(viewLifecycleOwner) {

            if (viewmodel.isFiltering && viewmodel.showRealtimeValues.value == true && !isScrolling) {
                adapter.differ.submitList(it)
            }

            if (adapter.itemCount != it.size && !isScrolling) {
                Log.i(TAG, "-= ${adapter.itemCount} vs. ${it.size} =-")
                adapter.differ.submitList(it)
            }
        }

        // Switch for the user to indicate whether or not we are going to show values in the listview.
        viewmodel.showRealtimeValues.observeForever { isChecked ->
            // Below code will run on a background thread from a shared pool of threads created
            // as needed, on-demand
            binding.swtchRealtimeValues.isChecked = isChecked
            adapter.setShowValues(isChecked)
            try {
                adapter.notifyItemRangeChanged(0, viewmodel.allPids.value!!.size)
            } catch (exception:Exception) {
                Log.e(TAG, "startObservingLiveData: ${exception.localizedMessage}"
                    , exception)

            }
        }
    }

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                isScrolling = true
            }
            if(newState == AbsListView.OnScrollListener.SCROLL_STATE_IDLE){
                isScrolling = false
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

    init { Log.i(TAG, "Initialized:ChoosePidsFragment") }
    companion object {
        private const val TAG = "FIMTOWN|ChoosePidsFragment"

        // This is used to prevent spamming PID value updates to subscribers by throttling the
        // amount of updates.  It will NOT throttle the evaluation of those values (dictated by
        // scan_interval preference) and subsequent broadcasts, just the updates used for viewing
        // values in a listview.
        private const val MINIMUM_WAIT_TIME_TO_UPDATE = 500
    }
}