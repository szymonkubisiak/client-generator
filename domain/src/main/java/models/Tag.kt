package models

data class Tag(override val key: String) : EndpointGroup {
	override fun toString() = key
}