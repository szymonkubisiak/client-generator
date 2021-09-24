import KotlinGeneratorRepoImpl.SortedSecurities
import Namer.domainFinalName
import Namer.kotlinizeVariableName
import Namer.repoMethodName
import Namer.usecaseClassName
import models.*
import utils.PackageConfig


@Suppress("NAME_SHADOWING")
class KotlinGeneratorUsecase(
	pkg: PackageConfig,
	val domain: PackageConfig,
) : KotlinGeneratorBaseEndpoints(pkg) {

	override fun fileName(endpoint: EndpointGroup): String = endpoint.usecaseClassName()

	override fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.Single")
		writer.writeLine("import io.reactivex.Completable")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("")

		writer.writeLine("interface " + groupName.usecaseClassName() + " {")
		IndentedWriter(writer).use { writer ->
			endpoints.forEach { endpoint ->
				writeEndpointMethod(writer, endpoint)
			}
		}
		writer.writeLine("}")
	}

	private fun writeEndpointMethod(writer: IndentedWriter, endpoint: Endpoint) {
		writer.writeLine("")

		writer.writeLine("fun " + endpoint.repoMethodName() + "(")
		IndentedWriter(writer).use { writer ->
			val sortedSecurities = endpoint.security?.let(::SortedSecurities)
			sortedSecurities?.passed?.forEach { security ->
				val name = kotlinizeVariableName(security.key)
				val type = "String"
				if (endpoint.params.any { param -> param.transportName == security.key }) {
					writer.writeLine("//WARNING: security clashes with param:")
					writer.writeLine("//$name: $type,")
				} else {
					writer.writeLine("$name: $type,")
				}
			}
			for (param in endpoint.params) {
				val name = kotlinizeVariableName(param.transportName)

				val type = param.type.domainFinalName() + if (!param.mandatory) "? = null" else ""

				writer.writeLine("$name: $type,")
			}
		}
		endpoint.response?.also {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			writer.writeLine("): Single<$type>")
		} ?: run {
			writer.writeLine("): Completable")
		}
	}
}