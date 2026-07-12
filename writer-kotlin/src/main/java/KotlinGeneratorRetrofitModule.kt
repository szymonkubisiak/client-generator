import Namer.serviceClassName
import models.EndpointGroup
import utils.PackageConfig
import java.io.PrintWriter

@Suppress("NAME_SHADOWING")
class KotlinGeneratorRetrofitModule(
	val pkg: PackageConfig,
	val service: PackageConfig,
) {

	fun writeEndpoints(groups: List<EndpointGroup>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)

		val className = "RetrofitModule"

		PrintWriter("$directory/$className.kt").use { file ->
			val writer = BaseWriter(file)

			writer.writeLine("package " + pkg.toPackage())
			writer.writeLine("")
			writer.writeLine("import dagger.Module")
			writer.writeLine("import dagger.Provides")
			writer.writeLine("import " + service.toPackage() + ".*")
			writer.writeLine("")

			writer.writeLine("@Module")
			writer.writeLine("class $className {")

			IndentedWriter(writer).use { writer ->
				groups.forEach { group ->
					writeEndpoint(writer, group.serviceClassName())
				}

				writer.writeLine("private inline fun <reified S> provideService(wrapper: RetrofitProvider) = wrapper.provide().create(S::class.java)")
			}

			writer.writeLine("}")
			file.flush()
		}
	}

	fun writeEndpoint(writer: GeneratorWriter, serviceName: String) {

		writer.writeLine("@Provides")
		writer.writeLine("fun provide$serviceName(wrapper: RetrofitProvider): $serviceName = provideService(wrapper)")
		writer.writeLine("")
	}
}
