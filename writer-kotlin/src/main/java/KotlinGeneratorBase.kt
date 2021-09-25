import Namer.domainFinalName
import Namer.kotlinizeVariableName
import models.*
import utils.PackageConfig
import java.io.PrintWriter

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

		fun writeParamsDefinitions(writer: IndentedWriter, params: List<IField>, defaultNullParameters: Boolean = false ) {
			if(params.isNullOrEmpty()) return
			val nullableString = if(defaultNullParameters) "? = null" else "?"
			IndentedWriter(writer).use { writer ->
				for (param in params) {
					val name = kotlinizeVariableName(param.key)
					val type = param.type.domainFinalName() + if (!param.mandatory) nullableString else ""
					writer.writeLine("$name: $type,")
				}
			}
		}

		fun List<Security>?.handled() = this?.filter { it.key == jwtToken || it.key == xsrfToken } ?: emptyList()
		fun List<Security>?.passed() = this?.filter { it.key != jwtToken && it.key != xsrfToken } ?: emptyList()
		fun List<Security>?.hasJwt() = this?.any { it.key == jwtToken } ?: false
		fun List<Security>?.hasXsrf() = this?.any { it.key == xsrfToken } ?: false

		val jwtToken = "JWT"
		val xsrfToken = "X-XSRF-TOKEN"
	}
}
