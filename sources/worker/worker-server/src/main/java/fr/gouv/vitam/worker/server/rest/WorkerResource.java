package fr.gouv.vitam.worker.server.rest;

/*******************************************************************************
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
 *******************************************************************************/

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.databind.JsonNode;

import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.security.SanityChecker;
import fr.gouv.vitam.common.server.application.HttpHeaderHelper;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.worker.common.DescriptionStep;
import fr.gouv.vitam.worker.core.api.Worker;
import fr.gouv.vitam.worker.core.impl.WorkerFactory;
import fr.gouv.vitam.worker.core.plugin.PluginLoader;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

/**
 * Worker Resource implementation
 */
@Path("/worker/v1")
@javax.ws.rs.ApplicationPath("webresources")
public class WorkerResource extends ApplicationStatusResource {
    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(WorkerResource.class);
    private static final String WORKER_MODULE = "WORKER";
    private static final String CODE_VITAM = "code_vitam";

    public final WorkerFactory WORKER_FACTORY;

    private final int tenantId = 0;
    private final Worker workerMocked;

    /**
     * Constructor
     *
     * @param pluginLoader the plugin loader
     */
    public WorkerResource(PluginLoader pluginLoader) {
        LOGGER.info("init Worker Resource server");
        workerMocked = null;
        WORKER_FACTORY = WorkerFactory.getInstance(pluginLoader);
    }


    /**
     * Constructor for tests
     *
     * @param pluginLoader the plugin loader
     * @param worker the worker service be applied
     */
    WorkerResource(PluginLoader pluginLoader, Worker worker) {
        LOGGER.info("init Worker Resource server");
        workerMocked = worker;
        WORKER_FACTORY = WorkerFactory.getInstance(pluginLoader);
    }

    /**
     * Get a list of running steps
     *
     * @return Response containing the list of steps
     */
    @Path("tasks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getStepsList() {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Submit a step to be launched
     *
     * @param headers http header
     * @param descriptionStepJson the description of the step as a {fr.gouv.vitam.worker.common.DescriptionStep}
     * @return Response containing the status of the step
     */
    @Path("tasks")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submitStep(@Context HttpHeaders headers, JsonNode descriptionStepJson) {
        HttpHeaderHelper.checkVitamHeaders(headers);
        try {
            ParametersChecker.checkParameter("Must have a step description", descriptionStepJson);
            SanityChecker.checkJsonAll(JsonHandler.toJsonNode(descriptionStepJson));
            final ItemStatus responses;
            DescriptionStep descriptionStep = JsonHandler.getFromJsonNode(descriptionStepJson, DescriptionStep.class);
            if (workerMocked == null) {
                try (Worker worker = WORKER_FACTORY.create()) {
                    responses =
                        worker.run(descriptionStep.getWorkParams(),
                            descriptionStep.getStep());
                    return Response.status(Status.OK).entity(responses).build();
                }
            } else {
                responses = workerMocked.run(descriptionStep.getWorkParams(),
                    descriptionStep.getStep());
                return Response.status(Status.OK).entity(responses).build();
            }
        } catch (final InvalidParseOperationException exc) {
            LOGGER.error(exc);
            return Response.status(Status.PRECONDITION_FAILED).entity(getErrorEntity(Status.PRECONDITION_FAILED))
                .build();
        } catch (final IllegalArgumentException | ProcessingException | ContentAddressableStorageServerException exc) {
            LOGGER.error(exc);
            return Response.status(Status.BAD_REQUEST).entity(getErrorEntity(Status.BAD_REQUEST)).build();
        }
    }


    /**
     * Get the status of a step
     *
     * @param idAsync the id of the Async
     * @return Response containing the status of a specific step
     */
    @Path("tasks/{id_async}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStepStatus(@PathParam("id_async") String idAsync) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    /**
     * Modifying a step (pausing, resuming, prioritizing)
     *
     * @param idAsync the id of the Async
     * @return Response containing the status of the step
     */
    @Path("tasks/{id_async}")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response modifyStep(@PathParam("id_async") String idAsync) {
        final Status status = Status.NOT_IMPLEMENTED;
        return Response.status(status).entity(getErrorEntity(status)).build();
    }

    private VitamError getErrorEntity(Status status) {
        return new VitamError(status.name()).setHttpCode(status.getStatusCode()).setContext(WORKER_MODULE)
            .setState(CODE_VITAM).setMessage(status.getReasonPhrase()).setDescription(status.getReasonPhrase());
    }

}
