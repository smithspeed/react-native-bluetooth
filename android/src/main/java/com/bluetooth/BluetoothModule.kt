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
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import org.json.JSONObject
import org.json.JSONTokener


class BluetoothModule (reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext) {

  val SUCCESS: String = "SUCCESS"
  val FAILED: String = "FAILED"
  lateinit var DATA: String
  private var responsePromise: Promise? = null
  private var bluetoothAdapter: BluetoothAdapter? = null

  companion object {
    const val NAME = "Bluetooth"
  }


  override fun getName(): String {
    return NAME
  }

  private val bluetoothPermssionActivityStatus = object: BaseActivityEventListener() {
    override fun onActivityResult(
      activity: Activity?,
      requestCode: Int,
      resultCode: Int,
      data: Intent?
    ) {
      super.onActivityResult(activity, requestCode, resultCode, data)

      if(requestCode == 1){

        when(resultCode){

          RESULT_OK -> {
            val res = JSONObject();
            res.put("status","ENABLE")
            return resolve("Bluetooth is ON",SUCCESS,res.toString())
          }
          RESULT_CANCELED -> {
            val res = JSONObject();
            res.put("status","DISABLE")
            return resolve("Bluetooth is OFF",SUCCESS,res.toString())
          }
          else -> return resolve("Out of the box result code");
        }
      }

    }
  }

  init {
    reactContext.addActivityEventListener(bluetoothPermssionActivityStatus)

  }

  //Check Bluetooth is on or off and ask to on bluetooth
  private fun getBluetoothStatus(options: String? = null) {
    val bluetoothManager: BluetoothManager? = getSystemService(reactApplicationContext, BluetoothManager::class.java)

    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    if(bluetoothAdapter==null){
      return resolve("Device doesn't support Bluetooth");
    }

    if(options!=null){

      val items = JSONTokener(options).nextValue() as JSONObject

      if(items.has("requestToEnable") && items.get("requestToEnable") as Boolean){
        val activity = currentActivity ?: return resolve("Activity doesn't exist")
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

    if(bluetoothAdapter.isEnabled){
      val obj = JSONObject();
      obj.put("status","ENABLE")
      return resolve("Bluetooth is ON",SUCCESS,obj.toString())
    }
    else{
      val obj = JSONObject();
      obj.put("status","DISABLE")
      return resolve("Bluetooth is OFF",SUCCESS,obj.toString())
    }
  }

  private fun resolve(message: String, status: String= FAILED, data: String = "", actCode: String = ""){

    if(responsePromise==null){
      return;
    }

    val map: WritableMap = Arguments.createMap()
    map.putString("status",status)
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
  private fun logPrint(value: String?){
    if(value==null){
      return
    }
    Log.i("BluetoothLog*", value)
  }

  private fun sendEvent(reactContext: ReactContext, eventName: String, params: WritableMap?) {
    reactContext
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
      .emit(eventName, params)
  }

  // React method

  @ReactMethod
  fun bluetoothStatus(options: String? = null, prm: Promise) {
    try {
      responsePromise = prm

      getBluetoothStatus(options);
    }
    catch (e:Exception){
      resolve(e.message.toString()+" #BMBS");
    }
  }

  @ReactMethod
  fun addListener(eventName: String) {

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
              obj.putString("connectionState", "ENABLE");
              sendEvent(getReactApplicationContext(), "bluetoothDidUpdateState", obj)

            }
            BluetoothAdapter.STATE_OFF -> {

              val obj: WritableMap = Arguments.createMap()
              obj.putString("connectionState", "DISABLE");
              sendEvent(getReactApplicationContext(), "bluetoothDidUpdateState", obj)
            }
            BluetoothAdapter.STATE_TURNING_OFF -> {

              val obj: WritableMap = Arguments.createMap()
              obj.putString("connectionState", "TURNING_OFF");
              sendEvent(getReactApplicationContext(), "bluetoothDidUpdateState", obj)
            }
            BluetoothAdapter.STATE_TURNING_ON -> {

              val obj: WritableMap = Arguments.createMap()
              obj.putString("connectionState", "TURNING_ON");
              sendEvent(getReactApplicationContext(), "bluetoothDidUpdateState", obj)
            }
          }

        }
      }
    }
  }


  @ReactMethod
  fun removeListeners(eventName: String) {

  }


}
