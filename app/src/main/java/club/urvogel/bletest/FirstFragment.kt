package club.urvogel.bletest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.anyPermanentlyDenied
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.google.android.material.snackbar.Snackbar
import no.nordicsemi.android.support.v18.scanner.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private val request by lazy {
        permissionsBuilder(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH
        ).build()
    }
    lateinit var listView: ListView
    lateinit var arrayAdapter: ArrayAdapter<Device>

    var mScanner: BluetoothLeScannerCompat = BluetoothLeScannerCompat.getScanner()
    val mList = DeviceList()

    private val mScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
        }

        override fun onScanFailed(intErrorCode: Int) {
            super.onScanFailed(intErrorCode)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            mList.update(results)
            arrayAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_first, container, false)
        listView = view.findViewById(R.id.list_view)
        arrayAdapter = ArrayAdapter(
            view.context,
            android.R.layout.simple_list_item_1,
            mList.list
        )
        listView.adapter = arrayAdapter
        listView.setOnItemClickListener(OnItemClickListener { _, _, position, _ ->
            val device = mList.list[position]
            toggleConnect(view, device)
        })
        return view
    }

    private fun toggleConnect(view: View, device: Device) {
        Handler(Looper.getMainLooper()).postDelayed({
            Snackbar.make(view, "Connecting... ${device.bluetoothDevice.address}", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }, 100)
        val req = device.toggleConnect(context as Context, device.isBonded, getBluetoothAdapter().getBondedDevices())
        if (req != null) {
            req.done {
                arrayAdapter.notifyDataSetChanged()
                showSnackbar(if (device.uartManager?.isConnected == true) "Connected" else "Disconnected")
                Handler(Looper.getMainLooper()).postDelayed({
                    toggleConnect(view, device)
                }, 500)
            }.fail { bluetoothDevice, status ->
                arrayAdapter.notifyDataSetChanged()
                showSnackbar("Failed ${bluetoothDevice.address} status:${status}")

                Handler(Looper.getMainLooper()).postDelayed({
                    toggleConnect(view, device)
                }, 500)
            }.enqueue()
        } else {
            showSnackbar("Failed ${device.bluetoothDevice.address} bonding removed!!")
        }
    }

    fun showSnackbar(message: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            Snackbar.make(view as View, message, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }, 100)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val v = view
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_scan -> {
                if (v != null) {
                    onScanning()
                    Snackbar.make(v, "Scan", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter {
        val manager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter =
            manager.adapter ?: throw RuntimeException("no adapter")
        if (!bluetoothAdapter.isEnabled) throw RuntimeException("no service")
        return bluetoothAdapter
    }

    fun onScanning() {
        request.removeAllListeners()
        request.addListener {
            when {
                it.anyPermanentlyDenied() -> Toast.makeText(
                    requireContext(),
                    "NO PERMISSON",
                    Toast.LENGTH_SHORT
                ).show()
                it.allGranted() -> doScan()
            }
        }
        request.send()
    }

    fun doScan() {
        try {
            arrayAdapter.clear()
            mList.addDevices(getBluetoothAdapter().getBondedDevices())
            arrayAdapter.notifyDataSetChanged()
            val settings = ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000)
                .setUseHardwareBatchingIfSupported(false).build()
            mScanner.startScan(ArrayList<ScanFilter>(), settings, mScanCallback)
            Handler(Looper.getMainLooper()).postDelayed({
                mScanner.stopScan(mScanCallback)
                showSnackbar("Scan finished")
            }, 3000)

        } catch (ex: Exception) {
            ex.printStackTrace()
            return
        }
    }
}