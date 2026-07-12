import Namer.usecaseClassName
import models.EndpointGroup
import utils.PackageConfig
import java.io.PrintWriter

@Suppress("NAME_SHADOWING")
class KotlinGeneratorUsecaseModule(
	val pkg: PackageConfig,
	val useCaseDef: PackageConfig,
	val useCaseImpl: PackageConfig,
) {

	fun writeEndpoints(groups: List<EndpointGroup>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)

		val className = "UseCasesModule"

		PrintWriter("$directory/$className.kt").use { file ->
			val writer = BaseWriter(file)

			writer.writeLine("package " + pkg.toPackage())
			writer.writeLine("")
			writer.writeLine("import dagger.Module")
			writer.writeLine("import dagger.Binds")
			writer.writeLine("import " + useCaseDef.toPackage() + ".*")
			writer.writeLine("import " + useCaseImpl.toPackage() + ".*")
			writer.writeLine("")

			writer.writeLine("@Module")
			writer.writeLine("interface $className {")

			IndentedWriter(writer).use { writer ->
				groups.forEach { group ->
					writeEndpoint(writer, group)
				}
			}

			writer.writeLine("}")
			file.flush()
		}
	}

	fun writeEndpoint(writer: GeneratorWriter, endpoint: EndpointGroup) {

		val className = endpoint.usecaseClassName()
		writer.writeLine("")
		writer.writeLine("@Binds")
		writer.writeLine("fun bind$className(impl: ${className}Impl): $className")
	}
}
