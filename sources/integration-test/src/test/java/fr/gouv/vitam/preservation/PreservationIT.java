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
package fr.gouv.vitam.preservation;

import static fr.gouv.vitam.batch.report.model.PreservationStatus.OK;
import static fr.gouv.vitam.common.VitamServerRunner.NB_TRY;
import static fr.gouv.vitam.common.VitamServerRunner.PORT_SERVICE_ACCESS_INTERNAL;
import static fr.gouv.vitam.common.VitamServerRunner.SLEEP_TIME;
import static fr.gouv.vitam.common.client.VitamClientFactoryInterface.VitamClientType.PRODUCTION;
import static fr.gouv.vitam.common.database.builder.query.QueryHelper.exists;
import static fr.gouv.vitam.common.guid.GUIDFactory.newGUID;
import static fr.gouv.vitam.common.guid.GUIDFactory.newOperationLogbookGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromFileAsTypeRefence;
import static fr.gouv.vitam.common.json.JsonHandler.getFromStringAsTypeRefence;
import static fr.gouv.vitam.common.json.JsonHandler.writeAsFile;
import static fr.gouv.vitam.common.model.PreservationVersion.FIRST;
import static fr.gouv.vitam.common.model.PreservationVersion.LAST;
import static fr.gouv.vitam.common.model.administration.ActionTypePreservation.GENERATE;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.elimination.EndToEndEliminationIT.prepareVitamSession;
import static fr.gouv.vitam.metadata.client.MetaDataClientFactory.getInstance;
import static fr.gouv.vitam.preservation.ProcessManagementWaiter.waitOperation;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static fr.gouv.vitam.common.json.JsonHandler.getFromInputStream;

import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Sorts;
import fr.gouv.vitam.access.internal.client.AccessInternalClient;
import fr.gouv.vitam.access.internal.client.AccessInternalClientFactory;
import fr.gouv.vitam.access.internal.rest.AccessInternalMain;
import fr.gouv.vitam.batch.report.rest.BatchReportMain;
import fr.gouv.vitam.common.CommonMediaType;
import fr.gouv.vitam.common.DataLoader;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.VitamRuleRunner;
import fr.gouv.vitam.common.VitamServerRunner;
import fr.gouv.vitam.common.accesslog.AccessLogUtils;
import fr.gouv.vitam.common.client.VitamClientFactory;
import fr.gouv.vitam.common.database.builder.query.QueryHelper;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamException;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.PreservationRequest;
import fr.gouv.vitam.common.model.ProcessAction;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.administration.AccessContractModel;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.ActivationStatus;
import fr.gouv.vitam.common.model.administration.GriffinModel;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
import fr.gouv.vitam.common.model.objectgroup.ObjectGroupResponse;
import fr.gouv.vitam.common.model.objectgroup.VersionsModel;
import fr.gouv.vitam.common.model.processing.WorkFlow;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.client.AdminManagementClient;
import fr.gouv.vitam.functional.administration.client.AdminManagementClientFactory;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.griffin.GriffinReport;
import fr.gouv.vitam.functional.administration.rest.AdminManagementMain;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClient;
import fr.gouv.vitam.ingest.internal.client.IngestInternalClientFactory;
import fr.gouv.vitam.ingest.internal.upload.rest.IngestInternalMain;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParametersFactory;
import fr.gouv.vitam.logbook.common.parameters.LogbookTypeProcess;
import fr.gouv.vitam.logbook.rest.LogbookMain;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.core.database.collections.MetadataCollections;
import fr.gouv.vitam.metadata.core.database.collections.ObjectGroup;
import fr.gouv.vitam.metadata.rest.MetadataMain;
import fr.gouv.vitam.processing.data.core.ProcessDataAccessImpl;
import fr.gouv.vitam.processing.management.rest.ProcessManagementMain;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.exception.StorageNotFoundException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.server.rest.StorageMain;
import fr.gouv.vitam.storage.offers.common.rest.DefaultOfferMain;
import fr.gouv.vitam.worker.core.plugin.preservation.model.InputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.OutputPreservation;
import fr.gouv.vitam.worker.core.plugin.preservation.model.ResultPreservation;
import fr.gouv.vitam.worker.server.rest.WorkerMain;
import fr.gouv.vitam.workspace.rest.WorkspaceMain;
import net.javacrumbs.jsonunit.JsonAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.bson.Document;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Ingest Internal integration test
 */

