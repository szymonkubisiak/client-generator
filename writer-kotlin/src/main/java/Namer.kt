import models.BuiltinTypeDescr
import models.StructTypeDescr
import models.TypeDescr

/**
 * This is the central authority on what names structs and adapters will end up with
 */
object Namer {
	private const val T2DAdapterTemplate = "%sTransportToDomainConverter"
	private fun getAdapterT2DName(name: String) = T2DAdapterTemplate.format(name)

	fun TypeDescr.adapterT2DName() = getAdapterT2DName(this.transportName)
	fun TypeDescr.transportFinalName(): String {
		return when (this) {
			is StructTypeDescr -> this.transportName + "Pojo"
			is BuiltinTypeDescr -> TypeResolver.instance.resolveTransportType(this)
		}
	}

	fun TypeDescr.domainFinalName() = this.transportName// + "Dom"
}