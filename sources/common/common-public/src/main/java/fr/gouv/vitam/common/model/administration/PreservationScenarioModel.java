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
package fr.gouv.vitam.common.model.administration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.gouv.vitam.common.model.ModelConstants;

import javax.validation.constraints.NotEmpty;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.empty;

/**
 * PreservationScenarioModel class
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PreservationScenarioModel {

    private static final String TAG_DESCRIPTION = "Description";

    private static final String TAG_NAME = "Name";

    public static final String TAG_IDENTIFIER = "Identifier";

    public static final String TAG_CREATION_DATE = "CreationDate";

    public static final String TAG_LAST_UPDATE = "LastUpdate";

    private static final String TAG_ACTION_LIST = "ActionList";

    private static final String TAG_GRIFFIN_BY_FORMAT = "GriffinByFormat";

    private static final String TAG_DEFAULT_GRIFFIN = "DefaultGriffin";

    private static final String TAG_METADATA_FILTER = "MetadataFilter";

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_ID)
    private String id;

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_TENANT)
    private Integer tenant;

    @JsonProperty(ModelConstants.HASH + ModelConstants.TAG_VERSION)
    private Integer version;

    @NotEmpty
    @JsonProperty(TAG_NAME)
    private String name;

    @NotEmpty
    @JsonProperty(TAG_IDENTIFIER)
    private String identifier;

    @JsonProperty(TAG_DESCRIPTION)
    private String description;

    @JsonProperty(TAG_CREATION_DATE)
    private String creationDate;

    @JsonProperty(TAG_LAST_UPDATE)
    private String lastUpdate;

    @NotEmpty
    @JsonProperty(TAG_ACTION_LIST)
    private List<ActionTypePreservation> actionList;

    @JsonProperty(TAG_METADATA_FILTER)
    private List<String> metadataFilter;

    @JsonProperty(TAG_GRIFFIN_BY_FORMAT)
    private List<GriffinByFormat> griffinByFormat;

    @JsonProperty(TAG_DEFAULT_GRIFFIN)
    private GriffinByFormat defaultGriffin;

    public PreservationScenarioModel() {
    }

    public PreservationScenarioModel(@NotEmpty String name,
        @NotEmpty String identifier,
        @NotEmpty List<ActionTypePreservation> actionList,
        @NotEmpty List<String> metadataFilter,
        @NotEmpty List<GriffinByFormat> griffinByFormat,
        @NotEmpty GriffinByFormat defaultGriffin) {
        this.name = name;
        this.identifier = identifier;
        this.actionList = actionList;
        this.metadataFilter = metadataFilter;
        this.griffinByFormat = griffinByFormat;
        this.defaultGriffin = defaultGriffin;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getTenant() {
        return tenant;
    }

    public void setTenant(Integer tenant) {
        this.tenant = tenant;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public List<ActionTypePreservation> getActionList() {
        return actionList;
    }

    public void setActionList(List<ActionTypePreservation> actionList) {
        this.actionList = actionList;
    }

    public List<String> getMetadataFilter() {
        return metadataFilter;
    }

    public void setMetadataFilter(List<String> metadataFilter) {
        this.metadataFilter = metadataFilter;
    }

    public List<GriffinByFormat> getGriffinByFormat() {
        return griffinByFormat;
    }

    public void setGriffinByFormat(List<GriffinByFormat> griffinByFormat) {
        this.griffinByFormat = griffinByFormat;
    }

    public GriffinByFormat getDefaultGriffin() {
        return defaultGriffin;
    }

    public void setDefaultGriffin(GriffinByFormat defaultGriffin) {
        this.defaultGriffin = defaultGriffin;
    }

    public Optional<String> getGriffinIdentifierByFormat(String format) {

        if (griffinByFormat == null || griffinByFormat.isEmpty()) {
            return empty();
        }

        for (GriffinByFormat element : griffinByFormat) {
            if (element.getFormatList().contains(format)) {
                return Optional.of(element.getGriffinIdentifier());
            }
        }
        return empty();
    }

    @JsonIgnore
    public Optional<GriffinByFormat> getGriffinByFormat(String format) {

        if (griffinByFormat == null || griffinByFormat.isEmpty()) {
            return empty();
        }

        Optional<GriffinByFormat> first =
            griffinByFormat.stream()
                .filter(e -> e.getFormatList().contains(format))
                .findFirst();

        if (first.isPresent()) {
            return first;
        }

        if (defaultGriffin == null) {
            return empty();
        }

        return Optional.of(defaultGriffin);
    }


    @JsonIgnore
    public Set<String> getAllGriffinIdentifiers() {

        Set<String> identifierSet = new HashSet<>();

        if (griffinByFormat == null || griffinByFormat.isEmpty()) {
            return identifierSet;
        }

        griffinByFormat.forEach(g -> identifierSet.add(g.getGriffinIdentifier()));

        if (defaultGriffin != null)
            identifierSet.add(defaultGriffin.getGriffinIdentifier());

        return identifierSet;
    }

    @Override public String toString() {
        return "PreservationScenarioModel{" +
            "id='" + id + '\'' +
            ", tenant=" + tenant +
            ", version=" + version +
            ", name='" + name + '\'' +
            ", identifier='" + identifier + '\'' +
            ", description='" + description + '\'' +
            ", creationDate='" + creationDate + '\'' +
            ", lastUpdate='" + lastUpdate + '\'' +
            ", actionList=" + actionList +
            ", metadataFilter=" + metadataFilter +
            ", griffinByFormat=" + griffinByFormat +
            ", defaultGriffin=" + defaultGriffin +
            '}';
    }
}
