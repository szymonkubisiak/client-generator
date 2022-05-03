import Namer.domainFinalName
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
		writer.writeLine("import io.reactivex.rxjava3.core.Single")
		writer.writeLine("import io.reactivex.rxjava3.core.Completable")
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
		writeParamsDefinitions(writer, endpoint.security.passed() + endpoint.params.filter(::isParamNotImplicit), true)
		endpoint.response?.also {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			writer.writeLine("): Single<$type>")
		} ?: run {
			writer.writeLine("): Completable")
		}
	}
}