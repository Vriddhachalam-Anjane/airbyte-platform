/*
 * Copyright (c) 2023 Airbyte, Inc., all rights reserved.
 */

package io.airbyte.config.specs;

import com.fasterxml.jackson.databind.JsonNode;
import io.airbyte.commons.cli.Clis;
import io.airbyte.commons.constants.AirbyteCatalogConstants;
import io.airbyte.commons.io.IOs;
import io.airbyte.commons.json.Jsons;
import io.airbyte.commons.yaml.Yamls;
import io.airbyte.config.CatalogDefinitionsConfig;
import io.airbyte.config.CombinedConnectorCatalog;
import io.airbyte.config.StandardDestinationDefinition;
import io.airbyte.config.StandardSourceDefinition;
import io.airbyte.protocol.models.ConnectorSpecification;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This script is responsible for generating a set of connection configuration properties that have
 * been marked as <code>secret</code> and therefore should be automatically masked if/when the
 * configuration object is logged.
 * <p>
 * Specs are stored in a separate file from the definitions in an effort to keep the definitions
 * yaml files human-readable and easily-editable, as specs can be rather large.
 * <p>
 * The generated mask file is created in the same location as the spec files provided to this
 * script.
 */
public class ConnectorSpecMaskGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorSpecMaskGenerator.class);

  private static final String LOCAL_CONNECTOR_CATALOG_PATH = CatalogDefinitionsConfig.getLocalCatalogWritePath();

  private static final Option RESOURCE_ROOT_OPTION = Option.builder("r").longOpt("resource-root").hasArg(true).required(true)
      .desc("path to what project to pull resources from").build();
  private static final Options OPTIONS = new Options().addOption(RESOURCE_ROOT_OPTION);

  public static Path getResourcePath(final String projectPath, final String relativePath) {
    return Path.of(projectPath, relativePath);
  }

  public static void main(final String[] args) {
    final CommandLine parsed = Clis.parse(args, OPTIONS);
    final String resource = parsed.getOptionValue(RESOURCE_ROOT_OPTION.getOpt());
    final Path catalogPath = getResourcePath(resource, LOCAL_CONNECTOR_CATALOG_PATH);
    final Path maskWritePath = getResourcePath(resource, AirbyteCatalogConstants.LOCAL_SECRETS_MASKS_PATH);

    LOGGER.info("Looking for catalog file at '{}'...", catalogPath);

    final File inputFile = catalogPath.toFile();

    if (inputFile != null && inputFile.exists()) {
      LOGGER.info("Found catalog for processing.");
      final String jsonString = readFile(inputFile);
      final CombinedConnectorCatalog catalog = Jsons.deserialize(jsonString, CombinedConnectorCatalog.class);
      final Stream<ConnectorSpecification> destinationSpecs = catalog.getDestinations().stream().map(StandardDestinationDefinition::getSpec);
      final Stream<ConnectorSpecification> sourceSpecs = catalog.getSources().stream().map(StandardSourceDefinition::getSpec);

      final Set<String> secretPropertyNames = Stream.concat(destinationSpecs, sourceSpecs)
          .map(ConnectorSpecMaskGenerator::findSecrets)
          .flatMap(Set::stream)
          .collect(Collectors.toCollection(TreeSet::new));

      final String outputString = String.format("# This file is generated by %s.\n", ConnectorSpecMaskGenerator.class.getName())
          + "# Do NOT edit this file directly. See generator class for more details.\n"
          + Yamls.serialize(Map.of("properties", secretPropertyNames));
      IOs.writeFile(maskWritePath, outputString);
      LOGGER.info("Finished generating spec mask file '{}'.", maskWritePath);
    } else {
      throw new RuntimeException(String.format("No catalog files found in '%s'.  Nothing to generate.", resource));
    }
  }

  private static Set<String> findSecrets(final ConnectorSpecification spec) {
    final SpecMaskPropertyGenerator specMaskPropertyGenerator = new SpecMaskPropertyGenerator();
    final JsonNode properties = spec.getConnectionSpecification().get("properties");
    return specMaskPropertyGenerator.getSecretFieldNames(properties);
  }

  private static String readFile(final File file) {
    try {
      LOGGER.info("Reading spec file '{}'...", file.getAbsolutePath());
      return FileUtils.readFileToString(file, Charset.defaultCharset());
    } catch (final IOException e) {
      LOGGER.error("Unable to read contents of '{}'.", file.getAbsolutePath(), e);
      return null;
    }
  }

}
