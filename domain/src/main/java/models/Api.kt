package models

class Api(
	val structs: List<Struct>,
	val paths: List<Endpoint>,
) {

	init {
		structs
			.filterIsInstance<StructActual>()
			.forEach {
				it.incoming = false
				it.outgoing = false
				it.outgoingAsForm = false
			}
		paths.forEach { endpoint ->
			getStruct(endpoint.response?.type)?.markIncoming()
			endpoint.params
				.map { it.type }
				.filterIsInstance<RefTypeDescr>()
				.forEach {
					if (endpoint.isWwwForm())
						getStruct(it)?.markOutgoingAsForm()
					else
						getStruct(it)?.markOutgoing()
				}
		}
	}

	fun getStruct(key: String) : Struct? {
		return structs.firstOrNull { it.key == key }
	}

	fun getStruct(refType: RefTypeDescr?) : Struct? {
		refType ?: return null
		return getStruct(refType.key)
	}

	fun getStruct(anyType: TypeDescr?): Struct? {
		return getStruct(anyType as? RefTypeDescr)
	}

	private fun Struct.markIncoming() {
		this.incoming = true
		(this as? StructActual)?.fields
			?.forEach {
				getStruct(it.type)?.markIncoming()
			}
	}

	private fun Struct.markOutgoing() {
		this.outgoing = true
		(this as? StructActual)?.fields
			?.forEach {
				getStruct(it.type)?.markOutgoing()
			}
	}

	private fun Struct.markOutgoingAsForm() {
		this.outgoingAsForm = true
		//Outgoing as PostForm cannot include to objects
	}
}