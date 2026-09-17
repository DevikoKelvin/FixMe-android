package com.erela.fixme.helpers

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import com.erela.fixme.R
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.erela.fixme.objects.laundry.LaundrySlipRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * One paired ESC/POS printer, named for a person rather than for its MAC address.
 *
 * `address` is what we connect to; `name` is what the operator recognises on a shelf of identical
 * black boxes.
 */
data class PairedPrinter(val name: String, val address: String)


/**
 * The counter's Bluetooth slip printer — GA, 17 September 2026.
 *
 * THE SAME FILE AS THE COMPOSE APP's, package line apart. It has no UI in it at all: the server
 * hands down lines already measured to the paper, and this opens a socket and writes them. Two
 * copies of a socket are cheaper than one shared module between two apps that share nothing else.
 *
 * WHY THERE IS NO LAYOUT IN HERE. The server hands down lines already measured to the paper's
 * width, because the slip's wording and columns belong next to the web slip they have to match. A
 * second layout in Kotlin is two slips for one batch, drifting apart the first time either is
 * edited. This class owns the socket and nothing else.
 *
 * SPP, NOT BLE. Every portable ESC/POS printer of this class speaks the Serial Port Profile over
 * RFCOMM on the well-known UUID below; the low-energy stack is for sensors and would need a
 * GATT service this hardware does not publish.
 *
 * PAIRED DEVICES ONLY, DELIBERATELY. Discovery needs location permission on every Android version
 * we ship to, and pairing a printer is a thing somebody does once in the system settings. Asking
 * for location to find a printer sitting on the same desk is a permission prompt nobody should
 * have to explain.
 *
 * ponytail: opens a socket per print and closes it. A printer that is about to receive thirty
 * slips in a row would want the connection held open - keep one here and close it on screen exit
 * the day that happens.
 */
object ThermalPrinter {

    /** The Serial Port Profile. Fixed by the spec, not by the printer. */
    private val SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    /** Initialise: clears whatever the last job left in the printer's state. */
    private val INIT = byteArrayOf(0x1B, 0x40)

    /** Four blank lines, so the last line clears the tear bar. */
    private val FEED = byteArrayOf(0x1B, 0x64, 0x04)

    /** Partial cut. Ignored by the many portable heads that have no cutter. */
    private val CUT = byteArrayOf(0x1D, 0x56, 0x01)

