//import SwaggerConverter.swagger2api
import io.swagger.models.Swagger
import io.swagger.parser.SwaggerParser
import io.swagger.parser.util.InlineModelResolver
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.AuthorizationValue
import io.swagger.v3.parser.core.models.ParseOptions
import models.Api
import models.EndpointGroup
import models.Profile
import models.StructActual
import readback.KotlinReadbackTransport
import utils.Package
import utils.PackageConfig
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.PrintWriter
import java.util.*

object Main {

	val properties = try {
		FileInputStream("private.config")
	} catch (ex: FileNotFoundException) {
		FileInputStream("app.config")
	}.use {
			Properties().apply {
				load(it)
			}
		}

	//API-specific customizations; both files are optional — without them the profile is neutral
	val profile = try {
		FileInputStream("private.profile")
	} catch (ex: FileNotFoundException) {
		try {
			FileInputStream("app.profile")
		} catch (ex2: FileNotFoundException) {
			null
		}
	}?.use {
		Profile(Properties().apply { load(it) })
	} ?: Profile(Properties())

	val kotlinTReadback: KotlinReadbackTransport
	val kotlinT: KotlinGeneratorTransport
	val kotlinD: KotlinGeneratorDomain
	val kotlinT2D: KotlinGeneratorConverters
	val kotlinRetrofit: KotlinGeneratorRetrofit
	val kotlinRetrofitModule: KotlinGeneratorRetrofitModule
	val kotlinGeneratorRepo: KotlinGeneratorRepo
	val kotlinGeneratorRepoImpl: KotlinGeneratorRepoImpl
	val kotlinGeneratorRepoModule: KotlinGeneratorRepoModule
	val kotlinGeneratorUsecase: KotlinGeneratorUsecase
	val kotlinGeneratorUsecaseImpl: KotlinGeneratorUsecaseImpl
	val kotlinGeneratorUsecaseModule: KotlinGeneratorUsecaseModule

