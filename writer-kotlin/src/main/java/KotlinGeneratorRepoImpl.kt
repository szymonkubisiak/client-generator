import Namer.domainFinalName
import Namer.kotlinizeVariableName
import Namer.repoClassName
import Namer.repoMethodName
import Namer.serviceClassName
import models.*
import utils.PackageConfig
import java.io.PrintWriter


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
			writer.writeLine("import io.reactivex.Completable")
			writer.writeLine("import io.reactivex.Single")
			writer.writeLine("")
			writer.writeLine("interface JwtProvider{")
			IndentedWriter(writer).use { writer ->
				writer.writeLine("fun <T> executeWithJwt(callee: (jwt: String) -> Single<T>): Single<T>")
				writer.writeLine("fun <T> executeWithJwtAndXsrf(callee: (jwt: String, xsrf: String) -> Single<T>): Single<T>")
				writer.writeLine("fun executeWithJwtAndXsrf(callee: (jwt: String, xsrf: String) -> Completable): Completable")
			}
			writer.writeLine("}")
		}
	}

	override fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>) {
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
		val sortedSecurities = endpoint.security?.let(::SortedSecurities)

		if (sortedSecurities == null || sortedSecurities.handled.isEmpty()) {
			writer.writeLine("override fun " + endpoint.repoMethodName() + "(")
		} else {
			writeEndpointMethodWrapper(writer, endpoint, sortedSecurities)
			writer.writeLine("private fun " + endpoint.repoMethodName() + "Internal(")
			IndentedWriter(writer).use { writer ->
				sortedSecurities.handled.forEach { security ->
					val name = kotlinizeVariableName(security.key)
					val type = "String"
					if (endpoint.params.any { param -> param.transportName == security.key }) {
						writer.writeLine("//WARNING: security clashes with param:")
						writer.writeLine("//$name: $type,")
					} else {
						writer.writeLine("$name: $type,")
					}
				}
			}
		}
		IndentedWriter(writer).use { writer ->
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
		}
		writeParams(writer, endpoint.params)
		writeFunctionReturnType(writer, endpoint)

		IndentedWriter(writer).use { writer ->

			writer.writeLine("return http." + endpoint.repoMethodName() + "(")

			IndentedWriter(writer).use { writer ->
				endpoint.security?.forEach { security ->
					val name = kotlinizeVariableName(security.key)
					val type = "String"
					if (endpoint.params.any { param -> param.transportName == security.key }) {
						writer.writeLine("//WARNING: security clashes with param:")
						writer.writeLine("//$name,")
					} else {
						writer.writeLine("$name,")
					}
				}
				for (param in endpoint.params) {
					val name = kotlinizeVariableName(param.transportName)
					val isForm = KotlinGeneratorRetrofit.isWwwForm(param)
					val conversionIt = if (!isForm)
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
						"$expression.let { $conversionIt },"
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
		sortedSecurities: SortedSecurities
	) {
		writer.writeLine("override fun " + endpoint.repoMethodName() + "(")
		writeParams(writer, endpoint.params)
		writeFunctionReturnType(writer, endpoint)

		val hasJwt = sortedSecurities.hasJwt()
		val hasXsrf = sortedSecurities.hasXsrf()

		val returnType = endpoint.response?.let {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			"<$type>"
		} ?: ""

		IndentedWriter(writer).use { writer ->
			//writer.writeLine("return jwt.executeWithJwt(::" + endpoint.repoMethodName() + "Internal)")
			if (hasJwt && !hasXsrf) {
				writer.writeLine("return jwt.executeWithJwt { jwt ->")
			} else if (hasJwt && hasXsrf) {
				writer.writeLine("return jwt.executeWithJwtAndXsrf$returnType { jwt, xsrf ->")
			} else {
				writer.writeLine("TODO(\"handle other jwt/xsrf combinations\")")
				return@use
			}

			IndentedWriter(writer).use { writer ->
				writer.writeLine(endpoint.repoMethodName() + "Internal(")
				IndentedWriter(writer).use { writer ->
					if (hasJwt) writer.writeLine("jwt,")
					if (hasXsrf) writer.writeLine("xsrf,")
					for (param in endpoint.params) {
						val name = param.transportName
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

	private fun writeParams(writer: IndentedWriter, params: List<Param>) {
		IndentedWriter(writer).use { writer ->
			for (param in params) {
				val name = kotlinizeVariableName(param.transportName)
				val type = param.type.domainFinalName() + if (!param.mandatory) "?" else ""
				writer.writeLine("$name: $type,")
			}
		}
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


	class SortedSecurities(input: List<Security>) {
		//TODO: sort jwt/xsrf
		val handled = input.filter { it.key == jwtToken || it.key == xsrfToken }
		val passed = input.filter { it.key != jwtToken && it.key != xsrfToken }

		fun hasJwt() = handled.any { it.key == jwtToken }
		fun hasXsrf() = handled.any { it.key == xsrfToken }

		companion object {
			val jwtToken = "JWT"
			val xsrfToken = "X-XSRF-TOKEN"
		}
	}
}