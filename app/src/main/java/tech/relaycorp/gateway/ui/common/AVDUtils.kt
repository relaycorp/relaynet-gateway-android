package tech.relaycorp.gateway.ui.common

import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.vectordrawable.graphics.drawable.Animatable2Compat
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat

fun ImageView.startLoopingAvd(@DrawableRes avdResId: Int) {
    val animated = AnimatedVectorDrawableCompat.create(context, avdResId)
    animated?.registerAnimationCallback(object : Animatable2Compat.AnimationCallback() {
        override fun onAnimationEnd(drawable: Drawable?) {
            this@startLoopingAvd.post { animated.start() }
        }
    })
    setImageDrawable(animated)
    animated?.start()
}

fun ImageView.stopLoopingAvd() {
    (drawable as? AnimatedVectorDrawableCompat)?.apply {
        clearAnimationCallbacks()
        stop()
    }
}
