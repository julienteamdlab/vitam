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
package fr.gouv.vitam.metadata.core.database.collections;

import com.fasterxml.jackson.databind.JsonNode;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.util.JSON;
import fr.gouv.vitam.common.LocalDateUtil;
import fr.gouv.vitam.common.SingletonUtils;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.metadata.api.exception.MetaDataNotFoundException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.bson.Document;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.gouv.vitam.common.graph.GraphUtils.createGraphRelation;

/**
 * Unit class:<br>
 *
 * @formatter:off { MD content, _id: UUID, _tenant: tenant, _profil: documentType,, _min: depthmin, _max: depthmax,
 * _mgt. Management structure, _uds: { UUID1 : depth1, UUID2 : depth2, ... }, // not indexed and not to
 * be in ES! _us: [ UUID1, UUID2, ... }, // indexed and equivalent to _uds _up: [ UUID1, UUID2, ... ], //
 * limited to immediate parent _og: UUID, _nbc : immediateChildNb }
 * @formatter:on
 */
public class Unit extends MetadataDocument<Unit> {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(Unit.class);

    private static final long serialVersionUID = -4351321928647834270L;

    /**
     * UNITDEPTHS : { UUID1 : depth2, UUID2 : depth2 }
     */
    public static final String UNITDEPTHS = "_uds";
    /**
     * UNITUPS : [ UUID1, UUID2 ]
     */
    public static final String UNITUPS = "_us";
    /**
     * MINDEPTH : min
     */
    public static final String MINDEPTH = "_min";
    /**
     * MAXDEPTH : max
     */
    public static final String MAXDEPTH = "_max";
    /**
     * Management : { various rules per themes }
     */
    public static final String MANAGEMENT = "_mgt";
    /**
     * UnitType : normal or holding scheme
     */
    public static final String UNIT_TYPE = "_unitType";
    public static final String GRAPH = "_graph";
    public static final String PARENT_ORIGINATING_AGENCIES = "_us_sp";

    @Override
    public MetadataDocument<Unit> newInstance(JsonNode content) {
        return new Unit(content);
    }

    /**
     * Es projection (no UPS)
     */
    public static final BasicDBObject UNIT_ES_PROJECTION = new BasicDBObject(UNITDEPTHS, 0);

    public static final BasicDBObject UNIT_VITAM_GRAPH_PROJECTION =
        new BasicDBObject(UP, 1)
            .append(UNITUPS, 1)
            .append(GRAPH, 1)
            .append(ORIGINATING_AGENCIES, 1)
            .append(UNITDEPTHS, 1)
            .append(ORIGINATING_AGENCY, 1)
            .append(PARENT_ORIGINATING_AGENCIES, 1)
            .append(ID, 1)
            .append(OG, 1);

    /**
     * Unit Id, Vitam fields Only projection (no content nor management)
     */
    public static final BasicDBObject UNIT_VITAM_PROJECTION =
        new BasicDBObject(NBCHILD, 1).append(TYPE, 1).append(UNITUPS, 1).append(UNITDEPTHS, 1)
            .append(MINDEPTH, 1).append(MAXDEPTH, 1)
            .append(TENANT_ID, 1).append(MetadataDocument.UP, 1).append(MetadataDocument.ID, 1)
            .append(ORIGINATING_AGENCIES, 1).append(MetadataDocument.OG, 1);
    /**
     * Storage Rule
     */
    public static final String STORAGERULE = MANAGEMENT + ".StorageRule";
    /**
     * Appraisal Rule
     */
    public static final String APPRAISALRULE = MANAGEMENT + ".AppraisalRule";
    /**
     * Access Rule
     */
    public static final String ACCESSRULE = MANAGEMENT + ".AccessRule";
    /**
     * Dissemination Rule
     */
    public static final String DISSEMINATIONRULE = MANAGEMENT + ".DisseminationRule";
    /**
     * Reuse Rule
     */
    public static final String REUSERULE = MANAGEMENT + ".ReuseRule";
    /**
     * Classification Rule
     */
    public static final String CLASSIFICATIONRULE = MANAGEMENT + ".ClassificationRule";

    /**
     * Rule
     */
    public static final String RULE = ".Rules.Rule";
    /**
     * Rule end date (computed)
     */
    public static final String END = ".Rules._end";


    @SuppressWarnings("javadoc")
    public static final String STORAGERULES = STORAGERULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String STORAGEEND = STORAGERULE + END;
    @SuppressWarnings("javadoc")
    public static final String APPRAISALRULES = APPRAISALRULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String APPRAISALEND = APPRAISALRULE + END;
    @SuppressWarnings("javadoc")
    public static final String ACCESSRULES = ACCESSRULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String ACCESSEND = ACCESSRULE + END;
    @SuppressWarnings("javadoc")
    public static final String DISSEMINATIONRULES = DISSEMINATIONRULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String DISSEMINATIONEND = DISSEMINATIONRULE + END;
    @SuppressWarnings("javadoc")
    public static final String REUSERULES = REUSERULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String REUSEEND = REUSERULE + END;
    @SuppressWarnings("javadoc")
    public static final String CLASSIFICATIONRULES = CLASSIFICATIONRULE + RULE;
    @SuppressWarnings("javadoc")
    public static final String CLASSIFICATIONEND = CLASSIFICATIONRULE + END;

