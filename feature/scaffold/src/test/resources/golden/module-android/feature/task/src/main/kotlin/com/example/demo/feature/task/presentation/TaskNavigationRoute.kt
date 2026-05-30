package com.example.demo.feature.task.presentation

import kotlinx.serialization.Serializable

/** Task module navigation routes. */
sealed interface TaskNavigationRoute {

    @Serializable
    object Task : TaskNavigationRoute
}