    /**
     * Whether the app may talk to a paired device at all.
     *
     * BLUETOOTH_CONNECT arrived in Android 12; below that the old `BLUETOOTH` permission is
     * install-time and always granted, so there is nothing to ask for.
     */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    /** The runtime permission to request, or null where the platform needs none. */
    fun permission(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT
        else null

    private fun adapter(context: Context): BluetoothAdapter? =
        context.getSystemService<BluetoothManager>()?.adapter

    fun isReady(context: Context): Boolean = adapter(context)?.isEnabled == true

    /**
     * Everything already paired with this phone.
     *
     * NOT FILTERED BY DEVICE CLASS. A printer should report itself as an imaging device and many
     * of these report themselves as "uncategorised" instead, so filtering on it hides exactly the
     * cheap hardware this is for. The operator picks from the list once.
     */
    @SuppressLint("MissingPermission")
    fun paired(context: Context): List<PairedPrinter> {
        if (!hasPermission(context)) return emptyList()

        return adapter(context)?.bondedDevices.orEmpty()
            .map { PairedPrinter(it.name ?: it.address, it.address) }
            .sortedBy { it.name.lowercase() }
    }

    /**
     * Send one slip. Returns null on success, or a sentence to show the operator.
     *
     * A SENTENCE, NOT AN EXCEPTION. Every failure here is something the person holding the phone
     * can act on - the printer is off, out of range, or was never paired - and each deserves
     * saying in those words rather than as an IOException nobody can act on.
     *
     * THE SENTENCES ARE RESOURCES, so they follow the app's language like everything else the
     * operator reads. They were Indonesian literals in here, which is fine until an English
     * session prints and gets one line of Indonesian in the middle of its own toasts.
     */
    @SuppressLint("MissingPermission")
    suspend fun print(context: Context, address: String, rows: List<LaundrySlipRow>): String? =
        withContext(Dispatchers.IO) {
            if (!hasPermission(context)) {
                return@withContext context.getString(R.string.printer_no_permission)
            }

            val adapter = adapter(context)
                ?: return@withContext context.getString(R.string.printer_no_bluetooth)

            if (!adapter.isEnabled) {
                return@withContext context.getString(R.string.printer_bluetooth_off)
            }

            val device: BluetoothDevice = runCatching { adapter.getRemoteDevice(address) }
                .getOrNull() ?: return@withContext context.getString(R.string.printer_unknown)

            var socket: BluetoothSocket? = null

            try {
                socket = device.createRfcommSocketToServiceRecord(SPP)

                // NO `cancelDiscovery()` HERE, AND THAT IS THE POINT [GA, 18 Sep 2026]. It was
                // called to free the radio in case another app had a scan running - and it is
                // annotated `@RequiresPermission(BLUETOOTH_SCAN)`, which this app deliberately
                // never asks for. So on Android 12 and up it threw SecurityException the instant
                // a printer was picked, the catch below reported it as "Bluetooth permission has
                // not been granted", and the operator was told to grant a permission they had
                // already granted.
                //
                // Nothing here scans. Asking for BLUETOOTH_SCAN to tidy up after somebody else's
                // scan would be a location-class permission bought to work around another app's
                // behaviour.
                socket.connect()

                val out = socket.outputStream

                out.write(INIT)

                rows.forEach { row ->
                    if (row.isQr) {
                        out.write(qrCommands(row.value))
                    } else {
                        // ISO-8859-1 maps one character to one byte, which is what the printer's
                        // default code page expects. The server already replaced everything
                        // outside printable ASCII, so nothing here becomes two bytes and shifts a
                        // column.
                        out.write((row.value + "\n").toByteArray(Charsets.ISO_8859_1))
                    }
                }

                out.write(FEED)
                out.write(CUT)
                out.flush()

                null
            } catch (e: IOException) {
                context.getString(R.string.printer_connect_failed)
            } catch (e: SecurityException) {
                context.getString(R.string.printer_no_permission)
            } finally {
                runCatching { socket?.close() }
            }
        }

    /**
     * One QR code, drawn by the printer rather than by us.
     *
     * `GS ( k` IS THE WHOLE REASON THIS IS CHEAP. ESC/POS printers carry their own QR renderer, so
     * a code costs a few dozen bytes and comes out sharper than any bitmap we could rasterise and
     * push down a 9600-baud-feeling serial link.
     *
     * FOUR COMMANDS, IN THIS ORDER, and the order is the spec's: choose the model, set the module
     * size, set the error correction, store the payload, print what was stored.
     *
     * MODULE SIZE 5 AND ECC M. The payload here is either a 19-character transaction number or a
     * signed URL of about 120; both fit an 80mm head at size 5 with room to spare, and M is what
     * the paper slip uses - clean paper scanned once, unlike a patch laundered for years.
     */
    private fun qrCommands(payload: String): ByteArray {
        val data = payload.toByteArray(Charsets.ISO_8859_1)

        // The store command's length covers the payload plus its own three-byte header.
        val length = data.size + 3

        return byteArrayOf(0x1B, 0x61, 0x01) +                                  // centre
            byteArrayOf(0x1D, 0x28, 0x6B, 0x04, 0x00, 0x31, 0x41, 0x32, 0x00) + // model 2
            byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x43, 0x05) +       // module size 5
            byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x45, 0x31) +       // ECC M
            byteArrayOf(
                0x1D, 0x28, 0x6B,
                (length and 0xFF).toByte(), (length shr 8 and 0xFF).toByte(),
                0x31, 0x50, 0x30
            ) + data +                                                          // store
            byteArrayOf(0x1D, 0x28, 0x6B, 0x03, 0x00, 0x31, 0x51, 0x30) +       // print
            byteArrayOf(0x0A) +
            byteArrayOf(0x1B, 0x61, 0x00)                                       // back to left
    }
}
