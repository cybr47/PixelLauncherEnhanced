package com.drdisagree.pixellauncherenhanced.ui.fragments

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ComponentName
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.drdisagree.pixellauncherenhanced.R
import com.drdisagree.pixellauncherenhanced.data.common.Constants.ACTION_APP_LIST_UPDATED
import com.drdisagree.pixellauncherenhanced.data.config.AppLabelPreferences
import com.drdisagree.pixellauncherenhanced.data.config.RPrefs
import com.drdisagree.pixellauncherenhanced.data.model.AppInfoModel
import com.drdisagree.pixellauncherenhanced.databinding.FragmentHiddenAppsBinding
import com.drdisagree.pixellauncherenhanced.ui.adapters.AppListAdapter
import com.drdisagree.pixellauncherenhanced.utils.AppUtils
import com.drdisagree.pixellauncherenhanced.utils.MiscUtils.dpToPx
import com.drdisagree.pixellauncherenhanced.utils.MiscUtils.setupToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class EditAppLabels : Fragment() {

    private lateinit var binding: FragmentHiddenAppsBinding
    private var appList: MutableList<AppInfoModel>? = null
    private var adapter: AppListAdapter? = null

    private val packageUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_APP_LIST_UPDATED) {
                initAppList()
            }
        }
    }

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        }

        override fun afterTextChanged(s: Editable) {
            if (binding.search.text.toString().trim().isNotEmpty()) {
                binding.clear.visibility = View.VISIBLE
                filterList(binding.search.text.toString().trim())
            } else {
                binding.clear.visibility = View.GONE
                filterList("")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHiddenAppsBinding.inflate(inflater, container, false)

        setupToolbar(
            requireContext() as AppCompatActivity,
            R.string.edit_app_labels_title,
            true,
            binding.header.toolbar,
            binding.header.collapsingToolbar
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerView) { view, insets ->
            val navBarInset = insets
                .getInsets(WindowInsetsCompat.Type.navigationBars())
                .bottom
            val baseBottomPadding = dpToPx(16)

            view.setPadding(
                view.paddingLeft,
                view.paddingTop,
                view.paddingRight,
                baseBottomPadding + navBarInset
            )

            insets
        }
        ViewCompat.requestApplyInsets(binding.recyclerView)

        initAppList()
    }

    private fun initAppList() {
        binding.recyclerView.visibility = View.GONE
        binding.progressBar.visibility = View.VISIBLE
        binding.search.removeTextChangedListener(textWatcher)

        CoroutineScope(Dispatchers.IO).launch {
            val allApps = AppUtils.getAllLaunchableApps()
            appList = allApps.map { appInfo ->
                appInfo.apply {
                    subtitle = AppLabelPreferences.getCustomLabel(RPrefs, componentName ?: return@map null)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { getString(R.string.current_custom_label_summary, it) }
                        ?: packageName
                }
            }.filterNotNull().toMutableList()
            adapter = AppListAdapter(appList!!, showSwitch = false) { appInfo ->
                if (appInfo.componentName != null) {
                    showEditDialog(appInfo)
                }
            }
            delay(300)

            withContext(Dispatchers.Main) {
                try {
                    binding.recyclerView.adapter = adapter
                    binding.search.addTextChangedListener(textWatcher)

                    binding.progressBar.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE

                    binding.clear.setOnClickListener {
                        binding.search.setText("")
                        binding.clear.visibility = View.GONE
                    }
                    if (binding.search.text.toString().trim().isNotEmpty()) {
                        filterList(binding.search.text.toString().trim { it <= ' ' })
                    }
                } catch (_: Exception) {

                }
            }
        }
    }

    private fun filterList(query: String) {
        if (appList == null) return

        val startsWithNameList: MutableList<AppInfoModel> = ArrayList()
        val containsNameList: MutableList<AppInfoModel> = ArrayList()
        val startsWithPackageNameList: MutableList<AppInfoModel> = ArrayList()
        val containsPackageNameList: MutableList<AppInfoModel> = ArrayList()

        for (app in appList!!) {
            if (app.appName.lowercase(Locale.getDefault())
                    .startsWith(query.lowercase(Locale.getDefault()))
            ) {
                startsWithNameList.add(app)
            } else if (app.appName.lowercase(Locale.getDefault())
                    .contains(query.lowercase(Locale.getDefault()))
            ) {
                containsNameList.add(app)
            } else if (app.packageName.lowercase(Locale.getDefault()).startsWith(
                    query.lowercase(
                        Locale.getDefault()
                    )
                )
            ) {
                startsWithPackageNameList.add(app)
            } else if (app.packageName.lowercase(Locale.getDefault()).contains(
                    query.lowercase(
                        Locale.getDefault()
                    )
                )
            ) {
                containsPackageNameList.add(app)
            }
        }

        val filteredList: MutableList<AppInfoModel> = ArrayList()
        filteredList.addAll(startsWithNameList)
        filteredList.addAll(containsNameList)
        filteredList.addAll(startsWithPackageNameList)
        filteredList.addAll(containsPackageNameList)

        adapter = AppListAdapter(filteredList, showSwitch = false) { appInfo ->
            if (appInfo.componentName != null) {
                showEditDialog(appInfo)
            }
        }
        binding.recyclerView.adapter = adapter
    }

    private fun showEditDialog(appInfo: AppInfoModel) {
        val componentName = appInfo.componentName ?: return
        val currentLabel = AppLabelPreferences.getCustomLabel(RPrefs, componentName)
            ?.takeIf { it.isNotBlank() }
            ?: appInfo.appName

        val dialogView = LayoutInflater.from(requireContext()).inflate(
            R.layout.dialog_edit_app_label,
            null,
            false
        )

        val iconView = dialogView.findViewById<ImageView>(R.id.edit_label_icon)
        val input = dialogView.findViewById<TextInputEditText>(R.id.edit_label_input)
        val resetButton = dialogView.findViewById<MaterialButton>(R.id.edit_label_reset)
        val saveButton = dialogView.findViewById<MaterialButton>(R.id.edit_label_save)

        iconView.setImageDrawable(appInfo.appIcon)
        input.setText(currentLabel)
        input.setSelection(currentLabel.length)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()

        resetButton.setOnClickListener {
            AppLabelPreferences.removeCustomLabel(RPrefs.edit(), componentName)
            appInfo.subtitle = appInfo.packageName
            adapter?.notifyItemChanged(appList?.indexOf(appInfo) ?: 0)
            Toast.makeText(requireContext(), getString(R.string.label_reset), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            val newLabel = input.text?.toString()?.trim()
            if (newLabel.isNullOrEmpty()) {
                AppLabelPreferences.removeCustomLabel(RPrefs.edit(), componentName)
                appInfo.subtitle = appInfo.packageName
            } else {
                AppLabelPreferences.setCustomLabel(RPrefs.edit(), componentName, newLabel)
                appInfo.subtitle = getString(R.string.current_custom_label_summary, newLabel)
            }
            adapter?.notifyItemChanged(appList?.indexOf(appInfo) ?: 0)
            Toast.makeText(requireContext(), getString(R.string.label_saved), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter().apply {
            addAction(ACTION_APP_LIST_UPDATED)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                packageUpdateReceiver,
                intentFilter,
                Context.RECEIVER_EXPORTED
            )
        } else {
            requireContext().registerReceiver(
                packageUpdateReceiver,
                intentFilter
            )
        }
    }

    override fun onDestroy() {
        try {
            requireContext().unregisterReceiver(packageUpdateReceiver)
        } catch (_: Exception) {
        
        }
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            parentFragmentManager.popBackStackImmediate()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
