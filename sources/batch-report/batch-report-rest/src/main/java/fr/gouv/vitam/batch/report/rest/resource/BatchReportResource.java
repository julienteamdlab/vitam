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
package fr.gouv.vitam.batch.report.rest.resource;

import com.fasterxml.jackson.databind.JsonNode;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.batch.report.rest.repository.EliminationActionUnitRepository;
import fr.gouv.vitam.batch.report.rest.service.BatchReportServiceImpl;
import fr.gouv.vitam.common.GlobalDataRest;
import fr.gouv.vitam.common.ParametersChecker;
import fr.gouv.vitam.common.error.VitamError;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.server.application.resources.ApplicationStatusResource;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * public resource to mass-report
 */
@Path("/batchreport/v1")
public class BatchReportResource extends ApplicationStatusResource {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(EliminationActionUnitRepository.class);
    private BatchReportServiceImpl batchReportServiceImpl;


    public BatchReportResource(BatchReportServiceImpl batchReportServiceImpl) {
        this.batchReportServiceImpl = batchReportServiceImpl;
    }

    @Path("/append")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response appendReport(ReportBody reportBody,
        @HeaderParam(GlobalDataRest.X_TENANT_ID) int tenantId) {

        switch (reportBody.getReportType()) {
            case ELIMINATION_ACTION_UNIT:
                batchReportServiceImpl
                    .appendEliminationActionUnitReport(reportBody.getProcessId(), reportBody.getEntries(), tenantId);
                break;
            case ELIMINATION_ACTION_OBJECTGROUP:
                batchReportServiceImpl
                    .appendEliminationActionObjectGroupReport(reportBody.getProcessId(), reportBody.getEntries(),
                        tenantId);
                break;
            default:
                throw new IllegalStateException("Unsupported report type " + reportBody.getReportType());
        }
        return Response.status(Response.Status.CREATED).build();
    }


    @Path("/elimination_action_unit/unit_export/{processId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response exportUnitReport(
        @PathParam("processId") String processId, JsonNode body)
        throws ContentAddressableStorageServerException, IOException {

        try {
            ParametersChecker.checkParameter("processId should be filed", processId);

            ReportExportRequest reportExportRequest = parseEliminationReportRequest(body);

            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            batchReportServiceImpl
                .exportEliminationActionUnitReport(processId, reportExportRequest.getFilename(), tenantId);
            return Response.status(Response.Status.OK).build();

        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
    }

    @Path("/elimination_action_objectgroup/objectgroup_export/{processId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response exportObjectGroup(@PathParam("processId") String processId, JsonNode body)
        throws ContentAddressableStorageServerException, IOException {

        try {

            ParametersChecker.checkParameter("processId should be filed", processId);

            ReportExportRequest reportExportRequest = parseEliminationReportRequest(body);
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            batchReportServiceImpl
                .exportEliminationActionObjectGroupReport(processId, reportExportRequest.getFilename(), tenantId);
            return Response.status(Response.Status.OK).build();

        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
    }

    @Path("/elimination_action_unit/objectgroup_export/{processId}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response exportDistinctObjectGroup(@PathParam("processId") String processId, JsonNode body)
        throws ContentAddressableStorageServerException, IOException {

        try {

            ParametersChecker.checkParameter("processId should be filed",
                processId);

            ReportExportRequest reportExportRequest = parseEliminationReportRequest(body);
            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();

            batchReportServiceImpl.exportDistinctObjectGroupOfDeletedUnits(
                processId, reportExportRequest.getFilename(), tenantId);
            return Response.status(Response.Status.OK).build();
        } catch (InvalidParseOperationException | IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
    }

    private ReportExportRequest parseEliminationReportRequest(JsonNode body)
        throws InvalidParseOperationException {
        ReportExportRequest reportExportRequest =
            JsonHandler.getFromJsonNode(body, ReportExportRequest.class);
        ParametersChecker.checkParameter(reportExportRequest.getFilename());
        return reportExportRequest;
    }

    @Path("/cleanup/{reportType}/{processId}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteReport(@PathParam("processId") String processId,
        @PathParam("reportType") ReportType reportType) {

        try {

            ParametersChecker
                .checkParameter("ProcessId, reportType should be filed", processId, reportType);

            int tenantId = VitamThreadUtils.getVitamSession().getTenantId();
            switch (reportType) {
                case ELIMINATION_ACTION_UNIT:
                    batchReportServiceImpl.deleteEliminationUnitByProcessId(processId, tenantId);
                    break;
                case ELIMINATION_ACTION_OBJECTGROUP:
                    batchReportServiceImpl.deleteEliminationObjectGroupByIdAndTenant(processId, tenantId);
                    break;
                default:
                    Response.Status status = Response.Status.BAD_REQUEST;
                    VitamError vitamError = new VitamError(status.name()).setHttpCode(status.getStatusCode())
                        .setMessage("Report type not find")
                        .setDescription("Report type not find");
                    return Response.status(status).entity(vitamError).build();
            }
            Response.Status status = Response.Status.NO_CONTENT;
            return Response.status(status).build();

        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
    }
}
