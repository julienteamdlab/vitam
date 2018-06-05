/**
 * Copyright French Prime minister Office/SGMAP/DINSIC/Vitam Program (2015-2019)
 * <p>
 * contact.vitam@culture.gouv.fr
 * <p>
 * This software is a computer program whose purpose is to implement a digital archiving back-office system managing
 * high volumetry securely and efficiently.
 * <p>
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL 2.1 license as
 * circulated by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
 * <p>
 * As a counterpart to the access to the source code and rights to copy, modify and redistribute granted by the license,
 * users are provided only with a limited warranty and the software's author, the holder of the economic rights, and the
 * successive licensors have only limited liability.
 * <p>
 * In this respect, the user's attention is drawn to the risks associated with loading, using, modifying and/or
 * developing or reproducing the software by the user in light of its specific status of free software, that may mean
 * that it is complicated to manipulate, and that also therefore means that it is reserved for developers and
 * experienced professionals having in-depth computer knowledge. Users are therefore encouraged to load and test the
 * software's suitability as regards their requirements in conditions enabling the security of their systems and/or data
 * to be ensured and, more generally, to use and operate it in the same conditions as regards security.
 * <p>
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL 2.1 license and that you
 * accept its terms.
 */
package fr.gouv.vitam.worker.core.plugin.massprocessing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.VisibleForTesting;
import fr.gouv.vitam.common.VitamConfiguration;
import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.query.VitamFieldsHelper;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken;
import fr.gouv.vitam.common.database.builder.request.exception.InvalidCreateOperationException;
import fr.gouv.vitam.common.database.builder.request.multiple.SelectMultiQuery;
import fr.gouv.vitam.common.database.builder.request.multiple.UpdateMultiQuery;
import fr.gouv.vitam.common.database.builder.request.single.Select;
import fr.gouv.vitam.common.database.parser.request.multiple.SelectParserMultiple;
import fr.gouv.vitam.common.database.parser.request.multiple.UpdateParserMultiple;
import fr.gouv.vitam.common.error.VitamCode;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.exception.VitamDBException;
import fr.gouv.vitam.common.json.JsonHandler;
import fr.gouv.vitam.common.logging.VitamLogger;
import fr.gouv.vitam.common.logging.VitamLoggerFactory;
import fr.gouv.vitam.common.model.ItemStatus;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.metadata.api.exception.MetaDataClientServerException;
import fr.gouv.vitam.metadata.api.exception.MetaDataDocumentSizeException;
import fr.gouv.vitam.metadata.api.exception.MetaDataExecutionException;
import fr.gouv.vitam.metadata.client.MetaDataClient;
import fr.gouv.vitam.metadata.client.MetaDataClientFactory;
import fr.gouv.vitam.processing.common.exception.ProcessingException;
import fr.gouv.vitam.processing.common.parameter.WorkerParameters;
import fr.gouv.vitam.worker.common.HandlerIO;
import fr.gouv.vitam.worker.core.handler.ActionHandler;
import fr.gouv.vitam.workspace.api.exception.ContentAddressableStorageServerException;

import static fr.gouv.vitam.common.json.JsonHandler.createObjectNode;

/**
 * Check distribution threshold.
 */
public class CheckDistributionThreshold extends ActionHandler {

    private static final VitamLogger LOGGER = VitamLoggerFactory.getInstance(ActionHandler.class);

    /**
     * CHECK_DISTRIBUTION_THRESHOLD
     */
    private static final String CHECK_DISTRIBUTION_THRESHOLD = "CHECK_DISTRIBUTION_THRESHOLD";

    /**
     * metaDataClientFactory
     */
    private MetaDataClientFactory metaDataClientFactory;

    /**
     * Constructor.
     */
    public CheckDistributionThreshold() {
        this(MetaDataClientFactory.getInstance());
    }

    /**
     * Constructor.
     *
     * @param metaDataClientFactory
     */
    @VisibleForTesting
    CheckDistributionThreshold(MetaDataClientFactory metaDataClientFactory) {
        this.metaDataClientFactory = metaDataClientFactory;
    }

