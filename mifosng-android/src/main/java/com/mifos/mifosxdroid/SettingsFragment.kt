@file:Suppress("DEPRECATION")

package com.mifos.mifosxdroid

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.preference.*
import androidx.preference.Preference.OnPreferenceChangeListener
import android.widget.Toast
import com.mifos.mifosxdroid.dialogfragments.syncsurveysdialog.SyncSurveysDialogFragment
import com.mifos.mifosxdroid.online.DashboardActivity
import com.mifos.mifosxdroid.passcode.PassCodeActivity
import com.mifos.mobile.passcode.utils.PasscodePreferencesHelper
import com.mifos.utils.*

/**
 * Created by mayankjindal on 22/07/17.
 */
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    private lateinit var mEnableSyncSurvey: SwitchPreference
    private lateinit var mInstanceUrlPref: EditTextPreference
    private lateinit var languages: Array<String>
    private var languageCallback: LanguageCallback? = null
    private var mRootKey : String? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        mRootKey = rootKey
        languages = requireActivity().resources.getStringArray(R.array.language_option)

        initSurveyPreferences()
        initInstanceUrlPreferences()
        initLanguagePreferences()
        initThemePreferences()
    }

    private fun initSurveyPreferences() {
        mEnableSyncSurvey =
            (findPreference(requireContext().getString(R.string.sync_survey)) as SwitchPreference?)!!
        mEnableSyncSurvey.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            if (newValue as Boolean) {
                val syncSurveysDialogFragment = SyncSurveysDialogFragment.newInstance()
                val fragmentTransaction = parentFragmentManager.beginTransaction()
                fragmentTransaction.addToBackStack(FragmentConstants.FRAG_SURVEYS_SYNC)
                syncSurveysDialogFragment.isCancelable = false
                fragmentTransaction.let {
                    syncSurveysDialogFragment.show(
                        it,
                        requireContext().getString(R.string.sync_clients)
                    )
                }
            }
            true
        }
    }

    private fun initInstanceUrlPreferences() {
        mInstanceUrlPref = (findPreference(
            requireContext().getString(R.string.hint_instance_url)
        ) as EditTextPreference?)!!
        val instanceUrl = PrefManager.instanceUrl
        mInstanceUrlPref.text = instanceUrl
        mInstanceUrlPref.isSelectable = true
        mInstanceUrlPref.dialogTitle = "Edit Instance Url"
        mInstanceUrlPref.setDialogIcon(R.drawable.ic_baseline_edit_24)
        mInstanceUrlPref.onPreferenceChangeListener =
            OnPreferenceChangeListener { _, o ->
                val newUrl = o.toString()
                if (newUrl != instanceUrl) {
                    PrefManager.instanceUrl = newUrl
                    Toast.makeText(activity, newUrl, Toast.LENGTH_SHORT).show()
                    startActivity(Intent(activity, DashboardActivity::class.java))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        activity?.finishAffinity()
                    } else {
                        activity?.finish()
                    }
                }
                true
            }
    }

    private fun initLanguagePreferences() {
        val langPref = findPreference("language_type") as ListPreference?
        langPref?.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            LanguageHelper.setLocale(requireActivity(), newValue.toString())
            val intent = Intent(requireActivity(), requireActivity().javaClass)
            intent.putExtra(Constants.HAS_SETTING_CHANGED, true)
            startActivity(intent)
            requireActivity().finish()
            preferenceScreen = null
            setPreferencesFromResource(R.xml.preferences,mRootKey)
            preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            true
        }
    }

    private fun initThemePreferences() {
        val themePreference =
            findPreference(requireContext().getString(R.string.mode_key)) as ListPreference?
        themePreference?.onPreferenceChangeListener = OnPreferenceChangeListener { _, newValue ->
            val themeOption = newValue as String
            ThemeHelper.applyTheme(themeOption)
            Toast.makeText(activity, "Switched to $themeOption Mode", Toast.LENGTH_SHORT).show()
            true
        }
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    fun setLanguageCallback(languageCallback: LanguageCallback?) {
        this.languageCallback = languageCallback
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, s: String) {
        val preference = findPreference(s) as ListPreference?
        LanguageHelper.setLocale(requireActivity(), preference?.value)
    }

    interface LanguageCallback

    override fun onPreferenceTreeClick(
        preference: Preference
    ): Boolean {
        when (preference.key) {
            getString(R.string.password) -> {
                //    TODO: create changePasswordActivity and implement the logic for password change
            }

            getString(R.string.passcode) -> {
                activity?.let {
                    val passCodePreferencesHelper = PasscodePreferencesHelper(activity)
                    val currPassCode = passCodePreferencesHelper.passCode
                    passCodePreferencesHelper.savePassCode("")
                    val intent = Intent(it, PassCodeActivity::class.java).apply {
                        putExtra(Constants.CURR_PASSWORD, currPassCode)
                        putExtra(Constants.IS_TO_UPDATE_PASS_CODE, true)
                    }
                    startActivity(intent)
                }
            }
        }
        return super.onPreferenceTreeClick(preference)
    }
}
