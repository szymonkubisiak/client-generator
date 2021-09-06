package models

sealed class Struct {
	abstract val type: RefTypeDescr
}

class StructActual (
	override val type: RefTypeDescr,
	val fields: List<Field>,
	val description: String?,
): Struct(){
	override fun toString(): String {
		return "Struct " + type.toString()
	}
}

class StructEnum (
	override val type: RefTypeDescr,
	val transportType: BuiltinTypeDescr,
	val values: List<String>,
	val description: String?,
): Struct() {
	override fun toString(): String {
		return "Enum " + type.toString()
	}
}
