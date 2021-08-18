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
	val pkg: PackageConfig,
	val retrofit: PackageConfig,
	val t2d: PackageConfig,
	val domain: PackageConfig,
	val repos: PackageConfig,
) {

	fun fileName(endpoint: EndpointGroup): String = endpoint.repoClassName()

	fun writeEndpoits(input: List<Endpoint>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)
		Utils.cleanupDirectory(directory)

		PrintWriter("$directory/JwtProvider.kt").use { it ->
			val writer = BaseWriter(it)
			writer.writeLine("package " + pkg.toPackage())
			writer.writeLine("")
			writer.writeLine("import io.reactivex.Single")
			writer.writeLine("")
			writer.writeLine("interface JwtProvider{")
			writer.writeLine("\tfun <T> executeWithJwt(callee: (jwt: String) -> T): T")
			//writer.writeLine("fun <T> executeWithJwtAndXsrf(callee: (jwt: String, xsrf: String) -> T): T")
			writer.writeLine("}")
		}

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

		writer.writeLine("class " + repoClassName.repoClassName() + "Impl @Inject constructor(")
		IndentedWriter(writer).use { writer ->
			writer.writeLine("private val http: " + repoClassName.serviceClassName() + ",")
			endpoints.mapNotNull { it.security }.flatten().firstOrNull { it.key == "JWT" }?.also {
				writer.writeLine("private val jwt: JwtProvider,")
			}
		}
		writer.writeLine("): ${repoClassName.repoClassName()} {")
		IndentedWriter(writer).use { writer ->
			endpoints.forEach { endpoint ->
				writeEndpointMethod(writer, endpoint)
			}
		}
		writer.writeLine("}")
	}

	private fun writeEndpointMethod(writer: IndentedWriter, endpoint: Endpoint) {
		writer.writeLine("")

		if (!isPureJwtSecured(endpoint.security)) {
			writer.writeLine("override fun " + endpoint.repoMethodName() + "(")
		} else {
			writeEndpointMethodWrapper(writer, endpoint)
			writer.writeLine("private fun " + endpoint.repoMethodName() + "Internal(")
			IndentedWriter(writer).use { writer ->
				for (security in endpoint.security ?: emptyList()) {
					val name = kotlinizeVariableName(security.key)
					val type = "String"
					writer.writeLine("$name: $type,")
				}
			}
		}

		writeParams(writer, endpoint.params)
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
				for (security in endpoint.security ?: emptyList()) {
					val name = kotlinizeVariableName(security.key)
					val type = "String"
					writer.writeLine("$name,")
				}
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

	private fun writeEndpointMethodWrapper(writer: IndentedWriter, endpoint: Endpoint) {
		writer.writeLine("override fun " + endpoint.repoMethodName() + "(")
		writeParams(writer, endpoint.params)
		endpoint.response?.also {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			writer.writeLine("): Single<$type> {")
		} ?: run {
			writer.writeLine("): Completable {")
		}

		//TODO: check the actual security, because for now we're treating everything as "JWT"
		//endpoint.security.

		IndentedWriter(writer).use { writer ->
			//writer.writeLine("return jwt.executeWithJwt(::" + endpoint.repoMethodName() + "Internal)")
			writer.writeLine("return jwt.executeWithJwt {")
			IndentedWriter(writer).use { writer ->
				writer.writeLine( endpoint.repoMethodName() + "Internal(")
				IndentedWriter(writer).use { writer ->
					writer.writeLine("it,")
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
				val name = param.transportName
				val type = param.type.domainFinalName()
				writer.writeLine("$name: $type,")
			}
		}
	}

	companion object {
		fun isPureJwtSecured(security: List<Security>?): Boolean {
			if (security == null) return false
			if (security.size > 1) return false
			if (security[0].key != "JWT") return false
			return true
		}
	}
}