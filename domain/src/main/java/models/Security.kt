package models

class Security(
	override val key: String,
	val location: Param.Location,
) : IField {
	override val type: TypeDescr
		get() = BuiltinTypeDescr("string", null)
	override val isArray: Boolean = false
	override val mandatory: Boolean = true
	override val description: String? = null
}