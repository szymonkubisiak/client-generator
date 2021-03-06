import Namer.repoClassName
import Namer.serviceClassName
import models.Endpoint
import models.EndpointGroup
import utils.PackageConfig
import java.io.PrintWriter

@Suppress("NAME_SHADOWING")
class KotlinGeneratorRepoModule(
	val pkg: PackageConfig,
	val repoDef: PackageConfig,
	val repoImpl: PackageConfig,
) {

	fun writeEndpoints(input: List<Endpoint>) {
		val directory = pkg.toDir()
		Utils.createDirectories(directory)

		val className = "RepoModule"

		PrintWriter("$directory/$className.kt").use { file ->
			val writer = BaseWriter(file)

			writer.writeLine("package " + pkg.toPackage())
			writer.writeLine("")
			writer.writeLine("import dagger.Module")
			writer.writeLine("import dagger.Binds")
			writer.writeLine("import " + repoDef.toPackage() + ".*")
			writer.writeLine("import " + repoImpl.toPackage() + ".*")
			writer.writeLine("")

			writer.writeLine("@Module")
			writer.writeLine("interface $className {")

			IndentedWriter(writer).use { writer ->
				val tags = input.flatMap { it.tags }.distinct()
				tags.forEach { tag ->
					writeEndpoint(writer, tag)
				}

				input.filter { it.tags.isNullOrEmpty() }
					.forEach { one ->
						writeEndpoint(writer, one)
					}
			}

			writer.writeLine("}")
			file.flush()
		}

		writeExtras(directory)
	}

	fun writeEndpoint(writer: GeneratorWriter, endpoint: EndpointGroup) {

		val className = endpoint.repoClassName()
		writer.writeLine("")
		writer.writeLine("@Binds")
		writer.writeLine("fun bind$className(impl: ${className}Impl): $className")
	}

	fun writeExtras(directory: String) {
		PrintWriter("$directory/RetrofitProvider.kt").use { writer ->
			writeRetrofitProvider(BaseWriter(writer))
			writer.flush()
		}
	}

	private fun writeRetrofitProvider(writer: GeneratorWriter) {
		writer.writeLine("package " + pkg.toPackage())
		writer.writeLine("")
		writer.writeLine("import retrofit2.Retrofit")
		writer.writeLine("")
		writer.writeLine("interface RetrofitProvider {")
		writer.writeLine("\tfun provide(): Retrofit")
		writer.writeLine("}")
	}
}
