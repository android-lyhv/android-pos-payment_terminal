package jp.remise.remiseapmdriversample

object PrinterSettingConstant {
    const val LANGUAGE_ENGLISH = 0
    const val LANGUAGE_JAPANESE = 1
    const val LANGUAGE_FRENCH = 2
    const val LANGUAGE_PORTUGUESE = 3
    const val LANGUAGE_SPANISH = 4
    const val LANGUAGE_GERMAN = 5
    const val LANGUAGE_RUSSIAN = 6
    const val LANGUAGE_SIMPLIFIED_CHINESE = 7
    const val LANGUAGE_TRADITIONAL_CHINESE = 8
    const val LANGUAGE_CJK_UNIFIED_IDEOGRAPH = 9
    const val PAPER_SIZE_TWO_INCH = 384
    const val PAPER_SIZE_THREE_INCH = 576
    const val PAPER_SIZE_FOUR_INCH = 832
    const val PAPER_SIZE_ESCPOS_THREE_INCH = 512
    const val PAPER_SIZE_DOT_THREE_INCH = 210
    const val PAPER_SIZE_SK1_TWO_INCH = 432
    const val IF_TYPE_ETHERNET = "TCP:"
    const val IF_TYPE_BLUETOOTH = "BT:"
    const val IF_TYPE_USB = "USB:"
}
enum class PaymentConnectionType{
    USB, TCP_IP
}