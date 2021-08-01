package models

class Api(
	val structs: List<Struct>,
	val paths: List<Endpoint>? = null,
) {

	val incoming = ArrayList<StructTypeDescr>()
	val outgoing = ArrayList<StructTypeDescr>()

	init {
		if (paths != null) {
			paths.forEach { endpoint ->

				(endpoint.response as? StructTypeDescr)?.also { incoming.add(it) }

				endpoint.params
					.map { it.type }
					.filterIsInstance<StructTypeDescr>()
					.forEach { outgoing.add(it) }

			}
		}
	}
}