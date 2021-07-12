package models

data class Field (
    val transportName: String,
    val type: TypeDescr,
    val isArray: Boolean,
    val mandatory: Boolean,
    val description: String?
)