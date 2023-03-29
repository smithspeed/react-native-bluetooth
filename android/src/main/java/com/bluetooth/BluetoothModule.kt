package com.bluetooth

import android.Manifest
import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.SparseArray
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import org.json.JSONObject
import org.json.JSONTokener


class BluetoothModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext), PermissionListener {

    val SUCCESS: String = "SUCCESS"
    val FAILED: String = "FAILED"
    lateinit var DATA: String
    private var responsePromise: Promise? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var permissionListener : PermissionListener? = null
    private var mCallbacks: SparseArray<Callback>? = null
    private var mRequestCodeForBluetoothPermission = 0
    private val GRANTED = "granted"
    private val DENIED = "denied"
    private val UNAVAILABLE = "unavailable"
    private val BLOCKED = "blocked"


    companion object {
        const val NAME = "Bluetooth"
    }


    override fun getName(): String {

        return NAME
    }

    private val bluetoothPermssionActivityStatus = object : BaseActivityEventListener() {
        override fun onActivityResult(
            activity: Activity?,
            requestCode: Int,
            resultCode: Int,
            data: Intent?
        ) {
            super.onActivityResult(activity, requestCode, resultCode, data)

            if (requestCode == 1) {

                when (resultCode) {

                    RESULT_OK -> {
                        val res = JSONObject()
                        res.put("status", "ENABLE")
                        return resolve("Bluetooth is ON", SUCCESS, res.toString())
                    }
                    RESULT_CANCELED -> {
                        val res = JSONObject();
                        res.put("status", "DISABLE")
                        return resolve("Bluetooth is OFF", SUCCESS, res.toString())
                    }
                    else -> return resolve("Out of the box result code");
                }

            }
        }
    }

