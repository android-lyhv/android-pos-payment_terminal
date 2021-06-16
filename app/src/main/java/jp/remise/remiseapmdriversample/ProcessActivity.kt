package jp.remise.remiseapmdriversample

import android.annotation.SuppressLint
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.starmicronics.starioextension.ICommandBuilder
import com.starmicronics.starioextension.StarIoExt
import java.io.IOException
import java.util.Calendar
import java.util.LinkedHashMap
import jp.remise.remiseapmdriver.ApmDriver
import jp.remise.remiseapmdriver.ApmDriverClientListener
import jp.remise.remiseapmdriver.Connection.TerminalConnectionException
import jp.remise.remiseapmdriver.Parameter.JOBType
import jp.remise.remiseapmdriver.Parameter.PaymentParameter
import jp.remise.remiseapmdriver.Parameter.PaymentType
import jp.remise.remiseapmdriver.Result.APPOperationCheckResult
import jp.remise.remiseapmdriver.Result.CallTerminalResult
import jp.remise.remiseapmdriver.Result.CardResult
import jp.remise.remiseapmdriver.Result.CardTranCheckResult
import jp.remise.remiseapmdriver.Result.EMoneyTranCheckResult
import jp.remise.remiseapmdriver.Result.QRResult
import jp.remise.remiseapmdriver.Result.QRTranCheckResult
import jp.remise.remiseapmdriver.setting.ConnectionSettingException
import jp.remise.remiseapmdriver.setting.IConnectionSetting
import jp.remise.remiseapmdriver.setting.SerialConnectionRS232Setting
import jp.remise.remiseapmdriver.setting.SerialConnectionUSBSetting
import jp.remise.remiseapmdriver.setting.TcpConnectionSetting
import jp.remise.remiseapmdriversample.databinding.ActivityProcessBinding

class ProcessActivity : AppCompatActivity(), View.OnClickListener {
    /**
     * 通信用SDK
     */
    private var apmDriver: ApmDriver? = null

    /**
     * 処理中結果
     */
    private var processingLogTextView: TextView? = null

    /**
     * 最終処理結果
     */
    private var completeLogTextView: TextView? = null

    /**
     * 戻るボタン
     */
    private var backButton: Button? = null

    /**
     * 中断ボタン
     */
    private var cancelButton: Button? = null

    /**
     * 決済処理方法
     */
    enum class StatusProcess {
        NONE,
        PROCESSING,
        SUCCESS
    }

    private lateinit var printerSettings: PrinterSettings
    private lateinit var binding: ActivityProcessBinding
    var paymentType: PaymentType? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProcessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初期処理
        init()
        val connectionSetting = createConnectionSetting()

        // パラメータ作成
        val parameter: PaymentParameter?
        parameter = createPaymentParameter()

