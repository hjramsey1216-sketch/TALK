package com.example.imessageclone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * FUN FONT SETUP
 * --------------
 * `FontFamily.Cursive` below is a real, built-in Android font family (maps to
 * "Comic Neue" style fonts on most devices) — it compiles and runs right now,
 * no extra setup.
 *
 * For something bouncier — Fredoka, Baloo 2, Nunito, etc — the safe way to
 * pull in a Google Font is Android Studio's built-in wizard, NOT hand-written
 * XML: it generates the provider certificate file for you correctly, which
 * is fiddly/error-prone to write by hand.
 *
 *   1. Right-click res/ -> New -> Other -> Google Font (or open the Resource
 *      Manager panel, click "+", choose "Add Font to Project").
 *   2. Search "Fredoka" (or whichever font), select weights, click OK. This
 *      creates res/font/fredoka.xml AND res/values/font_certs.xml correctly.
 *   3. Replace FontFamily.Cursive below with:
 *
 *      val Fredoka = FontFamily(Font(R.font.fredoka))
 *
 *      ...and use `Fredoka` instead of `FunFont` in the TextStyles below.
 */
val FunFont: FontFamily = FontFamily.Cursive

val TalkTypography = Typography(
    titleLarge = TextStyle(fontFamily = FunFont, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    titleMedium = TextStyle(fontFamily = FunFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = FunFont, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FunFont, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = FunFont, fontWeight = FontWeight.Medium, fontSize = 11.sp)
)
