package models

class Param constructor(
	override val key: String,
	override val type: TypeDescr,
	override val isArray: Boolean,
	override val mandatory: Boolean,
	override val description: String?,
	override val location: Location,
	override val isStringmap: Boolean = false,
	val defaultValue: Any? = null,
) : IParam {
	sealed class Location {
		object PATH : Location()
		object QUERY : Location()
		data class BODY(val mediaType: String) : Location()
		object HEADER : Location()
		object COOKIE : Location()
	}
}