package xyz.xenondevs.stringremapper

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.RepositorySystemSession
import org.eclipse.aether.artifact.Artifact
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.resolution.ArtifactRequest
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import java.io.File

@Mojo(name = "remap", defaultPhase = LifecyclePhase.PROCESS_CLASSES)
class StringRemapMojo : AbstractMojo() {
    
    @Parameter(defaultValue = "\${project}", readonly = true, required = true)
    private lateinit var project: MavenProject
    
    @Parameter(defaultValue = "\${session}")
    private lateinit var session: MavenSession
    
    @Component
    private lateinit var repoSystem: RepositorySystem
    
    @Parameter(defaultValue = "\${repositorySystemSession}", readonly = true, required = true)
    private lateinit var repoSession: RepositorySystemSession
    
    @Parameter(defaultValue = "\${project.remoteProjectRepositories}", readonly = true, required = true)
    private val repositories: List<RemoteRepository>? = null
    
    @Parameter(name = "version", required = true)
    private lateinit var version: String
    
    @Parameter(name = "goal", required = true)
    private lateinit var goal: String
    
    @Parameter(name = "classesIn", required = true)
    private lateinit var classesIn: List<String>
    
    @Parameter(name = "classesOut", required = true)
    private lateinit var classesOut: List<String>
    
    override fun execute() {
        SLF4JBridgeHandler.removeHandlersForRootLogger()
        SLF4JBridgeHandler.install()
        val logger = LoggerFactory.getLogger(this::class.java)
        
        val classesIn = classesIn.map(::File)
        val classesOut = classesOut.map(::File)
        val goal = RemapGoal.valueOf(goal.uppercase())
        
        logger.debug("Input classes: {}", classesIn)
        logger.debug("Output classes: {}", classesOut)
        logger.debug("Goal: {}", goal)
        
        ProjectRemapper(
            logger, 
            File(project.build.directory),
            classesIn, classesOut, 
            version,
            goal
        ).remap()
    }
    
    private fun resolveArtifact(cords: String): Artifact {
        val artifact = DefaultArtifact(cords)
        val req = ArtifactRequest()
        req.artifact = artifact
        req.repositories = repositories
        return repoSystem.resolveArtifact(repoSession, req).artifact
    }
    
}