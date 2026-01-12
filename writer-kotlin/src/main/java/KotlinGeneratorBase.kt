import Namer.domainFinalName
import Namer.kotlinizeVariableName
import models.*
import utils.PackageConfig
import java.io.PrintWriter

const val jwtToken = "JWT" //duplication in OpenApiConverter
const val xsrfToken = "X-XSRF-TOKEN"

abstract class KotlinGeneratorBase(
	val pkg: PackageConfig,
	protected val typeResolver: TypeResolver = TypeResolver.instance,
) {

	companion object {
	fun PackageConfig.createDirectory() {
		Utils.createDirectories(this.asDir)
	}

	fun PackageConfig.cleanupDirectory() {
		Utils.cleanupDirectory(this.asDir)
	}

	fun PackageConfig.createAndCleanupDirectory() {
		createDirectory()
		cleanupDirectory()
	}

	fun PackageConfig.openFile(fileName: String): BaseWriter {
		val outerWriter = PrintWriter("$asDir/$fileName")
		val retval = BaseWriter(outerWriter)
		return retval
	}

		fun writeParamsDefinitions(writer: IndentedWriter, argParams: List<IParam>, defaultNullParameters: Boolean = false ) {
			if(argParams.isNullOrEmpty()) return
			val params = argParams.preprocessParams()
			val nullableString = if(defaultNullParameters) "? = null" else "?"
			IndentedWriter(writer).use { writer ->
				for (param in params) {
					val name = kotlinizeVariableName(param.key)
					val type = param.domainFinalName() + if (!param.mandatory) nullableString else ""
					writer.writeLine("$name: $type,")
					//val comment = param.description?.let { " // $it" } ?: ""
					//writer.writeLine("$name: $type,$comment")
				}
			}
		}

		fun writeParamsCalls(writer: IndentedWriter, params: List<IParam> ) {
			if(params.isNullOrEmpty()) return
			IndentedWriter(writer).use { writer ->
				for (param in params) {
					val name = kotlinizeVariableName(param.key)
					writer.writeLine("$name,")
				}
			}
		}

		fun List<Security>?.handled() = this?.filter { it.key == jwtToken || it.key == xsrfToken } ?: emptyList()
		fun List<Security>?.passed() = this?.filter { it.key != jwtToken && it.key != xsrfToken } ?: emptyList()


		fun needDates(endpoints: List<Endpoint>): Boolean {
			return endpoints.any { endpoint ->
				endpoint.params.any { param ->
					needDates(param.type)
				}
			}
		}

		fun needDates(model: Struct): Boolean {
			return (model as? StructActual)?.fields?.any {
				needDates(it.type)
			} ?: false
		}

		fun needDates(type: TypeDescr): Boolean {
			return type.domainFinalName() == "LocalDate" || type.domainFinalName() == "LocalDateTime"
		}
	}
}

private fun List<IParam>.preprocessParams() = this
