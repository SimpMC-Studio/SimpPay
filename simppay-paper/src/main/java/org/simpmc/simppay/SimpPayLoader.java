package org.simpmc.simppay;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

public class SimpPayLoader implements PluginLoader {
    @Override
    public void classloader(PluginClasspathBuilder pluginClasspathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://maven-central.storage.googleapis.com/maven2/").build());
        resolver.addRepository(new RemoteRepository.Builder("xenondevs", "default", "https://repo.xenondevs.xyz/releases/").build());

        resolver.addDependency(new Dependency(new DefaultArtifact("xyz.xenondevs.invui:invui:pom:1.49"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.squareup.okhttp3:okhttp:5.0.0-alpha.12"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("de.exlll:configlib-paper:4.8.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.j256.ormlite:ormlite-jdbc:6.1"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.zaxxer:HikariCP:6.3.0"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.h2database:h2:2.3.232"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("org.projectlombok:lombok:1.18.34"), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("commons-codec:commons-codec:1.18.0"), null));
        pluginClasspathBuilder.addLibrary(resolver);
    }
}
