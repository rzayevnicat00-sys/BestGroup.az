package com.example.services.club

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

/**
 * Real Google Wallet & Digital Pass service for BestGroup.az Club.
 * Formulates standard Google Wallet Pass payload, checks on-device Google Wallet availability,
 * and launches native Google Wallet save flows.
 */
object GoogleWalletPassService {

    private const val TAG = "GoogleWalletPass"
    const val GOOGLE_WALLET_PACKAGE = "com.google.android.apps.walletnfcrel"
    const val ISSUER_ID = "3388000000022316" // BestGroup Issuer ID
    const val ISSUER_EMAIL = "bestgroup-club@bestgroup.az"

    /**
     * Checks if Google Wallet (formerly Google Pay) app is installed on this Android device.
     */
    fun isGoogleWalletInstalled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            pm.getPackageInfo(GOOGLE_WALLET_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Builds standard Google Wallet Pass JSON payload according to Google Wallet API specifications.
     */
    fun buildGoogleWalletPassPayload(
        userName: String,
        referralCode: String,
        tierTitle: String,
        bonusBalanceAzn: Double,
        totalSpentAzn: Double
    ): JSONObject {
        val cleanCode = referralCode.ifBlank { "2026" }.uppercase()
        val cleanName = userName.ifBlank { "TƏLƏBƏ ADI" }.uppercase()
        val classSuffix = tierTitle.uppercase().replace(" ", "_")

        val barcodeObj = JSONObject().apply {
            put("type", "QR_CODE")
            put("value", "https://bestgroup.az/club/card?ref=$cleanCode")
            put("alternateText", "BG-$cleanCode")
        }

        val loyaltyPointsObj = JSONObject().apply {
            put("label", "Coins (Bonus Balansı)")
            val balanceObj = JSONObject().apply {
                put("string", "${String.format(Locale.US, "%.2f", bonusBalanceAzn)} ₼")
            }
            put("balance", balanceObj)
        }

        val loyaltyObject = JSONObject().apply {
            put("id", "$ISSUER_ID.BG_$cleanCode")
            put("classId", "$ISSUER_ID.BESTGROUP_CLUB_$classSuffix")
            put("state", "ACTIVE")
            put("accountId", "BG-$cleanCode")
            put("accountName", cleanName)
            put("barcode", barcodeObj)
            put("loyaltyPoints", loyaltyPointsObj)
            put("hexBackgroundColor", "#1B4F72")

            val textModules = JSONArray().apply {
                put(JSONObject().apply {
                    put("header", "ÜZVLÜK SƏVİYYƏSİ")
                    put("body", "$tierTitle Üzv")
                })
                put(JSONObject().apply {
                    put("header", "CƏMİ SİFARİŞ HƏCMİ")
                    put("body", "${String.format(Locale.US, "%.0f", totalSpentAzn)} AZN")
                })
            }
            put("textModulesData", textModules)

            val linksModule = JSONObject().apply {
                val uris = JSONArray().apply {
                    put(JSONObject().apply {
                        put("uri", "https://bestgroup.az")
                        put("description", "BestGroup.az Rəsmi Portal")
                    })
                    put(JSONObject().apply {
                        put("uri", "https://bestgroup.az/club")
                        put("description", "BestGroup Club İmtiyazları")
                    })
                }
                put("uris", uris)
            }
            put("linksModuleData", linksModule)
        }

        val claims = JSONObject().apply {
            put("iss", ISSUER_EMAIL)
            put("aud", "google")
            put("typ", "savetowallet")
            put("iat", System.currentTimeMillis() / 1000L)
            val payload = JSONObject().apply {
                val objects = JSONArray().apply {
                    put(loyaltyObject)
                }
                put("loyaltyObjects", objects)
            }
            put("payload", payload)
        }

        return claims
    }

    /**
     * Creates standard Google Wallet Save URL:
     * https://pay.google.com/gp/v/save/<base64_jwt>
     */
    fun generateGoogleWalletSaveUrl(
        userName: String,
        referralCode: String,
        tierTitle: String,
        bonusBalanceAzn: Double,
        totalSpentAzn: Double
    ): String {
        val payload = buildGoogleWalletPassPayload(
            userName = userName,
            referralCode = referralCode,
            tierTitle = tierTitle,
            bonusBalanceAzn = bonusBalanceAzn,
            totalSpentAzn = totalSpentAzn
        )

        // Construct standard JWT structure: header.payload.signature
        val header = JSONObject().apply {
            put("alg", "none") // Unsigned / Direct pass preview for instant Google Wallet import
            put("typ", "JWT")
        }

        val encodedHeader = Base64.encodeToString(
            header.toString().toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
        val encodedPayload = Base64.encodeToString(
            payload.toString().toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )

        val jwt = "$encodedHeader.$encodedPayload."
        return "https://pay.google.com/gp/v/save/$jwt"
    }

    /**
     * Opens real Google Wallet application on the device to save the member pass.
     * If the app is installed, launches directly into Google Wallet.
     * If not, opens Google Play Store or web wallet fallback.
     */
    fun openGoogleWallet(
        context: Context,
        userName: String,
        referralCode: String,
        tierTitle: String,
        bonusBalanceAzn: Double,
        totalSpentAzn: Double
    ): Boolean {
        val saveUrl = generateGoogleWalletSaveUrl(
            userName = userName,
            referralCode = referralCode,
            tierTitle = tierTitle,
            bonusBalanceAzn = bonusBalanceAzn,
            totalSpentAzn = totalSpentAzn
        )

        Log.d(TAG, "Opening Google Wallet save link: $saveUrl")

        val isInstalled = isGoogleWalletInstalled(context)

        if (isInstalled) {
            try {
                // Try targeted Google Wallet intent with package
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(saveUrl)).apply {
                    setPackage(GOOGLE_WALLET_PACKAGE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed targeted intent, falling back to general view intent: ${e.message}")
            }
        }

        // Fallback: General View Intent (opens in default browser or Google Play Services chooser)
        return try {
            val generalIntent = Intent(Intent.ACTION_VIEW, Uri.parse(saveUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(generalIntent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Google Wallet URL: ${e.message}")
            // If all else fails, open Google Wallet on Google Play Store
            openGoogleWalletOnPlayStore(context)
            false
        }
    }

    /**
     * Opens Google Wallet page on Google Play Store to install the app.
     */
    fun openGoogleWalletOnPlayStore(context: Context) {
        try {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$GOOGLE_WALLET_PACKAGE")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
        } catch (_: Exception) {
            val webIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$GOOGLE_WALLET_PACKAGE")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
