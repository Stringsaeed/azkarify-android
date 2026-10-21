package sh.saeed.azkarify.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import sh.saeed.azkarify.R

/**
 * Material Symbols Rounded (weight 400) imported as VectorDrawables by
 * `scripts/import_symbols.py`. Directional symbols mirror in right-to-left layouts.
 */
object AzSymbol {
    val today = R.drawable.sym_routine
    val todayFilled = R.drawable.sym_routine_fill
    val azkar = R.drawable.sym_menu_book
    val azkarFilled = R.drawable.sym_menu_book_fill
    val counter = R.drawable.sym_adjust
    val counterFilled = R.drawable.sym_adjust_fill
    val settings = R.drawable.sym_settings
    val settingsFilled = R.drawable.sym_settings_fill

    val star = R.drawable.sym_star
    val starFilled = R.drawable.sym_star_fill
    val location = R.drawable.sym_location_on
    val myLocation = R.drawable.sym_my_location
    val schedule = R.drawable.sym_schedule
    val check = R.drawable.sym_check
    val search = R.drawable.sym_search
    val more = R.drawable.sym_more_vert
    val back = R.drawable.sym_arrow_back
    val forward = R.drawable.sym_arrow_forward
    val chevron = R.drawable.sym_chevron_right
    val textSize = R.drawable.sym_format_size
    val focus = R.drawable.sym_fullscreen
    val close = R.drawable.sym_close
    val add = R.drawable.sym_add
    val remove = R.drawable.sym_remove
    val expand = R.drawable.sym_keyboard_arrow_down
    val notifications = R.drawable.sym_notifications
    val language = R.drawable.sym_language
    val palette = R.drawable.sym_palette
    val darkMode = R.drawable.sym_dark_mode
    val lightMode = R.drawable.sym_light_mode
    val info = R.drawable.sym_info
    val reset = R.drawable.sym_restart_alt
    val privacy = R.drawable.sym_shield
    val policy = R.drawable.sym_policy
    val favorite = R.drawable.sym_favorite
    val sunrise = R.drawable.sym_wb_twilight
    val sunny = R.drawable.sym_wb_sunny
    val door = R.drawable.sym_door_front
    val night = R.drawable.sym_bedtime
    val dua = R.drawable.sym_volunteer_activism

    /** Category symbols, by Hisn al-Muslim category id (design README, Iconography). */
    @DrawableRes
    fun category(id: Int): Int =
        when (id) {
            1, 68 -> R.drawable.sym_wb_twilight
            in 2..5 -> R.drawable.sym_apparel
            6, 7 -> R.drawable.sym_door_open
            8, 9 -> R.drawable.sym_water_drop
            10, 11 -> R.drawable.sym_home
            in 12..14 -> R.drawable.sym_mosque
            15 -> R.drawable.sym_campaign
            27 -> R.drawable.sym_routine
            in 28..31, 67 -> R.drawable.sym_bedtime
            in 34..46 -> R.drawable.sym_cloud
            47, 48 -> R.drawable.sym_family_restroom
            in 49..51 -> R.drawable.sym_medical_services
            in 52..60 -> R.drawable.sym_spa
            61 -> R.drawable.sym_air
            62 -> R.drawable.sym_thunderstorm
            in 63..66 -> R.drawable.sym_rainy
            in 69..76 -> R.drawable.sym_restaurant
            77, 78 -> R.drawable.sym_sick
            79, 80 -> R.drawable.sym_favorite
            in 95..105 -> R.drawable.sym_luggage
            in 115..125 -> R.drawable.sym_deployed_code
            else -> R.drawable.sym_volunteer_activism
        }
}

/** One symbol at its optical size; `contentDescription` null for decorative use. */
@Composable
fun AzIcon(
    @DrawableRes symbol: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tint: Color = LocalContentColor.current,
) {
    Icon(painterResource(symbol), contentDescription, modifier.size(size), tint = tint)
}
