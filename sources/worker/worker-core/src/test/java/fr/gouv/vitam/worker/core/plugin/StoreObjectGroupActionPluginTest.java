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
package fr.gouv.vitam.worker.core.plugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.model.processing.IOParameter;
import fr.gouv.vitam.common.model.processing.ProcessingUri;
import fr.gouv.vitam.common.model.processing.UriPrefix;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.processing.common.parameter.WorkerParametersFactory;
import fr.gouv.vitam.storage.engine.client.StorageClient;
import fr.gouv.vitam.storage.engine.client.StorageClientFactory;
import fr.gouv.vitam.storage.engine.client.exception.StorageServerClientException;
import fr.gouv.vitam.storage.engine.common.model.response.StoredInfoResult;
import fr.gouv.vitam.worker.core.impl.HandlerIOImpl;
import fr.gouv.vitam.workspace.client.WorkspaceClient;
import fr.gouv.vitam.workspace.client.WorkspaceClientFactory;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({WorkspaceClientFactory.class, MetaDataClientFactory.class, StorageClientFactory.class})
public class StoreObjectGroupActionPluginTest {

    private static final String CONTAINER_NAME = "aeaaaaaaaaaaaaabaa4quakwgip7nuaaaaaq";
    private static final String OBJECT_GROUP_GUID = "aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq";
    private static final String OBJECT_GROUP_GUID_2 = "aebaaaaaaaakwtamaaxakak32oqku2qaaaaq";
    StoreObjectGroupActionPlugin plugin;
    private WorkspaceClient workspaceClient;
    private WorkspaceClientFactory workspaceClientFactory;
    private MetaDataClient metadataClient;
    private StorageClient storageClient;
    private HandlerIOImpl action;
    private static final String OBJECT_GROUP = "storeObjectGroupHandler/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json";
    private static final String OBJECT_GROUP_2 = "storeObjectGroupHandler/aebaaaaaaaakwtamaaxakak32oqku2qaaaaq.json";
    private final InputStream objectGroup;
    private final InputStream objectGroup2;
    private List<IOParameter> out;

    public StoreObjectGroupActionPluginTest() throws FileNotFoundException {
        objectGroup = PropertiesUtils.getResourceAsStream(OBJECT_GROUP);
        objectGroup2 = PropertiesUtils.getResourceAsStream(OBJECT_GROUP_2);
    }

    @Before
    public void setUp() throws Exception {
        workspaceClient = mock(WorkspaceClient.class);
        metadataClient = mock(MetaDataClient.class);
        storageClient = mock(StorageClient.class);
        PowerMockito.mockStatic(WorkspaceClientFactory.class);
        PowerMockito.mockStatic(MetaDataClientFactory.class);
        PowerMockito.mockStatic(StorageClientFactory.class);
        workspaceClientFactory = mock(WorkspaceClientFactory.class);
        PowerMockito.when(WorkspaceClientFactory.getInstance()).thenReturn(workspaceClientFactory);
        PowerMockito.when(WorkspaceClientFactory.getInstance().getClient()).thenReturn(workspaceClient);
        action = new HandlerIOImpl(CONTAINER_NAME, "workerId", com.google.common.collect.Lists.newArrayList());
        
        out = new ArrayList<>();
        out.add(new IOParameter().setUri(new ProcessingUri(UriPrefix.MEMORY, "objectGroupId.json")));
        action.addOutIOParameters(out);
    }

    @After
    public void clean() {
        action.partialClose();
    }

    @Test
    public void givenWorkspaceErrorWhenExecuteThenReturnResponseFATAL() throws Exception {
        final StorageClientFactory storageClientFactory = PowerMockito.mock(StorageClientFactory.class);

        final WorkerParameters paramsObjectGroups =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(OBJECT_GROUP_GUID + ".json"))
                .setObjectName(OBJECT_GROUP_GUID + ".json").setCurrentStep("Store ObjectGroup");

        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        when(workspaceClient.getObject(CONTAINER_NAME, "ObjectGroup/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json"))
            .thenReturn(Response.status(Status.OK).entity(objectGroup).build());

