package jp.remise.remiseapmdriversample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.starmicronics.stario.PortInfo
import com.starmicronics.stario.StarIOPort
import com.starmicronics.starioextension.ConnectionCallback
import com.starmicronics.starioextension.ICommandBuilder
import com.starmicronics.starioextension.StarIoExt
import com.starmicronics.starioextension.StarIoExtManager
import jp.remise.remiseapmdriversample.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.UUID

class MainActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var starIoExtManager: StarIoExtManager
    private lateinit var printerSettings: PrinterSettings
    private lateinit var binding: ActivityMainBinding

    // オフラインフラグ
    private var radioButtonOfflineOff: RadioButton? = null
    private var radioButtonOfflineOn: RadioButton? = null

    // 接続方法
    private var radioButtonConnectionRS232: RadioButton? = null
    private var radioButtonConnectionUSB: RadioButton? = null
    private var radioButtonConnectionTCPIP: RadioButton? = null

    // IPアドレス（TCP/IP通信時のみ使用）
    private var editTextIP: EditText? = null

    // 請求番号
    private var editTextOrderID: EditText? = null

    // 金額
    private var editTextAmount: EditText? = null

    // 精算機コード
    private var editTextMachineCode: EditText? = null

    // 画面全体
    private val linearLayoutMain: LinearLayout? = null

    // 支払方法/状態確認
    private var radioButtonMethodCard: RadioButton? = null
    private var radioButtonMethodCardTrancheck: RadioButton? = null
    private var radioButtonMethodCardStop: RadioButton? = null
    private var radioButtonMethodQR: RadioButton? = null
    private var radioButtonMethodQRTrancheck: RadioButton? = null
    private var radioButtonMethodQRStop: RadioButton? = null
    private var radioButtonMethodEMoney: RadioButton? = null
    private var radioButtonMethodEMoneyTrancheck: RadioButton? = null
    private var radioButtonMethodEMoneyStop: RadioButton? = null
    private var radioButtonMethodEMoneyBalance: RadioButton? = null
    private var radioButtonMethodEMoneyBalanceStop: RadioButton? = null
    private val radioButtonMethodEMoneyHistory: RadioButton? = null
    private val radioButtonMethodEMoneyHistoryStop: RadioButton? = null
    private var radioButtonMethodPaymentSelect: RadioButton? = null
    private var radioButtonMethodPaymentSelectStop: RadioButton? = null
    private var radioButtonMethodTerminalStatusCheck: RadioButton? = null
    private var radioButtonMethodInstCard: RadioButton? = null
    private var radioButtonMethodInstQR: RadioButton? = null
    private var radioButtonMethodInstEMoney: RadioButton? = null
    private var radioButtonMethodAPPOperationCheck: RadioButton? = null

    // 処理区分（カード決済のみ有効）
    private var radioButtonJobCAPTURE: RadioButton? = null
    private var radioButtonJobVOID: RadioButton? = null
    private var radioButtonJobRETURN: RadioButton? = null
    private var radioButtonJobCHECK: RadioButton? = null

    // トランザクションID
    private var editTextTranID: EditText? = null

    // 取消対象トランザクションID
    private var editTextCanTranID: EditText? = null

    // ジョブID
    private var editTextJobID: EditText? = null

    // UID
    private var editTextUID: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 全画面表示
        val decor = this.window.decorView
        decor.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        // クリックイベント：実行ボタン
        findViewById<View>(R.id.execButton).setOnClickListener(this)

        // クリックイベント：請求番号採番ボタン
        findViewById<View>(R.id.buttonOrderID).setOnClickListener(this)

        // クリックイベント：UID採番ボタン
        findViewById<View>(R.id.buttonUID).setOnClickListener(this)

        // クリックイベント：接続方法
        findViewById<View>(R.id.radioButtonConnectionRS232).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonConnectionUSB).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonConnectionTCPIP).setOnClickListener(this)

        // クリックイベント：画面全体
        findViewById<View>(R.id.LinearLayoutMain).setOnClickListener(this)

        // クリックイベント：支払方法/状態確認
        findViewById<View>(R.id.radioButtonMethodCard).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodCardTrancheck).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodCardStop).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodQR).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodQRTrancheck).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodQRStop).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodEMoney).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodEMoneyTrancheck).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodEMoneyStop).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodEMoneyBalance).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodEMoneyBalanceStop).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodTerminalStatusCheck).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodInstCard).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodInstQR).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodInstEMoney).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodPaymentSelect).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodPaymentSelectStop).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonMethodAPPOperationCheck).setOnClickListener(this)

        // クリックイベント：処理区分
        findViewById<View>(R.id.radioButtonJobCAPTURE).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonJobVOID).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonJobRETURN).setOnClickListener(this)
        findViewById<View>(R.id.radioButtonJobCHECK).setOnClickListener(this)

        // オフラインフラグ
        radioButtonOfflineOff = findViewById(R.id.radioButtonOfflineOff)
        radioButtonOfflineOn = findViewById(R.id.radioButtonOfflineOn)

        // 接続方法
        radioButtonConnectionRS232 = findViewById(R.id.radioButtonConnectionRS232)
        radioButtonConnectionUSB = findViewById(R.id.radioButtonConnectionUSB)
        radioButtonConnectionTCPIP = findViewById(R.id.radioButtonConnectionTCPIP)

        // IPアドレス（TCP/IP通信時のみ使用）
        editTextIP = findViewById(R.id.editTextIP)

        // 請求番号
        editTextOrderID = findViewById(R.id.editTextOrderID)
        val cl = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyyMMddHHmmssSS")
        val dateTime = sdf.format(cl.time)
        editTextOrderID!!.setText(dateTime)

        // 金額
        editTextAmount = findViewById(R.id.editTextAmount)

        // 精算機コード
        editTextMachineCode = findViewById(R.id.editTextMachineCode)

        // 支払方法/状態確認
        radioButtonMethodCard = findViewById(R.id.radioButtonMethodCard)
        radioButtonMethodCardTrancheck = findViewById(R.id.radioButtonMethodCardTrancheck)
        radioButtonMethodCardStop = findViewById(R.id.radioButtonMethodCardStop)
        radioButtonMethodQR = findViewById(R.id.radioButtonMethodQR)
        radioButtonMethodQRTrancheck = findViewById(R.id.radioButtonMethodQRTrancheck)
        radioButtonMethodQRStop = findViewById(R.id.radioButtonMethodQRStop)
        radioButtonMethodEMoney = findViewById(R.id.radioButtonMethodEMoney)
        radioButtonMethodEMoneyStop = findViewById(R.id.radioButtonMethodEMoneyStop)
        radioButtonMethodEMoneyBalance = findViewById(R.id.radioButtonMethodEMoneyBalance)
        radioButtonMethodEMoneyBalanceStop = findViewById(R.id.radioButtonMethodEMoneyBalanceStop)
        radioButtonMethodEMoneyTrancheck = findViewById(R.id.radioButtonMethodEMoneyTrancheck)
        radioButtonMethodPaymentSelect = findViewById(R.id.radioButtonMethodPaymentSelect)
        radioButtonMethodPaymentSelectStop = findViewById(R.id.radioButtonMethodPaymentSelectStop)
        radioButtonMethodTerminalStatusCheck =
            findViewById(R.id.radioButtonMethodTerminalStatusCheck)
        radioButtonMethodInstCard = findViewById(R.id.radioButtonMethodInstCard)
        radioButtonMethodInstQR = findViewById(R.id.radioButtonMethodInstQR)
        radioButtonMethodInstEMoney = findViewById(R.id.radioButtonMethodInstEMoney)
        radioButtonMethodAPPOperationCheck = findViewById(R.id.radioButtonMethodAPPOperationCheck)

        // 処理区分（カード決済のみ有効）
        radioButtonJobCAPTURE = findViewById(R.id.radioButtonJobCAPTURE)
        radioButtonJobVOID = findViewById(R.id.radioButtonJobVOID)
        radioButtonJobRETURN = findViewById(R.id.radioButtonJobRETURN)
        radioButtonJobCHECK = findViewById(R.id.radioButtonJobCHECK)

        // トランザクションID
        editTextTranID = findViewById(R.id.editTextTranID)

        // 取消対象トランザクションID
        editTextCanTranID = findViewById(R.id.editTextCanTranID)

        // ジョブID
        editTextJobID = findViewById(R.id.editTextJobID)

        // UID
        val uid = UUID.randomUUID()
        editTextUID = findViewById(R.id.editTextUID)
        editTextUID!!.setText(uid.toString())

        // 起動時のデバイス検知
        val usbReceiver: UsbReceiver
        usbReceiver = UsbReceiver(this)
        usbReceiver.getUsbSerialDriver(Globals.DeviceVendorId.SALO01_RS232.id)
        usbReceiver.getUsbDevice(Globals.DeviceVendorId.SALO01_USB.id)
        findViewById<View>(R.id.btn_payment).setOnClickListener { callExecProcess() }
        binding.btnPayment.alpha = 0.5f
        binding.btnPayment.isEnabled = false
        lifecycleScope.launch {
            val printersInfo =
                searchPrinter(this@MainActivity, PrinterSettingConstant.IF_TYPE_USB)
            if (printersInfo.isEmpty()) {
                return@launch
            }
            onCreatePrinterManager(printersInfo[0])
            connectPrinter()
        }
    }

    /**
     * ボタンクリック
     *
     * @param view ビュー
     */
    override fun onClick(view: View) {
        // 実行ボタン
        if (view === findViewById<View>(R.id.execButton)) {
            callExecProcess()

            // 請求番号採番ボタン
        } else if (view === findViewById<View>(R.id.buttonOrderID)) {
            val cl = Calendar.getInstance()
            val sdf = SimpleDateFormat("yyyyMMddHHmmssSS")
            val dateTime = sdf.format(cl.time)
            editTextOrderID!!.setText(dateTime)
        } else if (view === findViewById<View>(R.id.buttonUID)) {
            val uid = UUID.randomUUID()
            editTextUID!!.setText(uid.toString())
        } else if (view === findViewById<View>(R.id.radioButtonConnectionRS232)) {
            editTextIP!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonConnectionUSB)) {
            editTextIP!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonConnectionTCPIP)) {
            editTextIP!!.isEnabled = true
        } else if (view === findViewById<View>(R.id.radioButtonMethodCard)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = true
            radioButtonJobVOID!!.isEnabled = true
            radioButtonJobRETURN!!.isEnabled = true
            radioButtonJobCHECK!!.isEnabled = true

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
            if (radioButtonJobCAPTURE!!.isChecked || radioButtonJobCHECK!!.isChecked) {
                // 取消対象トランザクションID
                editTextCanTranID!!.isEnabled = false
            } else if (radioButtonJobVOID!!.isChecked) {
                // 取消対象トランザクションID
                editTextCanTranID!!.isEnabled = true
            } else if (radioButtonJobRETURN!!.isChecked) {
                // 取消対象トランザクションID
                editTextCanTranID!!.isEnabled = true
            }
        } else if (view === findViewById<View>(R.id.radioButtonMethodCardTrancheck)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = true
            radioButtonJobVOID!!.isEnabled = true
            radioButtonJobRETURN!!.isEnabled = true
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = true

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodQR)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodQRTrancheck)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = true
        } else if (view === findViewById<View>(R.id.radioButtonMethodEMoney)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodEMoneyBalance)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false

