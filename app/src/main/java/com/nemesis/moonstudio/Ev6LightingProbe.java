package com.nemesis.moonstudio;

import android.os.IBinder;
import android.os.Parcel;
import java.lang.reflect.Method;

/**
 * Read-only Kia/Hyundai SML status request.
 * This class intentionally contains no color or brightness write transactions.
 */
public final class Ev6LightingProbe {
    public static final String EXPECTED_MODEL = "wp_daudioplus";
    public static final String EXPECTED_DEVICE = "wp_daudioplus_cvev_us";
    public static final String EXPECTED_FIRMWARE = "CVEV.USA.S5W_M.V015.012.260507";
    public static final String SERVICE_NAME = "com.hkmc.micom.system";
    public static final String DESCRIPTOR = "com.hkmc.micom.ISystemService";
    public static final String STATUS_ACTION = "com.mobis.intent.action.SML_STATUS_CHANGED";
    public static final String STATUS_EXTRA = "com.mobis.intent.extra.SML_STATUS";
    public static final int STATUS_TRANSACTION = 74;

    private Ev6LightingProbe() { }

    public static String getSystemProperty(String key) {
        try {
            Class<?> type = Class.forName("android.os.SystemProperties");
            Method get = type.getMethod("get", String.class, String.class);
            return (String) get.invoke(null, key, "");
        } catch (Exception e) {
            return "";
        }
    }

    public static String firmwareMismatch() {
        String model = getSystemProperty("ro.product.model");
        String device = getSystemProperty("ro.product.device");
        String firmware = getSystemProperty("ro.build.product2");
        if (!EXPECTED_MODEL.equals(model)) return "Model mismatch: " + model;
        if (!EXPECTED_DEVICE.equals(device)) return "Device mismatch: " + device;
        if (!EXPECTED_FIRMWARE.equals(firmware)) return "Firmware mismatch: " + firmware;
        return null;
    }

    public static void requestStatus() throws Exception {
        String mismatch = firmwareMismatch();
        if (mismatch != null) throw new Exception("Safety gate blocked request. " + mismatch);

        // Use the EV6's mapped AIDL interface method instead of a hard-coded
        // Binder transaction number. This is the same method HMG-API maps as
        // framework.system_service.sml_status for the EV6 260507 firmware.
        Class<?> manager = Class.forName("android.os.ServiceManager");
        Method getService = manager.getMethod("getService", String.class);
        IBinder binder = (IBinder) getService.invoke(null, SERVICE_NAME);
        if (binder == null) throw new Exception("Vehicle lighting Binder service is unavailable");

        Class<?> stub = Class.forName("com.hkmc.micom.ISystemService$Stub");
        Object service = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
        if (service == null) throw new Exception("EV6 ISystemService interface unavailable");
        service.getClass().getMethod("requestSMLStatus").invoke(service);
    }

    public static String decodeStatus(byte[] bytes) throws Exception {
        if (bytes == null || bytes.length != 9) {
            throw new Exception("Status response was not the expected 9 bytes");
        }
        int red = ((bytes[0] & 255) << 8) | (bytes[1] & 255);
        int green = ((bytes[2] & 255) << 8) | (bytes[3] & 255);
        int blue = ((bytes[4] & 255) << 8) | (bytes[5] & 255);
        int brightness = bytes[6] & 255;
        int dim = bytes[7] & 255;
        int drive = bytes[8] & 255;
        if (red > 511 || green > 511 || blue > 511 ||
                brightness < 1 || brightness > 11 ||
                dim < 1 || dim > 2 || drive < 1 || drive > 2) {
            throw new Exception("Status values were outside the documented ranges");
        }
        return "RGB " + red + ", " + green + ", " + blue +
                " · Brightness raw " + brightness +
                " · Dim option " + dim + " · Drive mode " + drive;
    }
}
