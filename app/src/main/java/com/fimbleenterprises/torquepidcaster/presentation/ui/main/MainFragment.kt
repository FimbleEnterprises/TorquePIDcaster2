package com.fimbleenterprises.torquepidcaster.presentation.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fimbleenterprises.torquepidcaster.PluginActivity
import com.fimbleenterprises.torquepidcaster.R
import com.fimbleenterprises.torquepidcaster.databinding.FragmentMainBinding
import com.fimbleenterprises.torquepidcaster.domain.service.ServiceRunningState
import com.fimbleenterprises.torquepidcaster.domain.service.TorqueServiceConnectionState
import com.fimbleenterprises.torquepidcaster.domain.service.WakelockState
import com.fimbleenterprises.torquepidcaster.presentation.adapter.PIDsAdapter
import com.fimbleenterprises.torquepidcaster.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.torquepidcaster.util.Helpers
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_main.*
import org.joda.time.DateTime
import javax.inject.Inject

@AndroidEntryPoint
class MainFragment : Fragment() {

    private var _binding: FragmentMainBinding? = null
    private lateinit var viewmodel: MainViewModel
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        viewmodel = (activity as PluginActivity).viewmodel
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        startObservingLiveData()
    }



    private fun startObservingLiveData() {

        viewmodel.allPids.observe(viewLifecycleOwner) {
            binding.txtLog.text = "PIDs were updated ${
                Helpers.DatesAndTimes.getPrettyDateAndTime(
                 DateTime.now(), 
                false, 
                true, 
                true
            )}"
        }

        viewmodel.triggeredPids.observe(viewLifecycleOwner) {

            val now = Helpers.DatesAndTimes.getPrettyDateAndTime(
                 DateTime.now(),
                false,
                true,
                false
            )

            val existingText = binding.txtLog2.text

            binding.txtLog2.text = getString(
                R.string.trigger_log,
                now,it.fullName,
                it.getValue().toString(),
                it.operator?.name,
                it.threshold.toString()
            )

            binding.txtLog2.append("\n$existingText")
        }

        viewmodel.serviceConnectionState.observe(viewLifecycleOwner) {
            when (it) {
                ServiceRunningState.RUNNING -> {
                    binding.txtServiceStatusValue.text = getString(R.string.yes)
                }
                ServiceRunningState.STOPPED -> {
                    binding.txtServiceStatusValue.text = getString(R.string.no)
                }
                ServiceRunningState.STARTING -> {
                    binding.txtServiceStatusValue.text = getString(R.string.starting)
                }
            }
        }
        viewmodel.torqueConnectionState.observe(viewLifecycleOwner) {
            when (it) {
                TorqueServiceConnectionState.CONNECTED -> {
                    binding.txtTorqueStatusValue.text = getString(R.string.yes)
                }
                TorqueServiceConnectionState.DISCONNECTED -> {
                    binding.txtTorqueStatusValue.text = getString(R.string.no)
                }
            }
        }
        viewmodel.wakelockState.observe(viewLifecycleOwner) {
            when (it) {
                WakelockState.ISHELD -> {
                    binding.txtWakelockStatusValue.text = getString(R.string.yes)
                }
                WakelockState.NOTHELD -> {
                    binding.txtWakelockStatusValue.text = getString(R.string.no)
                }
            }
        }
        viewmodel.isConnectedToEcu.observe(viewLifecycleOwner) {
            when (it) {
                true -> {
                    txtEcuStatusValue.text = getString(R.string.yes)
                }
                false -> {
                    txtEcuStatusValue.text = getString(R.string.no)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}