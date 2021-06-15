package jp.remise.remiseapmdriversample

import com.starmicronics.stario.PortInfo

object PrinterExt

fun PortInfo.toPortName(): String {
    return if (this.portName.startsWith(PrinterSettingConstant.IF_TYPE_BLUETOOTH)) {
        return PrinterSettingConstant.IF_TYPE_BLUETOOTH + this.macAddress
    } else {
        this.portName
    }
}

fun PortInfo.toModelName(): String {
    return if (this.portName.startsWith(PrinterSettingConstant.IF_TYPE_BLUETOOTH)) {
        return this.portName.substring(PrinterSettingConstant.IF_TYPE_BLUETOOTH.length)
    } else {
        this.modelName
    }
}