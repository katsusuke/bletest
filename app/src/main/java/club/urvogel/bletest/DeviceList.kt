package club.urvogel.bletest

import android.bluetooth.BluetoothDevice
import no.nordicsemi.android.support.v18.scanner.ScanResult
import java.util.*

class DeviceList {
    val list: ArrayList<Device> = ArrayList<Device>()

    /**
     * Updates the list of not bonded devices.
     * @param results list of results from the scanner
     */
    fun update(results: List<ScanResult>) {
        for (result in results) {
            val device: Device? = findDevice(result)
            if (device == null) {
                list.add(Device(result))
            } else {
                device.name =
                    if (result.scanRecord != null) result.scanRecord!!.deviceName else null
                device.rssi = result.rssi
            }
        }
    }

    private fun findDevice(result: ScanResult): Device? {
        for (device in list) if (device.matches(result)) return device
        return null
    }

    fun findDevice(key: String?): Device? {
        for (result in list) {
            if (result.bluetoothDevice.getAddress().equals(key)) {
                return result
            }
        }
        return null
    }

    fun clear() {
        list.clear()
    }

    fun addDevices(bondedDevices: Set<BluetoothDevice>) {
        for (newDevice in bondedDevices) {
            addDevice(newDevice)
        }
    }

    fun addDevice(newDevice: BluetoothDevice) {
        val device: Device? = findDevice(newDevice.address)
        if (device == null) {
            list.add(Device(newDevice))
        }
    }
}