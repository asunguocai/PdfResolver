package me.wanzio.pdfresolver

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import java.io.Serializable
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private const val TAG = "PermissionFragment"

class PermissionFragment : Fragment() {

    interface OnGrantListener : Serializable {
        fun onGranted(permissions: List<String>)
        fun onGrantFailed(permissions: List<String>)
    }

    companion object {
        val TAG = PermissionFragment::class.java.simpleName
        private val KEY_LISTENER = "me.wanzio.pdfresolver.PermissionFragment:KEY_LISTENER"
        private val KEY_PERMISSIONS = "me.wanzio.pdfresolver.PermissionFragment:KEY_PERMISSIONS"
        private val KEY_CUR_PERMISSION_INDEX =
            "me.wanzio.pdfresolver.PermissionFragment:KEY_CUR_PERMISSION_INDEX"
        private val KEY_HAS_CALLED_LISTENER_EVENT =
            "me.wanzio.pdfresolver.PermissionFragment:KEY_HAS_CALLED_LISTENER_EVENT"
        private const val REQUEST_CODE_PERMISSION_START = 666

        fun createInstance(
            unGrantedPermission: Set<String>,
            listener: OnGrantListener? = null
        ): PermissionFragment {
            val instance = PermissionFragment()
            val args = Bundle().apply {
                putSerializable(KEY_LISTENER, listener)
                putStringArray(KEY_PERMISSIONS, unGrantedPermission.toTypedArray())
            }
            instance.arguments = args
            return instance
        }
    }

    private var mOnGrantListener: OnGrantListener? by ArgsDelegate(
        KEY_LISTENER,
        null
    )
    private var mNeedRequestPermissions by ArgsDelegate(
        KEY_PERMISSIONS,
        listOf(),
        getter = {
            it as Array<String>
            it.toList()
        }
    )

    private lateinit var mDialog: AlertDialog
    private var mCurPermissionIndex by ArgsDelegate(
        KEY_CUR_PERMISSION_INDEX,
        0,
        setter = { key, old, args ->
            args.putInt(
                key, if (old != null) {
                    old + 1
                } else {
                    0
                }
            )
        }
    )

    private fun addPermissionIfRequire() {
        if (isAdded) {
            if (mCurPermissionIndex == mNeedRequestPermissions.size) {
                callListenerEvent()
                return
            }
            Log.d(TAG,"CUR index: $mCurPermissionIndex")
            val permission = mNeedRequestPermissions.get(mCurPermissionIndex++)

            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(),
                        permission
                    )
                ) {
                    // 需要弹出Dialog去显示请求原因
                    // 是否同意在DialogInterface#onClickListenerz中回调
                    showRequestPermissionRationale(permission)
                } else {
                    requestPermission(permission)
                }
            } else {
                addPermissionIfRequire()
            }
        }
    }

    private var mHashCalledListenerEvent: Boolean
        get() {
            return arguments?.getBoolean(KEY_HAS_CALLED_LISTENER_EVENT, false) ?: false
        }
        set(value) {
            if (arguments == null)
                arguments = Bundle()
            arguments!!.putBoolean(KEY_HAS_CALLED_LISTENER_EVENT, value)
        }

    private fun callListenerEvent() {
        if (isAdded && !mHashCalledListenerEvent) {
            if (mCurPermissionIndex < mNeedRequestPermissions.size) {
                onFaild()
            } else {
                mOnGrantListener?.onGranted(mNeedRequestPermissions)
                removeSelf()
            }
        }
    }


    private fun showRequestPermissionRationale(permission: String) {
        mDialog = AlertDialog.Builder(requireContext())
            .setTitle("Hint")
            .setMessage("We need some permission to access imgs what do you want!")
            .setPositiveButton(
                "Yes"
            ) { dialog, which ->
                requestPermission(permission)
            }
            .setNegativeButton(
                "No"
            ) { dialog, which ->
                onFaild()
            }
            .create()
        mDialog.show()
    }

    private fun requestPermission(permission: String) {
        ActivityCompat.requestPermissions(
            requireActivity(), arrayOf(permission),
            REQUEST_CODE_PERMISSION_START
        )
    }

    private fun onFaild() {
        mOnGrantListener?.onGrantFailed(mNeedRequestPermissions.filterIndexed { index, s ->
            index < mCurPermissionIndex
        })
    }

    private fun removeSelf() {
        if (isAdded) {
            requireActivity().supportFragmentManager.beginTransaction()
                .remove(this)
                .commitAllowingStateLoss()
        }
    }

    override fun onStart() {
        super.onStart()
        addPermissionIfRequire()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mDialog.isInitialized && mDialog.isShowing) {
            mDialog.dismiss()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSION_START) {
            if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                callListenerEvent()
            } else {
                if (mCurPermissionIndex < mNeedRequestPermissions.lastIndex) {
                    addPermissionIfRequire()
                } else {
                    callListenerEvent()
                }
            }

        }
    }

    class ArgsDelegate<T>(
        private val key: String,
        private val defaultValue: T,
        private val getter: (dataInBundle: Any) -> T = { it -> it as T },
        private val setter: (key: String, oldValue: T?, args: Bundle) -> Unit = { key, old, args -> }
    ) :
        ReadWriteProperty<Fragment, T> {

        private var curValue: T? = null

        private fun requireArgs(thisRef: Fragment): Bundle {
            if (thisRef.arguments == null) {
                thisRef.arguments = Bundle()
            }
            return thisRef.arguments!!
        }

        override fun getValue(thisRef: Fragment, property: KProperty<*>): T {
            val dataInBundle = requireArgs(thisRef).get(key)
            curValue = if (dataInBundle != null) {
                getter(dataInBundle)
            } else {
                defaultValue
            }
            return curValue!!
        }

        override fun setValue(thisRef: Fragment, property: KProperty<*>, value: T) {
            setter(key, curValue, requireArgs(thisRef))
        }

    }
}