package com.qrify.app

import android.app.Application
import android.util.Log
import com.apexhub.sdk.ApexHubConfig
import com.apexhub.sdk.ApexHubUpdater
import com.apexhub.sdk.UpdateStrategy

/**
 * App entry point. Initializes the ApexHub OTA updater and schedules silent
 * background update checks (WorkManager). The foreground "check on launch"
 * happens in [MainActivity].
 */
class QrifyApp : Application() {

    lateinit var updater: ApexHubUpdater
        private set

    override fun onCreate() {
        super.onCreate()

        updater = ApexHubUpdater(
            context = this,
            config = ApexHubConfig(
                publicKey = BuildConfig.APEXHUB_PUBLIC_KEY,
                packageName = BuildConfig.APEXHUB_PACKAGE,
                channel = "stable",
                checkIntervalHours = 6,
                updateStrategy = UpdateStrategy.FLEXIBLE,
                allowMeteredNetwork = false
            )
        )

        // Silent background checks every ~6h; posts a notification when an
        // update is available even if the app is closed.
        try {
            updater.schedulePeriodicCheck(appDisplayName = getString(R.string.app_name))
        } catch (t: Throwable) {
            Log.w(TAG, "Could not schedule periodic update check", t)
        }
    }

    companion object {
        private const val TAG = "QrifyApp"
    }
}
