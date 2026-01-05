package com.example.smartnotifier.ui.logs

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.smartnotifier.R
import com.example.smartnotifier.databinding.BottomsheetNotificationLogsBinding
import com.example.smartnotifier.ui.main.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

class NotificationLogBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomsheetNotificationLogsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    var onClosed: (() -> Unit)? = null

    private val logAdapter = NotificationLogAdapter { log ->
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.addRuleFromLog(log)
            Toast.makeText(
                requireContext(),
                getString(R.string.toast_rule_added_from_log),
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomsheetNotificationLogsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerNotificationLogs.adapter = logAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.notificationLogs.collectLatest { logs ->
                logAdapter.submitList(logs)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onClosed?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
