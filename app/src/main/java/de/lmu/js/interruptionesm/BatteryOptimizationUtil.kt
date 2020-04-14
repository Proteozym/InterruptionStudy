package de.lmu.js.interruptionesm


/*
 * MIT License
 *
 * Copyright (c) 2018 Markus Deutsch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import android.app.AlertDialog
import android.app.AlertDialog.Builder
import android.content.ComponentName
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.Nullable
import java.util.*


/**
 * Get a dialog that informs the user to disable battery optimization for your app.
 *
 *
 * Use the dialog like that:
 * final AlertDialog dialog = BatteryOptimizationUtil.getBatteryOptimizationDialog(context);
 * if(dialog != null) dialog.show();
 *
 *
 * Alter the dialog texts so that they fit your needs. You can provide additional actions that
 * should be performed if the positive or negative button are clicked by using the provided method:
 * getBatteryOptimizationDialog(Context, OnBatteryOptimizationAccepted, OnBatteryOptimizationCanceled)
 *
 *
 * Source: https://gist.github.com/moopat/e9735fa8b5cff69d003353a4feadcdbc
 *
 *
 * @author Markus Deutsch @moopat
 */
object BatteryOptimizationUtil {
    /**
     * Get the battery optimization dialog.
     * By default the dialog will send the user to the relevant activity if the positive button is
     * clicked, and closes the dialog if the negative button is clicked.
     *
     * @param context Context
     * @return the dialog or null if battery optimization is not available on this device
     */
    @Nullable
    fun getBatteryOptimizationDialog(context: Context): AlertDialog? {
        return getBatteryOptimizationDialog(context, null, null)
    }

    /**
     * Get the battery optimization dialog.
     * By default the dialog will send the user to the relevant activity if the positive button is
     * clicked, and closes the dialog if the negative button is clicked. Callbacks can be provided
     * to perform additional actions on either button click.
     *
     * @param context          Context
     * @param positiveCallback additional callback for the positive button. can be null.
     * @param negativeCallback additional callback for the negative button. can be null.
     * @return the dialog or null if battery optimization is not available on this device
     */
    @Nullable
    fun getBatteryOptimizationDialog(
        context: Context,
        @Nullable positiveCallback: OnBatteryOptimizationAccepted?,
        @Nullable negativeCallback: OnBatteryOptimizationCanceled?
    ): AlertDialog? {
        /*
         * If there is no resolvable component return right away. We do not use
         * isBatteryOptimizationAvailable() for this check in order to avoid checking for
         * resolvable components twice.
         */
        val componentName =
            getResolveableComponentName(context) ?: return null
        return Builder(context)
            .setTitle("Checking Battery Optimization")
            .setMessage("Have you enabled battery optimization or Auto Start feature? If yes, please ignore this message.")
            .setNegativeButton(
                "No",
                DialogInterface.OnClickListener { dialog, which -> negativeCallback?.onBatteryOptimizationCanceled() })
            .setPositiveButton(
                "Yes",
                DialogInterface.OnClickListener { dialog, which ->
                    positiveCallback?.onBatteryOptimizationAccepted()
                    val intent = Intent()
                    intent.component = componentName
                    context.startActivity(intent)
                }).create()
    }

    /**
     * Find out if battery optimization settings are available on this device.
     *
     * @param context Context
     * @return true if battery optimization is available
     */
    fun isBatteryOptimizationAvailable(context: Context): Boolean {
        return getResolveableComponentName(context) != null
    }

    @Nullable
    private fun getResolveableComponentName(context: Context): ComponentName? {
        for (componentName in componentNames) {
            val intent = Intent()
            intent.component = componentName
            if (context.packageManager
                    .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null
            ) return componentName
        }
        return null
    }

    /**
     * Get a list of all known ComponentNames that provide battery optimization on different
     * devices.
     * Based on Shivam Oberoi's answer on StackOverflow: https://stackoverflow.com/a/48166241/2143225
     *
     * @return list of ComponentName
     */
    private val componentNames: List<ComponentName>
        private get() {
            val names: MutableList<ComponentName> =
                ArrayList()
            names.add(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.letv.android.letvsafe",
                    "com.letv.android.letvsafe.AutobootManageActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.startupapp.StartupAppListActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                )
            )
            names.add(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.htc.pitroad",
                    "com.htc.pitroad.landingpage.activity.LandingPageActivity"
                )
            )
            names.add(
                ComponentName(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.MainActivity"
                )
            )
            return names
        }

    interface OnBatteryOptimizationAccepted {
        /**
         * Called if the user clicks the "OK" button of the battery optimization dialog. This does
         * not mean that the user has performed the necessary steps to exclude the app from
         * battery optimizations.
         */
        fun onBatteryOptimizationAccepted()
    }

    interface OnBatteryOptimizationCanceled {
        /**
         * Called if the user clicks the "Cancel" button of the battery optimization dialog.
         */
        fun onBatteryOptimizationCanceled()
    }
}