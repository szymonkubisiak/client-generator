package models

interface IField {
	val key: String
	val type: TypeDescr
	val isArray: Boolean
	val mandatory: Boolean
	val description: String?
}

class Field(
	override val key: String,
	override val type: TypeDescr,
	override val isArray: Boolean,
	override val mandatory: Boolean,
	override val description: String?
) : IField