        Mockito.doThrow(new StorageServerClientException("Error storage")).when(storageClient)
            .storeFileFromWorkspace(anyObject(), anyObject(), anyObject(), anyObject());
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(StorageClientFactory.getInstance()).thenReturn(storageClientFactory);

        plugin = new StoreObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(paramsObjectGroups, action);
        assertEquals(StatusCode.FATAL, response.getGlobalStatus());
    }

    @Test
    public void givenWorkspaceExistWhenExecuteThenReturnResponseOK()
        throws Exception {
        final StorageClientFactory storageClientFactory = PowerMockito.mock(StorageClientFactory.class);

        final WorkerParameters paramsObjectGroups =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(OBJECT_GROUP_GUID + ".json"))
                .setObjectName(OBJECT_GROUP_GUID + ".json").setCurrentStep("Store ObjectGroup");

        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        when(workspaceClient.getObject(CONTAINER_NAME, "ObjectGroup/aeaaaaaaaaaam7myaaaamakxfgivuryaaaaq.json"))
            .thenReturn(Response.status(Status.OK).entity(objectGroup).build());

        Mockito.doReturn(getStorageResult()).when(storageClient).storeFileFromWorkspace(anyObject(),
            anyObject(), anyObject(), anyObject());
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(StorageClientFactory.getInstance()).thenReturn(storageClientFactory);


        plugin = new StoreObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(paramsObjectGroups, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    @Test
    public void givenWorkspaceExistAndPdoWhenExecuteThenReturnResponseOK()
        throws Exception {
        final StorageClientFactory storageClientFactory = PowerMockito.mock(StorageClientFactory.class);

        final WorkerParameters paramsObjectGroups =
            WorkerParametersFactory.newWorkerParameters().setWorkerGUID(GUIDFactory
                .newGUID()).setContainerName(CONTAINER_NAME).setUrlMetadata("http://localhost:8083")
                .setUrlWorkspace("http://localhost:8083")
                .setObjectNameList(Lists.newArrayList(OBJECT_GROUP_GUID_2 + ".json"))
                .setObjectName(OBJECT_GROUP_GUID_2 + ".json").setCurrentStep("Store ObjectGroup");

        final MetaDataClientFactory mockedMetadataFactory = mock(MetaDataClientFactory.class);
        PowerMockito.when(MetaDataClientFactory.getInstance()).thenReturn(mockedMetadataFactory);
        PowerMockito.when(mockedMetadataFactory.getClient()).thenReturn(metadataClient);
        when(workspaceClient.getObject(CONTAINER_NAME, "ObjectGroup/" + OBJECT_GROUP_GUID_2 + ".json"))
            .thenReturn(Response.status(Status.OK).entity(objectGroup2).build());

        Mockito.doReturn(getStorageResult()).when(storageClient).storeFileFromWorkspace(anyObject(),
            anyObject(), anyObject(), anyObject());
        when(storageClientFactory.getClient()).thenReturn(storageClient);
        when(StorageClientFactory.getInstance()).thenReturn(storageClientFactory);


        plugin = new StoreObjectGroupActionPlugin();

        final ItemStatus response = plugin.execute(paramsObjectGroups, action);
        assertEquals(StatusCode.OK, response.getGlobalStatus());
    }

    private StoredInfoResult getStorageResult() {
        final StoredInfoResult storedInfoResult = new StoredInfoResult();
        storedInfoResult.setInfo("Info");
        storedInfoResult.setId("obj");
        storedInfoResult.setCreationTime(LocalDateUtil.getString(LocalDateUtil.now()));
        storedInfoResult.setLastAccessTime(LocalDateUtil.getString(LocalDateUtil.now()));
        storedInfoResult.setLastCheckedTime(LocalDateUtil.getString(LocalDateUtil.now()));
        storedInfoResult.setLastModifiedTime(LocalDateUtil.getString(LocalDateUtil.now()));
        storedInfoResult.setNbCopy(1);
        storedInfoResult.setStrategy("default");
        storedInfoResult.setOfferIds(Arrays.asList("idoffer1"));
        return storedInfoResult;
    }

}
