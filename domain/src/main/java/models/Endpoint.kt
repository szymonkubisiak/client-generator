package models

class Endpoint(
	val name: String,
	val path: String,
	val tags: List<Tag>,
	val operation: String,
	val params: List<Param>,
	val response: Field?, //TODO: create a better suiting type
	val mediaType: String?,
	val security: List<Security>?,
) : EndpointGroup {
	override val key = name
}

class Param(
	override val transportName: String,
	override val type: TypeDescr,
	override val isArray: Boolean,
	override val mandatory: Boolean,
	override val description: String?,
	val location: Location,
) : IField {
	sealed class Location {
		object PATH : Location()
		object QUERY : Location()
		data class BODY(val mediaType: String) : Location()
		object HEADER : Location()
		object COOKIE : Location()
	}
}
