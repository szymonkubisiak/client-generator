package models

class Api(
	val structs: List<Struct>,
	val paths: List<Endpoint>,
) {

	init {
		if (paths != null) {
			paths.forEach { endpoint ->

				(endpoint.response?.type as? RefTypeDescr)?.definition?.markIncoming()

				endpoint.params
					.map { it.type }
					.filterIsInstance<RefTypeDescr>()
					.forEach {
						if (endpoint.isWwwForm())
							it.definition?.markOutgoingAsForm()
						else
							it.definition?.markOutgoing()
					}
			}
		}
	}

	companion object {
		fun Struct.markIncoming() {
			this.incoming = true
			(this as? StructActual)?.fields
				?.forEach {
					(it.type as? RefTypeDescr)?.definition?.markIncoming()
				}
		}

		fun Struct.markOutgoing() {
			this.outgoing = true
			(this as? StructActual)?.fields
				?.forEach {
					(it.type as? RefTypeDescr)?.definition?.markOutgoing()
				}
		}

		fun Struct.markOutgoingAsForm() {
			this.outgoingAsForm = true
			//Outgoing as PostForm cannot include to objects
		}
	}
}