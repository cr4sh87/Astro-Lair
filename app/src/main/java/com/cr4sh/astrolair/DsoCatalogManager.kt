package com.cr4sh.astrolair

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.URL

object DsoCatalogManager {

    private const val TAG = "DsoCatalogManager"
    private const val LOCAL_FILE_NAME = "dso_catalog.json"
    private const val PREFS_NAME = "astro_lair_catalog"
    private const val PREF_LAST_UPDATE = "last_update_millis"
    private const val MAX_AGE_MILLIS = 7L * 24L * 60L * 60L * 1000L

    // TODO: metti qui il tuo URL reale (raw GitHub o altro)
    private const val REMOTE_URL =
        "https://raw.githubusercontent.com/cr4sh87/astro-lair/main/catalog/dso_catalog.json"

    private var loaded: Boolean = false
    private var targetsByCatalog: Map<String, List<TargetObject>> = emptyMap()

    fun init(context: Context) {
        if (loaded) return

        val file = File(context.filesDir, LOCAL_FILE_NAME)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUpdate = prefs.getLong(PREF_LAST_UPDATE, 0L)
        val now = System.currentTimeMillis()

        val needDownload = !file.exists() || (now - lastUpdate > MAX_AGE_MILLIS)

        if (needDownload) {
            try {
                Log.d(TAG, "Scarico catalogo DSO da $REMOTE_URL")
                val jsonText = URL(REMOTE_URL).readText()
                file.writeText(jsonText)
                prefs.edit().putLong(PREF_LAST_UPDATE, now).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Errore download catalogo DSO: ${e.message}")
                // se non c'è file locale, useremo fallback
            }
        }

        val text = if (file.exists()) file.readText() else null
        if (text != null && text.isNotBlank()) {
            try {
                parseCatalog(text)
                loaded = true
                return
            } catch (e: Exception) {
                Log.e(TAG, "Errore parsing catalogo DSO: ${e.message}")
            }
        }

        // Fallback: usa il vecchio repository hard-coded
        Log.w(TAG, "Uso fallback hardcoded per i target")
        val catalogs = mutableMapOf<String, List<TargetObject>>()
        TargetCatalogRepository.getCatalogNames().forEach { cat ->
            catalogs[cat] = TargetCatalogRepository.getTargetsForCatalog(cat)
        }
        targetsByCatalog = catalogs
        loaded = true
    }

    private fun parseCatalog(jsonText: String) {
        val root = JSONObject(jsonText)
        val objects = root.getJSONArray("objects")

        val tmpMap = mutableMapOf<String, MutableList<TargetObject>>()

        for (i in 0 until objects.length()) {
            val obj = objects.getJSONObject(i)

            val catalog = obj.optString("catalog", "Unknown")
            val code = obj.optString("code", "")
            val name = obj.optString("name", "")
            val type = obj.optString("type", "")
            val mag = obj.optDouble("mag", Double.NaN)
            val sb = obj.optDouble("surface_brightness", Double.NaN)
            val ra = obj.optString("ra_deg", "")
            val dec = obj.optString("dec_deg", "")
            val constellation = obj.optString("constellation", "")

            val target = TargetObject(
                catalog = catalog,
                code = code,
                name = name,
                type = type,
                magnitude = if (mag.isNaN()) 0.0 else mag,
                surfaceBrightness = if (sb.isNaN()) null else sb,
                ra = ra,
                dec = dec,
                constellation = constellation
            )

            val list = tmpMap.getOrPut(catalog) { mutableListOf() }
            list.add(target)
        }

        // Ordiniamo per codice numerico quando possibile
        targetsByCatalog = tmpMap.mapValues { (_, list) ->
            list.sortedBy {
                it.code.filter { ch -> ch.isDigit() }.toIntOrNull() ?: Int.MAX_VALUE
            }
        }
    }

    fun getCatalogNames(): List<String> = targetsByCatalog.keys.sorted()

    fun getTargetsForCatalog(catalog: String): List<TargetObject> =
        targetsByCatalog[catalog] ?: emptyList()
}
