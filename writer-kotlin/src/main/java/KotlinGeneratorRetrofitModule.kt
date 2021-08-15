import Namer.serviceClassName
import models.Endpoint
import utils.PackageConfig
import java.io.PrintWriter

@Suppress("NAME_SHADOWING")
class KotlinGeneratorRetrofitModule(
	val pkg: PackageConfig,
	val service: PackageConfig,
) {

	fun writeEndpoints(input: List<Endpoint>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)
		Utils.cleanupDirectory(directory)

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
				val tags = input.flatMap { it.tags }.distinct()
				tags.forEach { tag ->
					writeEndpoint(writer, tag.serviceClassName())
				}

				input.filter { it.tags.isNullOrEmpty() }
					.forEach { one ->
						writeEndpoint(writer, one.serviceClassName())
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
