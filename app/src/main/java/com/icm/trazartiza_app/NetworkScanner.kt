import android.net.wifi.WifiManager
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import java.net.InetAddress

class NetworkScanner(private val context: Context, private val onDeviceFound: (String) -> Unit) {

    fun startScan() {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wifiManager.connectionInfo.ipAddress
        val ipString = String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )

        val subnet = ipString.substringBeforeLast(".")

        for (i in 1..254) {
            val host = "$subnet.$i"
            PingTask(host, onDeviceFound).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private class PingTask(private val ip: String, val onFound: (String) -> Unit) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            return try {
                val address = InetAddress.getByName(ip)
                address.isReachable(200)
            } catch (e: Exception) {
                false
            }
        }

        override fun onPostExecute(result: Boolean) {
            if (result) {
                Log.d("NetworkScanner", "Dispositivo activo: $ip")
                onFound(ip)
            }
        }
    }
}
