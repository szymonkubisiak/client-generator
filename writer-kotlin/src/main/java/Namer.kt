import models.*

/**
 * This is the central authority on what names structs and adapters will end up with
 */
object Namer {

	fun TypeDescr.adapterT2DName() = "%sTransportToDomainConverter".format(this.key)
	fun TypeDescr.adapterD2TName() = "%sDomainToTransportConverter".format(this.key)
	fun TypeDescr.adapterD2MapName() = "%sDomainToMapConverter".format(this.key)

	fun TypeDescr.transportFinalName(): String {
		return when (this) {
			is RefTypeDescr ->
				when (val definition = this.definition!!) {
					is StructActual -> this.key + "Pojo"
					is StructEnum -> TypeResolver.instance.resolveTransportType(definition.transportType)
				}
			is BuiltinTypeDescr -> TypeResolver.instance.resolveTransportType(this)
		}
	}

	fun TypeDescr.domainFinalName():String {
		return when (this) {
			is BuiltinTypeDescr -> TypeResolver.instance.resolveDomainType(this)
			is RefTypeDescr -> this.key
		}
	}

	fun IField.domainFinalName():String {
		return when {
			isArray -> "List<${type.domainFinalName()}>"
			isStringmap -> "Map<String, ${type.domainFinalName()}>"
			else -> type.domainFinalName()
		}
	}

	fun IField.transportFinalName():String {
		return when {
			isArray -> "List<${type.transportFinalName()}>"
			isStringmap -> "Map<String, ${type.transportFinalName()}>"
			else -> type.transportFinalName()
		}
	}

	fun EndpointGroup.serviceClassName() = this.key.capitalize() + "Service"
	fun EndpointGroup.serviceMethodName() = this.key.decapitalize()

	fun EndpointGroup.repoClassName() = this.key.capitalize() + "Repo"
	fun EndpointGroup.repoMethodName() = this.key.decapitalize()

	fun EndpointGroup.usecaseClassName() = this.key.capitalize() + "UC"
	fun EndpointGroup.usecaseMethodName() = this.key.decapitalize()

	fun kotlinizeVariableName(name: String) = if(isValidVariableName(name)) name else "`$name`"
	fun isValidVariableName(name: String) = name.matches(variableNameRegex)

	private val variableNameRegex = "^[a-zA-Z_$][a-zA-Z_$0-9]*$".toRegex()
}