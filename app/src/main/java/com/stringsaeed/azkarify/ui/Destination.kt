package com.stringsaeed.azkarify.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable data object Home : Destination

    @Serializable data object Favorites : Destination

    @Serializable data object Counter : Destination

    @Serializable data object Quick : Destination

    @Serializable data object Settings : Destination

    @Serializable data object Journeys : Destination

    @Serializable data object Prayer : Destination

    @Serializable data class Category(val id: Int) : Destination

    @Serializable data class Journey(val id: String) : Destination
}

@Composable
internal fun rememberDestinations(): NavBackStack<Destination> =
    rememberSerializable(serializer = NavBackStackSerializer(Destination.serializer())) {
        NavBackStack(Destination.Home)
    }
