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
package fr.gouv.vitam.common.database.api.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.gouv.vitam.common.database.api.VitamRepositoryStatus;
import fr.gouv.vitam.common.database.server.mongodb.VitamDocument;
import fr.gouv.vitam.common.elasticsearch.ElasticsearchRule;
import fr.gouv.vitam.common.exception.DatabaseException;
import fr.gouv.vitam.common.guid.GUIDFactory;
import org.apache.commons.lang3.RandomUtils;
import org.bson.Document;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 */
public class VitamElasticsearchRepositoryTest {

    private static String ES_CONFIGURATION_FILE = "/elasticsearch-configuration.json";

    public static final String TESTINDEX = "testindex";
    private static VitamElasticsearchRepository repository;


    private static final String mapping = "{\n" +
        "  \"properties\": {\n" +
        "    \"Identifier\": {\n" +
        "      \"type\": \"keyword\"\n" +
        "    },\n" +
        "    \"Name\": {\n" +
        "      \"type\": \"text\",\n" +
        "      \"fielddata\": true\n" +
        "    },\n" +
        "    \"Description\": {\n" +
        "      \"type\": \"text\"\n" +
        "    },\n" +
        "    \"_tenant\": {\n" +
        "      \"type\": \"long\"\n" +
        "    },\n" +
        "    \"_v\": {\n" +
        "      \"type\": \"long\"\n" +
        "    },\n" +
        "    \"_score\": {\n" +
        "      \"type\": \"object\",\n" +
        "      \"enabled\": false\n" +
        "    }\n" +
        "  }\n" +
        "}";
    @ClassRule
    public static TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public ElasticsearchRule elasticsearchRule = new ElasticsearchRule(tempFolder.newFolder(), TESTINDEX);

    public VitamElasticsearchRepositoryTest() throws IOException {}


    @Before
    public void setUpBeforeClass() throws Exception {
        repository = new VitamElasticsearchRepository(elasticsearchRule.getClient(), TESTINDEX, false);
        /*
         * findByIdentifierAndTenant works only if identifier is term (not text) As es by default detect Identifier as
         * text we shoud pre-create index with correct mapping
         */
        createIndexWithMapping(elasticsearchRule.getClient());
    }