public class PreservationIT extends VitamRuleRunner {
    private static final Integer tenantId = 0;
    private static final String contractId = "contract";
    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW";
    private static final String WORKFLOW_IDENTIFIER = "PROCESS_SIP_UNITARY";
    private WorkFlow workflow = WorkFlow.of(CONTEXT_ID, WORKFLOW_IDENTIFIER, "INGEST");

    private static final HashSet<Class> servers = Sets.newHashSet(
        AccessInternalMain.class,
        AdminManagementMain.class,
        ProcessManagementMain.class,
        LogbookMain.class,
        WorkspaceMain.class,
        MetadataMain.class,
        WorkerMain.class,
        IngestInternalMain.class,
        StorageMain.class,
        DefaultOfferMain.class,
        BatchReportMain.class
    );

    private static final String mongoName = mongoRule.getMongoDatabase().getName();
    private static final String esName = elasticsearchRule.getClusterName();
    private static final String GRIFFIN_LIBREOFFICE = "griffin-libreoffice";

    @Rule
    public TemporaryFolder tmpGriffinFolder = new TemporaryFolder();

    @ClassRule
    public static VitamServerRunner runner = new VitamServerRunner(PreservationIT.class, mongoName, esName, servers);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        handleBeforeClass(0, 1);
        String configurationPath =
            PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString();
        FormatIdentifierFactory.getInstance().changeConfigurationFile(configurationPath);
        new DataLoader("integration-ingest-internal").prepareData();

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        handleAfterClass(0, 1);
        runAfter();
        VitamClientFactory.resetConnections();
    }

    @Before
    public void setUpBefore() throws Exception {
        getVitamSession().setRequestId(newOperationLogbookGUID(0));
        getVitamSession().setTenantId(tenantId);
        File griffinsExecFolder = PropertiesUtils.getResourceFile("preservation" + File.separator);
        VitamConfiguration.setVitamGriffinExecFolder(griffinsExecFolder.getAbsolutePath());
        VitamConfiguration.setVitamGriffinInputFilesFolder(tmpGriffinFolder.getRoot().getAbsolutePath());

        AccessInternalClientFactory factory = AccessInternalClientFactory.getInstance();
        factory.changeServerPort(PORT_SERVICE_ACCESS_INTERNAL);
        factory.setVitamClientType(PRODUCTION);

        Path griffinExecutable = griffinsExecFolder.toPath().resolve("griffin-libreoffice/griffin");
        boolean griffinIsExecutable = griffinExecutable.toFile().setExecutable(true);
        if (!griffinIsExecutable) {
            throw new IllegalStateException("Wrong path");
        }

        AccessContractModel contract = getAccessContractModel();
        AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();

        client.importAccessContracts(singletonList(contract));

        getVitamSession().setTenantId(1);
        getVitamSession().setRequestId(newGUID());

        List<GriffinModel> griffinModelList = getGriffinModels("preservation/griffins.json");
        client.importGriffins(griffinModelList);

        getVitamSession().setTenantId(0);
        getVitamSession().setRequestId(newGUID());
        List<PreservationScenarioModel> preservationScenarioModelList = getPreservationScenarioModels();

        client.importPreservationScenarios(preservationScenarioModelList);

        doIngest("elimination/TEST_ELIMINATION.zip");
        doIngest("preservation/OG_with_3_parents.zip");

        FormatIdentifierFactory.getInstance()
            .changeConfigurationFile(
                PropertiesUtils.getResourcePath("integration-ingest-internal/format-identifiers.conf").toString());
    }

    private void doIngest(String zip) throws FileNotFoundException, VitamException {

        final GUID ingestOperationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
        prepareVitamSession();

        VitamThreadUtils.getVitamSession().setRequestId(ingestOperationGuid);

        final InputStream zipInputStreamSipObject =
            PropertiesUtils.getResourceAsStream(zip);

        // init default logbook operation
        final List<LogbookOperationParameters> params = new ArrayList<>();
        final LogbookOperationParameters initParameters = LogbookParametersFactory.newLogbookOperationParameters(
            ingestOperationGuid, "Process_SIP_unitary", ingestOperationGuid,
            LogbookTypeProcess.INGEST, StatusCode.STARTED,
            ingestOperationGuid.toString(), ingestOperationGuid);
        params.add(initParameters);

        // call ingest
        IngestInternalClientFactory.getInstance().changeServerPort(VitamServerRunner.PORT_SERVICE_INGEST_INTERNAL);
        final IngestInternalClient client = IngestInternalClientFactory.getInstance().getClient();
        final Response response2 = client.uploadInitialLogbook(params);
        assertEquals(response2.getStatus(), Response.Status.CREATED.getStatusCode());

        // init workflow before execution
        client.initWorkflow(workflow);

        client.upload(zipInputStreamSipObject, CommonMediaType.ZIP_TYPE, workflow, ProcessAction.RESUME.name());

        waitOperation(NB_TRY, SLEEP_TIME, ingestOperationGuid.getId());
    }

    private void buildAndSavePreservationResultFile() throws IOException, InvalidParseOperationException {

        Map<String, String> objectIdsToFormat = getAllBinariesIds();

        ResultPreservation resultPreservation = new ResultPreservation();

        resultPreservation.setId("batchId");
        resultPreservation.setRequestId(getVitamSession().getRequestId());

        Map<String, List<OutputPreservation>> values = new HashMap<>();

        for (Map.Entry<String, String> entry : objectIdsToFormat.entrySet()) {

            List<OutputPreservation> outputPreservationList = new ArrayList<>();
            for (ActionTypePreservation action : singletonList(GENERATE)) {

                OutputPreservation outputPreservation = new OutputPreservation();

                outputPreservation.setStatus(OK);
                outputPreservation.setAnalyseResult("VALID_ALL");
                outputPreservation.setAction(action);

                outputPreservation.setInputPreservation(new InputPreservation(entry.getKey(), entry.getValue()));
                outputPreservation.setOutputName("GENERATE-" + entry.getKey() + ".pdf");
                outputPreservationList.add(outputPreservation);
            }

            values.put(entry.getKey(), outputPreservationList);
        }

        resultPreservation.setOutputs(values);
        Path griffinIdDirectory = tmpGriffinFolder.newFolder(GRIFFIN_LIBREOFFICE).toPath();
        writeAsFile(resultPreservation, griffinIdDirectory.resolve("result.json").toFile());
    }

    private Map<String, String> getAllBinariesIds() {

        List<ObjectGroupResponse> objectModelsForUnitResults = getAllObjectModels();

        Map<String, String> allObjectIds = new HashMap<>();

        for (ObjectGroupResponse objectGroup : objectModelsForUnitResults) {

            Optional<VersionsModel> versionsModelOptional =
                objectGroup.getFirstVersionsModel("BinaryMaster");

            VersionsModel model = versionsModelOptional.get();
            allObjectIds.put(model.getId(), model.getFormatIdentification().getFormatId());
        }
        return allObjectIds;
    }

    private List<ObjectGroupResponse> getAllObjectModels() {

        try (MetaDataClient client = getInstance().getClient()) {
            Select select = new Select();
            select.setQuery(exists("#id"));

            ObjectNode finalSelect = select.getFinalSelect();
            JsonNode response = client.selectObjectGroups(finalSelect);

            JsonNode results = response.get("$results");
            return getFromStringAsTypeRefence(results.toString(), new TypeReference<List<ObjectGroupResponse>>() {
            });

        } catch (VitamException | InvalidFormatException | InvalidCreateOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<PreservationScenarioModel> getPreservationScenarioModels() throws Exception {
        File resourceFile = PropertiesUtils.getResourceFile("preservation/scenarios.json");
        return getFromFileAsTypeRefence(resourceFile, new TypeReference<List<PreservationScenarioModel>>() {
        });
    }

    private List<GriffinModel> getGriffinModels(String resourcesFile)
        throws FileNotFoundException, InvalidParseOperationException {

        File resourceFile = PropertiesUtils.getResourceFile(resourcesFile);
        return getFromFileAsTypeRefence(resourceFile, new TypeReference<List<GriffinModel>>() {
        });
    }

    private AccessContractModel getAccessContractModel() {
        AccessContractModel contract = new AccessContractModel();
        contract.setName(contractId);
        contract.setIdentifier(contractId);
        contract.setStatus(ActivationStatus.ACTIVE);
        contract.setEveryOriginatingAgency(true);
        contract.setCreationdate("10/12/1800");
        contract.setActivationdate("10/12/1800");
        contract.setDeactivationdate("31/12/4200");
        return contract;
    }


    @Test
    @RunWithCustomExecutor
    public void should_import_griffin_with_warning() throws Exception {
        getVitamSession().setTenantId(0);


        try (AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient();
            StorageClient storageClient = StorageClientFactory.getInstance().getClient();
        ) {

            GUID guid = newGUID();
            getVitamSession().setRequestId(guid);

            // Given
            List<GriffinModel> griffinModelList = getGriffinModels("preservation/griffins.json");
            // When
            client.importGriffins(griffinModelList);

            // Then
            String requestId = getVitamSession().getRequestId();

            GriffinReport griffinReport = getGriffinReport(storageClient, requestId);

            assertThat(griffinReport.getStatusCode()).isEqualTo(StatusCode.OK);

            guid = newGUID();
            getVitamSession().setRequestId(guid);
            // given

            griffinModelList = getGriffinModels("preservation/griffins_updated.json");
            client.importGriffins(griffinModelList);

            // When

            requestId = getVitamSession().getRequestId();

            griffinReport = getGriffinReport(storageClient, requestId);

            //Then

            assertThat(griffinReport.getStatusCode()).isEqualTo(StatusCode.WARNING);

        }
    }

    private GriffinReport getGriffinReport(StorageClient storageClient, String requestId)
        throws StorageServerClientException, StorageNotFoundException, InvalidParseOperationException {

        Response response = storageClient.getContainerAsync("default", requestId + ".json",
            DataCategory.REPORT, AccessLogUtils.getNoLogAccessLog());

        return getFromInputStream((InputStream) response.getEntity(), GriffinReport.class);
    }

    @Test
    @RunWithCustomExecutor
    public void should_import_preservationReferential() throws Exception {
        getVitamSession().setTenantId(0);


        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient();
            AdminManagementClient client = AdminManagementClientFactory.getInstance().getClient()) {

            GUID guid = newGUID();
            getVitamSession().setRequestId(guid);

            List<GriffinModel> griffinModelList = getGriffinModels("preservation/griffins.json");
            client.importGriffins(griffinModelList);

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(guid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));


            assertThat(jsonNode.iterator()).extracting(j -> j.get("outDetail").asText())
                .contains("GRIFFIN_REPORT.OK");

            guid = newGUID();
            getVitamSession().setRequestId(guid);
            List<PreservationScenarioModel> preservationScenarioModelList = getPreservationScenarioModels();

            client.importPreservationScenarios(preservationScenarioModelList);

            // When
            jsonNode = (ArrayNode) accessClient
                .selectOperationById(guid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

        }
    }

    @Test
    @RunWithCustomExecutor
    public void should_execute_preservation_workflow_without_error() throws Exception {
        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            getVitamSession().setTenantId(tenantId);
            getVitamSession().setContractId(contractId);
            getVitamSession().setContextId("Context_IT");
            getVitamSession().setRequestId(operationGuid);

            // Check Accession Register Detail
            List<String> excludeFields = Lists
                .newArrayList("_id", "StartDate", "LastUpdate", "EndDate", "Opc", "Opi", "CreationDate",
                    "OperationIds");

            // Get accession register details before start preservation
            long countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().count();
            assertThat(countDetails).isEqualTo(2);

            // Assert AccessionRegisterSummary
            assertJsonEquals("preservation/expected/accession_register_ratp_before.json",
                JsonHandler.toJsonNode(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "RATP"))),
                excludeFields);

            assertJsonEquals("preservation/expected/accession_register_FRAN_NP_009913_before.json",
                JsonHandler.toJsonNode(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                    .find(new Document("OriginatingAgency", "FRAN_NP_009913"))),
                excludeFields);


            buildAndSavePreservationResultFile();

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.exists("#id"));

            ObjectNode finalSelect = select.getFinalSelect();
            PreservationRequest preservationRequest =
                new PreservationRequest(finalSelect, "PSC-000001", "BinaryMaster", LAST, "BinaryMaster");
            accessClient.startPreservation(preservationRequest);

            waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            validateAccessionRegisterDetails(excludeFields);
        }
    }


    @Test
    @RunWithCustomExecutor
    public void should_execute_preservation_workflow_with_various_usage() throws Exception {
        // Given
        try (AccessInternalClient accessClient = AccessInternalClientFactory.getInstance().getClient()) {
            GUID operationGuid = GUIDFactory.newOperationLogbookGUID(tenantId);
            getVitamSession().setTenantId(tenantId);
            getVitamSession().setContractId(contractId);
            getVitamSession().setContextId("Context_IT");
            getVitamSession().setRequestId(operationGuid);

            buildAndSavePreservationResultFile();

            SelectMultiQuery select = new SelectMultiQuery();
            select.setQuery(QueryHelper.exists("#id"));

            ObjectNode finalSelect = select.getFinalSelect();
            PreservationRequest preservationRequest =
                new PreservationRequest(finalSelect, "PSC-000001", "Dissemination", FIRST, "BinaryMaster");
            accessClient.startPreservation(preservationRequest);

            waitOperation(NB_TRY, SLEEP_TIME, operationGuid.toString());

            // When
            ArrayNode jsonNode = (ArrayNode) accessClient
                .selectOperationById(operationGuid.getId(), new SelectMultiQuery().getFinalSelect()).toJsonNode()
                .get("$results")
                .get(0)
                .get("events");

            // Then
            assertThat(jsonNode.iterator()).extracting(j -> j.get("outcome").asText())
                .allMatch(outcome -> outcome.equals(StatusCode.OK.name()));

            JsonNode objectGroup =
                JsonHandler.toJsonNode(MetadataCollections.OBJECTGROUP.getCollection().find(new Document("_ops", operationGuid.getId())).sort(
                    Sorts.ascending(ObjectGroup.NBCHILD)));
            Assertions.assertThat(objectGroup.get(0).get("_qualifiers").get(1).get("qualifier").asText()).isEqualTo("Dissemination");
        }
    }

    private void validateAccessionRegisterDetails(List<String> excludeFields) throws FileNotFoundException, InvalidParseOperationException {
        long countDetails;
        // Get accession register details after start preservation
        countDetails = FunctionalAdminCollections.ACCESSION_REGISTER_DETAIL.getCollection().count();
        assertThat(countDetails).isEqualTo(4);

        // Assert AccessionRegisterSummary
        assertJsonEquals("preservation/expected/accession_register_ratp_after.json",
            JsonHandler.toJsonNode(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                .find(new Document("OriginatingAgency", "RATP"))),
            excludeFields);

        assertJsonEquals("preservation/expected/accession_register_FRAN_NP_009913_after.json",
            JsonHandler.toJsonNode(FunctionalAdminCollections.ACCESSION_REGISTER_SUMMARY.getCollection()
                .find(new Document("OriginatingAgency", "FRAN_NP_009913"))),
            excludeFields);
    }

    private void assertJsonEquals(String resourcesFile, JsonNode actual, List<String> excludeFields)
        throws FileNotFoundException, InvalidParseOperationException {
        JsonNode expected = getFromInputStream(PropertiesUtils.getResourceAsStream(resourcesFile));
        if (excludeFields != null) {
            expected.forEach(e -> {
                ObjectNode ee = (ObjectNode) e;
                ee.remove(excludeFields);
                if (ee.has("Events")) {
                    ee.get("Events").forEach(a -> ((ObjectNode) a).remove(excludeFields));
                }
            });
            actual.forEach(e -> {
                ObjectNode ee = (ObjectNode) e;
                ee.remove(excludeFields);
                if (ee.has("Events")) {
                    ee.get("Events").forEach(a -> ((ObjectNode) a).remove(excludeFields));
                }

            });
        }

        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.whenIgnoringPaths(excludeFields.toArray(new String[] {})));
    }

    @After
    public void afterTest() throws Exception {
        VitamThreadUtils.getVitamSession().setContextId(CONTEXT_ID);

        ProcessDataAccessImpl.getInstance().clearWorkflow();
        runAfterMongo(Sets.newHashSet(

            FunctionalAdminCollections.PRESERVATION_SCENARIO.getName(),
            FunctionalAdminCollections.GRIFFIN.getName(),
            MetadataCollections.UNIT.getName(),
            MetadataCollections.OBJECTGROUP.getName()
        ));

        runAfterEs(Sets.newHashSet(
            FunctionalAdminCollections.PRESERVATION_SCENARIO.getName().toLowerCase(),
            FunctionalAdminCollections.GRIFFIN.getName().toLowerCase(),
            MetadataCollections.UNIT.getName().toLowerCase() + "_0",
            MetadataCollections.OBJECTGROUP.getName().toLowerCase() + "_0"
        ));
    }

}
