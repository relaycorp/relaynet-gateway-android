package tech.relaycorp.gateway

import android.content.Context
import android.os.Build
import android.os.StrictMode
import android.os.StrictMode.OnThreadViolationListener
import android.os.StrictMode.OnVmViolationListener
import android.os.strictmode.Violation
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import tech.relaycorp.gateway.common.Logging.logger
import java.util.logging.Level

object StrictModeSetup {
    operator fun invoke(context: Context) {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(
                            ContextCompat.getMainExecutor(context),
                            ThreadPolicyViolationListener(),
                        )
                    } else {
                        penaltyLog()
                    }
                }
                .build(),
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(
                            ContextCompat.getMainExecutor(context),
                            VMViolationListener(),
                        )
                    } else {
                        penaltyLog()
                    }
                }
                .build(),
        )
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private class ThreadPolicyViolationListener : OnThreadViolationListener {
        override fun onThreadViolation(violation: Violation) {
            val isAllowed = violation.stackTrace.any { element ->
                THREAD_POLICY_ALLOWLIST.any {
                    element.toString().contains(it)
                }
            }
            if (isAllowed) {
                logger.log(Level.FINE, "StrictMode violation allowed: $violation")
            } else {
                throw violation
            }
        }
    }

    private val THREAD_POLICY_ALLOWLIST = listOf(
        // Huawei startup font loading
        "Typeface.loadSystemFonts",
        // Xiaomi startup font loading
        "TypefaceUtils.loadFontSettings",
        "TypefaceUtils.initSystemFont",
    )

    @RequiresApi(Build.VERSION_CODES.P)
    private class VMViolationListener : OnVmViolationListener {
        override fun onVmViolation(violation: Violation) {
            val isAllowed = violation.stackTrace.any { element ->
                VM_POLICY_ALLOWLIST.any {
                    element.toString().contains(it)
                }
            }
            if (isAllowed) {
                logger.log(Level.FINE, "StrictMode violation allowed: $violation")
            } else {
                throw violation
            }
        }
    }

    private val VM_POLICY_ALLOWLIST = listOf(
        // UntaggedSockets (we aren't able to tag Netty socket threads)
        "okhttp3.internal.connection.RealConnection.connectSocket",
        "io.ktor.client.engine",
        "io.ktor.network.sockets",
        "io.netty.channel.socket",
        "io.grpc.okhttp",
    )
}
