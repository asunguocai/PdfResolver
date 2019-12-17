package me.wanzio.pdfresolver.utils

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import me.wanzio.pdfresolver.PermissionFragment

object PermissionUtil {

    fun requirePermissions(
        context: AppCompatActivity,
        permission: Set<String>,
        onGrantListener: PermissionFragment.OnGrantListener
    ) {
        /**
         * 没有在这里限制重复调用
         */
        val instance = PermissionFragment.createInstance(
            permission,
            onGrantListener
        )
        context.supportFragmentManager.beginTransaction()
            .add(instance, PermissionFragment.TAG)
            .commitAllowingStateLoss()
        context.lifecycle.addObserver(object : LifecycleObserver {

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun onDestroy() {
                if (instance.isAdded) {
                    context.supportFragmentManager.beginTransaction()
                        .remove(instance)
                        .commitAllowingStateLoss()
                }
                context.lifecycle.removeObserver(this)
            }
        })
    }


}