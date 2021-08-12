package models

sealed class Struct {
	abstract val type: StructTypeDescr
}

class StructActual (
	override val type: StructTypeDescr,
	val fields: List<Field>,
	val description: String?,
): Struct()

class StructEnum (
	override val type: StructTypeDescr,
	val transportType: BuiltinTypeDescr,
	val values: List<String>,
	val description: String?,
): Struct()
