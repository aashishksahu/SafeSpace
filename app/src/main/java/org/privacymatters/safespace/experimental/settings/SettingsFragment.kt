package org.privacymatters.safespace.experimental.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.privacymatters.safespace.AboutActivity
import org.privacymatters.safespace.AuthActivity
import org.privacymatters.safespace.R
import org.privacymatters.safespace.lib.fileManager.Operations
import org.privacymatters.safespace.utils.Constants
import org.privacymatters.safespace.utils.EncPref
import org.privacymatters.safespace.utils.SetTheme

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var backupExportDirActivityResult: ActivityResultLauncher<Intent>
    private lateinit var importBackupActivityResult: ActivityResultLauncher<Intent>
    private lateinit var sharedPref: SharedPreferences
    private lateinit var ops: Operations

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ops = Operations(requireActivity().application)

        sharedPref =
            requireActivity().getSharedPreferences(Constants.SHARED_PREF_FILE, Context.MODE_PRIVATE)

        // Import Backup
        importBackupActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    result.data.also { intent ->
                        val uri = intent?.data
                        if (uri != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                when (ops.importBackup(uri)) {
                                    0 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
//                                            updateRecyclerView()
                                        }
                                    }

                                    4 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            backupError(4)
                                        }
                                    }

                                    1 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            backupError(1)
                                        }
                                    }
                                }
                            }
                            Toast.makeText(
                                requireActivity().applicationContext,
                                getString(R.string.import_backup_msg),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            }

        // Export Backup
        backupExportDirActivityResult =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    result.data.also { intent ->
                        val uri = intent?.data
                        if (uri != null) {
                            CoroutineScope(Dispatchers.IO).launch {
                                when (ops.exportBackup(uri)) {
                                    0 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            Toast.makeText(
                                                requireActivity().baseContext,
                                                getString(R.string.export_backup_success),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    4 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            backupError(4)
                                        }
                                    }

                                    1 -> {
                                        CoroutineScope(Dispatchers.Main).launch {
                                            backupError(1)
                                        }
                                    }
                                }
                            }
                            Toast.makeText(
                                requireActivity().applicationContext,
                                getString(R.string.export_backup_msg),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }

            }

    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<Preference>("change_pin")
            ?.setOnPreferenceClickListener {
                context?.let { changePin(it) }
                true // Return true if the click is handled.
            }

        findPreference<Preference>("change_theme")
            ?.setOnPreferenceClickListener {
                context?.let { changeTheme(it) }
                true // Return true if the click is handled.
            }

        findPreference<Preference>("import_backup")
            ?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "application/zip"
                importBackupActivityResult.launch(intent)

                true // Return true if the click is handled.
            }

        findPreference<Preference>("export_backup")
            ?.setOnPreferenceClickListener {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                backupExportDirActivityResult.launch(intent)

                true // Return true if the click is handled.
            }

        findPreference<Preference>("about")
            ?.setOnPreferenceClickListener {
                val intent = Intent(requireActivity(), AboutActivity::class.java)
                startActivity(intent)
                true // Return true if the click is handled.
            }
    }

    private fun changePin(context: Context) {
        val builder = MaterialAlertDialogBuilder(context)

        val inflater: LayoutInflater = layoutInflater
        val changePinLayout = inflater.inflate(R.layout.change_pin, null)
        val currentPinEditText =
            changePinLayout.findViewById<EditText>(R.id.editTextCurrentPin)

        builder.setTitle(getString(R.string.change_pin))
            .setCancelable(true)
            .setView(changePinLayout)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->

                if (currentPinEditText.text.toString() == EncPref.getString(
                        Constants.HARD_PIN,
                        context
                    )
                ) {

                    // clear pin from shared prefs and reset enrollment status
                    EncPref.clearString(Constants.HARD_PIN, requireActivity().applicationContext)
                    EncPref.clearBoolean(
                        Constants.HARD_PIN_SET,
                        requireActivity().applicationContext
                    )
                    sharedPref.edit()
                        .putBoolean(Constants.USE_BIOMETRIC, false)
                        .putBoolean(Constants.USE_BIOMETRIC_BCKP, false)
                        .apply()

                    // restart application to set pin again
                    requireActivity().finish()
                    val intent =
                        Intent(requireActivity().applicationContext, AuthActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)

                } else {
                    Toast.makeText(
                        context,
                        getString(R.string.pin_error5),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
            .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun changeTheme(context: Context) {
        val builder = MaterialAlertDialogBuilder(context)

        val inflater: LayoutInflater = layoutInflater
        val changeThemeLayout = inflater.inflate(R.layout.change_theme, null)
        val changeThemeTextView =
            changeThemeLayout.findViewById<TextInputLayout>(R.id.changeThemeTextLayout)

        builder.setTitle(getString(R.string.change_theme))
            .setCancelable(true)
            .setView(changeThemeLayout)
            .setPositiveButton(getString(R.string.ok)) { _, _ ->

                SetTheme.setTheme(
                    (requireActivity() as AppCompatActivity).delegate,
                    requireActivity().applicationContext,
                    changeThemeTextView.editText?.text.toString()
                )

                sharedPref.edit()
                    .putString(
                        getString(R.string.change_theme),
                        changeThemeTextView.editText?.text.toString()
                    )
                    .apply()
            }
            .setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }

    private fun backupError(errorCode: Int) {
        val msg = when (errorCode) {
            4 -> getString(R.string.backup_err_space)
            1 -> getString(R.string.backup_err_other)
            else -> getString(R.string.backup_err_other)
        }

        Toast.makeText(requireActivity().applicationContext, msg, Toast.LENGTH_SHORT).show()

    }
}