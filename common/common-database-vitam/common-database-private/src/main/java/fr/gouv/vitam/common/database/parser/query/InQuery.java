/*******************************************************************************
 * This file is part of Vitam Project.
 *
 * Copyright Vitam (2012, 2016)
 *
 * This software is governed by the CeCILL 2.1 license under French law and abiding by the rules of distribution of free
 * software. You can use, modify and/ or redistribute the software under the terms of the CeCILL license as circulated
 * by CEA, CNRS and INRIA at the following URL "http://www.cecill.info".
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
 * The fact that you are presently reading this means that you have had knowledge of the CeCILL license and that you
 * accept its terms.
 *******************************************************************************/
package fr.gouv.vitam.common.database.parser.query;

import java.util.HashSet;
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import fr.gouv.vitam.common.database.builder.query.Query;
import fr.gouv.vitam.common.database.builder.request.configuration.BuilderToken.QUERY;
import fr.gouv.vitam.common.database.parser.request.adapter.VarNameAdapter;
import fr.gouv.vitam.common.exception.InvalidParseOperationException;
import fr.gouv.vitam.common.json.JsonHandler;

/**
 * In and Nin queries
 *
 *
 *
 */
public class InQuery extends fr.gouv.vitam.common.database.builder.query.InQuery {
    /**
     * For Parsing
     *
     * @param req
     * @param request
     * @param adapter
     * @throws InvalidParseOperationException
     */
    public InQuery(final QUERY req, final JsonNode request, final VarNameAdapter adapter)
        throws InvalidParseOperationException {
        super();
        currentQUERY = req;
        final ObjectNode sub = ((ObjectNode) currentObject).putObject(req.exactToken());
        adapter.setVarsValue(sub, request);
        final Entry<String, JsonNode> requestItem = JsonHandler.checkUnicity("InQuery", sub);
        final ArrayNode array = (ArrayNode) requestItem.getValue();
        for (final JsonNode value : array) {
            if (value.isBoolean()) {
                if (booleanVals == null) {
                    booleanVals = new HashSet<Boolean>();
                }
                booleanVals.add(value.asBoolean());
            } else if (value.isDouble()) {
                if (doubleVals == null) {
                    doubleVals = new HashSet<Double>();
                }
                doubleVals.add(value.asDouble());
            } else if (value.canConvertToLong()) {
                if (longVals == null) {
                    longVals = new HashSet<Long>();
                }
                longVals.add(value.asLong());
            } else if (value.has(Query.DATE)) {
                final String date = value.get(Query.DATE).asText();
                if (stringVals == null) {
                    stringVals = new HashSet<String>();
                }
                stringVals.add(date);
            } else {
                if (stringVals == null) {
                    stringVals = new HashSet<String>();
                }
                stringVals.add(value.asText());
            }
        }
        currentObject = array;
        setReady(true);
    }

}