    /**
     * Empty constructor
     */
    public Unit() {
        // empty
    }

    /**
     * Constructor from Json
     *
     * @param content of type JsonNode for building Unit
     */
    public Unit(JsonNode content) {
        super(content);
    }

    /**
     * Constructor from Document
     *
     * @param content of type Document for building Unit
     */
    public Unit(Document content) {
        super(content);
    }

    /**
     * Constructor from Json as Text
     *
     * @param content of type String for building Unit
     */
    public Unit(String content) {
        super(content);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected MongoCollection<Unit> getCollection() {
        return MetadataCollections.UNIT.getCollection();
    }

    @Override
    protected MetadataCollections getMetadataCollections() {
        return MetadataCollections.UNIT;
    }

    /**
     * @return the map of parent units with depth
     */
    public Map<String, Integer> getDepths() {
        final Object object = get(UNITDEPTHS);
        if (object == null) {
            return SingletonUtils.singletonMap();
        }
        final Map<String, Integer> map = new HashMap<>();
        if (object instanceof List) {
            final List<Document> list = (List<Document>) object;
            for (final Document document : list) {
                for (final Map.Entry<String, Object> entry : document.entrySet()) {
                    map.put(entry.getKey(), (Integer) entry.getValue());
                }
            }
        } else if (object instanceof HashMap) {
            for (final Map.Entry<String, Integer> entry : ((HashMap<String, Integer>) object).entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        } else {
            final Document list = (Document) object;
            for (final Map.Entry<String, Object> entry : list.entrySet()) {
                map.put(entry.getKey(), (Integer) entry.getValue());
            }
        }
        return map;
    }

    public void buildParentGraph(Collection<Unit> directParentUnits) {

        String id = getId();
        Set<String> directParents = new HashSet<>();
        Set<String> allParents = new HashSet<>();
        Set<String> graph = new HashSet<>();
        Map<String, Integer> parentDepths = new HashMap<>();
        Set<String> allOriginatingAgencies = new HashSet<>();
        MultiValuedMap<String, String> allParentOriginatingAgencies = new HashSetValuedHashMap<>();
        String originatingAgency = get(ORIGINATING_AGENCY, String.class);
        if (originatingAgency != null) {
            allOriginatingAgencies.add(originatingAgency);
        }

        if (!directParentUnits.isEmpty()) {

            for (Unit parentUnit : directParentUnits) {
                directParents.add(parentUnit.getId());
            }

            allParents.addAll(directParents);

            for (String directParent : directParents) {
                graph.add(createGraphRelation(id, directParent));
            }

            for (Unit parentUnit : directParentUnits) {

                allParents.addAll(parentUnit.getCollectionOrEmpty(UNITUPS));

                graph.addAll(parentUnit.getCollectionOrEmpty(GRAPH));

                allOriginatingAgencies.addAll(parentUnit.getCollectionOrEmpty(Unit.ORIGINATING_AGENCIES));

                Map<String, Collection<String>> parentUnitsByOriginatingAgencies =
                    parentUnit.getMapOrEmpty(Unit.PARENT_ORIGINATING_AGENCIES);
                parentUnitsByOriginatingAgencies
                    .forEach(allParentOriginatingAgencies::putAll);

                String parentOriginatingAgency = parentUnit.get(Unit.ORIGINATING_AGENCY, String.class);
                if (parentOriginatingAgency != null) {
                    allParentOriginatingAgencies.put(parentOriginatingAgency, parentUnit.getId());
                }

                Map<String, Integer> parentParentDepths = parentUnit.getMapOrEmpty(UNITDEPTHS);
                for (Entry<String, Integer> entry : parentParentDepths.entrySet()) {
                    parentDepths.put(entry.getKey(), entry.getValue() + 1);
                }
            }

            // Set/override direct parents depth to 1
            for (String directParent : directParents) {
                parentDepths.put(directParent, 1);
            }
        }

        put(UP, directParents);
        put(UNITUPS, allParents);
        put(GRAPH, graph);
        put(ORIGINATING_AGENCIES, allOriginatingAgencies);
        put(UNITDEPTHS, parentDepths);
        put(PARENT_ORIGINATING_AGENCIES, allParentOriginatingAgencies.asMap());

        put(MINDEPTH, 1);
        int maxDepth = parentDepths.values().stream().max(Comparator.naturalOrder()).orElse(0) + 1;
        put(MAXDEPTH, maxDepth);

        put(GRAPH_LAST_PERSISTED_DATE, LocalDateUtil.getFormattedDateForMongo(LocalDateUtil.now()));

        // Debug
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("DEBUG: UNIT {}", JSON.serialize(this));
        }
    }
}
