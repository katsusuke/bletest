package club.urvogel.bletest

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.BleManager
import no.nordicsemi.android.ble.callback.DataReceivedCallback
import no.nordicsemi.android.ble.data.Data
import java.util.*

class UARTManager: BleManager {
    val mBluetoothDevice: BluetoothDevice
    private val gattCallback: UARTBleManagerGattCallback

    constructor(context: Context, bluetoothDevice: BluetoothDevice): super(context){
        mBluetoothDevice = bluetoothDevice
        gattCallback = UARTBleManagerGattCallback(bluetoothDevice)
    }

    override fun getGattCallback(): BleManagerGattCallback {
        return gattCallback
    }

    private inner class UARTBleManagerGattCallback(mBluetoothDevice: BluetoothDevice) : BleManagerGattCallback(), DataReceivedCallback {
        val tag = "UARTManager.UARTBleManagerGattCallback"
        var mRXCharacteristic: BluetoothGattCharacteristic? = null
        var mTXCharacteristic:BluetoothGattCharacteristic? = null
        private val UART_SERVICE_UUID1 = UUID.fromString("ACA50001-7671-4B6A-8F80-80F6FEA814FA")
        private val UART_RX_CHARACTERISTIC_UUID1 = UUID.fromString("ACA50002-7671-4B6A-8F80-80F6FEA814FA")
        private val UART_TX_CHARACTERISTIC_UUID1 =  UUID.fromString("ACA50003-7671-4B6A-8F80-80F6FEA814FA")
        private val UUID_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        override fun initialize() {
            setNotificationCallback(mTXCharacteristic).with(this)
            enableNotifications(mTXCharacteristic).enqueue()
        }

        public override fun isRequiredServiceSupported(gatt: BluetoothGatt): Boolean {
            var service = gatt.getService(UART_SERVICE_UUID1)
            if (service == null) return false
            else {
                mRXCharacteristic = service.getCharacteristic(UART_RX_CHARACTERISTIC_UUID1)
                mTXCharacteristic = service.getCharacteristic(UART_TX_CHARACTERISTIC_UUID1)
                var writeRequest = false
                var writeCommand = false
                if (mRXCharacteristic != null && mTXCharacteristic != null) {
                    val descriptor: BluetoothGattDescriptor =
                        mTXCharacteristic!!.getDescriptor(UUID_DESCRIPTOR)
                    gatt.writeDescriptor(descriptor)
                    val rxProperties: Int = mRXCharacteristic!!.getProperties()
                    writeRequest = rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE > 0
                    writeCommand =
                        rxProperties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0

                    // Set the WRITE REQUEST type when the characteristic supports it.
                    // This will allow to send long write (also if the characteristic support it).
                    // In case there is no WRITE REQUEST property, this manager will divide texts
                    // longer then MTU-3 bytes into up to MTU-3 bytes chunks.
                    if (writeRequest) mRXCharacteristic!!.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                }
                if (!(mRXCharacteristic != null && mTXCharacteristic != null && (writeRequest || writeCommand))) {
                    Log.i(tag, "isRequiredServiceSuppor() == false")
                }
                return mRXCharacteristic != null && mTXCharacteristic != null && (writeRequest || writeCommand)
            }
        }

        override fun onDeviceDisconnected() {
            Log.i(tag, "onDeviceDisconnected()")
        }

        override fun onDataReceived(device: BluetoothDevice, data: Data) {
            Log.i(tag, "onDataReceived(${device.address}, ${data.value})")
        }
    }
}