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
	val description: String?,
	val deprecated: String?,
) : EndpointGroup {
	override val key = name

	fun isWwwForm(): Boolean {
			return params.any(Param::isWwwForm)
		}
}

