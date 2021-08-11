package models

/**
 * Type descriptor.
 * Describes a unique Type that results from definition of Field or a Struct
 */
sealed class TypeDescr {
	abstract val name: String
}

/**
 * Describes a type that is defined by a transport format alone,
 * or a type that's derived from it.
 * Basically, anything that uses a simple type for transport
 * for JSON those are: string, number, integer, boolean
 */
data class BuiltinTypeDescr internal constructor(override val name: String, var format: String?): TypeDescr()

/**
 * Describes a type that is defined in a given spec.
 */
data class StructTypeDescr (override val name: String): TypeDescr() {
	var definition: Struct? = null
}


class TypeDescrFactory {
	val uniqueTypes = HashMap<TypeDescr, TypeDescr>()

//	private fun canonicalize(newType: TypeDescr) : TypeDescr {
//		val retval = uniqueTypes.computeIfAbsent(newType) { k -> k }
//		return retval
//	}

	private fun <T:TypeDescr> canonicalize(newType: T) : T {
		val retval = uniqueTypes.computeIfAbsent(newType) { k -> k }
		return retval as T
	}

	fun getSimpleType(type: String, format: String?) : BuiltinTypeDescr {
		val newType = BuiltinTypeDescr(type, format)
		return canonicalize(newType)
	}

	fun getRefType(type: String) : StructTypeDescr {
		val newType = StructTypeDescr(type)
		return canonicalize(newType)
	}
}