package models

/**
 * Type descriptor.
 * Describes a reference to an unique Type that results from definition of Field or a Struct
 */
sealed class TypeDescr {
	abstract val key: String

	override fun toString(): String {
		return key
	}
}

/**
 * Describes a type that is defined by a transport format alone,
 * or a type that's derived from it.
 * Basically, anything that won't be generated as model
 * for JSON those are: string, number, integer, boolean
 */
data class BuiltinTypeDescr internal constructor(override val key: String, var format: String?): TypeDescr()

/**
 * Describes a type that is defined in a given spec and has both transport and domain forms
 */
data class RefTypeDescr internal constructor (override val key: String): TypeDescr() {
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

	fun getRefType(type: String) : RefTypeDescr {
		val newType = RefTypeDescr(type)
		return canonicalize(newType)
	}
}