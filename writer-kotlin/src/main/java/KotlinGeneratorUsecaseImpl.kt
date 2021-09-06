import KotlinGeneratorRepoImpl.SortedSecurities
import Namer.domainFinalName
import Namer.kotlinizeVariableName
import Namer.repoClassName
import Namer.repoMethodName
import Namer.usecaseClassName
import Namer.usecaseMethodName
import models.*
import utils.PackageConfig


@Suppress("NAME_SHADOWING")
class KotlinGeneratorUsecaseImpl(
	pkg: PackageConfig,
	val domain: PackageConfig,
	val ucDefs: PackageConfig,
	val repoDefs: PackageConfig,
) : KotlinGeneratorBaseEndpoints(pkg) {

	override fun fileName(endpoint: EndpointGroup): String = endpoint.usecaseClassName() + "Impl"

	override fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.Single")
		writer.writeLine("import io.reactivex.Completable")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("import " + ucDefs.toPackage() + ".*")
		writer.writeLine("import " + repoDefs.toPackage() + ".*")
		writer.writeLine("import javax.inject.Inject")
		writer.writeLine("")

		writer.writeLine("class " + groupName.usecaseClassName() + "Impl @Inject constructor(val repo: "+groupName.repoClassName()+") : " + groupName.usecaseClassName() + "{")
		IndentedWriter(writer).use { writer ->
			endpoints.forEach { endpoint ->
				writeEndpointMethod(writer, endpoint)
			}
		}
		writer.writeLine("}")
	}

	private fun writeEndpointMethod(writer: IndentedWriter, endpoint: Endpoint) {
		writer.writeLine("")

		writer.writeLine("override fun " + endpoint.usecaseMethodName() + "(")
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
				val name = param.transportName

				val type = param.type.domainFinalName() + if (!param.mandatory) "?" else ""

				writer.writeLine("$name: $type,")
			}
		}
		endpoint.response?.also {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			writer.writeLine("): Single<$type>{")
		} ?: run {
			writer.writeLine("): Completable{")
		}
		IndentedWriter(writer).use { writer ->
			writer.writeLine("return repo." + endpoint.repoMethodName() + "(")

			val sortedSecurities = endpoint.security?.let(::SortedSecurities)
			sortedSecurities?.passed?.forEach { security ->
				val name = kotlinizeVariableName(security.key)
				val type = "String"
				if (endpoint.params.any { param -> param.transportName == security.key }) {
					writer.writeLine("//WARNING: security clashes with param:")
					writer.writeLine("//$name,")
				} else {
					writer.writeLine("$name,")
				}
			}
			for (param in endpoint.params) {
				val name = param.transportName
				writer.writeLine("$name,")
			}
			writer.writeLine(")")
		}
		writer.writeLine("}")
	}
}