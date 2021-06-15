package jp.remise.remiseapmdriversample;


import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.XmlResourceParser;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UsbReceiver {
    private Context mContext;

    /**
     * USB接続
     */
    private UsbManager mUsbManager;

    /**
     * USBデバイス
     */
    private UsbDevice mUsbDevice;

    /**
     * USB接続時の対象デバイス
     */

    private PendingIntent mPermissionIntent = null;

    /**
     * USB接続時の対象デバイス
     */
    private ArrayList<HashMap<String, String>> mDeviceFilterList;

    /**
     * 権限リクエスト状態　NEWPOS_USB
     */
    private int permissionNEWPOS_USB = 0;

    /**
     * 権限リクエスト状態　NEWPOS_RS232
     */
    private int permissionNEWPOS_RS232 = 0;

    /**
     * コンストラクタ
     */
    public UsbReceiver(Context context) {
        this.mContext = context;
        this.mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        this.mDeviceFilterList = getTargetDevice();
    }

    /**
     * USB接続時の対象デバイス取得
     *
     * @return 対象デバイス
     */
    public ArrayList<HashMap<String, String>> getTargetDevice() {
        ArrayList<HashMap<String, String>> deviceFilterList = new ArrayList<>();
        try {
            XmlResourceParser parser = mContext.getResources().getXml(R.xml.device_filter);
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_DOCUMENT:
                        break;

                    case XmlPullParser.END_DOCUMENT:
                        break;

                    case XmlPullParser.START_TAG:
                        if (parser.getName().equals("usb-device")) {
                            String vid = parser.getAttributeValue(null, "vendor-id");
                            if (vid == null) {
                                continue;
                            }
                            String pid = parser.getAttributeValue(null, "product-id");
                            if (pid == null) {
                                continue;
                            }
                            HashMap<String, String> map = new HashMap<>();
                            map.put("VID", vid);
                            map.put("PID", pid);
                            deviceFilterList.add(map);
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        break;

                    case XmlPullParser.TEXT:
                        break;

                    default:
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception exc) {
            Log.e("getTargetDevice", "device_filter analyze error.", exc);
        }
        return deviceFilterList;
    }

    /**
     * USBデバイス取得
     *
     * @return USBデバイス
     */
    public UsbDevice getUsbDevice(int vendorId) {
        // デバイス取得
        HashMap<String, UsbDevice> devices = this.mUsbManager.getDeviceList();

        // デバイスがない
        if (devices == null || devices.size() == 0) {
            return null;
        }

        // 対象デバイス判定
        List<UsbDevice> lstUsbDevice = new ArrayList<>();

        for (Map.Entry<String, UsbDevice> entry : devices.entrySet()) {
            UsbDevice device = entry.getValue();
            for (HashMap<String, String> map : this.mDeviceFilterList) {
                if (map.get("VID").equals(String.valueOf(device.getVendorId()))
                        && map.get("PID").equals(String.valueOf(device.getProductId()))) {
                    lstUsbDevice.add(device);
                }
            }
        }

        if (lstUsbDevice.size() == 0) {
            return null;
        }

        // 権限取得
        for (UsbDevice device : lstUsbDevice) {
            if (device.getVendorId() == vendorId && device.getVendorId() == Globals.DeviceVendorId.SALO01_RS232.getId()) {
                if (!this.mUsbManager.hasPermission(device)) {
                    if (permissionNEWPOS_RS232 != 2) {
                        this.mUsbManager.requestPermission(device, PendingIntent.getBroadcast(mContext, 1, new Intent(Globals.ACTION_PERMISSION_RS232), 0));
                        break;
                    }
                } else {
                    permissionNEWPOS_RS232 = 1;
                }
                return device;
            } else if (device.getVendorId() == vendorId && device.getVendorId() == Globals.DeviceVendorId.SALO01_USB.getId()) {
                if (!this.mUsbManager.hasPermission(device)) {
                    if (permissionNEWPOS_USB != 2) {
                        this.mUsbManager.requestPermission(device, PendingIntent.getBroadcast(mContext, 0, new Intent(Globals.ACTION_PERMISSION_USB), 0));
                        break;
                    }
                } else {
                    permissionNEWPOS_USB = 1;
                }
                return device;
            }
        }
        return null;
    }

    /**
     * USBシリアルドライバー取得
     *
     * @return UUSBシリアルドライバー
     */
    public UsbSerialDriver getUsbSerialDriver(int vendorId) {
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(this.mUsbManager);
        if (availableDrivers.isEmpty()) {
            return null;
        }

        for (UsbSerialDriver usbSerialDriver : availableDrivers) {
            UsbDevice device = usbSerialDriver.getDevice();
            if (device.getVendorId() == vendorId && device.getVendorId() == Globals.DeviceVendorId.SALO01_RS232.getId()) {
                if (!this.mUsbManager.hasPermission(device)) {
                    if (permissionNEWPOS_RS232 != 2) {
                        this.mUsbManager.requestPermission(device, PendingIntent.getBroadcast(mContext, 1, new Intent(Globals.ACTION_PERMISSION_RS232), 0));
                        break;
                    }
                } else {
                    permissionNEWPOS_RS232 = 1;
                }
                return usbSerialDriver;
            } else if (device.getVendorId() == vendorId && device.getVendorId() == Globals.DeviceVendorId.SALO01_USB.getId()) {
                if (!this.mUsbManager.hasPermission(device)) {
                    if (permissionNEWPOS_USB != 2) {
                        this.mUsbManager.requestPermission(device, PendingIntent.getBroadcast(mContext, 0, new Intent(Globals.ACTION_PERMISSION_USB), 0));
                        break;
                    }
                } else {
                    permissionNEWPOS_USB = 1;
                }
                return usbSerialDriver;
            }
        }
        return null;
    }
}
