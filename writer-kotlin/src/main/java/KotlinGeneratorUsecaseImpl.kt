import Namer.domainFinalName
import Namer.kotlinizeVariableName
import Namer.repoClassName
import Namer.repoMethodName
import Namer.usecaseClassName
import Namer.usecaseMethodName
import models.*
import utils.PackageConfig


@Suppress("NAME_SHADOWING")
class KotlinGeneratorUsecaseImpl(
	pkg: PackageConfig,
	val domain: PackageConfig,
	val ucDefs: PackageConfig,
	val repoDefs: PackageConfig,
) : KotlinGeneratorBaseEndpoints(pkg) {

	override fun fileName(endpoint: EndpointGroup): String = endpoint.usecaseClassName() + "Impl"

	override fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>) {
		val implicitParams =
				endpoints.flatMap { endpoint -> endpoint.params.filter(::isParamImplicit) }.distinctBy { it.key }

		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import io.reactivex.rxjava3.core.Single")
		writer.writeLine("import io.reactivex.rxjava3.core.Completable")
		writer.writeLine("import " + domain.toPackage() + ".*")
		writer.writeLine("import " + ucDefs.toPackage() + ".*")
		writer.writeLine("import " + repoDefs.toPackage() + ".*")
		if (implicitParams.isNotEmpty()) writer.writeLine("import java.util.*")
		if (needDates(endpoints))
			writer.writeLine("import java.time.*")
		writer.writeLine("import javax.inject.Inject")
		writer.writeLine("")

		writer.writeLine("class " + groupName.usecaseClassName() + "Impl @Inject constructor(val repo: "+groupName.repoClassName()+") : " + groupName.usecaseClassName() + "{")
		IndentedWriter(writer).use { writer ->

			for (param in implicitParams) {
				val name = kotlinizeVariableName(param.key)
				val type = param.type.domainFinalName() + if (!param.mandatory) "?" else ""
				val defaultValue = when (param.key) {
					//TODO: move to config
					"x-locale" -> "Locale.getDefault().toLanguageTag()"
					else -> "\"unknown value\""
				}
				writer.writeLine("private val $name: $type = $defaultValue")
			}

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
			//TODO: make entire comment javadoc-compliant
			//writer.writeLine("/**")
			//descriptionLines.forEach { writer.writeLine("* $it") }
			endpoint.params
				.filter { !it.description.isNullOrEmpty() && isParamNotImplicit(it) }
				.forEachIndexed { index, param ->
					if (index == 0) writer.writeLine("*")
					val name = kotlinizeVariableName(param.key)
					val type = param.type.domainFinalName() + if (!param.mandatory) "?" else ""
					writer.writeLine("* @param  ${name.padEnd(14, ' ')} $type - ${param.description}")
				}
			writer.writeLine("*/")
		}
		writer.writeLine("override fun " + endpoint.usecaseMethodName() + "(")
		writeParamsDefinitions(writer, endpoint.security.passed() + endpoint.params.filter(::isParamNotImplicit))
		endpoint.response?.also {
			val rawType = it.type.domainFinalName()
			val type = if (!it.isArray) rawType else "List<$rawType>"
			writer.writeLine("): Single<$type> {")
		} ?: run {
			writer.writeLine("): Completable{")
		}
		IndentedWriter(writer).use { writer ->
			writer.writeLine("return repo." + endpoint.repoMethodName() + "(")
			writeParamsCalls (writer, endpoint.security.passed() + endpoint.params)
			writer.writeLine(")")
		}
		writer.writeLine("}")
	}
}