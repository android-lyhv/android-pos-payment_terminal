package jp.remise.remiseapmdriversample

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class PrinterSettings(
    val modelIndex: Int,
    val portName: String,
    val portSettings: String,
    val macAddress: String,
    val modelName: String,
    val cashDrawerOpenActiveHigh: Boolean,
    val paperSize: Int
): Parcelable