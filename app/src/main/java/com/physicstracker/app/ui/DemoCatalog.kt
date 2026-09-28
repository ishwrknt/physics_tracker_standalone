package com.physicstracker.app.ui

/** Explicit catalogue: no network downloads and no inferred measurement calibration. */
data class DemoMedia(
    val file: String,
    val title: String,
    val light: Boolean,
    val converted: Boolean = false,
    val guided: Boolean = false
) {
    val video: Boolean get() = !file.endsWith(".jpg")
    val asset: String get() = if (guided) "demos/$file" else "demos/supplied/$file"
    val projectName: String get() = "Demo — $title" + if (converted) " (converted)" else ""
    val sourceNote: String get() = when {
        guided -> "Synthetic teaching clip; not experimental evidence."
        converted -> "Converted teaching clip: Sorenson source transcoded to H.264; compression changes pixel values. Not an original intensity reference."
        else -> "User-supplied teaching media; no known physical calibration."
    }
    val instruction: String get() = if (light)
        "Choose Point, Line or Box, then Measure profile" + if (video) " or Start time measurement." else "."
        else "Tap a target, then use manual or automatic tracking. Calibrate only when you know a real distance."
}

object DemoCatalog {
    val items = listOf(
        DemoMedia("light-pulse.mp4", "Guided light profile and pulse", true, guided = true),
        DemoMedia("barPedulum.mp4", "Bar pendulum", false),
        DemoMedia("pendulum.mp4", "Pendulum · portrait", false),
        DemoMedia("classic_pendulum.mp4", "Pendulum · classic", false, true),
        DemoMedia("BallTossOut.mp4", "Ball toss", false),
        DemoMedia("PucksCollide.mp4", "Colliding pucks", false, true),
        DemoMedia("CupsClips.mp4", "Cups and clips", false, true),
        DemoMedia("ball_oil.mp4", "Ball in oil", false),
        DemoMedia("bouncing_cart.mp4", "Bouncing cart", false),
        DemoMedia("cart_pendulum.mp4", "Cart pendulum", false),
        DemoMedia("parachute_monkey.mp4", "Parachute", false),
        DemoMedia("pendulum_drag.mp4", "Pendulum with drag", false),
        DemoMedia("spring_wars.mp4", "Springs", false),
        DemoMedia("baseline_filters_gray.jpg", "Filter baseline · image", true),
        DemoMedia("baseline_gas_color.jpg", "Gas baseline · image", true),
        DemoMedia("3000K_fluor_lamp_gray.mp4", "Fluorescent lamp · 3000 K", true, true),
        DemoMedia("6300K_fluor_lamp_gray.mp4", "Fluorescent lamp · 6300 K", true, true),
        DemoMedia("60Wsoft_60V_gray.mp4", "60 W lamp · 60 V", true, true),
        DemoMedia("60Wsoft_filter_green139_gray.mp4", "Lamp · green filter", true, true),
        DemoMedia("60Wsoft_filter_pink128_gray.mp4", "Lamp · pink filter", true, true),
        DemoMedia("60Wsoft_thermal_color.mp4", "Thermal spectrum · colour", true, true),
        DemoMedia("60Wsoft_thermal_gray.mp4", "Thermal spectrum · grayscale", true, true),
        DemoMedia("fluor_green_532_color_60.mp4", "Green fluorescence · 532 nm", true, true),
        DemoMedia("fluor_red_532_color_80.mp4", "Red fluorescence · 532 nm", true, true),
        DemoMedia("fluor_red_uv_color.mp4", "Red fluorescence · UV", true, true),
        DemoMedia("gas_H.mp4", "Hydrogen spectrum", true, true),
        DemoMedia("gas_He.mp4", "Helium spectrum", true, true),
        DemoMedia("gas_Hg.mp4", "Mercury spectrum", true, true),
        DemoMedia("slits_2m_3_color.mp4", "Slit pattern", true, true),
        DemoMedia("sun_gray.mp4", "Sun spectrum", true, true)
    )
}