    private val permsResult = object : ActivityCompat.OnRequestPermissionsResultCallback {
        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            return resolve("Permission Blocked", SUCCESS)
        }

    }

    init {
        reactContext.addActivityEventListener(bluetoothPermssionActivityStatus)
        mCallbacks = SparseArray()
    }

    private fun getPermissionAwareActivity(): PermissionAwareActivity? {
        val activity = currentActivity
        checkNotNull(activity) { "Tried to use permissions API while not attached to an " + "Activity." }
        check(activity is PermissionAwareActivity) {
            ("Tried to use permissions API but the host Activity doesn't"
                + " implement PermissionAwareActivity.")
        }
        return activity
    }

    //Check Bluetooth is on or off and ask to on bluetooth
    private fun getBluetoothStatus(options: String? = null) {

        if (options != null) {

            val items = JSONTokener(options).nextValue() as JSONObject

            if (items.has("requestToEnable") && items.get("requestToEnable") as Boolean) {

                val activity = currentActivity ?: return resolve("Activity doesn't exist")

                if(Build.VERSION.SDK_INT >=  Build.VERSION_CODES.S){

                    if (ActivityCompat.checkSelfPermission(
                            reactApplicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        val permsActivity = getPermissionAwareActivity()

                        mCallbacks?.put(mRequestCodeForBluetoothPermission, object : Callback {
                            override fun invoke(vararg p0: Any?) {
                                val results = p0[0] as IntArray
                                //Log.i("dx*", "invoke: "+result)

                                if(results[0]==PackageManager.PERMISSION_GRANTED){

                                    //val obj = JSONObject();

                                    //obj.put("status","PERMISSION_GRANTED")

                                    //return resolve("Permission Granted", SUCCESS, obj.toString(), "OnPermission")
                                    isBluetoothEnable()
                                    return
                                }
                                else{

                                    val isPermissionBlockedByUser = p0[1] as PermissionAwareActivity

                                    if(isPermissionBlockedByUser.shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_CONNECT)){
                                        val obj = JSONObject();

                                        obj.put("status","PERMISSION_DENIED")

                                        return resolve("Permission Denied", SUCCESS, obj.toString(), "OnPermission")
                                    }
                                    else{

                                        val obj = JSONObject();

                                        obj.put("status","PERMISSION_BLOCKED")

                                        return resolve("Permission Blocked", SUCCESS, obj.toString(), "OnPermission")
                                    }
                                }
                            }
                        })

                        if (permsActivity != null) {
                            permsActivity?.requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),mRequestCodeForBluetoothPermission,this)
                            mRequestCodeForBluetoothPermission++
                        }

                        return
                    }
                    else{
                        isBluetoothEnable()
                        return
                    }
                }
                else{

                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

                    if (ActivityCompat.checkSelfPermission(
                            reactApplicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        activity.startActivityForResult(enableBtIntent, 1)
                        return
                    }
                }
            }
        }
        else{
            isBluetoothEnable()
        }
    }

    fun isBluetoothEnable(){

        val bluetoothManager: BluetoothManager? =
            getSystemService(reactApplicationContext, BluetoothManager::class.java)

        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

        if (bluetoothAdapter == null) {
            return resolve("Device doesn't support Bluetooth");
        }

        if (bluetoothAdapter.isEnabled) {
            val obj = JSONObject();
            obj.put("status", "ENABLE")
            return resolve("Bluetooth is ON", SUCCESS, obj.toString())
        } else {
            val obj = JSONObject();
            obj.put("status", "DISABLE")
            return resolve("Bluetooth is OFF", SUCCESS, obj.toString())
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>?,
        grantResults: IntArray?
    ): Boolean {
        mCallbacks?.get(requestCode)?.invoke(grantResults, getPermissionAwareActivity());
        mCallbacks?.remove(requestCode);
        return mCallbacks?.size() == 0;
    }


    private fun resolve(
        message: String,
        status: String = FAILED,
        data: String = "",
        actCode: String = ""
    ) {

        if (responsePromise == null) {
            return;
        }

        val map: WritableMap = Arguments.createMap()
        map.putString("status", status)
        map.putString("message", message)
        map.putString("data", data)
        map.putString("actCode", actCode)

        responsePromise!!.resolve(map)
        responsePromise = null
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {

        if (bluetoothAdapter == null) {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        }
        return bluetoothAdapter
    }

    //For Log
    private fun logPrint(value: String?) {
        if (value == null) {
            return
        }
        Log.i("BluetoothLog*", value)
    }

    private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {

        if (reactContext.hasCatalystInstance()) {
            reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                .emit(eventName, params)
        }

    }

    fun registerBluetoothStateBroadcast(){
        if (getBluetoothAdapter() == null) {
            return;
        }

        val filter: IntentFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        val activity = currentActivity ?: return resolve("Activity doesn't exist")
        activity.registerReceiver(receiver, filter)
    }

    // Create a BroadcastReceiver for ACTION_STATE_CHANGED.
    private val receiver = object : BroadcastReceiver(){

        override fun onReceive(context: Context?, intent: Intent?) {
            val action: String? = intent?.action

            when(action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)

                    when(state){
                        BluetoothAdapter.STATE_ON -> {

                            val obj: WritableMap = Arguments.createMap()
                            obj.putString("status", "ENABLE");
                            sendEvent(getReactApplicationContext(), "bluetoothDidUpdateState", obj)

                        }
                        BluetoothAdapter.STATE_OFF -> {

                            val obj: WritableMap = Arguments.createMap()
                            obj.putString("status", "DISABLE");
                            sendEvent(getReactApplicationContext(), "bluetoothDidUpdateState", obj)
                        }
                        BluetoothAdapter.STATE_TURNING_OFF -> {

                            val obj: WritableMap = Arguments.createMap()
                            obj.putString("status", "TURNING_OFF");
                            sendEvent(getReactApplicationContext(), "bluetoothDidUpdateState", obj)
                        }
                        BluetoothAdapter.STATE_TURNING_ON -> {

                            val obj: WritableMap = Arguments.createMap()
                            obj.putString("status", "TURNING_ON");
                            sendEvent(getReactApplicationContext(), "bluetoothDidUpdateState", obj)
                        }
                    }

                }
            }
        }
    }

    // React method
    @ReactMethod
    fun bluetoothStatus(options: String? = null, prm: Promise) {
        try {
            responsePromise = prm

            getBluetoothStatus(options);
        } catch (e: Exception) {
            resolve(e.message.toString() + " #BMBS");
        }
    }

    @ReactMethod
    fun addListener(type: String?) {
        // Keep: Required for RN built in Event Emitter Calls.
        registerBluetoothStateBroadcast()
    }

    @ReactMethod
    fun removeListeners(type: Int?) {
        // Keep: Required for RN built in Event Emitter Calls.
    }




}


