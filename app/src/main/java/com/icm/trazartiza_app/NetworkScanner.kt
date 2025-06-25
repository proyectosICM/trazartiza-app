import android.net.wifi.WifiManager
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import java.net.InetAddress

// Clase que escanea la red local en busca de dispositivos activos (responden a ping)
class NetworkScanner(private val context: Context, private val onDeviceFound: (String) -> Unit) {

    // Inicia el escaneo de la red local
    fun startScan() {
        // Obtiene el WifiManager del sistema
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Obtiene la dirección IP local como entero y la convierte a formato String
        val ipInt = wifiManager.connectionInfo.ipAddress
        val ipString = String.format(
            "%d.%d.%d.%d",
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )

        // Obtiene la subred (por ejemplo: 192.168.0)
        val subnet = ipString.substringBeforeLast(".")

        // Intenta hacer ping a todas las IPs del 1 al 254 en la subred
        for (i in 1..254) {
            val host = "$subnet.$i"
            // Lanza una tarea asíncrona por cada IP
            PingTask(host, onDeviceFound).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    // Tarea asíncrona que intenta hacer ping a una IP
    private class PingTask(private val ip: String, val onFound: (String) -> Unit) : AsyncTask<Void, Void, Boolean>() {
        // Intenta hacer ping a la dirección IP con un timeout de 200 ms
        override fun doInBackground(vararg params: Void?): Boolean {
            return try {
                val address = InetAddress.getByName(ip)
                address.isReachable(200)
            } catch (e: Exception) {
                false
            }
        }

        // Si el ping tuvo éxito, se ejecuta el callback
        override fun onPostExecute(result: Boolean) {
            if (result) {
                Log.d("NetworkScanner", "Dispositivo activo: $ip")
                onFound(ip)
            }
        }
    }
}
