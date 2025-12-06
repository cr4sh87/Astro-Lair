package com.cr4sh.astrolair

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.ListPopupWindow
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    // --- Sezioni / layout ---
    private lateinit var sectionMoon: View
    private lateinit var sectionWeather: View
    private lateinit var sectionSoho: View
    private lateinit var sectionSatellites: View
    private lateinit var sectionTargets: View

    // --- Luna ---
    private lateinit var moonSelectedDateTimeText: TextView
    private lateinit var moonPhaseText: TextView

    // --- Meteo / seeing ---
    private lateinit var seeingText: TextView
    private val obsLatitude = 37.6
    private val obsLongitude = 15.1

    // --- SOHO ---
    private lateinit var sohoImage: ImageView
    private lateinit var sohoStatusText: TextView
    private lateinit var sohoInstrumentSpinner: Spinner
    private lateinit var sohoFormatSpinner: Spinner
    private lateinit var sohoImageLoader: ImageLoader

    // --- Satelliti ---
    private lateinit var satellitesText: TextView
    private lateinit var satellitesCanvas: FrameLayout

    // --- Target planner ---
    private lateinit var targetCatalogSpinner: Spinner
    private lateinit var targetObjectText: EditText
    private lateinit var targetInfoText: TextView
    private lateinit var targetStackText: TextView

    private lateinit var targetPopup: ListPopupWindow
    private lateinit var targetSuggestionAdapter: TargetSuggestionAdapter
    private var currentSelectedTarget: TargetObject? = null
    private var currentCatalogTargets: List<TargetObject> = emptyList()

    // --- Varie ---
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    enum class Section {
        MOON, WEATHER, SOHO, SATELLITES, TARGETS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inizializza catalogo DSO (scarica/legge JSON generato)
        DsoCatalogManager.init(this)

        // Sezioni
        sectionMoon = findViewById(R.id.section_moon)
        sectionWeather = findViewById(R.id.section_weather)
        sectionSoho = findViewById(R.id.section_soho)
        sectionSatellites = findViewById(R.id.section_satellites)
        sectionTargets = findViewById(R.id.section_targets)

        // TOP BAR – gear configurazione strumenti
        findViewById<View>(R.id.button_settings).setOnClickListener {
            showEquipmentConfigDialog()
        }

        // BOTTOM NAV
        findViewById<Button>(R.id.button_menu_moon).setOnClickListener {
            showSection(Section.MOON)
        }
        findViewById<Button>(R.id.button_menu_weather).setOnClickListener {
            showSection(Section.WEATHER)
        }
        findViewById<Button>(R.id.button_menu_soho).setOnClickListener {
            showSection(Section.SOHO)
        }
        findViewById<Button>(R.id.button_menu_satellites).setOnClickListener {
            showSection(Section.SATELLITES)
        }
        findViewById<Button>(R.id.button_menu_targets).setOnClickListener {
            showSection(Section.TARGETS)
        }

        // --- Luna ---
        moonSelectedDateTimeText = findViewById(R.id.moon_selected_datetime)
        moonPhaseText = findViewById(R.id.moon_phase_text)

        val buttonNow: Button = findViewById(R.id.button_now)
        val buttonPick: Button = findViewById(R.id.button_pick_datetime)

        updateMoonForDateTime(ZonedDateTime.now())

        buttonNow.setOnClickListener {
            updateMoonForDateTime(ZonedDateTime.now())
        }

        buttonPick.setOnClickListener {
            showDateTimePicker()
        }

        // --- Meteo / seeing ---
        seeingText = findViewById(R.id.seeing_text)
        findViewById<Button>(R.id.button_refresh_weather).setOnClickListener {
            fetchWeatherAndSeeing()
        }

        // --- SOHO ---
        sohoImage = findViewById(R.id.soho_image)
        sohoStatusText = findViewById(R.id.soho_status_text)
        sohoInstrumentSpinner = findViewById(R.id.soho_instrument_spinner)
        sohoFormatSpinner = findViewById(R.id.soho_format_spinner)

        sohoImageLoader = ImageLoader.Builder(this)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()

        findViewById<Button>(R.id.button_refresh_sun).setOnClickListener {
            fetchSohoImage()
        }

        // --- Satelliti ---
        satellitesText = findViewById(R.id.satellites_text)
        satellitesCanvas = findViewById(R.id.satellites_canvas)

        findViewById<Button>(R.id.button_jupiter_moons).setOnClickListener {
            updateJupiterSatellites()
        }

        findViewById<Button>(R.id.button_saturn_moons).setOnClickListener {
            updateSaturnSatellites()
        }

        // --- Target planner ---
        targetCatalogSpinner = findViewById(R.id.target_catalog_spinner)
        targetObjectText = findViewById(R.id.target_object_text)
        targetInfoText = findViewById(R.id.target_info_text)
        targetStackText = findViewById(R.id.target_stack_text)

        setupTargetAutocomplete()

        findViewById<Button>(R.id.target_select_button).setOnClickListener {
            onTargetSelectButton()
        }

        findViewById<Button>(R.id.target_stack_button).setOnClickListener {
            showStackSuggestion()
        }

        // Sezione iniziale
        showSection(Section.MOON)
    }

    // -------------------------------------------------
    // Sezioni
    // -------------------------------------------------
    private fun showSection(section: Section) {
        sectionMoon.visibility = if (section == Section.MOON) View.VISIBLE else View.GONE
        sectionWeather.visibility = if (section == Section.WEATHER) View.VISIBLE else View.GONE
        sectionSoho.visibility = if (section == Section.SOHO) View.VISIBLE else View.GONE
        sectionSatellites.visibility = if (section == Section.SATELLITES) View.VISIBLE else View.GONE
        sectionTargets.visibility = if (section == Section.TARGETS) View.VISIBLE else View.GONE
    }

    // -------------------------------------------------
    // Luna
    // -------------------------------------------------
    private fun showDateTimePicker() {
        val now = LocalDateTime.now()
        val startDate = now.toLocalDate()

        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        val pickedDateTime =
                            LocalDateTime.of(
                                pickedDate,
                                now.toLocalTime()
                                    .withHour(hourOfDay)
                                    .withMinute(minute)
                            )
                        val zoned = pickedDateTime.atZone(ZoneId.systemDefault())
                        updateMoonForDateTime(zoned)
                    },
                    now.hour,
                    now.minute,
                    true
                ).show()
            },
            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth
        ).show()
    }

    private fun updateMoonForDateTime(dateTime: ZonedDateTime) {
        moonSelectedDateTimeText.text =
            "Data/ora: ${dateTime.format(dateTimeFormatter)} (${dateTime.zone.id})"

        val phase = MoonPhaseCalculator.phaseFraction(dateTime)
        val description = MoonPhaseCalculator.describePhase(phase)
        val percent = (phase * 100).toInt()

        moonPhaseText.text = """
            Fase: $description
            Illuminazione: ~$percent%
            Frazione di ciclo: ${String.format("%.3f", phase)}
        """.trimIndent()
    }

    // -------------------------------------------------
    // Meteo / seeing
    // -------------------------------------------------
    private fun fetchWeatherAndSeeing() {
        seeingText.text =
            "Richiesta meteo in corso per lat=$obsLatitude, lon=$obsLongitude..."

        Thread {
            try {
                val url =
                    "https://api.open-meteo.com/v1/forecast" +
                            "?latitude=$obsLatitude" +
                            "&longitude=$obsLongitude" +
                            "&hourly=cloud_cover,wind_speed_10m,relative_humidity_2m" +
                            "&forecast_days=1" +
                            "&timezone=auto"

                val jsonText = URL(url).readText()
                val root = JSONObject(jsonText)
                val hourly = root.getJSONObject("hourly")

                val times = hourly.getJSONArray("time")
                val clouds = hourly.getJSONArray("cloud_cover")
                val winds = hourly.getJSONArray("wind_speed_10m")
                val hums = hourly.getJSONArray("relative_humidity_2m")

                if (times.length() == 0) {
                    showWeatherError("Nessun dato orario ricevuto.")
                    return@Thread
                }

                val t0 = times.getString(0)
                val cloud0 = clouds.optDouble(0, Double.NaN)
                val wind0 = winds.optDouble(0, Double.NaN)
                val hum0 = hums.optDouble(0, Double.NaN)

                if (cloud0.isNaN() || wind0.isNaN() || hum0.isNaN()) {
                    showWeatherError("Dati meteo incompleti.")
                    return@Thread
                }

                val seeingIndex = computeSeeingIndex(
                    cloudCover = cloud0,
                    humidity = hum0,
                    windSpeed = wind0
                )

                val qualityLabel = when {
                    seeingIndex >= 80 -> "Ottimo"
                    seeingIndex >= 60 -> "Buono"
                    seeingIndex >= 40 -> "Discreto"
                    seeingIndex >= 20 -> "Scarso"
                    else -> "Pessimo"
                }

                val text = """
                    Ultimo dato modello: $t0
                    Copertura nuvolosa: ${cloud0.toInt()} %
                    Umidità relativa: ${hum0.toInt()} %
                    Vento a 10 m: ${String.format("%.1f", wind0)} m/s

                    Seeing stimato: ${seeingIndex.toInt()} / 100  ($qualityLabel)
                """.trimIndent()

                runOnUiThread {
                    seeingText.text = text
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showWeatherError("Errore durante il recupero meteo: ${e.message}")
            }
        }.start()
    }

    private fun showWeatherError(message: String) {
        runOnUiThread {
            seeingText.text = "Meteo / seeing: $message"
        }
    }

    private fun computeSeeingIndex(
        cloudCover: Double,
        humidity: Double,
        windSpeed: Double
    ): Double {
        val cloudPenalty = cloudCover        // 0..100
        val humidityPenalty = humidity * 0.6 // 0..60
        val windPenalty = min(windSpeed * 6.0, 40.0) // 0..40

        val raw = 100.0 - (cloudPenalty * 0.6 + humidityPenalty * 0.25 + windPenalty * 0.15)
        return max(0.0, min(100.0, raw))
    }

    // -------------------------------------------------
    // SOHO
    // -------------------------------------------------
    private fun fetchSohoImage() {
        val instrument = (sohoInstrumentSpinner.selectedItem as? String)?.uppercase() ?: "C2"
        val format = (sohoFormatSpinner.selectedItem as? String)?.uppercase() ?: "JPG"

        val instrumentPath = when (instrument) {
            "C2" -> "c2"
            "C3" -> "c3"
            else -> "c2"
        }

        val ext = when (format) {
            "GIF" -> "gif"
            else -> "jpg"
        }

        val urlStr =
            "https://soho.nascom.nasa.gov/data/realtime/$instrumentPath/512/latest.$ext"

        sohoStatusText.text =
            "Scaricando immagine da SOHO ($instrument, $format)..."

        val request = ImageRequest.Builder(this)
            .data(urlStr)
            .target(
                onSuccess = { result ->
                    sohoImage.setImageDrawable(result)
                    val fetchedAt = LocalDateTime.now().format(dateTimeFormatter)
                    sohoStatusText.text =
                        "Ultima immagine SOHO $instrument ($format) scaricata alle $fetchedAt."
                },
                onError = {
                    sohoStatusText.text =
                        "Errore nel recupero immagine SOHO."
                }
            )
            .build()

        sohoImageLoader.enqueue(request)
    }

    // -------------------------------------------------
    // Satelliti Giove / Saturno (vista schematica)
    // -------------------------------------------------
    private fun updateJupiterSatellites() {
        val nowUtc = ZonedDateTime.now(ZoneId.of("UTC"))
        val list = SatelliteModels.jupiterSatellitesAt(nowUtc)

        val builder = StringBuilder()
        builder.appendLine("Giove – modello semplificato (non per uso scientifico)")
        builder.appendLine("Tempo (UTC): ${nowUtc.format(dateTimeFormatter)}")
        builder.appendLine()
        builder.appendLine("Satelliti (da Ovest a Est, distanza in raggi gioviani):")
        list.forEach { s ->
            builder.appendLine(
                String.format(
                    "[%s] %-8s  %.2f Rᴊ",
                    if (s.x < 0) "Ovest" else "Est",
                    s.name,
                    abs(s.x)
                )
            )
        }
        satellitesText.text = builder.toString()
        renderSatellites(list, isJupiter = true)
    }

    private fun updateSaturnSatellites() {
        val nowUtc = ZonedDateTime.now(ZoneId.of("UTC"))
        val list = SatelliteModels.saturnSatellitesAt(nowUtc)

        val builder = StringBuilder()
        builder.appendLine("Saturno – modello semplificato (non per uso scientifico)")
        builder.appendLine("Tempo (UTC): ${nowUtc.format(dateTimeFormatter)}")
        builder.appendLine()
        builder.appendLine("Satelliti (da Ovest a Est, distanza in raggi saturniani):")
        list.forEach { s ->
            builder.appendLine(
                String.format(
                    "[%s] %-9s %.2f Rₛ",
                    if (s.x < 0) "Ovest" else "Est",
                    s.name,
                    abs(s.x)
                )
            )
        }
        satellitesText.text = builder.toString()
        renderSatellites(list, isJupiter = false)
    }

    private fun renderSatellites(list: List<SatellitePosition>, isJupiter: Boolean) {
        satellitesCanvas.post {
            satellitesCanvas.removeAllViews()

            val width = satellitesCanvas.width
            val height = satellitesCanvas.height
            if (width == 0 || height == 0) return@post

            val centerX = width / 2f
            val centerY = height / 2f

            val maxRadius = list.maxOfOrNull { abs(it.x) }?.coerceAtLeast(5.0) ?: 5.0
            val scale = (width / 2f - dpToPx(40f)) / maxRadius.toFloat()

            // Pianeta al centro
            val planetView = ImageView(this).apply {
                setImageResource(
                    if (isJupiter) R.drawable.planet_jupiter
                    else R.drawable.planet_saturn
                )
            }
            val size = dpToPx(40f).toInt()
            val planetLp = FrameLayout.LayoutParams(size, size)
            planetLp.leftMargin = (centerX - size / 2f).toInt()
            planetLp.topMargin = (centerY - size / 2f).toInt()
            satellitesCanvas.addView(planetView, planetLp)

            // Satelliti
            list.forEach { s ->
                val xPx = centerX + (s.x * scale).toFloat()

                val dot = View(this).apply {
                    setBackgroundResource(R.drawable.satellite_dot)
                }
                val dotSize = dpToPx(6f).toInt()
                val dotLp = FrameLayout.LayoutParams(dotSize, dotSize)
                dotLp.leftMargin = (xPx - dotSize / 2f).toInt()
                dotLp.topMargin = (centerY - dpToPx(3f)).toInt()
                satellitesCanvas.addView(dot, dotLp)

                val label = TextView(this).apply {
                    text = s.name
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                }
                val labelLp = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
                labelLp.leftMargin = (xPx - dpToPx(12f)).toInt()
                labelLp.topMargin = (centerY - dpToPx(22f)).toInt()
                satellitesCanvas.addView(label, labelLp)
            }
        }
    }

    private fun dpToPx(dp: Float): Float =
        dp * resources.displayMetrics.density

    // -------------------------------------------------
    // Target planner – catalogo + autocomplete (DsoCatalogManager)
    // -------------------------------------------------
    private fun setupTargetAutocomplete() {
        val catalogs = DsoCatalogManager.getCatalogNames()

        val catalogAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            catalogs
        )
        catalogAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        targetCatalogSpinner.adapter = catalogAdapter

        // Popup suggerimenti
        targetSuggestionAdapter = TargetSuggestionAdapter(this, emptyList())
        targetPopup = ListPopupWindow(this).apply {
            anchorView = targetObjectText
            setAdapter(targetSuggestionAdapter)
            isModal = true
            setOnItemClickListener { _, _, position, _ ->
                val item = targetSuggestionAdapter.getItem(position)
                currentSelectedTarget = item
                targetObjectText.setText(item.code)
                updateTargetInfo()
                dismiss()
            }
        }

        targetCatalogSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val catalog = catalogs[position]
                currentCatalogTargets = DsoCatalogManager.getTargetsForCatalog(catalog)
                targetObjectText.text.clear()
                currentSelectedTarget = currentCatalogTargets.firstOrNull()
                updateTargetInfo()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        if (catalogs.isNotEmpty()) {
            targetCatalogSpinner.setSelection(0)
        }

        targetObjectText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                showTargetSuggestions(query)
            }
        })
    }

    private fun showTargetSuggestions(query: String) {
        if (query.isBlank()) {
            targetPopup.dismiss()
            return
        }

        val q = query.lowercase()
        val matches = currentCatalogTargets.filter { t ->
            t.code.lowercase().contains(q) || t.name.lowercase().contains(q)
        }

        if (matches.isEmpty()) {
            targetPopup.dismiss()
            return
        }

        targetSuggestionAdapter.update(matches)
        if (!targetPopup.isShowing) {
            targetPopup.show()
        }
    }

    private fun onTargetSelectButton() {
        val text = targetObjectText.text.toString().trim()
        if (text.isEmpty()) {
            targetInfoText.text = "Inserisci un numero/etichetta dell'oggetto."
            return
        }

        // Se già selezionato via popup
        currentSelectedTarget?.let {
            updateTargetInfo()
            return
        }

        val q = text.lowercase()
        val match = currentCatalogTargets.firstOrNull { t ->
            t.code.lowercase().contains(q) || t.name.lowercase().contains(q)
        }

        if (match != null) {
            currentSelectedTarget = match
            updateTargetInfo()
        } else {
            targetInfoText.text = "Nessun oggetto trovato per '$text'."
        }
    }

    private fun updateTargetInfo() {
        val t = currentSelectedTarget
        if (t == null) {
            targetInfoText.text = "Nessun oggetto selezionato."
            return
        }
        val sb = StringBuilder()
        sb.appendLine("${t.code} – ${t.name}")
        sb.appendLine("Catalogo: ${t.catalog}")
        sb.appendLine("Tipo: ${t.type}")
        sb.appendLine("Costellazione: ${t.constellation}")
        sb.appendLine("Magnitudine: ${t.magnitude}")
        t.surfaceBrightness?.let {
            sb.appendLine("Luminosità superficiale: $it mag/arcsec²")
        }
        sb.appendLine("Coordinate (J2000):")
        sb.appendLine("  RA  ${t.ra}")
        sb.appendLine("  Dec ${t.dec}")

        targetInfoText.text = sb.toString()
    }

    // Adapter popup
    private class TargetSuggestionAdapter(
        private val context: Context,
        private var items: List<TargetObject>
    ) : BaseAdapter() {

        fun update(newItems: List<TargetObject>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): TargetObject = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_target_suggestion, parent, false)

            val t = items[position]

            view.findViewById<TextView>(R.id.suggestion_title).text =
                "${t.code} - ${t.name}"
            view.findViewById<TextView>(R.id.suggestion_constellation).text =
                t.constellation
            view.findViewById<ImageView>(R.id.suggestion_image)
                .setImageResource(R.drawable.ic_target_placeholder)

            return view
        }
    }

    // -------------------------------------------------
    // Config strumenti (gear)
    // -------------------------------------------------
    private fun showEquipmentConfigDialog() {
        val cfg = EquipmentManager.config
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_equipment_config, null)

        val primaryName = view.findViewById<EditText>(R.id.edit_primary_name)
        val primaryFocal = view.findViewById<EditText>(R.id.edit_primary_focal)
        val primaryRatio = view.findViewById<EditText>(R.id.edit_primary_ratio)
        val secondaryName = view.findViewById<EditText>(R.id.edit_secondary_name)
        val imagingCamera = view.findViewById<EditText>(R.id.edit_imaging_camera)
        val guideCamera = view.findViewById<EditText>(R.id.edit_guide_camera)

        primaryName.setText(cfg.primaryName)
        primaryFocal.setText(cfg.primaryFocalLengthMm.toString())
        primaryRatio.setText(cfg.primaryFocalRatio.toString())
        secondaryName.setText(cfg.secondaryName)
        imagingCamera.setText(cfg.imagingCamera)
        guideCamera.setText(cfg.guideCamera)

        AlertDialog.Builder(this)
            .setTitle("Configurazione strumenti")
            .setView(view)
            .setPositiveButton("Salva") { _, _ ->
                cfg.primaryName = primaryName.text.toString()
                cfg.primaryFocalLengthMm =
                    primaryFocal.text.toString().toDoubleOrNull()
                        ?: cfg.primaryFocalLengthMm
                cfg.primaryFocalRatio =
                    primaryRatio.text.toString().toDoubleOrNull()
                        ?: cfg.primaryFocalRatio
                cfg.secondaryName = secondaryName.text.toString()
                cfg.imagingCamera = imagingCamera.text.toString()
                cfg.guideCamera = guideCamera.text.toString()
            }
            .setNegativeButton("Annulla", null)
            .show()
    }

    // -------------------------------------------------
    // Stack suggestion
    // -------------------------------------------------
    private fun showStackSuggestion() {
        val target = currentSelectedTarget
        if (target == null) {
            targetStackText.text = "Seleziona prima un oggetto."
            return
        }
        val cfg = EquipmentManager.config
        val suggestion = computeStackSuggestion(target, cfg)

        val b = StringBuilder()
        b.appendLine("Stack suggerito per ${target.code} – ${target.name}")
        b.appendLine("Setup:")
        b.appendLine(
            "  Primario: ${
                cfg.primaryName.ifBlank { "n/d" }
            }  (f=${cfg.primaryFocalLengthMm}mm  f/${cfg.primaryFocalRatio})"
        )
        if (cfg.secondaryName.isNotBlank()) {
            b.appendLine("  Secondario: ${cfg.secondaryName}")
        }
        if (cfg.imagingCamera.isNotBlank()) {
            b.appendLine("  Camera CCD: ${cfg.imagingCamera}")
        }
        if (cfg.guideCamera.isNotBlank()) {
            b.appendLine("  Camera guida: ${cfg.guideCamera}")
        }
        b.appendLine()
        b.appendLine("Suggerimento stack:")
        b.appendLine(
            "  ${suggestion.numFrames} x ${suggestion.exposureSeconds}s  (~${suggestion.totalMinutes} min)"
        )
        b.appendLine()
        b.appendLine(suggestion.note)

        targetStackText.text = b.toString()
    }

    private fun computeStackSuggestion(
        target: TargetObject,
        cfg: EquipmentConfig
    ): StackSuggestion {
        val baseMag = 8.0
        val baseMinutes = 60.0

        val magFactor = Math.pow(10.0, (target.magnitude - baseMag) / 2.5)
        val ratioFactor = (cfg.primaryFocalRatio / 5.0) * (cfg.primaryFocalRatio / 5.0)
        var totalMinutes = baseMinutes * magFactor * ratioFactor
        totalMinutes = totalMinutes.coerceIn(20.0, 240.0)

        val typeLower = target.type.lowercase()
        var exposure = when {
            "nebulosa" in typeLower || "nebula" in typeLower -> 180
            "galassia" in typeLower || "galaxy" in typeLower -> 180
            "ammasso" in typeLower || "cluster" in typeLower -> 120
            else -> 120
        }

        if (cfg.primaryFocalRatio <= 4.0) {
            exposure = (exposure * 0.75).toInt().coerceAtLeast(60)
        }
        if (cfg.primaryFocalRatio >= 7.0) {
            exposure = (exposure * 1.3).toInt().coerceAtMost(600)
        }

        val numFrames = max(10, (totalMinutes * 60 / exposure).toInt())
        val roundedMinutes = (numFrames * exposure + 59) / 60

        val note =
            "Stima molto approssimativa basata su magnitudine, rapporto focale e integrazione totale desiderata."

        return StackSuggestion(
            exposureSeconds = exposure,
            numFrames = numFrames,
            totalMinutes = roundedMinutes,
            note = note
        )
    }
}

// Fuori dalla classe
data class StackSuggestion(
    val exposureSeconds: Int,
    val numFrames: Int,
    val totalMinutes: Int,
    val note: String
)
