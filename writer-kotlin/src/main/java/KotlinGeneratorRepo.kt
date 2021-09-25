import Namer.domainFinalName
import Namer.kotlinizeVariableName
import Namer.repoClassName
import Namer.repoMethodName
import models.*
import utils.PackageConfig


@Suppress("NAME_SHADOWING")
class KotlinGeneratorRepo(
	pkg: PackageConfig,
	val domain: PackageConfig,
) : KotlinGeneratorBaseEndpoints(pkg) {

	override fun fileName(endpoint: EndpointGroup): String = endpoint.repoClassName()

	override fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.Single")
		writer.writeLine("import io.reactivex.Completable")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("")

		writer.writeLine("interface " + groupName.repoClassName() + " {")
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
			for (param in endpoint.security.passed() + endpoint.params) {
				val name = kotlinizeVariableName(param.key)

				val type = param.type.domainFinalName()+ if (!param.mandatory) "?" else ""

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