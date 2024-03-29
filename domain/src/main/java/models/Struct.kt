package models

sealed class Struct {
	abstract val type: RefTypeDescr
	val key: String
		get () = type.key

	var incoming = false
	var outgoing = false
	var outgoingAsForm = false
}

data class StructActual constructor(
	override val type: RefTypeDescr,
	val fields: List<Field>,
	val description: String?,
	val artificialID: BuiltinTypeDescr? = null,
) : Struct() {
	override fun toString(): String {
		return "Struct " + type.toString()
	}
}

data class StructEnum constructor(
	override val type: RefTypeDescr,
	val transportType: BuiltinTypeDescr,
	val values: List<String>,
	val description: String?,
) : Struct() {
	override fun toString(): String {
		return "Enum " + type.toString()
	}
}
