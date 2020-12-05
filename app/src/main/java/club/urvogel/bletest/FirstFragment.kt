package club.urvogel.bletest

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import no.nordicsemi.android.support.v18.scanner.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
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
        return view
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
        try {
            arrayAdapter.clear()
            mList.addDevices(getBluetoothAdapter().getBondedDevices())
            arrayAdapter.notifyDataSetChanged()
            val settings = ScanSettings.Builder()
                .setLegacy(false)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000)
                .setUseHardwareBatchingIfSupported(false).build()
            val filters = ArrayList<ScanFilter>()
            mScanner.startScan(filters, settings, mScanCallback)
        } catch (ex: Exception) {
            ex.printStackTrace()
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            mScanner.stopScan(mScanCallback)
        }, 3000)
    }
}