        // 処理実行
        exec(connectionSetting, parameter)
        printerSettings = intent.getParcelableExtra("PRINTER_SETTINGS")
        showPaymentStatus(StatusProcess.PROCESSING)
        showPrintStatus(StatusProcess.NONE)
    }

    /**
     * 初期化処理
     */
    private fun init() {

        // ナビゲーションバーを隠す
        navigationHide()

        // Avtivity初期化
        findViewById<View>(R.id.backButton).setOnClickListener(this)
        findViewById<View>(R.id.cancelButton).setOnClickListener(this)
        processingLogTextView = findViewById(R.id.processingLogTextView)
        processingLogTextView!!.setTextIsSelectable(true)
        completeLogTextView = findViewById(R.id.completeLogTextView)
        completeLogTextView!!.setTextIsSelectable(true)
        backButton = findViewById(R.id.backButton)
        cancelButton = findViewById(R.id.cancelButton)

        // ボタン制御
        backButton!!.isEnabled = false
        cancelButton!!.isEnabled = true
    }

    /**
     * 端末接続情報生成
     */
    private fun createConnectionSetting(): IConnectionSetting? {

        // USB接続
        val usbManager: UsbManager
        // USBデバイス
        val usbDevice: UsbDevice
        // USBシリアルドライバー
        val usbSerialDriver: UsbSerialDriver
        // USBレシーバ
        val usbReceiver: UsbReceiver
        val intent = intent
        if (intent != null) {
            if (intent.getStringExtra("CONNECTIONTYPE") != null) {
                when (intent.getStringExtra("CONNECTIONTYPE")) {
                    "RS232" -> {
                        // RS232接続時の対象デバイス取得
                        usbManager = getSystemService(USB_SERVICE) as UsbManager
                        usbReceiver = UsbReceiver(this)
                        usbSerialDriver =
                            usbReceiver.getUsbSerialDriver(Globals.DeviceVendorId.SALO01_RS232.id)
                        val settingRS232 = SerialConnectionRS232Setting()
                        settingRS232.usbManager = usbManager
                        settingRS232.usbSerialDriver = usbSerialDriver
                        return settingRS232
                    }

                    "USB" -> {
                        // USB接続時の対象デバイス取得
                        usbManager = getSystemService(USB_SERVICE) as UsbManager
                        usbReceiver = UsbReceiver(this)
                        usbDevice = usbReceiver.getUsbDevice(Globals.DeviceVendorId.SALO01_USB.id)
                        val settingUSB = SerialConnectionUSBSetting()
                        settingUSB.usbManager = usbManager
                        settingUSB.usbDevice = usbDevice
                        return settingUSB
                    }

                    "TCPIP" -> {
                        val settingTCPIP = TcpConnectionSetting()
                        if (intent.getStringExtra("IP") != null) {
                            settingTCPIP.ip = intent.getStringExtra("IP")
                        }
                        settingTCPIP.port = 20000
                        return settingTCPIP
                    }
                }
            }
        }
        return null
    }

    /**
     * パラメータ生成
     */
    private fun createPaymentParameter(): PaymentParameter {
        val parameter = PaymentParameter()
        val intent = intent
        if (intent != null) {
            if (intent.getStringExtra("PAYMENTTYPE") != null) {
                when (intent.getStringExtra("PAYMENTTYPE")) {
                    "Card" -> parameter.paymentType = PaymentType.Card
                    "CardTranCheck" -> parameter.paymentType = PaymentType.CardTranCheck
                    "CardStop" -> parameter.paymentType = PaymentType.CardStop
                    "QR" -> parameter.paymentType = PaymentType.QR
                    "QRTranCheck" -> parameter.paymentType = PaymentType.QRTranCheck
                    "QRStop" -> parameter.paymentType = PaymentType.QRStop
                    "EMoney" -> parameter.paymentType = PaymentType.EMoney
                    "EMoneyStop" -> parameter.paymentType = PaymentType.EMoneyStop
                    "EMoneyBalance" -> parameter.paymentType = PaymentType.EMoneyBalance
                    "EMoneyBalanceStop" -> parameter.paymentType = PaymentType.EMoneyBalanceStop

                    "EMoneyHistory" -> {
                    }

                    "EMoneyTranCheck" -> parameter.paymentType = PaymentType.EMoneyTranCheck
                    "PaymentSelect" -> parameter.paymentType = PaymentType.PaymentSelect
                    "PaymentSelectStop" -> parameter.paymentType = PaymentType.PaymentSelectStop
                    "TerminalStatusCheck" -> parameter.paymentType = PaymentType.TerminalStatusCheck
                    "CardAPPInstallCheck" -> parameter.paymentType = PaymentType.CardAPPInstallCheck
                    "QRAPPInstallCheck" -> parameter.paymentType = PaymentType.QRAPPInstallCheck
                    "EMoneyAPPInstallCheck" -> parameter.paymentType =
                        PaymentType.EMoneyAPPInstallCheck
                    "APPOperationCheck" -> parameter.paymentType = PaymentType.APPOperationCheck
                }
            }
            if (!checkIntentNullOrEmpty(intent, "ORDERID")) {
                parameter.orderId = intent.getStringExtra("ORDERID")
            }
            if (!checkIntentNullOrEmpty(intent, "JOBTYPE")) {
                when (intent.getStringExtra("JOBTYPE")) {
                    "CAPTURE" -> parameter.job = JOBType.CAPTURE
                    "VOID" -> parameter.job = JOBType.VOID
                    "RETURN" -> parameter.job = JOBType.RETURN
                    "CHECK" -> parameter.job = JOBType.CHECK
                }
            }
            if (!checkIntentNullOrEmpty(intent, "AMOUNT")) {
                try {
                    parameter.amount = intent.getStringExtra("AMOUNT").toInt()
                } catch (e: NumberFormatException) {
                    // セットしない
                }
            }
            if (!checkIntentNullOrEmpty(intent, "MACHINECODE")) {
                parameter.machineCode = intent.getStringExtra("MACHINECODE")
            }
            if (!checkIntentNullOrEmpty(intent, "TRANID")) {
                parameter.tranID = intent.getStringExtra("TRANID")
            }
            if (!checkIntentNullOrEmpty(intent, "CANTRANID")) {
                parameter.canTranID = intent.getStringExtra("CANTRANID")
            }
            if (!checkIntentNullOrEmpty(intent, "JOBID")) {
                parameter.jobid = intent.getStringExtra("JOBID")
            }
            if (!checkIntentNullOrEmpty(intent, "UID")) {
                parameter.uid = intent.getStringExtra("UID")
            }
            if (!checkIntentNullOrEmpty(intent, "OFFLINE")) {
                when (intent.getStringExtra("OFFLINE")) {
                    "OFF" -> parameter.offlineMode = false
                    "ON" -> parameter.offlineMode = true
                }
            }
        }
        return parameter
    }

    /**
     * 端末連携処理
     */
    private fun exec(connectionSetting: IConnectionSetting?, parameter: PaymentParameter?) {
        paymentType = parameter!!.paymentType

        // インスタンス生成
        try {
            apmDriver = ApmDriver(connectionSetting, apmDriverClientListener)
            apmDriver!!.configLoad(this.classLoader.getResourceAsStream("rms_config.property"))
        } catch (e: ConnectionSettingException) {
            // 設定
            showLogException(e)
            apmDriver = null
            backButton!!.isEnabled = true
            cancelButton!!.isEnabled = false
            return
        } catch (e: TerminalConnectionException) {
            // 接続に失敗
            showLogException(e)
            apmDriver = null
            backButton!!.isEnabled = true
            cancelButton!!.isEnabled = false
            return
        } catch (e: IOException) {
            showLogException(e)
            apmDriver = null
            backButton!!.isEnabled = true
            cancelButton!!.isEnabled = false
            return
        } catch (e: Exception) {
            showLogException(e)
            apmDriver = null
            backButton!!.isEnabled = true
            cancelButton!!.isEnabled = false
            return
        }

        // 連携処理開始
        object : Thread() {
            override fun run() {
                try {
                    runOnUiThread {
                        processingLogTextView!!.text = ""
                        completeLogTextView!!.text = ""
                    }
                    val result = apmDriver!!.CallTerminalAsync(parameter)
                    val resultDatas = createLog(result)
                    showLogComplete(resultDatas)
                } catch (e: Exception) {
                    showLogException(e)
                    return
                } finally {
                    apmDriver = null
                    runOnUiThread {
                        backButton!!.isEnabled = true
                        cancelButton!!.isEnabled = false
                    }
                }
            }
        }.start()
    }

    /**
     * ボタンクリック
     *
     * @param view ビュー
     */
    override fun onClick(view: View) {
        // 中断ボタン
        if (view === findViewById<View>(R.id.cancelButton)) {
            object : Thread() {
                override fun run() {
                    try {
                        runOnUiThread { cancelButton!!.isEnabled = false }
                        apmDriver!!.Cancel()
                    } catch (e: Exception) {
                        showLogException(e)
                        return
                    }
                }
            }.start()
        } else if (view === findViewById<View>(R.id.backButton)) {
            finishAndRemoveTask()
        }
    }

    /**
     * イベント処理
     */
    var apmDriverClientListener: ApmDriverClientListener = object : ApmDriverClientListener {
        override fun CancelErrorResult(callTerminalResult: CallTerminalResult) {
            runOnUiThread { cancelButton!!.isEnabled = true }
            val resultDatas = createLog(callTerminalResult)
            showLogProcessing(resultDatas)
        }

        override fun TerminalProcessingStatus(callTerminalResult: CallTerminalResult) {
            val resultDatas = createLog(callTerminalResult)
            showLogProcessing(resultDatas)
        }
    }

    /**
     * 処理結果表示
     */
    private fun createLog(callTerminalResult: CallTerminalResult): LinkedHashMap<String, String> {
        val resultDatas = LinkedHashMap<String, String>()
        resultDatas["proccessResult"] =
            callTerminalResult.status.toString() + "(" + callTerminalResult.message + ")"
        resultDatas["proccessStatus"] = callTerminalResult.proccessStatus
        resultDatas["appStatus"] = callTerminalResult.appStatus
        resultDatas["errType"] = callTerminalResult.errType
        resultDatas["appErrcode"] = callTerminalResult.appErrcode
        resultDatas["appMessage"] = callTerminalResult.appMessage
        resultDatas["appVersion"] = callTerminalResult.appVersion
        resultDatas["method"] = callTerminalResult.method
        resultDatas["UID"] = callTerminalResult.uid
        if (paymentType == PaymentType.Card || paymentType == PaymentType.PaymentSelect && callTerminalResult.method == "01") {
            val cardResult = CardResult(callTerminalResult.resultData)
            resultDatas["cardType"] = cardResult.cardType
            resultDatas["orderId"] = cardResult.orderId
            resultDatas["tranId"] = cardResult.tranId
            resultDatas["card"] = cardResult.card
            resultDatas["expire"] = cardResult.expire
            resultDatas["refApproved"] = cardResult.refApproved
            resultDatas["refForwarded"] = cardResult.refForwarded
            resultDatas["refCardbrand"] = cardResult.refCardbrand
            resultDatas["arpcInfo"] = cardResult.arpcInfo
            resultDatas["errCode"] = cardResult.errCode
            resultDatas["errInfo"] = cardResult.errInfo
            resultDatas["errLevel"] = cardResult.errLevel
            resultDatas["result"] = cardResult.result
            resultDatas["refGatewayNo"] = cardResult.refGatewayNo
            resultDatas["datetime"] = cardResult.datetime
            resultDatas["memberId"] = cardResult.memberId
            resultDatas["jobId"] = cardResult.jobId
            resultDatas["payquickId"] = cardResult.payquickId
            resultDatas["AID"] = cardResult.aid
            resultDatas["appLabel"] = cardResult.appLabel
            resultDatas["cardSequenceNo"] = cardResult.cardSequenceNo
            resultDatas["ATC"] = cardResult.atc
            resultDatas["ARC"] = cardResult.arc
            resultDatas["spid"] = cardResult.spid
            resultDatas["TC"] = cardResult.tc
            resultDatas["pinErrInfo"] = cardResult.pinErrInfo
        } else if (paymentType == PaymentType.CardTranCheck) {
            val cardTranCheckResult = CardTranCheckResult(callTerminalResult.resultData)
            resultDatas["請求番号"] = cardTranCheckResult.orderId
            resultDatas["トランザクションID"] = cardTranCheckResult.tranId
            resultDatas["カード番号"] = cardTranCheckResult.card
            resultDatas["有効期限"] = cardTranCheckResult.expire
            resultDatas["承認番号"] = cardTranCheckResult.refApproved
            resultDatas["仕向け先コード"] = cardTranCheckResult.refForwarded
            resultDatas["カードブランド"] = cardTranCheckResult.refCardbrand
            resultDatas["エラーコード"] = cardTranCheckResult.errCode
            resultDatas["エラー詳細コード"] = cardTranCheckResult.errInfo
            resultDatas["エラーレベル"] = cardTranCheckResult.errLevel
            resultDatas["結果コード"] = cardTranCheckResult.result
            resultDatas["コミットフラグ"] = cardTranCheckResult.tranFlg
            resultDatas["ゲートウェイ番号"] = cardTranCheckResult.refGatewayNo
            resultDatas["処理日時"] = cardTranCheckResult.datetime
            resultDatas["メンバーID"] = cardTranCheckResult.memberId
            resultDatas["メンバー有効フラグ"] = cardTranCheckResult.yFlg
            resultDatas["AID"] = cardTranCheckResult.aid
            resultDatas["アプリケーションラベル"] = cardTranCheckResult.appLabel
            resultDatas["カードシーケンス番号"] = cardTranCheckResult.cardSequenceNo
            resultDatas["ジョブID"] = cardTranCheckResult.jobId
            resultDatas["ATC"] = cardTranCheckResult.atc
            resultDatas["ARC"] = cardTranCheckResult.arc
            resultDatas["端末識別番号"] = cardTranCheckResult.spid
            resultDatas["銀聯送信日時"] = cardTranCheckResult.cupSendDate
            resultDatas["銀聯処理通番"] = cardTranCheckResult.cupSequence
            resultDatas["銀聯加盟店会社コード"] = cardTranCheckResult.cupMarchantId
            resultDatas["TC"] = cardTranCheckResult.tc
        } else if (paymentType == PaymentType.QR || paymentType == PaymentType.PaymentSelect && callTerminalResult.method == "02") {
            val QRResult = QRResult(callTerminalResult.resultData)
            resultDatas["ジョブID"] = QRResult.jobId
            resultDatas["請求番号"] = QRResult.orderId
            resultDatas["処理区分"] = QRResult.job
            resultDatas["結果コード"] = QRResult.rCode
            resultDatas["エラー詳細コード"] = QRResult.errInfo
            resultDatas["取引金額"] = QRResult.cny
            resultDatas["ゲートウェイ番号"] = QRResult.gatewayno
            resultDatas["処理日時"] = QRResult.cenDate
            resultDatas["取引ステータス"] = QRResult.transStatus
            resultDatas["支払先コード"] = QRResult.payee
            resultDatas["QRコード名称"] = QRResult.payName
        } else if (paymentType == PaymentType.QRTranCheck) {
            val QRTranCheckResult = QRTranCheckResult(callTerminalResult.resultData)
            resultDatas["ジョブID"] = QRTranCheckResult.jobId
            resultDatas["請求番号"] = QRTranCheckResult.orderId
            resultDatas["結果コード"] = QRTranCheckResult.rCode
            resultDatas["取引金額"] = QRTranCheckResult.rTotal
            resultDatas["支払先コード"] = QRTranCheckResult.rPayee
            resultDatas["収納フラグ"] = QRTranCheckResult.recFlg
            resultDatas["収納日時"] = QRTranCheckResult.recDate
            resultDatas["確定フラグ"] = QRTranCheckResult.decisionFlg
            resultDatas["確定日時"] = QRTranCheckResult.decisionDate
            resultDatas["キャンセルフラグ"] = QRTranCheckResult.canFlg
            resultDatas["キャンセル日時"] = QRTranCheckResult.canDate
            resultDatas["取引ステータス"] = QRTranCheckResult.transStatus
        } else if (paymentType == PaymentType.EMoneyTranCheck) {
            val EMoneyTranCheckResult = EMoneyTranCheckResult(callTerminalResult.resultData)
            resultDatas["ジョブID"] = EMoneyTranCheckResult.jobId
            resultDatas["請求番号"] = EMoneyTranCheckResult.orderId
            resultDatas["結果コード"] = EMoneyTranCheckResult.rCode
            resultDatas["取引金額"] = EMoneyTranCheckResult.rTotal
            resultDatas["収納フラグ"] = EMoneyTranCheckResult.recFlg
            resultDatas["収納日時"] = EMoneyTranCheckResult.recDate
            resultDatas["確定フラグ"] = EMoneyTranCheckResult.decisionFlg
            resultDatas["確定日時"] = EMoneyTranCheckResult.decisionDate
            resultDatas["電子マネーアプリ処理結果"] = EMoneyTranCheckResult.appResult
        } else if (paymentType == PaymentType.EMoney || paymentType == PaymentType.PaymentSelect && callTerminalResult.method == "03") {
            // 無し
        } else if (paymentType == PaymentType.EMoneyTranCheck) {
            // 未実装
        } else if (paymentType == PaymentType.APPOperationCheck) {
            val APPOperationCheckResult = APPOperationCheckResult(callTerminalResult.resultData)
            resultDatas["カード決済アプリ動作状況"] = APPOperationCheckResult.card
            resultDatas["コード決済アプリ動作状況"] = APPOperationCheckResult.qr
            resultDatas["電子マネー決済アプリ動作状況"] = APPOperationCheckResult.emoney
            resultDatas["決済方法選択動作状況"] = APPOperationCheckResult.paymentSelect
        }
        return resultDatas
    }

    /**
     * 処理中ログ表示
     */
    private fun showLogProcessing(resultDatas: LinkedHashMap<String, String>) {
        runOnUiThread { //時刻表示するコードを追加
            val cal = Calendar.getInstance() //カレンダーを取得
            val iHour = cal[Calendar.HOUR] //時を取得
            val iMinute = cal[Calendar.MINUTE] //分を取得
            val iSecond = cal[Calendar.SECOND] //分を取得
            val iMilliSecond = cal[Calendar.MILLISECOND] //ミリ秒を取得
            val strTime = "$iHour:$iMinute:$iSecond.$iMilliSecond"

            //String log = processingLogTextView.getText().toString();
            val logSB = StringBuilder()
            logSB.append(strTime)
            logSB.append("\r\n")
            for ((key, value) in resultDatas) {
                logSB.append("> ")
                logSB.append(key)
                logSB.append("=")
                logSB.append(value)
                logSB.append("\r\n")
            }
            logSB.append("==================================================\n\n")
            Log.d("ProcessingLog", logSB.toString())
            //logSB.append(log);
            processingLogTextView!!.text = logSB.toString()
        }
    }

    /**
     * 最終結果表示
     */
    private fun showLogComplete(resultDatas: LinkedHashMap<String, String>) {
        runOnUiThread {
            for ((key, value) in resultDatas) {
                completeLogTextView!!.append(key)
                completeLogTextView!!.append("=")
                completeLogTextView!!.append(value)
                completeLogTextView!!.append("\r\n")
            }
            showPaymentStatus(StatusProcess.SUCCESS)
            printerOrder(resultDatas["orderId"] ?: "")
        }
    }

    /**
     * 処理結果表示（異常が発生した場合）
     */
    private fun showLogException(e: Exception) {
//        runOnUiThread {
//            val sw = StringWriter()
//            val pw = PrintWriter(sw)
//            e.printStackTrace(pw)
//            if (e is ConnectionSettingException) {
//                // 通信設定不備
//                val connectionSettingException = e
//                completeLogTextView!!.append("Communication setting error occurred")
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<ClassName>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(connectionSettingException.javaClass.name)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<Message>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(connectionSettingException.message)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<ResultStatus>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(connectionSettingException.status.toString())
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<StackTrace>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(sw.toString())
//            } else if (e is TerminalConnectionException) {
//                // 通信エラー
//                val terminalConnectionException = e
//                completeLogTextView!!.append("通信エラー発生")
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<ClassName>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(terminalConnectionException.javaClass.name)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<Message>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(terminalConnectionException.message)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<ResultStatus>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(terminalConnectionException.status.toString())
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<StackTrace>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(sw.toString())
//            } else if (e is InvalidParameterException) {
//                // リクエストパラメータ不正
//                val invalidParameterException = e
//                completeLogTextView!!.append("Request parameter error occurred")
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<ClassName>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(invalidParameterException.javaClass.name)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<Message>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(invalidParameterException.message)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<StackTrace>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(sw.toString())
//            } else if (e is HashCheckErrorException) {
//                // リクエストパラメータ不正
//                val hashCheckErrorException = e
//                completeLogTextView!!.append("Request parameter error occurred")
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<ClassName>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(hashCheckErrorException.javaClass.name)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<Message>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(hashCheckErrorException.message)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<StackTrace>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(sw.toString())
//            } else if (e is UIDUnmatchErrorException) {
//                // リクエストパラメータ不正
//                val uidUnmatchErrorException = e
//                completeLogTextView!!.append("Request parameter error occurred")
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<ClassName>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(uidUnmatchErrorException.javaClass.name)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<Message>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(uidUnmatchErrorException.message)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<StackTrace>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(sw.toString())
//            } else {
//                // その他例外エラー
//                completeLogTextView!!.append("例外エラー発生")
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<ClassName>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(e.javaClass.name)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<Message>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(e.message)
//                completeLogTextView!!.append("\r\n\r\n")
//                completeLogTextView!!.append("<StackTrace>")
//                completeLogTextView!!.append("\r\n")
//                completeLogTextView!!.append(sw.toString())
//            }
//        }
    }

    /******************************************************
     * ナビゲーションバーを隠す
     */
    private fun navigationHide() {
        // 全画面表示
        val decor = this.window.decorView
        decor.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    /**
     * Intentに対象の項目が存在するかチェックする。
     */
    private fun checkIntentNullOrEmpty(intent: Intent, name: String): Boolean {
        if (intent.getStringExtra(name) != null) {
            if (!intent.getStringExtra(name).isEmpty()) {
                return false
            }
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    private fun printerOrder(orderId: String) {
        showPrintStatus(StatusProcess.PROCESSING)
        Communication.sendCommands(
            this,
            createSampleCommand(orderId),
            printerSettings.portName,
            printerSettings.portSettings,
            10000,
            30000,
            this
        ) {
            if (it.result == Communication.Result.Success) {
                showPrintStatus(StatusProcess.SUCCESS)
            }
        }
    }

    private fun createSampleCommand(orderId: String): ByteArray {
        val emulation = ModelCapability.getEmulation(printerSettings.modelIndex)
        val builder = StarIoExt.createCommandBuilder(emulation)
        builder.beginDocument()
        builder.appendAlignment(ICommandBuilder.AlignmentPosition.Center)
        builder.append("ORDER\n".toByteArray())
        builder.append("$orderId\n".toByteArray())
        builder.appendAlignment(ICommandBuilder.AlignmentPosition.Left)
        builder.append("--------------------------------\n".toByteArray())
        builder.append("Sansevieria                ¥10\n".toByteArray())
        builder.append("x4\n".toByteArray())
        builder.append("Gerbera                     ¥5\n".toByteArray())
        builder.append("x2\n".toByteArray())
        builder.append("--------------------------------\n".toByteArray())
        builder.append("Total                      ¥50\n".toByteArray())
        builder.append("--------------------------------\n".toByteArray())
        builder.appendCutPaper(ICommandBuilder.CutPaperAction.PartialCutWithFeed)
        builder.endDocument()
        return builder.commands
    }

    private fun showPaymentStatus(statusProcess: StatusProcess) {
        when (statusProcess) {
            StatusProcess.NONE -> {
                binding.stepView.paymentLoading.isGone = true
                binding.stepView.paymentSuccess.isGone = true
                binding.stepView.paymentNone.isVisible = true
                binding.stepView.tvPaymentInfo.alpha = 0.5f
            }
            StatusProcess.PROCESSING -> {
                binding.stepView.paymentLoading.isVisible = true
                binding.stepView.paymentLoading.startRippleAnimation()
                binding.stepView.paymentSuccess.isGone = true
                binding.stepView.paymentNone.isGone = true
                binding.stepView.tvPaymentInfo.alpha = 1f
            }
            StatusProcess.SUCCESS -> {
                binding.stepView.paymentLoading.isGone = true
                binding.stepView.paymentSuccess.isVisible = true
                binding.stepView.paymentNone.isGone = true
                binding.stepView.tvPaymentInfo.alpha = 0.5f
            }
        }
    }

    private fun showPrintStatus(statusProcess: StatusProcess) {
        when (statusProcess) {
            StatusProcess.NONE -> {
                binding.stepView.printLoading.isGone = true
                binding.stepView.printSuccess.isGone = true
                binding.stepView.printNone.isVisible = true
                binding.stepView.tvPrint.alpha = 0.5f
            }
            StatusProcess.PROCESSING -> {
                binding.stepView.printLoading.isVisible = true
                binding.stepView.printLoading.startRippleAnimation()
                binding.stepView.printSuccess.isGone = true
                binding.stepView.printSuccess.isGone = true
                binding.stepView.tvPrint.alpha = 1f

            }
            StatusProcess.SUCCESS -> {
                binding.stepView.printLoading.isGone = true
                binding.stepView.printSuccess.isVisible = true
                binding.stepView.printNone.isGone = true
                binding.stepView.tvPrint.alpha = 0.5f
            }
        }
    }
}