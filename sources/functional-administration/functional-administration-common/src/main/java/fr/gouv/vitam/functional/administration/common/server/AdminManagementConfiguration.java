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

package fr.gouv.vitam.functional.administration.common.server;

import java.util.List;
import java.util.Map;

import fr.gouv.vitam.common.database.server.elasticsearch.ElasticsearchNode;
import fr.gouv.vitam.common.server.application.configuration.DbConfigurationImpl;
import fr.gouv.vitam.common.server.application.configuration.MongoDbNode;

/**
 * AdminManagementConfiguration inherated from DbConfigurationImpl
 */
public class AdminManagementConfiguration extends DbConfigurationImpl {

    private String workspaceUrl;
    private String processingUrl;

    private String clusterName;
    private List<ElasticsearchNode> elasticsearchNodes;


    // constructor
    AdminManagementConfiguration() {
        super();
    }

    private Map<Integer, List<String>> listEnableExternalIdentifiers;
    private Map<Integer, Map<String, String>> listMinimumRuleDuration;

    /**
     * Constructor
     *
     * @param mongoDbNodes the database hosts and ports
     * @param dbName the database name
     * @param clusterName the cluster name
     * @param elasticsearchNodes the list of Elasticsearch nodes
     */
    public AdminManagementConfiguration(List<MongoDbNode> mongoDbNodes, String dbName, String clusterName,
        List<ElasticsearchNode> elasticsearchNodes) {
        super(mongoDbNodes, dbName);
        this.clusterName = clusterName;
        this.elasticsearchNodes = elasticsearchNodes;
    }



    /**
     * @return url workspace
     */
    public String getWorkspaceUrl() {
        return workspaceUrl;
    }

    /**
     * @param workspaceUrl to set
     */
    public void setWorkspaceUrl(String workspaceUrl) {
        this.workspaceUrl = workspaceUrl;
    }

    /**
     * @return processing Url
     */
    public String getProcessingUrl() {
        return processingUrl;
    }

    /**
     * @param processingUrl to set
     */
    public void setProcessingUrl(String processingUrl) {
        this.processingUrl = processingUrl;
    }

    /**
     * @return the clusterName
     */
    public String getClusterName() {
        return clusterName;
    }

    /**
     * @param clusterName the clusterName to set
     * @return this
     */
    public AdminManagementConfiguration setClusterName(String clusterName) {
        this.clusterName = clusterName;
        return this;
    }

    /**
     * @return the elasticsearchNodes
     */
    public List<ElasticsearchNode> getElasticsearchNodes() {
        return elasticsearchNodes;
    }

    /**
     * @param elasticsearchNodes the elasticsearchNodes to set
     * @return AdminManagementConfiguration
     */
    public AdminManagementConfiguration setElasticsearchNodes(List<ElasticsearchNode> elasticsearchNodes) {
        this.elasticsearchNodes = elasticsearchNodes;
        return this;
    }

    /**
     * @return listEnableExternalIdentifiers
     */
    public Map<Integer, List<String>> getListEnableExternalIdentifiers() {
        return listEnableExternalIdentifiers;
    }

    /**
     * Setter for listEnableExternalIdentifiers;
     */
    public void setListEnableExternalIdentifiers(
        Map<Integer, List<String>> listEnableExternalIdentifiers) {

        this.listEnableExternalIdentifiers = listEnableExternalIdentifiers;
    }

    /**
     * @return listMinimumRuleDuration
     */
    public Map<Integer, Map<String, String>> getListMinimumRuleDuration() {
        return listMinimumRuleDuration;
    }

    /**
     * @param listMinimumRuleDuration
     * @return AdminManagementConfiguration
     */
    public AdminManagementConfiguration setListMinimumRuleDuration(
        Map<Integer, Map<String, String>> listMinimumRuleDuration) {
        this.listMinimumRuleDuration = listMinimumRuleDuration;
        return this;
    }

}