	init {
		val outputDir = Package(*properties.getProperty("app.kotlin.outputdir").split('/').toTypedArray())
		val outputPackage = Package(*properties.getProperty("app.kotlin.package").split('.').toTypedArray())

		//TODO: move those to some config
		val generatedPrefix = "generated"
		val master = PackageConfig(rootDir = outputDir, project = outputPackage, suffix = Package(generatedPrefix), module = Package.dummy)

		val manualModels = master.copy(module = Package("domain"), suffix = Package("models"))

		val transport = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "models"))
		kotlinT = KotlinGeneratorTransport(transport)

		kotlinTReadback = KotlinReadbackTransport(transport)

		val domain = master.copy(module = Package("domain"), suffix = Package(generatedPrefix, "models"))
		kotlinD = KotlinGeneratorDomain(domain, manualModels)

		val converters = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "converters"))
		kotlinT2D = KotlinGeneratorConverters(converters, kotlinT.pkg, kotlinD.pkg, manualModels)

		val retrofit = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "retrofit"))
		kotlinRetrofit = KotlinGeneratorRetrofit(retrofit, kotlinT.pkg)

		val connModule = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "dagger"))
		kotlinRetrofitModule = KotlinGeneratorRetrofitModule(connModule, kotlinRetrofit.pkg)

		val repo = master.copy(module = Package("domain"), suffix = Package(generatedPrefix, "repos"))
		kotlinGeneratorRepo = KotlinGeneratorRepo(repo, domain)

		val repoImpl = master.copy(module = Package("conn"), suffix = Package(generatedPrefix, "repos"))
		kotlinGeneratorRepoImpl = KotlinGeneratorRepoImpl(repoImpl, retrofit, converters, domain, repo)

		kotlinGeneratorRepoModule = KotlinGeneratorRepoModule(connModule, repo, repoImpl)

		val usecases = master.copy(module = Package("domain"), suffix = Package(generatedPrefix, "usecases"))
		kotlinGeneratorUsecase = KotlinGeneratorUsecase(usecases, domain)

		val usecasesImpl = master.copy(module = Package("domain"), suffix = Package(generatedPrefix, "usecases", "impl"))
		kotlinGeneratorUsecaseImpl = KotlinGeneratorUsecaseImpl(usecasesImpl, domain, usecases, repo)

		val domainModule = master.copy(module = Package("domain"), suffix = Package(generatedPrefix, "dagger"))
		kotlinGeneratorUsecaseModule = KotlinGeneratorUsecaseModule(domainModule, usecases, usecasesImpl)
	}

	@JvmStatic
	fun main(args: Array<String>) {
		println("Hello World!")
		Profile.active = profile
		profile.customTypes.forEach {
			TypeResolver.instance.addType(
				it.transportType, it.format,
				it.kotlinTransport, it.kotlinDomain,
				it.toDomainAdapter, it.toTransportAdapter,
			)
		}

		val inputfile = properties.getProperty("app.inputfile")
		val auth = properties.getProperty("app.inputfileBasicAuth")?.let {
			AuthorizationValue("Authorization", "Basic " + Base64.getEncoder().encodeToString(it.encodeToByteArray()), "header")
		}
		val openAPI: OpenAPI = OpenAPIV3Parser().read(inputfile, listOfNotNull(auth), ParseOptions().apply { isResolve = true })
		val apiTmp = OpenApiConverter().swagger2api(openAPI)

		val reorderedStructs = kotlinTReadback.reorderStructFields(apiTmp.structs)
		val filteredPaths = apiTmp.paths.filter { endpoint ->
			endpoint.tags.none { tag ->
				profile.ignoredTags.contains(tag.key)
			}
		}
		val api = Api(reorderedStructs, filteredPaths)

		val typesWithArtificialId = api.structs
			.mapNotNull { it as? StructActual }
			.filter { it.artificialID != null }

		typesWithArtificialId.forEach {
			val transportType = TypeResolver.instance.resolveTransportType(it.artificialID!!)
			TypeResolver.instance.
				addType(it.artificialID!!.key, "ID:${it.type.key}.ID", transportType, "${it.type.key}.ID", "${it.type.key}.ID(%s)", "%s.internal")
		}

		writeAllToFiles(api)
		//writeSampleToOut(api)
		println("Goodbye World!")
	}

	private fun writeSampleToOut(api: Api) {
		val outWriter = PrintWriter(System.out)
		api.structs.firstOrNull { it.type.key == "BasicTypes" }?.also {
			kotlinT.writeStruct(BaseWriter(outWriter), it)
			kotlinD.writeStruct(BaseWriter(outWriter), it)
			kotlinT2D.writeStruct(BaseWriter(outWriter), it)
		}
		outWriter.flush()
	}

	private fun writeAllToFiles(api: Api) {
		//single decision point: endpoint files are generated once per group, grouped by tag and by access level
		val groups: List<KotlinGeneratorBaseEndpoints.Group> =
			KotlinGeneratorBaseEndpoints.groupByTags(api.paths) +
					//KotlinGeneratorBaseEndpoints.groupBySecurity(api.paths) +
					emptyList()
		//all *Module generators share one alphabetical order, consistent with the group files above
		val moduleGroups: List<EndpointGroup> = groups.map { it.name }.sortedBy { it.key }

		kotlinT.writeStructs(api.structs)
		kotlinD.writeStructs(api.structs)
		kotlinT2D.writeStructs(api.structs)
		kotlinRetrofit.writeEndpoits(groups)
		kotlinRetrofitModule.writeEndpoints(moduleGroups)
		kotlinGeneratorRepo.writeEndpoits(groups)
		kotlinGeneratorRepoImpl.writeEndpoits(groups)
		kotlinGeneratorRepoModule.writeEndpoints(moduleGroups)
		kotlinGeneratorUsecase.writeEndpoits(groups)
		kotlinGeneratorUsecaseImpl.writeEndpoits(groups)
		kotlinGeneratorUsecaseModule.writeEndpoints(moduleGroups)
	}

	private fun parseAndPrepareSwagger(path: String): Swagger {
		val swagger = SwaggerParser().read(path)
		// resolve inline models
		InlineModelResolver().flatten(swagger)
		return swagger
	}
}