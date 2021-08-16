import Namer.domainFinalName
import Namer.repoClassName
import Namer.repoMethodName
import Namer.serviceClassName
import models.*
import utils.PackageConfig
import java.io.PrintWriter


@Suppress("NAME_SHADOWING")
class KotlinGeneratorRepoImpl(
	val pkg: PackageConfig,
	val retrofit: PackageConfig,
	val t2d: PackageConfig,
	val domain: PackageConfig,
	val repos: PackageConfig,
) {

	fun fileName(endpoint: Endpoint): String = endpoint.repoClassName()
	fun fileName(tag: Tag): String = tag.repoClassName()

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

	fun writeEndpointInternal(writer: GeneratorWriter, repoClassName: EndpointGroup, endpoints: List<Endpoint>) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.Single")
		writer.writeLine("import io.reactivex.Completable")
		writer.writeLine("import " + retrofit.toPackage() + ".*")
		writer.writeLine("import " + t2d.toPackage() + ".*")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("import " + repos.toPackage() + ".*")
		writer.writeLine("import javax.inject.Inject")
		writer.writeLine("")

		writer.writeLine("class " + repoClassName.repoClassName() + "Impl @Inject constructor(private val http: " + repoClassName.serviceClassName() + "): ${repoClassName.repoClassName()} {")
		IndentedWriter(writer).use { writer ->
			endpoints.forEach { endpoint ->
				writeEndpointMethod(writer, endpoint)
			}
		}
		writer.writeLine("}")
	}

	private fun writeEndpointMethod(writer: IndentedWriter, endpoint: Endpoint) {
		writer.writeLine("")

		writer.writeLine("override fun " + endpoint.repoMethodName() + "(")
		IndentedWriter(writer).use { writer ->

			for (param in endpoint.params) {
				val name = param.transportName

				val type = param.type.domainFinalName()

				writer.writeLine("$name: $type,")
			}
		}
		endpoint.response?.also {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			writer.writeLine("): Single<$type> {")
		} ?: run {
			writer.writeLine("): Completable {")
		}
		IndentedWriter(writer).use { writer ->
			writer.writeLine("return http." + endpoint.repoMethodName() + "(")

			IndentedWriter(writer).use { writer ->
				for (param in endpoint.params) {
					val name = param.transportName
					writer.writeLine("$name,")
				}
			}

			writer.writeLine(")")
			endpoint.response?.also { field ->
				val conversionIt = KotlinGeneratorT2D.resolveTransportToDomainConversion(field.type).format("it")
				var expression = "it"
				expression = if (field.isArray) {
					"$expression.map { $conversionIt }"
				} else {
					"$expression.let { $conversionIt }"
				}
				IndentedWriter(writer).use { writer ->
					writer.writeLine(".map { $expression }")
				}
			}
		}
		writer.writeLine("}")
	}
}