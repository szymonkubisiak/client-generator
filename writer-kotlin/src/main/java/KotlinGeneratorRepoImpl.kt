import Namer.domainFinalName
import Namer.kotlinizeVariableName
import Namer.repoClassName
import Namer.repoMethodName
import Namer.serviceClassName
import models.*
import utils.PackageConfig


@Suppress("NAME_SHADOWING")
class KotlinGeneratorRepoImpl(
	pkg: PackageConfig,
	val retrofit: PackageConfig,
	val t2d: PackageConfig,
	val domain: PackageConfig,
	val repos: PackageConfig,
) : KotlinGeneratorBaseEndpoints(pkg) {

	override fun fileName(endpoint: EndpointGroup): String = endpoint.repoClassName() + "Impl"

	override fun writeExtras() {
		pkg.openFile("JwtProvider.kt").use { writer ->
			writer.writeLine("package " + pkg.toPackage())
			writer.writeLine("")
			writer.writeLine("import io.reactivex.rxjava3.core.Completable")
			writer.writeLine("import io.reactivex.rxjava3.core.Single")
			writer.writeLine("")
			writer.writeLine("interface JwtProvider{")
			IndentedWriter(writer).use { writer ->
				writer.writeLine("fun <T: Any> executeWithJwt(callee: (jwt: String) -> Single<T>): Single<T>")
				writer.writeLine("fun <T: Any> executeWithOptionalJwt(callee: (jwt: String?) -> Single<T>): Single<T>")
				writer.writeLine("fun <T: Any> executeWithJwtAndXsrf(callee: (jwt: String, xsrf: String) -> Single<T>): Single<T>")
				writer.writeLine("fun executeWithJwtAndXsrf(callee: (jwt: String, xsrf: String) -> Completable): Completable")
			}
			writer.writeLine("}")
		}
	}

	override fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.rxjava3.core.Single")
		writer.writeLine("import io.reactivex.rxjava3.core.Completable")
		writer.writeLine("import " + retrofit.toPackage() + ".*")
		writer.writeLine("import " + t2d.toPackage() + ".*")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("import " + repos.toPackage() + ".*")
		if (needDates(endpoints))
			writer.writeLine("import java.time.*")
		writer.writeLine("import javax.inject.Inject")
		writer.writeLine("")

		writer.writeLine("class " + groupName.repoClassName() + "Impl @Inject constructor(")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("private val http: " + groupName.serviceClassName() + ",")
			endpoints.mapNotNull { it.security }.flatten().firstOrNull { it.key == "JWT" }?.also {
				writer.writeLine("private val jwt: JwtProvider,")
			}
		}
		writer.writeLine("): ${groupName.repoClassName()} {")
		IndentedWriter(writer).use { writer ->
			endpoints.forEach { endpoint ->
				writeEndpointMethod(writer, endpoint)
			}
		}
		writer.writeLine("}")
	}

	private fun writeEndpointMethod(writer: IndentedWriter, endpoint: Endpoint) {
		writer.writeLine("")
		endpoint.description?.split('\n')?.also { descriptionLines ->
			writer.writeLine("/*")
			descriptionLines.forEach { writer.writeLine(it) }
			writer.writeLine("*/")
		}
		val handledSecurities = endpoint.security.handled()

		if (handledSecurities.isEmpty()) {
			writer.writeLine("override fun " + endpoint.repoMethodName() + "(")
		} else {
			writeEndpointMethodWrapper(writer, endpoint)
			writer.writeLine("private fun " + endpoint.repoMethodName() + "Internal(")
			writeParamsDefinitions(writer, handledSecurities)
		}
		writeParamsDefinitions(writer, endpoint.security.passed() + endpoint.params)
		writeFunctionReturnType(writer, endpoint)

		IndentedWriter(writer).use { writer ->

			writer.writeLine("return http." + endpoint.repoMethodName() + "(")

			writeParamsCalls(writer, endpoint.security?: emptyList())
			IndentedWriter(writer).use { writer ->
				for (param in endpoint.params) {
					val name = kotlinizeVariableName(param.key)
					val isWwwForm = param.isWwwForm()
					val conversionIt = if (!isWwwForm)
						KotlinGeneratorConverters.resolveDomainToTransportConversion(param.type).format("it")
					else
						KotlinGeneratorConverters.resolveDomainToMapConversion(param.type).format("it")
					var expression = name
					if(!param.mandatory) {
						expression += "?"
					}
					expression = if (param.isArray) {
						"$expression.map { $conversionIt },"
					} else {
						val defaultizer = param.defaultValue?.let { defVal ->
							val isString = param.type.key == "string"
							if (isString) {
								"?.takeIf { it != \"$defVal\" }"
							} else {
								"?.takeIf { it != $defVal }"
							}
						} ?: ""
						"$expression.let { $conversionIt }$defaultizer,"
					}
					writer.writeLine(expression)
				}
			}

			writer.writeLine(")")
			endpoint.response?.also { field ->
				val conversionIt = KotlinGeneratorConverters.resolveTransportToDomainConversion(field.type).format("it")
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

	private fun writeEndpointMethodWrapper(
		writer: IndentedWriter,
		endpoint: Endpoint,
	) {
		writer.writeLine("override fun " + endpoint.repoMethodName() + "(")
		writeParamsDefinitions(writer, endpoint.params)
		writeFunctionReturnType(writer, endpoint)


		val handledSecurity = endpoint.security.handled()
		val jwt = handledSecurity.firstOrNull { it.key == jwtToken }
		val xsrf = handledSecurity.firstOrNull { it.key == xsrfToken }

		val returnType = endpoint.response?.let {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			"<$type>"
		} ?: ""

		IndentedWriter(writer).use { writer ->
			//writer.writeLine("return jwt.executeWithJwt(::" + endpoint.repoMethodName() + "Internal)")
			if (jwt != null && xsrf == null) {
				if(jwt.mandatory)
					writer.writeLine("return jwt.executeWithJwt { jwt ->")
				else
					writer.writeLine("return jwt.executeWithOptionalJwt { jwt ->")
			} else if (jwt != null && xsrf != null) {
				writer.writeLine("return jwt.executeWithJwtAndXsrf$returnType { jwt, xsrf ->")
			} else {
				writer.writeLine("TODO(\"handle other jwt/xsrf combinations\")")
				return@use
			}

			IndentedWriter(writer).use { writer ->
				writer.writeLine(endpoint.repoMethodName() + "Internal(")
				IndentedWriter(writer).use { writer ->
					if (jwt != null) writer.writeLine("jwt,")
					if (xsrf != null) writer.writeLine("xsrf,")
					for (param in endpoint.params) {
						val name = kotlinizeVariableName(param.key)
						writer.writeLine("$name,")
					}
				}
				writer.writeLine(")")
			}
			writer.writeLine("}")
		}
		writer.writeLine("}")
		writer.writeLine("")
	}


companion object {
	private fun writeFunctionReturnType(writer: IndentedWriter, endpoint: Endpoint) {
		endpoint.response?.also {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			writer.writeLine("): Single<$type> {")
		} ?: run {
			writer.writeLine("): Completable {")
		}
	}

}
}