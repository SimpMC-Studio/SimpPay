package org.simpmc.simppay;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

import java.util.ArrayList;
import java.util.List;

public class SimpPayLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder pluginClasspathBuilder) {
        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addRepository(new RemoteRepository.Builder("central", "default", "https://maven-central.storage.googleapis.com/maven2/").build());
        resolver.addRepository(new RemoteRepository.Builder("xenondevs", "default", "https://repo.xenondevs.xyz/releases/").build());
        resolver.addRepository(new RemoteRepository.Builder("spaceio", "default", "https://repo.spaceio.xyz/repository/maven-snapshots/").build());

        List<String> list = List.of(
                "xyz.xenondevs.invui:invui:pom:1.49",
                "com.squareup.okhttp3:okhttp:5.0.0-alpha.12",
                "de.exlll:configlib-paper:4.8.0",
                "com.j256.ormlite:ormlite-jdbc:6.1",
                "com.zaxxer:HikariCP:6.3.0",
                "com.h2database:h2:2.3.232",
                "org.projectlombok:lombok:1.18.34",
                "commons-codec:commons-codec:1.18.0"
        );

        list.forEach(artifact -> {
            resolver.addDependency(new Dependency(new DefaultArtifact(artifact), null));
        });
        pluginClasspathBuilder.addLibrary(resolver);
    }
}
