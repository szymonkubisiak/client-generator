import Namer.serviceClassName
import Namer.serviceMethodName
import Namer.transportFinalName
import models.Endpoint
import models.EndpointGroup
import models.Param
import models.Tag
import utils.PackageConfig
import java.io.PrintWriter

@Suppress("NAME_SHADOWING")
class KotlinGeneratorRetrofit(
	val pkg: PackageConfig,
	val conn: PackageConfig,
) {

	fun fileName(endpoint: EndpointGroup): String = endpoint.serviceClassName()

	fun writeEndpoits(input: List<Endpoint>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)
		Utils.cleanupDirectory(directory)

		val tags = input.flatMap { it.tags }.distinct()
		tags.forEach { tag ->
			PrintWriter("$directory/${fileName(tag)}.kt").use { writer ->
				writeEndpointInternal(BaseWriter(writer), tag.serviceClassName(), input.filter { it.tags.contains(tag) })
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
		writeEndpointInternal(writer, endpoint.serviceClassName(), listOf(endpoint))
	}

	fun writeEndpointInternal(writer: GeneratorWriter, serviceClassName: String, endpoints: List<Endpoint>) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.Single")
		writer.writeLine("import io.reactivex.Completable")
		writer.writeLine("import " + conn.toPackage() + ".*")
		writer.writeLine("import retrofit2.http.*")
		writer.writeLine("")

		writer.writeLine("interface " + serviceClassName + " {")
		IndentedWriter(writer).use { writer ->
			endpoints.forEach { endpoint ->
				writeEndpointMethod(writer, endpoint)
			}
		}
		writer.writeLine("}")
	}

	private fun writeEndpointMethod(writer: IndentedWriter, endpoint: Endpoint): Any {
		writer.writeLine("")
		writer.writeLine("@" + endpoint.retrofitAnnotation() + "(\"" + endpoint.path.trimStart('/') + "\")")
		writer.writeLine("fun " + endpoint.serviceMethodName() + "(")
		IndentedWriter(writer).use { writer ->
			endpoint.security?.forEach { security ->
				val name = Namer.kotlinizeVariableName(security.key)
				val location = security.location.retrofitAnnotation(name)
				val type = "String"
				if (endpoint.params.any { param -> param.transportName == security.key }) {
					writer.writeLine("//WARNING: security clashes with param:")
					writer.writeLine("//@$location $name: $type,")
				} else {
					writer.writeLine("@$location $name: $type,")

				}
			}
			for (param in endpoint.params) {
				val name = param.transportName
				val location = param.location.retrofitAnnotation(name)
				val type = param.type.transportFinalName()

				writer.writeLine("@$location $name: $type,")

				(param.location as? Param.Location.BODY)?.mediaType?.also {
					if (it.contains("form")) {
						writer.writeLine("//TODO: MediaType: " + it)
					}
				}
			}
		}
		return endpoint.response?.also {
			val rawType = it.type.transportFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			writer.writeLine("): Single<$type>")
		} ?: run {
			writer.writeLine("): Completable")
		}
	}

	companion object {
		fun Param.Location.retrofitAnnotation(name: String) =
			when (this) {
				Param.Location.PATH -> "Path(\"$name\")"
				Param.Location.QUERY -> "Query(\"$name\")"
				is Param.Location.BODY -> "Body"
				Param.Location.HEADER -> "Header(\"$name\")"
				Param.Location.COOKIE -> "Header(\"Cookie\")"
			}

		fun Endpoint.retrofitAnnotation() =
			this.operation.toUpperCase()
	}
}