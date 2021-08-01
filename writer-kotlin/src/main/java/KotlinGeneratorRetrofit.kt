import Namer.transportFinalName
import models.Endpoint
import models.Param

class KotlinGeneratorRetrofit {

	fun writeEndpoint(writer: GeneratorWriter, endpoint: Endpoint) {
		writer.writeLine("import io.reactivex.Single")
		writer.writeLine("import io.reactivex.Completable")
		writer.writeLine("import retrofit2.http.*")
		writer.writeLine("")

		writer.writeLine("interface " + endpoint.name + "Service {")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("@GET(\"" + endpoint.path.trimStart('/') + "\")")
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
				writer.writeLine("): Single<" + it.transportName + ">")
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
	}
}