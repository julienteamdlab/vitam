/*
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
package fr.gouv.vitam.worker.core.plugin.preservation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.batch.report.client.BatchReportClient;
import fr.gouv.vitam.batch.report.client.BatchReportClientFactory;
import fr.gouv.vitam.batch.report.model.PreservationReportModel;
import fr.gouv.vitam.batch.report.model.ReportBody;
import fr.gouv.vitam.batch.report.model.ReportExportRequest;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamClientInternalException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.exception.VitamRuntimeException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.common.model.request.ObjectDescription;

import java.util.List;
import java.util.stream.Collectors;

import static fr.gouv.vitam.batch.report.model.ReportType.PRESERVATION;
import static fr.gouv.vitam.storage.engine.common.model.DataCategory.REPORT;

/**
 * PreservationReportService
 */
public class PreservationReportService {

    private static final String PRESERVATION_REPORT = "preservationReport";

    private final BatchReportClientFactory batchReportClientFactory;
    private final StorageClientFactory storageClientFactory;

    public PreservationReportService() {
        this(BatchReportClientFactory.getInstance(), StorageClientFactory.getInstance());
    }

    @VisibleForTesting
    public PreservationReportService(BatchReportClientFactory reportFactory,
        StorageClientFactory storageClientFactory) {
        this.batchReportClientFactory = reportFactory;
        this.storageClientFactory = storageClientFactory;
    }

    public void appendPreservationEntries(String processId, List<PreservationReportModel> preservationReportModel)
        throws VitamClientInternalException {
        List<JsonNode> metadata = preservationReportModel.stream()
            .map(PreservationReportService::mapToJson)
            .collect(Collectors.toList());

        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            ReportBody reportBody = new ReportBody(processId, PRESERVATION, metadata);
            batchReportClient.appendReportEntries(reportBody);
        }
    }

    public void exportReport(String processId, String workspaceContainerGUID) throws VitamException {
        String filename = String.format("%s-%s.jsonl", PRESERVATION_REPORT, processId);
        try (BatchReportClient batchReportClient = batchReportClientFactory.getClient()) {
            batchReportClient.generatePreservationReport(processId, new ReportExportRequest(filename));
        }
        try(StorageClient client = storageClientFactory.getClient()) {
            ObjectDescription description = new ObjectDescription(REPORT, workspaceContainerGUID, filename, filename);
            client.storeFileFromWorkspace("default", REPORT, filename, description);
        }
    }

    private static JsonNode mapToJson(PreservationReportModel model) {
        try {
            return JsonHandler.toJsonNode(model);
        } catch (InvalidParseOperationException e) {
            throw new VitamRuntimeException("Could not serialize entries", e);
        }
    }
}
