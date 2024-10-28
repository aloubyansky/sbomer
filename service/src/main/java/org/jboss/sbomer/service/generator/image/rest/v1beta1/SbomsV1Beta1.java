/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.generator.image.rest.v1beta1;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.sbomer.core.dto.BaseSbomRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1SbomRecord;
import org.jboss.sbomer.core.errors.ErrorResponse;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.utils.PaginationParameters;
import org.jboss.sbomer.service.feature.sbom.mapper.V1Beta1Mapper;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.annotation.security.PermitAll;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1beta1/sboms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@PermitAll
@Tag(name = "v1beta1")
public class SbomsV1Beta1 {
    @Inject
    V1Beta1Mapper mapper;

    @Inject
    SbomService sbomService;

    @GET

    @Operation(summary = "Search SBOMs", description = "List paginated SBOMs using RSQL advanced search.")
    @Parameter(
            name = "query",
            description = "A RSQL query to search the SBOMs",
            examples = {
                    @ExampleObject(name = "Find all SBOMs with provided buildId", value = "buildId=eq=ABCDEFGHIJKLM"),
                    @ExampleObject(
                            name = "Find all SBOMs with provided purl",
                            value = "rootPurl=eq='pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar'") })
    @Parameter(
            name = "sort",
            description = "Optional RSQL sort",
            examples = { @ExampleObject(name = "Order SBOMs by id in ascending order", value = "id=asc="),
                    @ExampleObject(
                            name = "Order SBOMs by creation time in descending order",
                            value = "creationTime=desc=") })
    @APIResponse(responseCode = "200", description = "List of SBOMs in the system for a specified RSQL query.")
    @APIResponse(
            responseCode = "400",
            description = "Failed while parsing the provided RSQL string, please verify the correct syntax.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public Page<BaseSbomRecord> searchSboms(
            @Valid @BeanParam PaginationParameters paginationParams,
            @QueryParam("query") String rsqlQuery,
            @DefaultValue("creationTime=desc=") @QueryParam("sort") String sort) {

        // TODO: Pagination should be done here, not in the service
        Page<BaseSbomRecord> sboms = sbomService.searchSbomRecordsByQueryPaginated(
                paginationParams.getPageIndex(),
                paginationParams.getPageSize(),
                rsqlQuery,
                sort);

        return sboms;
    }

    @GET
    @Path("/{id}")
    @Operation(
            summary = "Get specific manifest",
            description = "Get specific manifest for the provided identifier. Both, manifest identifer and purl's are supported.")
    @Parameter(
            name = "id",
            description = "Manifest generation request identifier or purl",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Generation request identifier"),
                    @ExampleObject(
                            value = "pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar",
                            name = "Package URL") })
    @APIResponse(
            responseCode = "200",
            description = "The SBOM",
            content = @Content(schema = @Schema(implementation = V1Beta1SbomRecord.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Requested SBOM could not be found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public V1Beta1SbomRecord getSbomById(@PathParam("id") String identifier) {
        Sbom sbom = sbomService.get(identifier);

        if (sbom == null) {
            sbom = sbomService.findByPurl(identifier);
        }

        if (sbom == null) {
            throw new NotFoundException(
                    "Manifest with could not be found for provided identifier: '" + identifier + "'");
        }

        return mapper.toRecord(sbom);
    }

    @GET
    @Path("/{id}/bom")
    @Operation(
            summary = "Get the BOM content of particular SBOM",
            description = "Get the BOM content of particular SBOM")
    @Parameter(
            name = "id",
            description = "Manifest generation request identifier or purl",
            examples = { @ExampleObject(value = "88CA2291D4014C6", name = "Generation request identifier"),
                    @ExampleObject(
                            value = "pkg:maven/com.github.michalszynkiewicz.test/empty@1.0.0.redhat-00270?type=jar",
                            name = "Package URL") })
    @APIResponse(
            responseCode = "200",
            description = "The BOM in CycloneDX format",
            content = @Content(schema = @Schema(implementation = Map.class)))
    @APIResponse(
            responseCode = "400",
            description = "Could not parse provided arguments",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "404",
            description = "Requested SBOM could not be found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @APIResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public JsonNode getBomById(@PathParam("id") String identifier) {
        Sbom sbom = sbomService.get(identifier);

        if (sbom == null) {
            sbom = sbomService.findByPurl(identifier);
        }

        if (sbom == null) {
            throw new NotFoundException(
                    "Manifest with could not be found for provided identifier: '" + identifier + "'");
        }

        // TODO: We probably should ensure proper foormatting (ordering of keys)
        return sbom.getSbom();
    }
}
