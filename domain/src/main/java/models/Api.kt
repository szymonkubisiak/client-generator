package models

class Api(
	val structs: List<Struct>,
	val paths: List<Endpoint>,
) {

	val incoming = ArrayList<RefTypeDescr>()
	val outgoing = ArrayList<RefTypeDescr>()

	init {
		if (paths != null) {
			paths.forEach { endpoint ->

				(endpoint.response as? RefTypeDescr)?.also { incoming.add(it) }

				endpoint.params
					.map { it.type }
					.filterIsInstance<RefTypeDescr>()
					.forEach { outgoing.add(it) }

			}
		}
	}
}