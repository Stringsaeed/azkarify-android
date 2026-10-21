package sh.saeed.azkarify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable data object Today : Destination

    @Serializable data object Azkar : Destination

    @Serializable data object Counter : Destination

    @Serializable data object Settings : Destination

    @Serializable data object PrayerTimes : Destination

    @Serializable data class Category(val id: Int) : Destination

    @Serializable data class Journey(val id: String) : Destination
}

/** The four tabs; each keeps its own back stack. */
enum class Tab(val root: Destination) {
    Today(Destination.Today),
    Azkar(Destination.Azkar),
    Counter(Destination.Counter),
    Settings(Destination.Settings),
}

@Composable
internal fun rememberDestinations(root: Destination): NavBackStack<Destination> =
    rememberSerializable(root, serializer = NavBackStackSerializer(Destination.serializer())) {
        NavBackStack(root)
    }
