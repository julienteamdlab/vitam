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
package fr.gouv.vitam.storage;

import fr.gouv.vitam.storage.logbook.parameters.StorageLogbookParameters;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.CREATE_NEW;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Storage Logbook  Appender
 */
public class StorageLogAppender {

    private Path fileLocation;

    private final String lineSeparator = System.getProperty("line.separator");

    private final String PARAMS_CANNOT_BE_NULL = "Params cannot be null";

    private final List<Integer> tenantIds;

    private final Map<Integer, OutputStream> streams = new HashMap<>();

    private final Map<Integer, Path> filesNames = new HashMap<>();

    private final Map<Integer, LocalDateTime> beginLogTimes = new HashMap<>();

    private final Map<Integer, ReentrantLock> lockers = new HashMap<>();



    /**
     * <pre>{@code
     * usage
     * final ArrayList list = new ArrayList<>();
     * list.add(1);
     * list.add(2);
     * StorageLogAppender storageLogAppender = new StorageLogAppender(list, folder.getRoot().toPath());
     * }
     * </pre>
     *
     * @param tenantIds
     * @param path
     * @throws IOException
     */
    public StorageLogAppender(List<Integer> tenantIds, Path path) throws IOException {
        this.tenantIds = tenantIds;
        this.fileLocation = path;
        //create log by tenant
        for (Integer tenant : this.tenantIds) {
            ReentrantLock lock = new ReentrantLock();
            lockers.put(tenant, lock);
            try {
                lock.lock();
                createNewLog(tenant);
            }
            finally {
                lock.unlock();
            }

            beginLogTimes.put(tenant, LocalDateTime.now());
        }
    }


    private void createNewLog(Integer tenant) throws IOException {
        Path filepath = constructPath(tenant);
        filesNames.put(tenant, filepath);
        OutputStream out = openTenantStream(tenant, filepath);
        streams.put(tenant, out);
    }

    /**
     * Create
     *
     * @param tenant
     * @return
     */
    private Path constructPath(Integer tenant) {
        LocalDateTime date = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String file_name = fileLocation.toString() + "/"
            + tenant.toString()
            + "_" + date.toInstant(ZoneOffset.ofTotalSeconds(0)).toEpochMilli()
            + "_" + date.format(formatter)
            + UUID.randomUUID().toString()
            + ".log";
        return Paths.get(file_name);
    }

    private OutputStream openTenantStream(Integer tenant, Path path) throws IOException {
        return Files.newOutputStream(path, CREATE_NEW, APPEND);
    }

    /**
     * Secure log by tenant
     * create a new log
     * and close curent stream
     *
     * @param tenant the tenantId
     * @return the path of the last securised log
     * @throws IOException
     */
    public LogInformation secureAndCreateNewlogByTenant(Integer tenant) throws IOException {
        // get tenant Stream
        OutputStream out = streams.get(tenant);
        LocalDateTime endTime;
        //save the name
        Path lastPath = filesNames.get(tenant);
        ReentrantLock lock = lockers.get(tenant);
        try {
            lock.lock();
            endTime = LocalDateTime.now();
            out.flush();
            out.close();
            // create a new name he fure log
            createNewLog(tenant);
        } finally {
            lock.unlock();
        }
        return new LogInformation(lastPath, beginLogTimes.get(tenant), endTime);
    }

    /**
     * Call only when no more appending log is expected
     * for exemple when context is destroyed
     *
     * @param tenant
     * @return
     * @throws IOException
     */
    public LogInformation secureWithoutCreatingNewLogByTenant(Integer tenant) throws IOException {
        OutputStream out = streams.get(tenant);
        LocalDateTime endTime = null;
        //save the name
        Path lastPath = filesNames.get(tenant);
        ReentrantLock lock = lockers.get(tenant);
        try {
            lock.lock();
            endTime = LocalDateTime.now();
            out.flush();
            out.close();
        } finally {
            lock.unlock();
        }
        return new LogInformation(lastPath, beginLogTimes.get(tenant), endTime);
    }

    /**
     * Append logbookParameters to the current log.
     *
     * @param tenant
     * @param parameters
     * @return
     * @throws IOException
     */
    public StorageLogAppender append(Integer tenant, StorageLogbookParameters parameters) throws IOException {
        ReentrantLock lock = lockers.get(tenant);
        try {
            final OutputStream out = streams.get(tenant);
            lock.lock();
            out.write((parameters.getMapParameters().toString()+lineSeparator).getBytes());
        } finally {
            lock.unlock();
        }
        return this;
    }


    private String getTime() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");
        return LocalDateTime.now().format(formatter);
    }



    /***
     *Enumerations of Type Message
     */
    public enum StorageLogAppenderEnum {
        /**
         * CREATE type
         */
        CREATE,
        /**
         * Update type
         */
        UPDATE,
        /**
         * delete type
         */
        DELETE
    }

}