    @Test
    public void testSaveOneDocumentAndGetByIDOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        assertThat(document.get(VitamDocument.ID)).isNotNull();
        assertThat(document.get(VitamDocument.ID)).isEqualTo(id);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");
    }

    @Test
    public void testSaveOrUpdateOneDocumentAndGetByIDOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        VitamRepositoryStatus result = repository.saveOrUpdate(document);

        assertThat(VitamRepositoryStatus.CREATED.equals(result));
        assertThat(document.get(VitamDocument.ID)).isNotNull();
        assertThat(document.get(VitamDocument.ID)).isEqualTo(id);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");

        builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Title", "Test othersave")
            .endObject();

        document = Document.parse(Strings.toString(builder));
        result = repository.saveOrUpdate(document);

        assertThat(VitamRepositoryStatus.UPDATED.equals(result));
        response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test othersave");
    }

    @Test
    public void testSaveMultipleDocumentsAndPurgeDocumentsOK() throws IOException, DatabaseException {
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }

        // pruge all tenants
        for (int i = 0; i < 101; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, GUIDFactory.newGUID().toString())
                .field(VitamDocument.TENANT_ID, 1)
                .field("Title", "Test save " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }

        repository.save(documents);

        // purge tenant 0
        long deleted = repository.purge(0);
        assertThat(deleted).isEqualTo(101);

        // purge all other tenants
        deleted = repository.purge();
        assertThat(deleted).isEqualTo(101);
    }

    @Test
    public void testSaveOrUpdateMultipleDocumentsOK() throws IOException, DatabaseException {
        List<String> guids = new ArrayList<>();

        // insert tenant 0
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String guid = GUIDFactory.newGUID().toString();
            guids.add(guid);
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, guid)
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save " + RandomUtils.nextDouble())
                .endObject();
            documents.add(Document.parse(Strings.toString(builder)));
        }

        repository.saveOrUpdate(documents);
        for (int i = 0; i < 100; i++) {
            assertThat(documents.get(i).get(VitamDocument.ID)).isNotNull();
            assertThat(documents.get(i).get(VitamDocument.ID)).isEqualTo(guids.get(i));
        }

        // update tenant 0
        List<Document> updatedDocuments = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, guids.get(i))
                .field(VitamDocument.TENANT_ID, 0)
                .field("Title", "Test save updated")
                .endObject();
            updatedDocuments.add(Document.parse(Strings.toString(builder)));
        }

        repository.saveOrUpdate(updatedDocuments);
        for (int i = 0; i < 100; i++) {
            assertThat(updatedDocuments.get(i).get(VitamDocument.ID)).isNotNull();
            assertThat(updatedDocuments.get(i).get(VitamDocument.ID)).isEqualTo(guids.get(i));
        }

        Optional<Document> response = repository.getByID(guids.get(0), 0);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save updated");

        // purge tenant 0
        long deleted = repository.purge(0);
        assertThat(deleted).isEqualTo(100);

    }

    @Test
    public void testRemoveOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");

        repository.remove(id, tenant);
        response = repository.getByID(id, tenant);
        assertThat(response).isEmpty();
    }


    @Test(expected = DatabaseException.class)
    public void testRemoveNotExists() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        repository.remove(id, tenant);
    }

    @Test
    public void testGetByIDNotExistsOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;

        Client client = elasticsearchRule.getClient();
        // Just to create index as not yet developed in ElasticsearchRule
        if (!client.admin().indices().prepareExists(TESTINDEX).get().isExists()) {
            XContentBuilder builder = jsonBuilder()
                .startObject()
                .field(VitamDocument.ID, id)
                .field(VitamDocument.TENANT_ID, tenant)
                .field("Title", "Test save")
                .endObject();

            Document document = Document.parse(Strings.toString(builder));
            repository.save(document);
        }
        Optional<Document> response = repository.getByID(GUIDFactory.newGUID().toString(), tenant);
        assertThat(response).isEmpty();
    }

    @Test
    public void testFindByIdentifierAndTenantFoundOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field(VitamDocument.TENANT_ID, tenant)
            .field("Identifier", "FakeIdentifier")
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");

        response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");
    }

    @Test
    public void testFindByIdentifierFoundOK() throws IOException, DatabaseException {
        String id = GUIDFactory.newGUID().toString();
        Integer tenant = 0;
        XContentBuilder builder = jsonBuilder()
            .startObject()
            .field(VitamDocument.ID, id)
            .field("Identifier", "FakeIdentifier")
            .field("Title", "Test save")
            .endObject();

        Document document = Document.parse(Strings.toString(builder));
        repository.save(document);

        Optional<Document> response = repository.getByID(id, tenant);
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");

        response = repository.findByIdentifier("FakeIdentifier");
        assertThat(response).isPresent();
        assertThat(response.get()).extracting("Title").contains("Test save");
    }

    private void createIndexWithMapping(Client client) throws IOException {
        if (!client.admin().indices().prepareExists(TESTINDEX).get().isExists()) {
            final CreateIndexResponse createIndexResponse =
                client.admin().indices().prepareCreate(TESTINDEX)
                    .setSettings(settings())
                    .addMapping("typeunique", mapping, XContentType.JSON)
                    .get();
            assertThat(createIndexResponse.isAcknowledged()).isTrue();
        }
    }

    @Test
    public void testFindByIdentifierFoundEmpty() throws DatabaseException {
        Optional<Document> response = repository.findByIdentifier("FakeIdentifier");
        assertThat(response).isEmpty();
    }

    @Test
    public void testFindByIdentifierAndTenantFoundEmpty() throws DatabaseException {
        Integer tenant = 0;
        Optional<Document> response = repository.findByIdentifierAndTenant("FakeIdentifier", tenant);
        assertThat(response).isEmpty();
    }

    @Test(expected = DatabaseException.class)
    public void testRemoveByNameAndTenantNotImplemented() throws DatabaseException {
        repository.removeByNameAndTenant("FakeName", 0);
    }

    public Settings.Builder settings() throws IOException {
        return Settings.builder().loadFromStream(ES_CONFIGURATION_FILE,
            VitamElasticsearchRepositoryTest.class.getResourceAsStream(ES_CONFIGURATION_FILE), true);
    }
}
