package org.jboss.sbomer.cli.test.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.enterprise.inject.Alternative;

import org.jboss.sbomer.feature.sbom.cli.command.MavenCycloneDxGenerateCommand;
import org.jboss.sbomer.feature.sbom.cli.command.ProcessCommand;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Alternative
@Slf4j
@Command(
		mixinStandardHelpOptions = true,
		name = "maven-cyclonedx-plugin",
		aliases = { "maven-cyclonedx" },
		description = "SBOM generation for Maven projects using the CycloneDX Maven plugin",
		subcommands = { ProcessCommand.class })
public class MavenCycloneDxGenerateCommandMockAlternative extends MavenCycloneDxGenerateCommand {

	@Override
	protected void doClone(String url, String tag, Path path, boolean force) {
		log.info("Would clone url: {}, with tag: {}, into: {}, force: {}", url, tag, path, force);
	}

	@Override
	protected Path doGenerate() {

		try {
			Files.copy(getClass().getClassLoader().getResourceAsStream("boms/plain.json"), getParent().getOutput());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return getParent().getOutput();
	}

}
