import KotlinGeneratorRepoImpl.SortedSecurities
import Namer.domainFinalName
import Namer.kotlinizeVariableName
import Namer.repoMethodName
import Namer.usecaseClassName
import models.*
import utils.PackageConfig
import java.io.PrintWriter


@Suppress("NAME_SHADOWING")
class KotlinGeneratorUsecase(
	val pkg: PackageConfig,
	val domain: PackageConfig,
) {

	fun fileName(endpoint: EndpointGroup): String = endpoint.usecaseClassName()

	fun writeEndpoits(input: List<Endpoint>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)
		Utils.cleanupDirectory(directory)

		val tags = input.flatMap { it.tags }.distinct()
		tags.forEach { tag ->
			PrintWriter("$directory/${fileName(tag)}.kt").use { writer ->
				writeEndpointInternal(
					BaseWriter(writer),
					tag,
					input.filter { it.tags.contains(tag) })
				writer.flush()
			}
		}

		input.filter { it.tags.isNullOrEmpty() }
			.forEach { one ->
				PrintWriter("$directory/${fileName(one)}.kt").use { writer ->
					writeEndpoint(BaseWriter(writer), one)
					writer.flush()
				}
			}
	}

	fun writeEndpoint(writer: GeneratorWriter, endpoint: Endpoint) {
		writeEndpointInternal(writer, endpoint, listOf(endpoint))
	}

	fun writeEndpointInternal(writer: GeneratorWriter, className: EndpointGroup, endpoints: List<Endpoint>) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.Single")
		writer.writeLine("import io.reactivex.Completable")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("")

		writer.writeLine("interface " + className.usecaseClassName() + " {")
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
				val name = param.transportName

				val type = param.type.domainFinalName()

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