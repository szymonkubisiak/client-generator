package models

class Security constructor(
	override val key: String,
	override val location: Param.Location,
) : IParam {
	override val type: TypeDescr
		get() = BuiltinTypeDescr("string", null)
	override val isArray: Boolean = false
	override val mandatory: Boolean = true
	override val description: String? = null
}