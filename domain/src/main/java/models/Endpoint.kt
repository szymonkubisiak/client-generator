package models

class Endpoint (
	val name: String,
	val path: String,
	val operation: String,
	val params: List<Param>,
	val response: TypeDescr?,
	val mediaType: String?,
		)

class Param (
	override val transportName: String,
	override val type: TypeDescr,
	override val isArray: Boolean,
	override val mandatory: Boolean,
	override val description: String?,
	val location: Location,
) : IField{
	enum class Location {
		PATH,
		QUERY,
		BODY,
		HEADER,
	}
}
