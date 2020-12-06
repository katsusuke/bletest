package club.urvogel.bletest

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import no.nordicsemi.android.ble.Request
import no.nordicsemi.android.support.v18.scanner.ScanResult

class Device {
    val tag = "Device"
    var bluetoothDevice: BluetoothDevice
    var name: String? = null
    var rssi: Int? = null
    var isBonded = false
    var uartManager: UARTManager? = null

    constructor(scanResult: ScanResult) {
        bluetoothDevice = scanResult.device
        name = if (scanResult.scanRecord != null) scanResult.scanRecord!!.deviceName else null
        rssi = scanResult.rssi
        isBonded = false
    }

    constructor(device: BluetoothDevice) {
        this.bluetoothDevice = device
        name = device.name
        rssi = null
        isBonded = true
    }

    fun matches(scanResult: ScanResult): Boolean {
        return bluetoothDevice.address == scanResult.device.address
    }

    fun b2s(b: Boolean): String = if(b) "t" else "f"

    override fun toString(): String {
        val connected = uartManager?.isReady ?: false
        return "bond:" + b2s(isBonded) + " connected:" + b2s(connected) + " addr:" + bluetoothDevice.address + " " + name
    }

    fun toggleConnect(context: Context, previousBonded: Boolean, bondedDevices: Set<BluetoothDevice>): Request? {
        if (uartManager == null) {
            val n = if ( name == null) bluetoothDevice.address else name
            uartManager = UARTManager(context, n as String)
        }
        if (uartManager!!.isReady) {
            return uartManager!!.disconnect()
        } else {
            val req = uartManager!!.connect(bluetoothDevice).useAutoConnect(false)
            if (bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED) {
                if (!bondedDevices.contains(bluetoothDevice)) {
                    return null
                }
                req.timeout(3000)
            } else if(previousBonded) {
                return null
            }
            req.done {
                isBonded = bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED
            }.fail { _, status ->
                // status is no.nordicsemi.android.ble.callback.FailCallback
                Log.e(tag, "fail status:${status}")
                isBonded = bluetoothDevice.bondState == BluetoothDevice.BOND_BONDED
            }
            return req
        }
    }
}