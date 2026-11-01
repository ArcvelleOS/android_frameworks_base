/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.internal.util.custom;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import org.json.JSONObject;

import java.lang.reflect.Field;

/** Applies a Settings-selected game device profile before app providers and code are initialized. */
public final class GameDeviceIdentitySpoof {
    private static final String TAG = "GameDeviceIdentity";
    private static final String SETTING_GAME_IDENTITIES_ENABLED = "game_device_identity_enabled";
    private static final String SETTING_GAME_IDENTITIES = "game_device_identity_profiles";
    private static final String[] BUILD_FIELDS = {
            "BRAND", "DEVICE", "MANUFACTURER", "MODEL", "FINGERPRINT", "PRODUCT"
    };

    private GameDeviceIdentitySpoof() {}

    public static void apply(Context context, String packageName, int userId) {
        if (context == null || packageName == null || packageName.isEmpty()
                || Process.isIsolated()) {
            return;
        }

        try {
            if (Settings.System.getIntForUser(context.getContentResolver(),
                    SETTING_GAME_IDENTITIES_ENABLED, 0, userId) != 1) {
                return;
            }
            final String identities = Settings.System.getStringForUser(
                    context.getContentResolver(), SETTING_GAME_IDENTITIES, userId);
            if (identities == null || identities.isEmpty()) return;
            // Runs for every app, so skip the JSON parse unless the package has a profile.
            if (!identities.contains("\"" + packageName + "\"")) return;

            final JSONObject profiles = new JSONObject(identities);
            final JSONObject customProfile = profiles.optJSONObject(packageName);
            if (customProfile != null) {
                applyCustomProfile(customProfile);
                Log.d(TAG, "Applied custom game identity for " + packageName);
                return;
            }

            final String profile = profiles.optString(packageName, "");
            switch (profile) {
                case "pixel_6_pro":
                    applyProfile("google", "Google", "Pixel 6 Pro", "raven");
                    break;
                case "pixel_7_pro":
                    applyProfile("google", "Google", "Pixel 7 Pro", "cheetah");
                    break;
                case "pixel_8_pro":
                    applyProfile("google", "Google", "Pixel 8 Pro", "husky");
                    break;
                case "pixel_9_pro":
                    applyProfile("google", "Google", "Pixel 9 Pro", "caiman");
                    break;
                // Device identities adapted from PenguinOS's PerAppsPropsUtils catalogue.
                // Built-ins retain the running build's fingerprint, as the existing Pixels do.
                case "pixel_10_pro_xl":
                    applyProfile("google", "Google", "Pixel 10 Pro XL", "mustang");
                    break;
                case "asus_rog_6d_ultimate":
                    applyProfile("asus", "asus", "ASUS_AI2203_D", "AI2203", "AI2203");
                    break;
                case "asus_rog_8_pro":
                    applyProfile("asus", "asus", "ASUS_AI2401_D", "AI2401", "AI2401");
                    break;
                case "asus_rog_9_pro":
                    applyProfile("asus", "asus", "ASUS_AI2501", "AI2501", "AI2501");
                    break;
                case "oneplus_8_pro":
                    applyProfile("OnePlus", "OnePlus", "IN2023", "OnePlus8Pro", "OnePlus8Pro");
                    break;
                case "oneplus_12":
                    applyProfile("OnePlus", "OnePlus", "CPH2581", "OP594DL1", "OP594DL1");
                    break;
                case "oneplus_13":
                    applyProfile("OnePlus", "OnePlus", "PJZ110", "PJZ110", "PJZ110");
                    break;
                case "redmagic_9_pro":
                    applyProfile("nubia", "nubia", "REDMAGIC 9 Pro", "NX769J", "NX769J");
                    break;
                case "redmagic_10_pro":
                    applyProfile("nubia", "nubia", "REDMAGIC 10 Pro", "NX789J", "NX789J");
                    break;
                case "redmagic_11_pro":
                    applyProfile("REDMAGIC", "Nubia", "REDMAGIC 11 PRO", "NX809J", "NX809J");
                    break;
                case "galaxy_s25_ultra":
                    applyProfile("samsung", "samsung", "SM-S938B", "e3q", "e3qxxx");
                    break;
                case "poco_f5":
                    applyProfile("Xiaomi", "Xiaomi", "23049PCD8G", "marble");
                    break;
                default:
                    return;
            }
            Log.d(TAG, "Applied game identity profile '" + profile + "' for " + packageName);
        } catch (Exception e) {
            Log.w(TAG, "Unable to apply game identity for " + packageName, e);
        }
    }

    private static void applyCustomProfile(JSONObject profile) {
        for (String field : BUILD_FIELDS) {
            final String value = profile.optString(field, "");
            if (!value.isEmpty()) {
                setBuildField(field, value);
            }
        }
    }

    private static void applyProfile(String brand, String manufacturer, String model,
            String device) {
        applyProfile(brand, manufacturer, model, device, device);
    }

    private static void applyProfile(String brand, String manufacturer, String model,
            String device, String product) {
        setBuildField("BRAND", brand);
        setBuildField("MANUFACTURER", manufacturer);
        setBuildField("MODEL", model);
        setBuildField("DEVICE", device);
        if (product != null) {
            setBuildField("PRODUCT", product);
        }
    }

    private static void setBuildField(String name, String value) {
        Field field = null;
        try {
            field = Build.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.w(TAG, "Unable to set Build." + name, e);
        } finally {
            if (field != null) {
                try {
                    field.setAccessible(false);
                } catch (RuntimeException ignored) {
                    // The per-process field update is best-effort.
                }
            }
        }
    }
}
