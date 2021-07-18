package models

class Struct(
	val type: StructTypeDescr,
	val fields: List<Field>,
	val description: String?,
) {
	val transportName = type.transportName
}