package models

///**
// * Type descriptor.
// * Describes a unique Type that results from definition of Field,
// */
//
//interface TypeDescr
//
///**
// * Describes a type that is defined by a transport format alone,
// * or a type that's derived from it.
// * Basically, anything that uses a simple type for transport
// */
//data class BuiltinTypeDescr (val transportName: String): TypeDescr {
//
//}
//
///**
// * Describes a type that is defined in a given spec.
// *
// */
//data class ComplexTypeDescr (val transportName: String, var transportType: TypeDescr): TypeDescr {
//
//}

data class TypeDescr internal constructor(val transportName: String, var format: String?) {

//	constructor(type: String, format: String?) : this(type, null) {
//
//	}
	private val dbg = getCounter()

	companion object{
		private var counter = 0
		private fun getCounter() = counter++
	}
}


class TypeDescrFactory {
	val uniqueTypes = HashMap<TypeDescr, TypeDescr>()

	fun getType(type: String, format: String?) : TypeDescr {
		val newType = TypeDescr(type, format)
		val retval = uniqueTypes.computeIfAbsent(newType) { k -> k }
		return retval
	}
}