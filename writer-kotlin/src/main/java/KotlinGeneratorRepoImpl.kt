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

		val localeFields = Profile.active.localeParams.values.distinct()
		if (localeFields.isNotEmpty()) pkg.openFile("LocaleProvider.kt").use { writer ->
			writer.writeLine("package " + pkg.toPackage())
			writer.writeLine("")
			writer.writeLine("import io.reactivex.rxjava3.core.Single")
			writer.writeLine("")
			writer.writeLine("interface LocaleProvider {")
			IndentedWriter(writer).use { writer ->
				writer.writeLine("")
				writer.writeLine("interface LocaleDefinition {")
				IndentedWriter(writer).use { writer ->
					localeFields.forEach { writer.writeLine("val $it: String") }
				}
				writer.writeLine("}")
				writer.writeLine("")
				writer.writeLine("fun provideLocale(): Single<LocaleDefinition>")
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
			endpoints.mapNotNull { it.security }.flatten().firstOrNull { it.key == Profile.active.jwtScheme }?.also {
				writer.writeLine("private val jwt: JwtProvider,")
			}

			endpoints.any { it.handledParams().isNotEmpty() }.takeIf { it }?.also {
				writer.writeLine("private val localer: LocaleProvider,")
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

		if (handledSecurities.isEmpty() && endpoint.handledParams().isEmpty()) {
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
		writeParamsDefinitions(writer, endpoint.passedParams())
		writeFunctionReturnType(writer, endpoint)


		val handledSecurity = endpoint.security.handled()
		val jwt = handledSecurity.firstOrNull { it.key == Profile.active.jwtScheme }
		val xsrf = handledSecurity.firstOrNull { it.key == Profile.active.xsrfScheme }

		val returnType = endpoint.response?.let {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			"<$type>"
		} ?: ""

		val localeOpener = endpoint.handledParams().takeIf { it.isNotEmpty() }?.let {
			val flatMap = if (endpoint.response != null) "flatMap" else "flatMapCompletable"
			"localer.provideLocale().$flatMap { locale ->"
		}
		val securityOpener = when {
			jwt != null && xsrf == null -> if (jwt.mandatory) "jwt.executeWithJwt { jwt ->" else "jwt.executeWithOptionalJwt { jwt ->"
			jwt != null && xsrf != null -> "jwt.executeWithJwtAndXsrf$returnType { jwt, xsrf ->"
			xsrf != null -> {
				IndentedWriter(writer).use { writer ->
					writer.writeLine("TODO(\"handle other jwt/xsrf combinations\")")
				}
				writer.writeLine("}")
				writer.writeLine("")
				return
			}
			else -> null
		}

		IndentedWriter(writer).use { writer ->
			// locale is resolved outside of jwt, so token refresh retries don't fetch it again
			writeNested(writer, listOfNotNull(localeOpener, securityOpener), "return ") { writer, prefix ->
				writer.writeLine(prefix + endpoint.repoMethodName() + "Internal(")
				IndentedWriter(writer).use { writer ->
					if (jwt != null) writer.writeLine("jwt,")
					if (xsrf != null) writer.writeLine("xsrf,")
					for (param in endpoint.params) {
						val localeField = Profile.active.localeParams[param.key]
						val name = localeField?.let { "locale.$it" } ?: kotlinizeVariableName(param.key)
						writer.writeLine("$name,")
					}
				}
				writer.writeLine(")")
			}
		}
		writer.writeLine("}")
		writer.writeLine("")
	}


companion object {
	/** writes each opener as a lambda nested in the previous one, with body in the innermost */
	private fun writeNested(
		writer: IndentedWriter,
		openers: List<String>,
		prefix: String,
		body: (writer: IndentedWriter, prefix: String) -> Unit,
	) {
		if (openers.isEmpty()) {
			body(writer, prefix)
			return
		}
		writer.writeLine(prefix + openers.first())
		IndentedWriter(writer).use { writer ->
			writeNested(writer, openers.drop(1), "", body)
		}
		writer.writeLine("}")
	}

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