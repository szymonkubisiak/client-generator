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
	pkg: PackageConfig,
	val conn: PackageConfig,
) : KotlinGeneratorBaseEndpoints(pkg) {

	override fun fileName(endpoint: EndpointGroup): String = endpoint.serviceClassName()

	override fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>) {
		val serviceClassName = fileName(groupName)
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
		endpoint.description?.split('\n')?.also { descriptionLines ->
			writer.writeLine("/*")
			descriptionLines.forEach { writer.writeLine(it) }
			writer.writeLine("*/")
		}
		if (isWwwForm(endpoint)) {
			writer.writeLine("@FormUrlEncoded")
		}
		writer.writeLine("@" + endpoint.retrofitAnnotation() + "(\"" + endpoint.path.trimStart('/') + "\")")
		writer.writeLine("fun " + endpoint.serviceMethodName() + "(")
		IndentedWriter(writer).use { writer ->
			endpoint.security?.forEach { security ->
				val name = Namer.kotlinizeVariableName(security.key)
				val location = security.location.retrofitAnnotation(security.key)
				val type = "String"
				if (endpoint.params.any { param -> param.transportName == security.key }) {
					writer.writeLine("//WARNING: security clashes with param:")
					writer.writeLine("//@$location $name: $type,")
				} else {
					writer.writeLine("@$location $name: $type,")

				}
			}
			for (param in endpoint.params) {
				val name = Namer.kotlinizeVariableName(param.transportName)
				val location = param.location.retrofitAnnotation(param.transportName)
				val type = param.type.transportFinalName() + if (!param.mandatory) "?" else ""

				if (isWwwForm(param)) {
					writer.writeLine("@FieldMap(encoded = false) $name: Map<String, String?>,")
				} else {
					writer.writeLine("@$location $name: $type,")
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
		private const val mediaTypeWwwForm = "application/x-www-form-urlencoded"

		fun isWwwForm(input: Endpoint): Boolean {
			return input.params.any(::isWwwForm)
		}

		fun isWwwForm(input: Param): Boolean {
			val bodyParam = input.location as? Param.Location.BODY ?: return false
			return bodyParam.mediaType == mediaTypeWwwForm
		}

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