package models

interface IParam : IField {
	val location: Param.Location

	fun isWwwForm(): Boolean {
		val bodyParam = location as? Param.Location.BODY ?: return false
		return bodyParam.mediaType == mediaTypeWwwForm
	}
}

private const val mediaTypeWwwForm = "application/x-www-form-urlencoded"
