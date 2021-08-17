import models.*

/**
 * This is the central authority on what names structs and adapters will end up with
 */
object Namer {
	private const val T2DAdapterTemplate = "%sTransportToDomainConverter"
	private fun getAdapterT2DName(name: String) = T2DAdapterTemplate.format(name)

	fun TypeDescr.adapterT2DName() = getAdapterT2DName(this.key)
	fun TypeDescr.transportFinalName(): String {
		return when (this) {
			is StructTypeDescr ->
				when (val definition = this.definition!!) {
					is StructActual -> this.key + "Pojo"
					is StructEnum -> TypeResolver.instance.resolveTransportType(definition.transportType)
				}
			is BuiltinTypeDescr -> TypeResolver.instance.resolveTransportType(this)
		}
	}

	fun StructTypeDescr.domainFinalName() = this.key// + "Dom"

	fun TypeDescr.domainFinalName():String {
		return when (this) {
			is BuiltinTypeDescr -> TypeResolver.instance.resolveDomainType(this)
			is StructTypeDescr -> this.domainFinalName()
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