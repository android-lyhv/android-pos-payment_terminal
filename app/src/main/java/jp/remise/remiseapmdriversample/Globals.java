package jp.remise.remiseapmdriversample;

import android.app.Application;

/**
 * アプリケーション管理
 */
public class Globals extends Application
{
    /**
     * 権限アクション名　salo-01_USB
     */
    public static final String ACTION_PERMISSION_USB = "com.android.example.USB_PERMISSION.salo-01_USB";

    /**
     * 権限アクション名　salo-01_RS232(CP210x)
     */
    public static final String ACTION_PERMISSION_RS232 = "com.android.example.USB_PERMISSION.salo-01_RS232";

    /**
     * USB Device ベンダーID定義
     */
    public enum DeviceVendorId
    {
        SALO01_USB(1478),
        SALO01_RS232(4292),
        ;

        private final int vendorId;

        DeviceVendorId(final int id)
        {
            vendorId = id;
        }

        public int getId()
        {
            return vendorId;
        }
    }
}