    /**
     * Execute an action
     *
     * @param param   {@link WorkerParameters}
     * @param handler the handlerIo
     * @return CompositeItemStatus:response contains a list of functional message and status code
     * @throws ProcessingException                      if an error is encountered when executing the action
     * @throws ContentAddressableStorageServerException if a storage exception is encountered when executing the action
     */
    @Override
    public ItemStatus execute(WorkerParameters param, HandlerIO handler)
            throws ProcessingException, ContentAddressableStorageServerException {

        final ItemStatus itemStatus = new ItemStatus(CHECK_DISTRIBUTION_THRESHOLD);

        try (MetaDataClient client = metaDataClientFactory.getClient()) {
            // get initial query string
            JsonNode queryNode = handler.getJsonFromWorkspace("query.json");

            // parse multi query
            UpdateMultiQuery multiQuery = getUpdateQueryFromJson(queryNode);

            // get threshold 
            Long threshold = multiQuery.getThreshold();
            Long defaultThreashold = VitamConfiguration.getDistributionThreshold(); 
            
            // compute min and max
            long min, max;
            if(threshold != null) {
                max = Math.max(threshold, defaultThreashold);
                min = Math.min(threshold, defaultThreashold);
            } else {
                min = max = defaultThreashold;
            }
            
            // count elements
            SelectMultiQuery selectMulti = getSelectCountQueryFromUpdateQuery(multiQuery);
            JsonNode response = client.selectUnits(selectMulti.getFinalSelect());
            RequestResponseOK<JsonNode> responseOK = RequestResponseOK.getFromJsonNode(response);
            if (responseOK.isOk()) {
                // get total 
                long total = responseOK.getHits().getTotal();
                
                // check against client threshold if exists
                if (total <= min) {
                    // OK : total <= default_threshold && overcome_threshold
                    itemStatus.increment(StatusCode.OK);
                } else if (total > max) {
                    // KO : total > default_threshold && overcome_threshold
                    itemStatus.increment(StatusCode.KO);
                    itemStatus.setMessage(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_THRESHOLD_EXCEDEED.name());
                } else if (max == defaultThreashold) {
                    // KO : total > overcome_threshold (min)
                    itemStatus.increment(StatusCode.KO);
                    itemStatus.setMessage(VitamCode.INTERNAL_SECURITY_MASS_UPDATE_THRESHOLD_EXCEDEED.name());
                } else {
                    // Warning : default_threshold < total < overcome_threshold
                    itemStatus.increment(StatusCode.WARNING);
                }
            } else {
                // not able to get count
                itemStatus.increment(StatusCode.FATAL);
            }
            
        } catch (InvalidCreateOperationException | InvalidParseOperationException e) {
            // bad DSL query
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        } catch (VitamDBException | MetaDataExecutionException | 
                MetaDataDocumentSizeException | MetaDataClientServerException e) {
            // metadata error
            LOGGER.error(e);
            itemStatus.increment(StatusCode.FATAL);
        }
        
        return new ItemStatus(CHECK_DISTRIBUTION_THRESHOLD).setItemsStatus(CHECK_DISTRIBUTION_THRESHOLD, itemStatus);
    }

    /**
     * getUpdateQueryFromJson
     * 
     * @param queryNode
     * @return
     * @throws InvalidParseOperationException
     */
    private UpdateMultiQuery getUpdateQueryFromJson(JsonNode queryNode) throws InvalidParseOperationException {
        // parse multi query
        UpdateParserMultiple parser = new UpdateParserMultiple();
        parser.parse(queryNode);
        
        return  parser.getRequest();
    }

    /**
     * getSelectCountQueryFromUpdateQuery
     * 
     * @param multiQuery
     * @return
     * @throws InvalidCreateOperationException
     * @throws InvalidParseOperationException
     */
    @VisibleForTesting
    public SelectMultiQuery getSelectCountQueryFromUpdateQuery(UpdateMultiQuery multiQuery) 
            throws InvalidCreateOperationException, InvalidParseOperationException {
        // create multi select with same queries
        SelectMultiQuery selectMulti = new SelectMultiQuery();
        selectMulti.addQueries(multiQuery.getQueries().stream().toArray(Query[]::new));
        // to get only the count add filter.limit = 1
        selectMulti.setLimitFilter(0, 1);
        // get only the id, no need to get full document
        selectMulti.setProjection(JsonHandler.getFromString("{\"$fields\": { \"#id\": 1}}"));
        
        return selectMulti;
    }

    /**
     * Check mandatory parameter
     *
     * @param handler input output list
     * @throws ProcessingException when handler io is not complete
     */
    @Override
    public void checkMandatoryIOParameter(HandlerIO handler) throws ProcessingException {
        // Nothing
    }
}