//        } else if (view == findViewById(R.id.radioButtonMethodEMoneyHistory)) {
//            // 処理区分
//            this.radioButtonJobCAPTURE.setEnabled(false);
//            this.radioButtonJobVOID.setEnabled(false);
//            this.radioButtonJobRETURN.setEnabled(false);
//
//            // トランザクションID
//            this.editTextTranID.setEnabled(false);
//
//            // 取消対象トランザクションID
//            this.editTextCanTranID.setEnabled(false);
//
//            // ジョブID
//            this.editTextJobID.setEnabled(false);
        } else if (view === findViewById<View>(R.id.radioButtonMethodEMoneyTrancheck)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = true

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodPaymentSelect)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
            if (radioButtonJobCAPTURE!!.isChecked) {
                // 取消対象トランザクションID
                editTextCanTranID!!.isEnabled = false
            } else if (radioButtonJobVOID!!.isChecked) {
                // 取消対象トランザクションID
                editTextCanTranID!!.isEnabled = true
            } else if (radioButtonJobRETURN!!.isChecked) {
                // 取消対象トランザクションID
                editTextCanTranID!!.isEnabled = true
            }
        } else if (view === findViewById<View>(R.id.radioButtonMethodTerminalStatusCheck)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodPaymentSelectStop) || view === findViewById<View>(
                R.id.radioButtonMethodCardStop
            ) || view === findViewById<View>(R.id.radioButtonMethodQRStop) || view === findViewById<View>(
                R.id.radioButtonMethodEMoneyStop
            ) || view === findViewById<View>(R.id.radioButtonMethodEMoneyBalanceStop)
        ) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodInstCard)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodInstQR)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodInstEMoney)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonMethodAPPOperationCheck)) {
            // 処理区分
            radioButtonJobCAPTURE!!.isEnabled = false
            radioButtonJobVOID!!.isEnabled = false
            radioButtonJobRETURN!!.isEnabled = false
            radioButtonJobCHECK!!.isEnabled = false

            // トランザクションID
            editTextTranID!!.isEnabled = false

            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false

            // ジョブID
            editTextJobID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonJobCAPTURE) || view === findViewById<View>(
                R.id.radioButtonJobCHECK
            )
        ) {
            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = false
        } else if (view === findViewById<View>(R.id.radioButtonJobVOID)) {
            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = !radioButtonMethodCardTrancheck!!.isChecked
        } else if (view === findViewById<View>(R.id.radioButtonJobRETURN)) {
            // 取消対象トランザクションID
            editTextCanTranID!!.isEnabled = !radioButtonMethodCardTrancheck!!.isChecked
        }
    }

    private fun callExecProcess() {
        val intent = Intent(this@MainActivity, ProcessActivity::class.java)
        intent.putExtra("PRINTER_SETTINGS", printerSettings)
        // 接続方法
        val RadioGroupConnection = findViewById<View>(R.id.RadioGroupConnection) as RadioGroup
        when (RadioGroupConnection.checkedRadioButtonId) {
            R.id.radioButtonConnectionRS232 -> intent.putExtra("CONNECTIONTYPE", "RS232")
            R.id.radioButtonConnectionUSB -> intent.putExtra("CONNECTIONTYPE", "USB")
            R.id.radioButtonConnectionTCPIP -> intent.putExtra("CONNECTIONTYPE", "TCPIP")
        }

        // ジョブID
        intent.putExtra("IP", editTextIP!!.text.toString())

        // 支払方法/状態確認
        val RadioGroupMethod = findViewById<View>(R.id.RadioGroupMethod) as RadioGroup
        when (RadioGroupMethod.checkedRadioButtonId) {
            R.id.radioButtonMethodCard -> intent.putExtra("PAYMENTTYPE", "Card")
            R.id.radioButtonMethodCardTrancheck -> intent.putExtra("PAYMENTTYPE", "CardTranCheck")
            R.id.radioButtonMethodCardStop -> intent.putExtra("PAYMENTTYPE", "CardStop")
            R.id.radioButtonMethodQR -> intent.putExtra("PAYMENTTYPE", "QR")
            R.id.radioButtonMethodQRTrancheck -> intent.putExtra("PAYMENTTYPE", "QRTranCheck")
            R.id.radioButtonMethodQRStop -> intent.putExtra("PAYMENTTYPE", "QRStop")
            R.id.radioButtonMethodEMoney -> intent.putExtra("PAYMENTTYPE", "EMoney")
            R.id.radioButtonMethodEMoneyStop -> intent.putExtra("PAYMENTTYPE", "EMoneyStop")
            R.id.radioButtonMethodEMoneyBalance -> intent.putExtra("PAYMENTTYPE", "EMoneyBalance")
            R.id.radioButtonMethodEMoneyBalanceStop -> intent.putExtra(
                "PAYMENTTYPE",
                "EMoneyBalanceStop"
            )
            R.id.radioButtonMethodEMoneyTrancheck -> intent.putExtra(
                "PAYMENTTYPE",
                "EMoneyTranCheck"
            )
            R.id.radioButtonMethodTerminalStatusCheck -> intent.putExtra(
                "PAYMENTTYPE",
                "TerminalStatusCheck"
            )
            R.id.radioButtonMethodInstCard -> intent.putExtra("PAYMENTTYPE", "CardAPPInstallCheck")
            R.id.radioButtonMethodInstQR -> intent.putExtra("PAYMENTTYPE", "QRAPPInstallCheck")
            R.id.radioButtonMethodInstEMoney -> intent.putExtra(
                "PAYMENTTYPE",
                "EMoneyAPPInstallCheck"
            )
            R.id.radioButtonMethodPaymentSelect -> intent.putExtra("PAYMENTTYPE", "PaymentSelect")
            R.id.radioButtonMethodPaymentSelectStop -> intent.putExtra(
                "PAYMENTTYPE",
                "PaymentSelectStop"
            )
            R.id.radioButtonMethodAPPOperationCheck -> intent.putExtra(
                "PAYMENTTYPE",
                "APPOperationCheck"
            )
        }

        // 請求番号
        intent.putExtra("ORDERID", System.currentTimeMillis().toString())

        // 処理区分
        val RadioGroupJob = findViewById<View>(R.id.RadioGroupJob) as RadioGroup
        when (RadioGroupJob.checkedRadioButtonId) {
            R.id.radioButtonJobCAPTURE -> intent.putExtra("JOBTYPE", "CAPTURE")
            R.id.radioButtonJobVOID -> intent.putExtra("JOBTYPE", "VOID")
            R.id.radioButtonJobRETURN -> intent.putExtra("JOBTYPE", "RETURN")
            R.id.radioButtonJobCHECK -> intent.putExtra("JOBTYPE", "CHECK")
        }

        // 金額
        intent.putExtra("AMOUNT", editTextAmount!!.text.toString())

        // 精算機コード
        intent.putExtra("MACHINECODE", editTextMachineCode!!.text.toString())

        // トランザクションID
        intent.putExtra("TRANID", editTextTranID!!.text.toString())

        // 取消対象トランザクションID
        intent.putExtra("CANTRANID", editTextCanTranID!!.text.toString())

        // ジョブID
        intent.putExtra("JOBID", editTextJobID!!.text.toString())

        // UID
        intent.putExtra("UID", editTextUID!!.text.toString())

        // オフラインフラグ
        val RadioGroupOffline = findViewById<View>(R.id.RadioGroupOffline) as RadioGroup
        when (RadioGroupOffline.checkedRadioButtonId) {
            R.id.radioButtonOfflineOff -> intent.putExtra("OFFLINE", "OFF")
            R.id.radioButtonOfflineOn -> intent.putExtra("OFFLINE", "ON")
        }
        startActivityForResult(intent, 0)
    }

    private fun onCreatePrinterManager(portInfo: PortInfo) {
        printerSettings = getPrinterSetting(portInfo)
        starIoExtManager = StarIoExtManager(
            StarIoExtManager.Type.Standard,
            printerSettings.portName,
            printerSettings.portSettings,
            10000,
            this
        )
    }

    private fun connectPrinter() {
        starIoExtManager.connect(object : ConnectionCallback() {
            @SuppressLint("SetTextI18n")
            override fun onConnected(result: Boolean, resultCode: Int) {
                super.onConnected(result, resultCode)
                if (result) {
                    binding.btnPayment.alpha = 1f
                    binding.btnPayment.isEnabled = true
                    binding.tvDeviceInfo.text = "Connected ${printerSettings.modelName}"
                }
            }
        })
    }

    private suspend fun searchPrinter(
        context: Context,
        interfaceType: String
    ): List<PortInfo> {
        return try {
            return StarIOPort.searchPrinter(interfaceType, context)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun getPrinterSetting(portInfo: PortInfo): PrinterSettings {
        val modelIndex = ModelCapability.MPOP
        return PrinterSettings(
            modelIndex = modelIndex,
            portName = portInfo.toPortName(),
            portSettings = ModelCapability.getPortSettings(modelIndex),
            macAddress = portInfo.macAddress,
            modelName = portInfo.toModelName(),
            cashDrawerOpenActiveHigh = false,
            paperSize = PrinterSettingConstant.PAPER_SIZE_DOT_THREE_INCH
        )
    }
}