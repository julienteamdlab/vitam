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
package fr.gouv.vitam.storage.engine.server.storagetraceability;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import fr.gouv.vitam.common.model.RequestResponse;
import fr.gouv.vitam.common.model.RequestResponseOK;
import fr.gouv.vitam.storage.engine.common.exception.StorageException;
import fr.gouv.vitam.storage.engine.common.model.DataCategory;
import fr.gouv.vitam.storage.engine.common.model.OfferLog;
import fr.gouv.vitam.storage.engine.common.model.Order;
import fr.gouv.vitam.storage.engine.server.distribution.StorageDistribution;

/**
 * Service that allow Storage Traceability to use StorageDistribution in order to get some file and information in Offers
 */
public class TraceabilityStorageService {

    private static final Integer GET_LAST_BASE = 100;
    private final StorageDistribution distribution;

    public TraceabilityStorageService(StorageDistribution distribution) {
        this.distribution = distribution;
    }

    /**
     * Get the files of the last storage backup since the last traceability (fromDate) as a StorageTraceabilityIterator
     * 
     * @param strategyId The storage strategy ID
     * @param fromDate the limit date to get backup files
     * @return list of last saved files as iterator
     * @throws StorageException if some error technical problem while call StorageDistribution
     */
    public StorageTraceabilityIterator getLastSaved(String strategyId, LocalDateTime fromDate) throws StorageException {
        List<OfferLog> allFiles = new ArrayList<>();
        Long offset = null;
        
        while(true) {
            List<OfferLog> files = getLast(strategyId, DataCategory.STORAGELOG, offset, GET_LAST_BASE);
            
            // Directly return if no more items in DB
            if (files.size() < GET_LAST_BASE) {
                allFiles.addAll(files);
                break;
            }

            OfferLog file = files.get(files.size() - 1);
            LocalDateTime date = parseDateFromFileName(file.getFileName(), false);
            if (date.isBefore(fromDate) ) {
                allFiles.addAll(files);
                break;
            }
            
            offset = file.getSequence() - 1;
        }
        
        Integer index = 0;
        LocalDateTime date;
        OfferLog file;
        do {
            index ++;
            file = allFiles.get(index);
            date = parseDateFromFileName(file.getFileName(), false);
        } while(date.isAfter(fromDate) && index < allFiles.size());

        return new StorageTraceabilityIterator(allFiles.subList(0, index));
    }

    /**
     * Get the last storage traceability zip fileName
     * 
     * @param strategyId The storage strategy ID
     * @return the zip's fileName of the last storage traceability operation
     * @throws StorageException if some error technical problem while call StorageDistribution
     */
    public String getLastTraceability(String strategyId) throws StorageException {
        List<OfferLog> file = getLast(strategyId, DataCategory.STORAGETRACEABILITY, null, 1);
        if (file.isEmpty()) {
            return null;
        }
        return file.get(0).getFileName();
    }

    /**
     * Only direct call to @StorageDistribution.getContainerByCategory
     * 
     * @param strategyId strategyID
     * @param objectId file id or name
     * @param category storage category of the file
     * @return the file as stream
     * @throws StorageException if some error technical problem while call StorageDistribution
     */
    public Response getObject(String strategyId, String objectId, DataCategory category) throws StorageException {
        return this.distribution.getContainerByCategory(strategyId, objectId, category);
    }

    /**
     * parse @DataCategory.STORAGETRACEABILITY or @DataCategory.STORAGELOG fileName in order to get the startDate of the operation
     * 
     * @param fileName the fileName to parse
     * @param isTraceability the king of file (true for Traceability, false for backup)
     * @return The startDate of the linked operation
     */
    public LocalDateTime parseDateFromFileName(String fileName, boolean isTraceability) {
        String date;
        String[] splittedFileName = fileName.split("\\.")[0].split("_");
        if (isTraceability) {
            // FileNamePattern: <Tenant>_<FileKind>_<yyyyMMdd-Date>_<HHmmss-Time>.zip
            date = splittedFileName[2] + '-' + splittedFileName[3];
        } else {
            // FileNamePattern: <Tenant>_<file_kind>_<yyyyMMddHHmmssSSS-StartDate>_<yyyyMMddHHmmssSSS-EndDate>_<OperationID>.log
            date = fileName.split("_")[3].substring(0, 8) + '-' + fileName.split("_")[3].substring(8, 14);
        }
        return LocalDateTime.parse(date,
            DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss"));
    }

    private List<OfferLog> getLast(String strategyId, DataCategory category, Long offset, Integer limit) throws StorageException {
        RequestResponse<OfferLog> response = distribution.getOfferLogs(strategyId, category, offset, limit, Order.DESC);
        if (response.isOk()) {
            return ((RequestResponseOK<OfferLog>)response).getResults();
        }
        throw new StorageException("Response KO ?");
    }

}