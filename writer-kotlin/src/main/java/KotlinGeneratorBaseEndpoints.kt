
import models.Endpoint
import models.EndpointGroup
import utils.PackageConfig

abstract class KotlinGeneratorBaseEndpoints(pkg: PackageConfig) : KotlinGeneratorBase(pkg) {

	abstract fun fileName(endpoint: EndpointGroup): String
	abstract fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>)

	fun writeEndpoits(input: List<Endpoint>) {
		pkg.createAndCleanupDirectory()

		val tags = input.flatMap { it.tags }.distinct()
		tags.forEach { tag ->
			pkg.openFile("${fileName(tag)}.kt").use { writer ->
				writeEndpointInternal(writer, tag, input.filter { it.tags.contains(tag) })
			}
		}

		input.filter { it.tags.isEmpty() }
			.forEach { one ->
				pkg.openFile("${fileName(one)}.kt").use { writer ->
					writeEndpoint(writer, one)
				}
			}
	}

	private fun writeEndpoint(writer: GeneratorWriter, endpoint: Endpoint) {
		writeEndpointInternal(writer, endpoint, listOf(endpoint))
	}
}