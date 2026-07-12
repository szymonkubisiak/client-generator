package models

/**
 * Artificial endpoint group: endpoints split by access level (JWT presence)
 * rather than by a tag from the spec
 */
data class AccessGroup(override val key: String) : EndpointGroup {
	override fun toString() = key
}
