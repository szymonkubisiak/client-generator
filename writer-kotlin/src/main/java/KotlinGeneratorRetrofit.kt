import Namer.serviceClassName
import Namer.serviceMethodName
import Namer.transportFinalName
import models.Endpoint
import models.Param
import utils.PackageConfig
import java.io.PrintWriter

@Suppress("NAME_SHADOWING")
class KotlinGeneratorRetrofit(
	val pkg: PackageConfig,
	val conn: PackageConfig,
) {

	fun fileName(endpoint: Endpoint): String = endpoint.serviceClassName()

	fun writeEndpoits(input: List<Endpoint>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)
		Utils.cleanupDirectory(directory)
		input.forEach { one ->
			PrintWriter("$directory/${fileName(one)}.kt").use { writer ->
				writeEndpoint(BaseWriter(writer), one)
				writer.flush()
			}
		}
	}

	fun writeEndpoint(writer: GeneratorWriter, endpoint: Endpoint) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.Single")
		writer.writeLine("import io.reactivex.Completable")
		writer.writeLine("import " + conn.toPackage() + ".*")
		writer.writeLine("import retrofit2.http.*")
		writer.writeLine("")

		endpoint.security?.also { writer.writeLine("//security: " + it.joinToString { it.key }) }
		writer.writeLine("interface " + endpoint.serviceClassName() + " {")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("@" + endpoint.retrofitAnnotation() + "(\"" + endpoint.path.trimStart('/') + "\")")
			writer.writeLine("fun " + endpoint.serviceMethodName() + "(")
			IndentedWriter(writer).use { writer ->
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
			endpoint.response?.also {
				val rawType = it.type.transportFinalName()
				val type = if (!it.isArray) rawType else "List<$rawType>"
				writer.writeLine("): Single<$type>")
			} ?: run {
				writer.writeLine("): Completable")
			}

		}
		writer.writeLine("}")
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