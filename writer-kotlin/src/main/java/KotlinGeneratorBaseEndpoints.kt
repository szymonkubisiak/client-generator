import models.AccessGroup
import models.Endpoint
import models.EndpointGroup
import models.Param
import models.Profile
import utils.PackageConfig

abstract class KotlinGeneratorBaseEndpoints(pkg: PackageConfig) : KotlinGeneratorBase(pkg) {

	abstract fun fileName(endpoint: EndpointGroup): String
	abstract fun writeEndpointInternal(writer: GeneratorWriter, groupName: EndpointGroup, endpoints: List<Endpoint>)
	open fun writeExtras() {}

	data class Group(val name: EndpointGroup, val contents: List<Endpoint>)

	fun writeEndpoits(groups: List<Group>) {
		pkg.createAndCleanupDirectory()

		groups.forEach { group ->
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
			return param.location == Param.Location.HEADER
		}

		fun isParamNotImplicit(param: Param) = !isParamImplicit(param)

		/** one group per tag; untagged endpoints fly solo, one group each */
		fun groupByTags(input: List<Endpoint>): List<Group> {
			val tagged = input.flatMap { it.tags }
				.distinct()
				.map { tag ->
					Group(tag, input.filter { it.tags.contains(tag) })
				}

			val solo = input.filter { it.tags.isEmpty() }
				.map { one ->
					Group(one, listOf(one))
				}

			return tagged + solo
		}

		/** two groups by access level: no JWT is public, mandatory or optional JWT is logged-in;
		 * profile regexes override the split for endpoints whose access level doesn't follow their security
		 * (e.g. login/token calls need no JWT yet belong to the logged-in area) */
		fun groupBySecurity(input: List<Endpoint>): List<Group> {
			val profile = Profile.active
			val jwtScheme = profile.jwtScheme ?: return emptyList()

			val (loggedIn, public) = input.partition { endpoint ->
				when {
					profile.loggedInOverrideRegex?.matches(endpoint.name) == true -> true
					profile.publicOverrideRegex?.matches(endpoint.name) == true -> false
					endpoint.tags.any { profile.loggedInTags.contains(it.key) } -> true
					else -> endpoint.security?.any { it.key == jwtScheme } == true
				}
			}

			return listOf(
				Group(AccessGroup("public"), public),
				Group(AccessGroup("loggedIn"), loggedIn),
			).filter { it.contents.isNotEmpty() }
		}
	}
}