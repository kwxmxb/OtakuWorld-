package com.programmersbox.animeworldtv

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.preference.LeanbackPreferenceFragmentCompat
import androidx.leanback.preference.LeanbackSettingsFragmentCompat
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseUser
import com.programmersbox.anime_sources.Sources
import com.programmersbox.models.sourcePublish
import com.programmersbox.thirdpartyutils.into
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo

class SettingsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_fragment)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings_fragment, SettingsFragment())
                .commitNow()
        }
    }
}

class SettingsFragment : LeanbackSettingsFragmentCompat(), DialogPreference.TargetFragment {
    private var mPreferenceFragment: PreferenceFragmentCompat? = null
    override fun onPreferenceStartInitialScreen() {
        mPreferenceFragment = buildPreferenceFragment(R.xml.leanback_preferences, null)
        startPreferenceFragment(mPreferenceFragment!!)
    }

    override fun onPreferenceStartFragment(
        preferenceFragment: PreferenceFragmentCompat?,
        preference: Preference?
    ): Boolean {
        return false
    }

    override fun <T : Preference?> findPreference(key: CharSequence): T? =
        mPreferenceFragment?.findPreference(key)

    override fun onPreferenceStartScreen(
        preferenceFragment: PreferenceFragmentCompat?,
        preferenceScreen: PreferenceScreen
    ): Boolean {
        startPreferenceFragment(buildPreferenceFragment(R.xml.leanback_preferences, preferenceScreen.key))
        return true
    }

    private fun buildPreferenceFragment(preferenceResId: Int, root: String?): LeanbackPreferenceFragmentCompat {
        val fragment = PrefFragment()
        val args = Bundle()
        args.putInt(PREFERENCE_RESOURCE_ID, preferenceResId)
        args.putString(PREFERENCE_ROOT, root)
        fragment.arguments = args
        return fragment
    }

    class PrefFragment : LeanbackPreferenceFragmentCompat() {

        private val disposable = CompositeDisposable()

        override fun onCreatePreferences(bundle: Bundle?, s: String?) {
            //setPreferencesFromResource(R.xml.leanback_preferences, s)
            val root = arguments?.getString(PREFERENCE_ROOT, null)
            val prefResId = arguments?.getInt(PREFERENCE_RESOURCE_ID)!!
            if (root == null) {
                addPreferencesFromResource(prefResId)
            } else {
                setPreferencesFromResource(prefResId, root)
            }

            findPreference<Preference>("user_account")?.let { p ->

                fun accountChanges(user: FirebaseUser?) {
                    activity?.let {
                        Glide.with(this@PrefFragment)
                            .load(user?.photoUrl)
                            .placeholder(R.mipmap.ic_launcher)
                            .error(R.mipmap.ic_launcher)
                            .fallback(R.mipmap.ic_launcher)
                            .circleCrop()
                            .into<Drawable> { resourceReady { image, _ -> p.icon = image } }
                    }
                    p.title = user?.displayName ?: "User"
                }

                FirebaseAuthentication.auth.addAuthStateListener { accountChanges(it.currentUser) }

                accountChanges(FirebaseAuthentication.currentUser)
            }

            findPreference<Preference>("about_version")?.let { p ->
                p.title =
                    getString(
                        R.string.currentVersion,
                        context?.packageManager?.getPackageInfo(requireContext().packageName, 0)?.versionName.orEmpty()
                    )
            }

            findPreference<Preference>("current_source")?.let { p ->

                p.setOnPreferenceClickListener {
                    val list = Sources.values()
                    val service = requireContext().currentService
                    MaterialAlertDialogBuilder(requireContext(), R.style.Theme_MaterialComponents)
                        .setTitle(R.string.chooseASource)
                        .setSingleChoiceItems(
                            list.map { it.serviceName }.toTypedArray(),
                            list.indexOfFirst { it.serviceName == service }
                        ) { d, i ->
                            sourcePublish.onNext(list[i])
                            requireContext().currentService = list[i].serviceName
                            d.dismiss()
                        }
                        .setPositiveButton(R.string.done) { d, _ -> d.dismiss() }
                        .show()
                    true
                }
                sourcePublish.subscribe { p.title = getString(R.string.currentSource, it.serviceName) }
                    .addTo(disposable)
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            if (preference.key.equals("user_account")) {
                // Open an AuthenticationActivity
                //startActivity(Intent(activity, AuthenticationActivity::class.java))
                FirebaseAuthentication.currentUser?.let {
                    MaterialAlertDialogBuilder(this@PrefFragment.requireContext())
                        .setTitle("Log Out")
                        .setMessage("Are you sure?")
                        .setPositiveButton("Yes") { d, _ ->
                            FirebaseAuthentication.signOut()
                            d.dismiss()
                        }
                        .setNegativeButton("No") { d, _ -> d.dismiss() }
                        .show()
                } ?: FirebaseAuthentication.signIn(requireActivity())
            }
            return super.onPreferenceTreeClick(preference)
        }

        override fun onDestroy() {
            super.onDestroy()
            disposable.dispose()
        }

    }

    companion object {
        private const val PREFERENCE_RESOURCE_ID = "preferenceResource"
        private const val PREFERENCE_ROOT = "root"
    }
}