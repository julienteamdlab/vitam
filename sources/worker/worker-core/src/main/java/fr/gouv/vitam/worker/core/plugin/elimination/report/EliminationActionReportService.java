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
package fr.gouv.vitam.worker.core.plugin.elimination.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.batch.report.model.ReportType;
import fr.gouv.vitam.common.collection.CloseableIterator;
import fr.gouv.vitam.common.collection.CloseableIteratorUtils;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.stream.VitamAsyncInputStream;
import fr.gouv.vitam.worker.core.distribution.JsonLineIterator;
import fr.gouv.vitam.worker.core.distribution.JsonLineModel;
import fr.gouv.vitam.worker.core.plugin.elimination.exception.EliminationException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageNotFoundException;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

public class EliminationActionReportService {

    static final String UNIT_REPORT_JSONL = "unitReport.jsonl";
    static final String OBJECT_GROUP_REPORT_JSONL = "objectGroupReport.jsonl";
    static final String DISTINCT_REPORT_JSONL = "unitObjectGroups.jsonl";
    static final String ACCESSION_REGISTER_REPORT_JSONL = "accession_register.jsonl";

    private final BatchReportClientFactory batchReportClientFactory;
    private final WorkspaceClientFactory workspaceClientFactory;

    public EliminationActionReportService() {
        this(
            BatchReportClientFactory.getInstance(),
            WorkspaceClientFactory.getInstance());
    }

    @VisibleForTesting
    EliminationActionReportService(
        BatchReportClientFactory batchReportClientFactory,
        WorkspaceClientFactory workspaceClientFactory) {
        this.batchReportClientFactory = batchReportClientFactory;
        this.workspaceClientFactory = workspaceClientFactory;
    }

    public void appendUnitEntries(String processId, List<EliminationActionUnitReportEntry> entries)
        throws EliminationException {

        appendEntries(processId, entries, ReportType.ELIMINATION_ACTION_UNIT);
    }

    public void appendObjectGroupEntries(String processId, List<EliminationActionObjectGroupReportEntry> entries)
        throws EliminationException {

        appendEntries(processId, entries, ReportType.ELIMINATION_ACTION_OBJECTGROUP);
    }

    private void appendEntries(String processId, List<?> entries, ReportType reportType)
        throws EliminationException {

        List<JsonNode> metadataEntries = entries.stream()
            .map(EliminationActionReportService::pojoToJson).collect(Collectors.toList());

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            ReportBody reportBody = new ReportBody();
            reportBody.setProcessId(processId);
            reportBody.setReportType(reportType);
            reportBody.setEntries(metadataEntries);
            batchReportClient.appendReportEntries(reportBody);
        } catch (VitamClientInternalException e) {
            throw new EliminationException(StatusCode.FATAL, "Could not append entries into report", e);
        }
    }

    private static JsonNode pojoToJson(Object entry) {
        try {
            return JsonHandler.toJsonNode(entry);
        } catch (InvalidParseOperationException e) {
            throw new RuntimeException("Could not serialize entries", e);
        }
    }

    public CloseableIterator<EliminationActionUnitReportEntry> exportUnits(String processId)
        throws EliminationException {

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            batchReportClient.generateEliminationActionUnitReport(processId,
                new ReportExportRequest(UNIT_REPORT_JSONL));

        } catch (VitamClientInternalException e) {
            throw new EliminationException(StatusCode.FATAL, "Could not generate unit report to workspace", e);
        }

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            Response reportResponse = workspaceClient.getObject(processId, UNIT_REPORT_JSONL);
            JsonLineIterator jsonLineIterator = new JsonLineIterator(new VitamAsyncInputStream(reportResponse));

            return CloseableIteratorUtils.map(jsonLineIterator, jsonLineModel -> {
                try {
                    return JsonHandler
                        .getFromJsonNode(jsonLineModel.getParams(), EliminationActionUnitReportEntry.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException("Could not parse json line entry", e);
                }
            });

        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            throw new EliminationException(StatusCode.FATAL, "Could not load report from workspace", e);
        }
    }

    public CloseableIterator<String> exportDistinctObjectGroups(String processId) throws EliminationException {

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            batchReportClient.generateEliminationActionDistinctObjectGroupInUnitReport(processId,
                new ReportExportRequest(DISTINCT_REPORT_JSONL));

        } catch (VitamClientInternalException e) {
            throw new EliminationException(StatusCode.FATAL,
                "Could not generate distinct object group report for deleted units to workspace", e);
        }

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            Response reportResponse = workspaceClient.getObject(processId, DISTINCT_REPORT_JSONL);
            JsonLineIterator jsonLineIterator = new JsonLineIterator(new VitamAsyncInputStream(reportResponse));

            return CloseableIteratorUtils.map(jsonLineIterator, JsonLineModel::getId);

        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            throw new EliminationException(StatusCode.FATAL, "Could not load report from workspace", e);
        }
    }

    public CloseableIterator<EliminationActionObjectGroupReportExportEntry> exportObjectGroups(String processId)
        throws EliminationException {

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            batchReportClient.generateEliminationActionObjectGroupReport(processId,
                new ReportExportRequest(OBJECT_GROUP_REPORT_JSONL));

        } catch (VitamClientInternalException e) {
            throw new EliminationException(StatusCode.FATAL, "Could not generate unit report to workspace", e);
        }

        try (WorkspaceClient workspaceClient = workspaceClientFactory.getClient()) {

            Response reportResponse = workspaceClient.getObject(processId, OBJECT_GROUP_REPORT_JSONL);
            JsonLineIterator jsonLineIterator = new JsonLineIterator(new VitamAsyncInputStream(reportResponse));

            return CloseableIteratorUtils.map(jsonLineIterator, jsonLineModel -> {
                try {
                    return JsonHandler
                        .getFromJsonNode(jsonLineModel.getParams(),
                            EliminationActionObjectGroupReportExportEntry.class);
                } catch (InvalidParseOperationException e) {
                    throw new RuntimeException("Could not parse json line entry", e);
                }
            });

        } catch (ContentAddressableStorageServerException | ContentAddressableStorageNotFoundException e) {
            throw new EliminationException(StatusCode.FATAL, "Could not load report from workspace", e);
        }
    }

    public void exportAccessionRegisters(String processId) throws EliminationException {

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {

            batchReportClient.generateEliminationActionAccessionRegisterReport(
                processId,
                new ReportExportRequest(ACCESSION_REGISTER_REPORT_JSONL));

        } catch (VitamClientInternalException e) {
            throw new EliminationException(StatusCode.FATAL,
                "Could not generate elimination action accession register reports (" + processId + ")", e);
        }
    }

    public void cleanupReport(String processId) throws EliminationException {
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            batchReportClient.cleanupReport(processId, ReportType.ELIMINATION_ACTION_UNIT);
            batchReportClient.cleanupReport(processId, ReportType.ELIMINATION_ACTION_OBJECTGROUP);
        } catch (VitamClientInternalException e) {
            throw new EliminationException(StatusCode.FATAL,
                "Could not cleanup elimination action reports (" + processId + ")", e);
        }
    }
}
