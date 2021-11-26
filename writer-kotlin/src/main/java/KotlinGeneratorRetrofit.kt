import Namer.serviceClassName
import Namer.serviceMethodName
import Namer.transportFinalName
import models.*
import utils.PackageConfig

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
		writer.writeLine("import io.reactivex.rxjava3.core.Single")
		writer.writeLine("import io.reactivex.rxjava3.core.Completable")
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
			((endpoint.security ?: emptyList()) + endpoint.params).forEach { param ->
				val name = Namer.kotlinizeVariableName(param.key)
				val isWwwForm = isWwwForm(param)

				val location = if (isWwwForm)
					"FieldMap(encoded = false)"
				else
					param.retrofitAnnotation()

				val type = if (isWwwForm)
					"Map<String, String?>"
				else
					(param.type.transportFinalName() + if (!param.mandatory) "?" else "")

				writer.writeLine("@$location $name: $type,")
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

		fun isWwwForm(input: IParam): Boolean {
			val bodyParam = input.location as? Param.Location.BODY ?: return false
			return bodyParam.mediaType == mediaTypeWwwForm
		}

		fun IParam.retrofitAnnotation() =
			when (location) {
				Param.Location.PATH -> "Path(\"$key\")"
				Param.Location.QUERY -> "Query(\"$key\")"
				is Param.Location.BODY -> "Body"
				Param.Location.HEADER -> "Header(\"$key\")"
				Param.Location.COOKIE -> "Header(\"Cookie\")"
			}

		fun Endpoint.retrofitAnnotation() =
			this.operation.toUpperCase()
	}
}