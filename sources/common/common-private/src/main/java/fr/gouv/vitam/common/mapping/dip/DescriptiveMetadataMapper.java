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
package fr.gouv.vitam.common.mapping.dip;

import java.util.Collections;

import javax.xml.datatype.DatatypeConfigurationException;

import fr.gouv.culture.archivesdefrance.seda.v2.DescriptiveMetadataContentType;
import fr.gouv.vitam.common.model.unit.DescriptiveMetadataModel;

/**
 * Map the object DescriptiveMetadataModel generated from Unit data base model
 * To a jaxb object DescriptiveMetadataContentType
 * This help convert DescriptiveMetadataModel to xml using jaxb
 */
public class DescriptiveMetadataMapper {

    /**
     * Map local DescriptiveMetadataModel to jaxb DescriptiveMetadataContentType
     *
     * @param metadataModel
     * @return
     */
    public DescriptiveMetadataContentType map(DescriptiveMetadataModel metadataModel)
        throws DatatypeConfigurationException {

        DescriptiveMetadataContentType dmc = new DescriptiveMetadataContentType();
        dmc.setAcquiredDate(metadataModel.getAcquiredDate());

        AgentTypeMapper agentTypeMapper = new AgentTypeMapper();

        metadataModel.getAddressee().forEach(item -> {
            try {
                dmc.getAddressee().add(agentTypeMapper.convert(item));
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        });

        //        mapMapToElement(dmc, metadataModel.getAny());
        dmc.getAny().addAll(
            TransformJsonTreeToListOfXmlElement.mapJsonToElement(Collections.singletonList(metadataModel.getAny())));

        dmc.setArchivalAgencyArchiveUnitIdentifier(metadataModel.getArchivalAgencyArchiveUnitIdentifier());

        dmc.setAuthorizedAgent(agentTypeMapper.convert(metadataModel.getAuthorizedAgent()));
        dmc.setCoverage(metadataModel.getCoverage());
        dmc.setCreatedDate(metadataModel.getCreatedDate());
        dmc.setCustodialHistory(metadataModel.getCustodialHistory());

        if (metadataModel.getDescriptions() != null) {
            dmc.getDescription().addAll(metadataModel.getDescriptions().getTextTypes());
        }
        dmc.setDescriptionLanguage(metadataModel.getDescriptionLanguage());
        dmc.setDescriptionLevel(metadataModel.getDescriptionLevel());
        dmc.setDocumentType(metadataModel.getDocumentType());
        dmc.setEndDate(metadataModel.getEndDate());
        dmc.getEvent().addAll(metadataModel.getEvent());
        dmc.setFilePlanPosition(metadataModel.getFilePlanPosition());
        dmc.setGps(metadataModel.getGps());
        dmc.setHref(metadataModel.getHref());
        dmc.setId(metadataModel.getId());
        dmc.getKeyword().addAll(metadataModel.getKeyword());
        dmc.setLanguage(metadataModel.getLanguage());
        dmc.setOriginatingAgency(metadataModel.getOriginatingAgency());
        dmc
            .setOriginatingAgencyArchiveUnitIdentifier(metadataModel.getOriginatingAgencyArchiveUnitIdentifier());
        dmc.setOriginatingSystemId(metadataModel.getOriginatingSystemId());
        dmc.setReceivedDate(metadataModel.getReceivedDate());

        metadataModel.getRecipient().forEach(item -> {
            try {
                dmc.getRecipient().add(agentTypeMapper.convert(item));
            } catch (DatatypeConfigurationException e) {
                throw new RuntimeException(e);
            }
        });

        dmc.setRegisteredDate(metadataModel.getRegisteredDate());
        dmc.setRelatedObjectReference(metadataModel.getRelatedObjectReference());
        dmc.setRestrictionEndDate(metadataModel.getRestrictionEndDate());
        dmc.setRestrictionRuleIdRef(metadataModel.getRestrictionRuleIdRef());
        dmc.setRestrictionValue(metadataModel.getRestrictionValue());
        dmc.setRegisteredDate(metadataModel.getReceivedDate());
        dmc.setSentDate(metadataModel.getSentDate());
        dmc.setSignature(metadataModel.getSignature());
        dmc.setSource(metadataModel.getSource());
        dmc.setStartDate(metadataModel.getStartDate());
        dmc.setStatus(metadataModel.getStatus());
        dmc.setSubmissionAgency(metadataModel.getSubmissionAgency());
        dmc.setSystemId(metadataModel.getSystemId());
        dmc.getTag().addAll(metadataModel.getTag());

        if (metadataModel.getTitles() != null) {
            dmc.getTitle().addAll(metadataModel.getTitles().getTextTypes());
        }
        dmc.setTransactedDate(metadataModel.getTransactedDate());
        dmc.setTransferringAgencyArchiveUnitIdentifier(
            metadataModel.getTransferringAgencyArchiveUnitIdentifier());
        dmc.setType(metadataModel.getType());
        dmc.setVersion(metadataModel.getVersion());
        dmc.getWriter().addAll(metadataModel.getWriter());

        return dmc;
    }

}
