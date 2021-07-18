import models.TypeDescr

/**
 * This is the central authority on what names structs and adapters will end up with
 */
object Namer {
	private const val T2DAdapterTemplate = "%sTransportToDomainAdapter"
	private fun getAdapterT2DName(name: String) = T2DAdapterTemplate.format(name)

	fun TypeDescr.adapterT2DName() = getAdapterT2DName(this.transportName)
	fun TypeDescr.transportFinalName() = this.transportName// + "Tra"
	fun TypeDescr.domainFinalName() = this.transportName// + "Dom"
}