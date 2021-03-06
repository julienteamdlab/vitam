/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 *
 * contact.vitam@culture.gouv.fr
 *
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 *
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */

package fr.gouv.vitam.common.format.identification.siegfried;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.VitamConfiguration;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.exception.VitamApplicationServerException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.server.application.AbstractVitamApplication;
import fr.gouv.vitam.common.server.application.configuration.DefaultVitamApplicationConfiguration;
import fr.gouv.vitam.common.server.application.junit.VitamJerseyTest;

public class SiegfriedClientRestTest extends VitamJerseyTest {

    private static final String HOSTNAME = "localhost";
    private SiegfriedClientRest client;

    private static final String SAMPLE_VERSION_RESPONSE = "version-response.json";
    private static final String SAMPLE_OK_RESPONSE = "ok-response.json";

    private static final JsonNode JSON_NODE_VERSION = getJsonNode(SAMPLE_VERSION_RESPONSE);
    private static final JsonNode JSON_NODE_RESPONSE_OK = getJsonNode(SAMPLE_OK_RESPONSE);


    // ************************************** //
    // Start of VitamJerseyTest configuration //
    // ************************************** //
    public SiegfriedClientRestTest() {
        super(SiegfriedClientFactory.getInstance());
    }

    // Override the beforeTest if necessary
    @Override
    public void beforeTest() throws VitamApplicationServerException {
        client = (SiegfriedClientRest) getClient();
    }

    // Define the getApplication to return your Application using the correct Configuration
    @Override
    public StartApplicationResponse<AbstractApplication> startVitamApplication(int reservedPort) {
        final TestVitamApplicationConfiguration configuration = new TestVitamApplicationConfiguration();
        configuration.setJettyConfig(DEFAULT_XML_CONFIGURATION_FILE);
        final AbstractApplication application = new AbstractApplication(configuration);
        try {
            application.start();
        } catch (final VitamApplicationServerException e) {
            throw new IllegalStateException("Cannot start the application", e);
        }
        SiegfriedClientFactory.changeMode(HOSTNAME, reservedPort);
        return new StartApplicationResponse<AbstractApplication>()
            .setServerPort(application.getVitamServer().getPort())
            .setApplication(application);
    }

    // Define your Application class if necessary
    public final class AbstractApplication
        extends AbstractVitamApplication<AbstractApplication, TestVitamApplicationConfiguration> {
        protected AbstractApplication(TestVitamApplicationConfiguration configuration) {
            super(TestVitamApplicationConfiguration.class, configuration);
        }

        @Override
        protected void configureVitamParameters() {
            VitamConfiguration.setSecret("vitamsecret");
            VitamConfiguration.setFilterActivation(false);
        }

        @Override
        protected void registerInResourceConfig(ResourceConfig resourceConfig) {
            resourceConfig.registerInstances(new MockResource(mock));
        }

        @Override
        protected boolean registerInAdminConfig(ResourceConfig resourceConfig) {
            // do nothing as @admin is not tested here
            return false;
        }
    }
    // Define your Configuration class if necessary
    public static class TestVitamApplicationConfiguration extends DefaultVitamApplicationConfiguration {

    }

    private static JsonNode getJsonNode(String file) {
        try {
            return JsonHandler.getFromFile(PropertiesUtils.findFile(file));
        } catch (final Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Path("/identify")
    public static class MockResource {
        private final ExpectedResults expectedResponse;

        public MockResource(ExpectedResults expectedResponse) {
            this.expectedResponse = expectedResponse;
        }

        @GET
        @Path("/{encoded64}")
        @Produces(MediaType.APPLICATION_JSON)
        public Response getStatus() {
            return expectedResponse.get();
        }
    }

    @Test
    public void statusExecutionWithResponse() throws Exception {
        when(mock.get())
            .thenReturn(Response.status(Response.Status.OK).entity(JSON_NODE_VERSION).build());
        RequestResponse<JsonNode> jsonNodeRequestResponse = client.status(Paths.get("Path"));
        assertTrue(jsonNodeRequestResponse.toJsonNode().has("$results"));
        assertEquals("1.6.4", jsonNodeRequestResponse.toJsonNode().get("$results").get(0).get("siegfried").asText());
    }

    @Test(expected = FormatIdentifierNotFoundException.class)
    public void statusExecutionNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.status(Paths.get("Path"));
    }

    @Test(expected = FormatIdentifierTechnicalException.class)
    public void statusExecutionInternalError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.status(Paths.get("Path"));
    }

    @Test
    public void analysePathExecutionWithResponse() throws Exception {
        when(mock.get())
            .thenReturn(Response.status(Response.Status.OK).entity(JSON_NODE_RESPONSE_OK).build());
        RequestResponse<JsonNode> jsonNodeRequestResponse = client.analysePath(Paths.get("Path"));
        assertTrue(jsonNodeRequestResponse.toJsonNode().has("$results"));
        assertNotNull(jsonNodeRequestResponse.toJsonNode().get("$results").get(0).get("files"));
    }

    @Test(expected = FormatIdentifierNotFoundException.class)
    public void analysePathExecutionNotFound() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.NOT_FOUND).build());
        client.analysePath(Paths.get("Path"));
    }

    @Test(expected = FormatIdentifierTechnicalException.class)
    public void analysePathExecutionInternalError() throws Exception {
        when(mock.get()).thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        client.analysePath(Paths.get("Path"));
    }
}
