package ${packageName}.presentation

import kotlinx.serialization.Serializable

/** ${pascal} module navigation routes. */
sealed interface ${pascal}NavigationRoute {

    @Serializable
    object ${pascal} : ${pascal}NavigationRoute
}
