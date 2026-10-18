package app.podium.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.podium.R
import app.podium.data.Ring
import app.podium.domain.Medal

/**
 * Munich, summer 1972: white paper, a light-blue sky, green grass and silver, pictograms in thick
 * strokes, a sans-serif in tidy rows. No red and no black. Gold, silver and bronze only for medals.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val accent: Color,
    val accentSoft: Color,
    val ghost: Color,
    val ahead: Color,
    val aheadSoft: Color,
    val behind: Color,
    val behindSoft: Color,
    val track: Color,
    val grass: Color,
    val gold: Color,
    val silver: Color,
    val bronze: Color,
)

val MunichPalette = Palette(
    bg = Color(0xFFF5F4EF),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEDF1F3),
    line = Color(0xFFDCE2E6),
    ink = Color(0xFF17242E),
    muted = Color(0xFF56646E),
    faint = Color(0xFF8E9BA4),
    accent = Color(0xFF1C88CC),
    accentSoft = Color(0xFFE1EFF9),
    ghost = Color(0xFFA3AEB6),
    ahead = Color(0xFF2B9653),
    aheadSoft = Color(0xFFE2F3E7),
    behind = Color(0xFFDE6A2E),
    behindSoft = Color(0xFFFBEADF),
    track = Color(0xFF7FBCE6),
    grass = Color(0xFF7CC08A),
    gold = Color(0xFFD4A11E),
    silver = Color(0xFFA2ACB4),
    bronze = Color(0xFFBF7640),
)

/** Each part of life has the colour of one of the Games' sports. */
fun Ring.color(): Color = when (this) {
    Ring.HOME -> Color(0xFF2B92D4)
    Ring.BODY -> Color(0xFF3C9D50)
    Ring.WORK -> Color(0xFFEA8424)
    Ring.STUDY -> Color(0xFF7C5DB6)
    Ring.SELF -> Color(0xFF169E9B)
    Ring.OTHER -> Color(0xFF86929A)
}

@Composable
fun Medal.color(): Color = when (this) {
    Medal.GOLD -> U.c.gold
    Medal.SILVER -> U.c.silver
    Medal.BRONZE -> U.c.bronze
}

val LocalPalette = staticCompositionLocalOf { MunichPalette }

@OptIn(ExperimentalTextApi::class)
private fun inter(res: Int, w: Int) = Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))

/** Inter for reading. */
val Sans = FontFamily(inter(R.font.inter, 400), inter(R.font.inter, 500), inter(R.font.inter, 600), inter(R.font.inter, 700))

/** Inter Tight for the clock, the numbers and the titles: close-set like the boards in the stadium. */
val Tight = FontFamily(inter(R.font.inter_tight, 500), inter(R.font.inter_tight, 600), inter(R.font.inter_tight, 700), inter(R.font.inter_tight, 800))

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(24.dp)
    val RLg = RoundedCornerShape(18.dp)
    val RMd = RoundedCornerShape(14.dp)
    val RSm = RoundedCornerShape(10.dp)
}

/** Every number lines up: the clock mustn't jitter as its digits change. */
private const val TNUM = "tnum, lnum"

object UType {
    val Clock: TextStyle @Composable get() = TextStyle(fontFamily = Tight, fontWeight = FontWeight.Bold, fontSize = 76.sp, lineHeight = 80.sp, letterSpacing = (-0.02).em, color = U.c.ink, fontFeatureSettings = TNUM)
    val Hero: TextStyle @Composable get() = TextStyle(fontFamily = Tight, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-0.02).em, color = U.c.ink, fontFeatureSettings = TNUM)
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Tight, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 34.sp, letterSpacing = (-0.01).em, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Tight, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 27.sp, color = U.c.ink)
    val Name: TextStyle @Composable get() = TextStyle(fontFamily = Tight, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 22.sp, color = U.c.ink)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.12.em, color = U.c.faint)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Tight, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = U.c.ink, fontFeatureSettings = TNUM)
    val Time: TextStyle @Composable get() = TextStyle(fontFamily = Tight, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = U.c.ink, fontFeatureSettings = TNUM)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
}

@Composable
fun PodiumTheme(content: @Composable () -> Unit) {
    val p = MunichPalette
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.accent,
                onPrimary = Color.White,
                primaryContainer = p.accentSoft,
                onPrimaryContainer = p.ink,
                secondary = p.grass,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.behind,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
