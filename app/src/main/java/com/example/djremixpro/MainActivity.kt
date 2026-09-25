package com.example.djremixpro

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.example.djremixpro.app.DJRemixProApp
import com.example.djremixpro.core.ui.ShellNavigator
import com.example.djremixpro.core.ui.ToastHost
import com.example.djremixpro.core.ui.ToastMessage
import com.example.djremixpro.databinding.ActivityMainBinding
import com.example.djremixpro.feature.shell.MainViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Toast lifetime, App.dc.html:279. */
private const val TOAST_MS = 2200L

class MainActivity : AppCompatActivity(), ToastHost, ShellNavigator {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }
    private val toastRetainer: ToastRetainer by viewModels()

    private val tabDestinations = setOf(
        R.id.homeFragment, R.id.libraryFragment, R.id.recordingsFragment, R.id.learnFragment,
    )
    private val subDestinations = setOf(R.id.settingsFragment, R.id.languageFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host) as NavHostFragment
        navController = navHost.navController

        if (savedInstanceState == null && !isOnboardingDone()) {
            // First launch: Splash → Onboarding → Permission → Home (D-25).
            navController.navigate(
                R.id.splashFragment, null,
                navOptions { popUpTo(R.id.homeFragment) { inclusive = true } },
            )
        }

        binding.bottomNav.setupWithNavController(navController)
        binding.bottomNav.setOnItemReselectedListener { /* stay on the current tab */ }
        binding.buttonBack.setOnClickListener { navController.navigateUp() }
        binding.buttonSettings.setOnClickListener {
            navController.navigate(R.id.action_global_settings)
        }
        binding.miniPlayer.setOnStopClickListener { viewModel.onMiniPlayerStop() }
        navController.addOnDestinationChangedListener { _, destination, _ -> renderChrome(destination) }

        applyInsets()
        toastRetainer.stillVisible()?.let(binding.toast::show)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val destinationId = navController.currentDestination?.id
                    binding.miniPlayer.bind(if (destinationId in onboardingDestinations) null else state.miniPlayer)
                }
            }
        }
    }

    private val onboardingDestinations = setOf(
        R.id.splashFragment, R.id.onboardingFragment, R.id.permissionFragment,
    )

    private fun isOnboardingDone(): Boolean {
        val settings = (application as DJRemixProApp).container.settingsRepository.settings
        // DataStore is already warmed up by DJRemixProApp, so this read is served from memory.
        return runBlocking { settings.first().onboardingDone }
    }

    private fun renderChrome(destination: NavDestination) {
        val id = destination.id
        val isTab = id in tabDestinations
        val isSub = id in subDestinations
        val showChrome = isTab || isSub
        binding.topBar.isVisible = showChrome
        binding.textTitle.text = destination.label
        binding.buttonBack.isVisible = isSub
        binding.buttonSettings.isVisible = isTab
        isTabDestination = isTab
        updateBottomNav()
        if (!showChrome) binding.miniPlayer.bind(null)
        else binding.miniPlayer.bind(viewModel.uiState.value.miniPlayer)
        ViewCompat.requestApplyInsets(binding.root)
    }

    private var isTabDestination = false
    private var isImeVisible = false

    /** The bottom nav only shows on tab screens and hides while the keyboard is open. */
    private fun updateBottomNav() {
        val show = isTabDestination && !isImeVisible
        binding.bottomNav.isVisible = show
        binding.bottomNavDivider.isVisible = show
    }

    private fun applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            if (imeVisible != isImeVisible) {
                isImeVisible = imeVisible
                updateBottomNav()
            }
            // BottomNavigationView pads itself for the navigation bar; when it is hidden the root does it.
            val bottom = if (binding.bottomNav.isVisible) 0 else maxOf(bars.bottom, ime.bottom)
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bottom)
            insets
        }
    }

    override fun showToast(message: ToastMessage) {
        toastRetainer.remember(message)
        binding.toast.show(message)
    }

    /**
     * Changing theme or language recreates the Activity right after the toast is posted
     * ("Đã chọn …", B02). The retainer outlives recreation so the toast is shown again.
     */
    class ToastRetainer : ViewModel() {
        private var pending: ToastMessage? = null
        private var shownAt = 0L

        fun remember(message: ToastMessage) {
            pending = message
            shownAt = SystemClock.uptimeMillis()
        }

        fun stillVisible(): ToastMessage? =
            pending?.takeIf { SystemClock.uptimeMillis() - shownAt < TOAST_MS }
    }

    override fun openTab(destinationId: Int, args: Bundle?) {
        if (args == null) {
            binding.bottomNav.selectedItemId = destinationId
            return
        }
        navController.navigate(
            destinationId, args,
            navOptions {
                launchSingleTop = true
                popUpTo(R.id.homeFragment) { saveState = false }
            },
        )
    }
}
