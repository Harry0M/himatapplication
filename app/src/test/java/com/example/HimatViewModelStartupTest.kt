package com.example

import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HimatViewModel
import com.google.firebase.FirebaseApp
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Building the ViewModel with nobody signed in used to crash the app before it drew a single frame.
 *
 * `viewModelScope` dispatches on `Dispatchers.Main.immediate`, so the `init` block's `launch` ran
 * where it stood instead of waiting, and `currentUser` — a StateFlow that always holds a value —
 * delivered `null` while the ViewModel was still half built. That went to `stopRealtimeSync` →
 * `resetNavigation` → `clearSupplierQueue`, which reads a property declared two thousand lines below
 * where the `init` block used to sit, so it was still null: a NullPointerException on every cold start.
 *
 * This test builds the real ViewModel and then lets the looper run, which is the whole crash path. The
 * old code threw inside the constructor, so `buildViewModel` alone would have failed here.
 *
 * What it does not do is guard the fix completely. Two things now keep the startup path safe — the
 * wiring is last in the class, and it is dispatched rather than run in place — and either one on its
 * own is enough, so undoing just one of them leaves these tests green. The comment on that `init`
 * block is the other half of the guard; this test is what notices if the path breaks some other way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HimatViewModelStartupTest {

    /**
     * On a phone the Firebase provider in the manifest does this before any code runs. A unit test has
     * no provider, so the ViewModel's very first field would fail for a reason that has nothing to do
     * with what is being checked here.
     */
    @Before
    fun initialiseFirebase() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        if (FirebaseApp.getApps(application).isEmpty()) {
            FirebaseApp.initializeApp(application)
        }
    }

    private fun buildViewModel(): HimatViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        return HimatViewModel(application)
    }

    @Test
    fun `the view model can be built with nobody signed in`() {
        val viewModel = buildViewModel()

        // The sign-in wiring is posted rather than run during construction, so nothing has touched
        // the later properties yet. Draining the looper is what used to blow up.
        shadowOf(Looper.getMainLooper()).idle()

        assertNotNull("currentUser flow was never created", viewModel.currentUser)
        assertNull("nobody is signed in, so there is no user", viewModel.currentUser.value)
    }

    /** The properties the startup path reaches have to be real objects by the time it runs. */
    @Test
    fun `the state the startup path touches is initialised`() {
        val viewModel = buildViewModel()
        shadowOf(Looper.getMainLooper()).idle()

        // clearSupplierQueue reads this one; it was the null that crashed the app
        assertNotNull("supplierQueue was not initialised", viewModel.supplierQueue)
        assertNull("no supplier queue is open at startup", viewModel.supplierQueue.value)

        // resetNavigation writes these two
        assertNotNull(viewModel.currentScreen)
        assertEquals(AppScreen.DASHBOARD, viewModel.currentScreen.value)
    }

    /** Nobody is signed in, so the app must say "still checking", not "restricted". */
    @Test
    fun `a fresh start has not decided authorization yet`() {
        val viewModel = buildViewModel()
        shadowOf(Looper.getMainLooper()).idle()

        assertNull("authorization should be undecided, not refused", viewModel.isAuthorized.value)
        assertEquals(false, viewModel.isSuperAdmin.value)
    }

    /** Draining the looper repeatedly must stay calm — the collectors keep running for the app's life. */
    @Test
    fun `the startup wiring survives repeated looper drains`() {
        val viewModel = buildViewModel()
        repeat(5) { shadowOf(Looper.getMainLooper()).idle() }
        assertNull(viewModel.supplierQueue.value)
        assertEquals(AppScreen.DASHBOARD, viewModel.currentScreen.value)
    }
}
