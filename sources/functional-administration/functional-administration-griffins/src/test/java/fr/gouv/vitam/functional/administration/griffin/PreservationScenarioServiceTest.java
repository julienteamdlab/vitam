package fr.gouv.vitam.functional.administration.griffin;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.database.server.DbRequestResult;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.administration.ActionTypePreservation;
import fr.gouv.vitam.common.model.administration.GriffinByFormat;
import fr.gouv.vitam.common.model.administration.PreservationScenarioModel;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.functional.administration.common.FunctionalBackupService;
import fr.gouv.vitam.functional.administration.common.PreservationScenario;
import fr.gouv.vitam.functional.administration.common.exception.ReferentialException;
import fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections;
import fr.gouv.vitam.functional.administration.common.server.MongoDbAccessReferential;
import fr.gouv.vitam.logbook.common.parameters.LogbookOperationParameters;
import fr.gouv.vitam.logbook.common.parameters.LogbookParameterName;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClient;
import fr.gouv.vitam.logbook.operations.client.LogbookOperationsClientFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static fr.gouv.vitam.common.guid.GUIDFactory.newGUID;
import static fr.gouv.vitam.common.json.JsonHandler.getFromString;
import static fr.gouv.vitam.common.thread.VitamThreadUtils.getVitamSession;
import static fr.gouv.vitam.functional.administration.common.server.FunctionalAdminCollections.PRESERVATION_SCENARIO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PreservationScenarioServiceTest {
    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock private MongoDbAccessReferential mongoDbAccess;
    @Mock private FunctionalBackupService functionalBackupService;
    @Mock private LogbookOperationsClientFactory logbookOperationsClientFactory;
    @Mock private LogbookOperationsClient logbookOperationsClient;

    private PreservationScenarioService preservationScenarioService;
    private PreservationScenarioModel defaultScenarioModel;

    @Mock private DbRequestResult dbRequestResult;

    @Before
    public void setUp() {
        preservationScenarioService =
            new PreservationScenarioService(mongoDbAccess, functionalBackupService, logbookOperationsClientFactory);

        when(logbookOperationsClientFactory.getClient()).thenReturn(logbookOperationsClient);

        defaultScenarioModel = new PreservationScenarioModel(
            "name",
            "id",
            Collections.singletonList(ActionTypePreservation.GENERATE),
            Collections.singletonList("string"),
            Collections.singletonList(new GriffinByFormat()),
            new GriffinByFormat());
        defaultScenarioModel.setVersion(1);
        GUID guid = newGUID();

        getVitamSession().setTenantId(1);
        getVitamSession().setRequestId(guid);
    }

    @Test
    @RunWithCustomExecutor
    public void givenPreservationScenariosInDataBaseShouldCollectInsertUpdateAndDeleteList() throws Exception {

        //Given
        List<PreservationScenarioModel> allPreservationScenarioInDatabase = JsonHandler.getFromFileAsTypeRefence(
            PropertiesUtils.getResourceFile("scenarii.json"),
            new TypeReference<List<PreservationScenarioModel>>() {
            }
        );

        List<PreservationScenarioModel> listToImport = JsonHandler.getFromFileAsTypeRefence(
            PropertiesUtils.getResourceFile("scenarii_import.json"),
            new TypeReference<List<PreservationScenarioModel>>() {
            }
        );

        String requestId = getVitamSession().getRequestId();
        File preservationScenarioFile = PropertiesUtils.getResourceFile(
            "preservation_scenario_logbook_operation.json");
        JsonNode preservationScenarioOperation = JsonHandler.getFromFile(preservationScenarioFile);
        when(logbookOperationsClient.selectOperationById(requestId)).thenReturn(preservationScenarioOperation);

        //When
        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class))
            .thenReturn(allPreservationScenarioInDatabase);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);

        RequestResponse<PreservationScenarioModel> requestResponse =
            preservationScenarioService.importScenarios(listToImport);


        verify(mongoDbAccess, times(1)).insertDocuments(any(), eq(PRESERVATION_SCENARIO));

        verify(mongoDbAccess, times(1))
            .replaceDocument(any(), eq("IDENTIFIER2"), eq("Identifier"), eq(PRESERVATION_SCENARIO));

        verify(mongoDbAccess, times(1)).deleteDocument(any(), eq(PRESERVATION_SCENARIO));
    }

    @Test
    @RunWithCustomExecutor
    public void shouldImportScenario() throws Exception {
        GUID guid = GUIDFactory.newGUID();
        VitamThreadUtils.getVitamSession().setTenantId(1);
        VitamThreadUtils.getVitamSession().setRequestId(guid);

        //Given
        List<PreservationScenarioModel> allPreservationScenarioInDatabase = JsonHandler.getFromFileAsTypeRefence(
            PropertiesUtils.getResourceFile("scenarii.json"),
            new TypeReference<List<PreservationScenarioModel>>() {
            }
        );

        List<PreservationScenarioModel> listToImport = JsonHandler.getFromFileAsTypeRefence(
            PropertiesUtils.getResourceFile("scenarii_all.json"),
            new TypeReference<List<PreservationScenarioModel>>() {
            }
        );

        String requestId = getVitamSession().getRequestId();
        File preservationScenarioFile = PropertiesUtils.getResourceFile(
            "preservation_scenario_logbook_operation.json");
        JsonNode preservationScenarioOperation = JsonHandler.getFromFile(preservationScenarioFile);
        when(logbookOperationsClient.selectOperationById(requestId)).thenReturn(preservationScenarioOperation);

        //When
        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class))
            .thenReturn(allPreservationScenarioInDatabase);
        when(mongoDbAccess.findDocuments(any(JsonNode.class), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);

        RequestResponse<PreservationScenarioModel> requestResponse =
            preservationScenarioService.importScenarios(listToImport);

        ArgumentCaptor<LogbookOperationParameters> event1Captor = forClass(LogbookOperationParameters.class);
        ArgumentCaptor<LogbookOperationParameters> event2Captor = forClass(LogbookOperationParameters.class);

        //Then
        JsonNode result = JsonHandler.toJsonNode(requestResponse);
        int total = result.get("$hits").get("total").asInt();

        assertThat(total).isEqualTo(3);

        verify(logbookOperationsClient, times(1)).create(event1Captor.capture());
        verify(logbookOperationsClient, times(1)).update(event2Captor.capture());

        verify(mongoDbAccess, times(1)).insertDocuments(any(), eq(PRESERVATION_SCENARIO));
        verify(mongoDbAccess, times(1))
            .replaceDocument(any(), eq("IDENTIFIER2"), eq("Identifier"), eq(PRESERVATION_SCENARIO));

        assertThat(event1Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IMPORT_PRESERVATION_SCENARIO.STARTED");
        assertThat(event2Captor.getValue().getParameterValue(LogbookParameterName.outcomeDetail))
            .isEqualTo("IMPORT_PRESERVATION_SCENARIO.OK");

        verify(functionalBackupService)
            .saveCollectionAndSequence(guid, "STP_BACKUP_SCENARIO", PRESERVATION_SCENARIO, guid.getId());
    }


    @Test
    @RunWithCustomExecutor
    public void shouldGetScenarioById() throws Exception {
        //Given
        when(mongoDbAccess.findDocuments(any(), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);
        when(dbRequestResult.getRequestResponseOK(any(), any(), any()))
            .thenReturn(new RequestResponseOK<>());

        //When
        RequestResponse<PreservationScenarioModel> preservationScenario =
            preservationScenarioService.findPreservationScenario(getFromString("{}"));
        //Then
        assertThat(preservationScenario).isNotNull();
    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenNameIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setName(null);

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setName("");

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedWhenImportTwoDuplicatedScenarioIdentifiers() throws Exception {
        //Given
        PreservationScenarioModel secondScenarioModel = new PreservationScenarioModel(
            "name",
            "id",
            Collections.singletonList(ActionTypePreservation.GENERATE),
            Collections.singletonList("string"),
            Collections.singletonList(new GriffinByFormat()),
            new GriffinByFormat());

        // Then
        assertThatThrownBy(() -> preservationScenarioService
            .importScenarios(Lists.newArrayList(defaultScenarioModel, secondScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Duplicate scenario");

    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenIdentifierIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setIdentifier(null);

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setIdentifier("");

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

    }

    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenActionListIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setActionList(null);

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setActionList(new ArrayList<ActionTypePreservation>());

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

    }

    @Test
    @Ignore
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenGriffinByFormatIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setGriffinByFormat(null);

        //When
        when(mongoDbAccess.findDocuments(any(), eq(PRESERVATION_SCENARIO))).thenReturn(dbRequestResult);

        when(dbRequestResult.getDocuments(PreservationScenario.class, PreservationScenarioModel.class))
            .thenReturn(new ArrayList<>());


        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setGriffinByFormat(new ArrayList<GriffinByFormat>());

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

    }

    @Ignore
    @Test
    @RunWithCustomExecutor
    public void shouldFailedValidateScenarioWhenDefaultGriffinIsNullOrEmpty() throws Exception {
        //Given
        defaultScenarioModel.setDefaultGriffin(null);

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

        //Given
        defaultScenarioModel.setDefaultGriffin(new GriffinByFormat());

        // Then
        assertThatThrownBy(
            () -> preservationScenarioService.importScenarios(Collections.singletonList(defaultScenarioModel)))
            .isInstanceOf(ReferentialException.class).hasMessageContaining("Invalid scenario");

    }
}
