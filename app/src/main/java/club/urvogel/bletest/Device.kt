package club.urvogel.bletest

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import no.nordicsemi.android.support.v18.scanner.ScanResult

class Device {
    var bluetoothDevice: BluetoothDevice
    var name: String? = null
    var rssi: Int? = null
    var isBonded = false
    var bluetoothManager: BluetoothManager? = null

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

    override fun toString(): String {
        return "bond:" + (if(isBonded) "t" else "f") + " addr:" + bluetoothDevice.address + " " + name
    }
}