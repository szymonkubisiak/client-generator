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

	fun fileName(type: Endpoint): String = type.name + "Service"

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

		writer.writeLine("interface " + endpoint.name + "Service {")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("@" + endpoint.retrofitAnnotation() + "(\"" + endpoint.path.trimStart('/') + "\")")
			writer.writeLine("fun " + endpoint.name + "(")
			IndentedWriter(writer).use { writer ->
				for (param in endpoint.params) {

					val location = param.location.retrofitAnnotation()
					val name = param.transportName
					val type = param.type.transportFinalName()

					writer.writeLine("@$location(\"$name\") $name: $type,")

					(param.location as? Param.Location.BODY)?.mediaType?.also {
						if (it.contains("form")) {
							writer.writeLine("//TODO: MediaType: " + it)
						}
					}

				}
			}
			endpoint.response?.also {
				writer.writeLine("): Single<" + it.transportFinalName() + ">")
			} ?: run {
				writer.writeLine("): Completable")
			}

		}
		writer.writeLine("}")
	}

	companion object {
		fun Param.Location.retrofitAnnotation() =
			when (this) {
				Param.Location.PATH -> "Path"
				Param.Location.QUERY -> "Query"
				is Param.Location.BODY -> "Body"
				Param.Location.HEADER -> "Header"
			}

		fun Endpoint.retrofitAnnotation() =
			this.operation.toUpperCase()
	}
}