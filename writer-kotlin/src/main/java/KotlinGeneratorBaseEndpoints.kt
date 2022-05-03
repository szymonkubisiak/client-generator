import models.Endpoint
import models.EndpointGroup
import models.Param
import models.Tag
import utils.PackageConfig

abstract class KotlinGeneratorBaseEndpoints(pkg: PackageConfig) : KotlinGeneratorBase(pkg) {

	abstract fun fileName(endpoint: EndpointGroup): String
	abstract fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>)
	open fun writeExtras() {}

	data class Group(val name: EndpointGroup, val contents: List<Endpoint>)

	fun writeEndpoits(input: List<Endpoint>) {
		pkg.createAndCleanupDirectory()

		input.flatMap { it.tags }
			.distinct()
			.map { tag ->
				Group(tag, input.filter { it.tags.contains(tag) })
			}
			.forEach { group ->
				writeGroup(group.name, group.contents)
			}

		input.filter { it.tags.isEmpty() }
			.map { one ->
				Group(one, listOf(one))
			}
			.forEach { group ->
				writeGroup(group.name, group.contents)
			}

		writeExtras()
	}

	private fun writeGroup(name: EndpointGroup, contents: List<Endpoint>) {
		pkg.openFile("${fileName(name)}.kt").use { writer ->
			writeEndpointInternal(writer, name, contents)
		}
	}

	companion object {
		fun isParamImplicit(param: Param): Boolean {
			return param.mandatory && param.location == Param.Location.HEADER
		}

		fun isParamNotImplicit(param: Param) = !isParamImplicit(param)
	}
}