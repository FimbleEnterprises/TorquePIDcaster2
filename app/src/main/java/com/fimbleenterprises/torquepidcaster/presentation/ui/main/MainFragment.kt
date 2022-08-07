package com.fimbleenterprises.torquepidcaster.presentation.ui.main

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.AbsListView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fimbleenterprises.torquepidcaster.MyApp
import com.fimbleenterprises.torquepidcaster.PluginActivity
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.databinding.FragmentMainBinding
import com.fimbleenterprises.torquepidcaster.domain.service.ServiceRunningState
import com.fimbleenterprises.torquepidcaster.domain.service.TorqueServiceConnectionState
import com.fimbleenterprises.torquepidcaster.domain.service.WakelockState
import com.fimbleenterprises.torquepidcaster.presentation.adapters.TriggeredPidsAdapter
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.torquepidcaster.util.Helpers
import dagger.hilt.android.AndroidEntryPoint
import org.joda.time.DateTime
import javax.inject.Inject


@AndroidEntryPoint
class MainFragment : Fragment() {

    @Inject
    lateinit var adapter: TriggeredPidsAdapter
    private lateinit var binding: FragmentMainBinding
    private lateinit var viewmodel: MainViewModel
    private var isScrolling = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        viewmodel = (activity as PluginActivity).viewmodel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerview()
        startObservingLiveData()

        // Set initial values
        when (viewmodel.isConnectedToTorque.value) {
            true -> { binding.txtTorqueStatusValue.text = getString(R.string.yes) }
            false -> { binding.txtTorqueStatusValue.text = getString(R.string.no) }
            else -> {binding.txtTorqueStatusValue.text = getString(R.string.processing)}
        }
        when (viewmodel.isConnectedToEcu.value) {
            true -> { binding.txtEcuStatusValue.text = getString(R.string.yes) }
            false -> { binding.txtEcuStatusValue.text = getString(R.string.no) }
            else -> {binding.txtTorqueStatusValue.text = getString(R.string.processing)}
        }
        when (viewmodel.serviceRunning.value) {
            true -> { binding.txtServiceStatusValue.text = getString(R.string.yes) }
            false -> { binding.txtServiceStatusValue.text = getString(R.string.no) }
            else -> {binding.txtServiceStatusValue.text = getString(R.string.processing)}
        }

    }

    private fun initRecyclerview() {
        // Remember scroll position throughout lifecycle
        adapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(activity)
            addOnScrollListener(this@MainFragment.onScrollListener)
            adapter = this@MainFragment.adapter
        }

        binding.fab.setOnClickListener {
            adapter.differ.submitList(null)
            viewmodel.clearLog()
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

    /**
     * Flashes the icon indicating a broadcast was sent (the default ECU broadcast is always sent)
     */
    private fun doBroadcastAnimation() {
        // Have the icon blink on/off for half the scan interval's duration
        val duration: Float = (MyApp.AppPreferences.scanInterval / 2)
        binding.imageView.visibility = View.VISIBLE
        val handler = Handler(Looper.myLooper()!!)
        val runner = java.lang.Runnable {
            binding.imageView.visibility = View.INVISIBLE
        }
        handler.postDelayed(runner, duration.toLong())
    }

    private fun startObservingLiveData() {

        // Main textview; updated whenever the service gets updates from Torque
        viewmodel.allPids.observe(viewLifecycleOwner) {
            binding.txtLog.text = getString(R.string.pids_updated_main_textview,
                    Helpers.DatesAndTimes.getPrettyDateAndTime(
                        DateTime.now(),
                        false,
                        true,
                        true
                    )
            )
            doBroadcastAnimation()
        }

        // Effects a running log of (non-default) broadcasts sent
        viewmodel.triggeredPids.observe(viewLifecycleOwner) {
            if (!isScrolling) {
                binding.recyclerView.scrollToPosition(it.size)
                adapter.differ.submitList(it)
                adapter.notifyItemRangeChanged(0, it.size)
            }

        }

        // Our service is running state
        viewmodel.serviceConnectionState.observe(viewLifecycleOwner) {
            when (it) {
                ServiceRunningState.RUNNING -> {
                    binding.txtServiceStatusValue.text = getString(R.string.yes)
                    binding.txtServiceStatusValue.setTextColor(resources.getColor(R.color.colorGreen, null))
                }
                ServiceRunningState.STOPPED -> {
                    binding.txtServiceStatusValue.text = getString(R.string.no)
                    binding.txtServiceStatusValue.setTextColor(Color.RED)
                    binding.imageView.visibility = View.GONE
                }
                ServiceRunningState.STARTING -> {
                    binding.txtServiceStatusValue.text = getString(R.string.starting)
                    binding.txtServiceStatusValue.setTextColor(Color.YELLOW)
                    binding.imageView.visibility = View.GONE
                }
            }
            
        }

        // Connected to Torque state
        viewmodel.torqueConnectionState.observe(viewLifecycleOwner) {
            when (it) {
                TorqueServiceConnectionState.CONNECTED -> {
                    binding.txtTorqueStatusValue.text = getString(R.string.yes)
                    binding.txtTorqueStatusValue.setTextColor(resources.getColor(R.color.colorGreen, null))
                }
                TorqueServiceConnectionState.DISCONNECTED -> {
                    binding.txtTorqueStatusValue.text = getString(R.string.no)
                    binding.txtTorqueStatusValue.setTextColor(Color.RED)
                }
            }
        }

        // The last default broadcast sent (ECU connected/disconnected)
        viewmodel.defaultBroadcastValue.observe(viewLifecycleOwner) {
            binding.txtDefaultBroadcast.text = getString(
                R.string.default_broadcast_label,
                Helpers.DatesAndTimes.getPrettyDateAndTime(
                    DateTime.now(),
                    false,
                    true,
                    true
                ),
                it
            )
        }

        // Do we have a wakelock or not
        viewmodel.wakelockState.observe(viewLifecycleOwner) {
            when (it) {
                WakelockState.ISHELD -> {

                }
                WakelockState.NOTHELD -> {

                }
            }
        }

        // Is Torque connected to the vehicle's ECU
        viewmodel.isConnectedToEcu.observe(viewLifecycleOwner) {
            when (it) {
                true -> {
                    binding.txtEcuStatusValue.text = getString(R.string.yes)
                    binding.txtEcuStatusValue.setTextColor(resources.getColor(R.color.colorGreen, null))
                }
                false -> {
                    binding.txtEcuStatusValue.text = getString(R.string.no)
                    binding.txtEcuStatusValue.setTextColor(Color.RED)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }
}