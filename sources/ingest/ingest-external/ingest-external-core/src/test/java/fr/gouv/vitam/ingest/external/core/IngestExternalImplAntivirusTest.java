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
package fr.gouv.vitam.ingest.external.core;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import fr.gouv.vitam.common.PropertiesUtils;
import fr.gouv.vitam.common.format.identification.FormatIdentifier;
import fr.gouv.vitam.common.format.identification.FormatIdentifierFactory;
import fr.gouv.vitam.common.format.identification.exception.FileFormatNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierBadRequestException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierFactoryException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierNotFoundException;
import fr.gouv.vitam.common.format.identification.exception.FormatIdentifierTechnicalException;
import fr.gouv.vitam.common.format.identification.model.FormatIdentifierResponse;
import fr.gouv.vitam.common.format.identification.siegfried.FormatIdentifierSiegfried;
import fr.gouv.vitam.common.guid.GUID;
import fr.gouv.vitam.common.guid.GUIDFactory;
import fr.gouv.vitam.common.model.StatusCode;
import fr.gouv.vitam.common.parameter.ParameterHelper;
import fr.gouv.vitam.common.server.application.junit.AsyncResponseJunitTest;
import fr.gouv.vitam.common.thread.RunWithCustomExecutor;
import fr.gouv.vitam.common.thread.RunWithCustomExecutorRule;
import fr.gouv.vitam.common.thread.VitamThreadPoolExecutor;
import fr.gouv.vitam.common.thread.VitamThreadUtils;
import fr.gouv.vitam.ingest.external.common.config.IngestExternalConfiguration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.net.ssl.*")
@PrepareForTest({FormatIdentifierFactory.class})
public class IngestExternalImplAntivirusTest {
    private static final String PATH = "/tmp";
    private static final String SCRIPT_SCAN_CLAMAV_VIRUS = "scan-clamav_virus.sh";
    private static final String SCRIPT_SCAN_CLAMAV_VIRUS_FIXED = "scan-clamav_virus_fixed.sh";
    private static final String SCRIPT_SCAN_CLAMAV_UNKNOWN = "scan-clamav_unknown.sh";
    private static final String CONTEXT_ID = "DEFAULT_WORKFLOW";
    private static final String EXECUTION_MODE = "continu";
    private InputStream stream;
    private static final Integer TENANT_ID = 0;

    @Rule
    public RunWithCustomExecutorRule runInThread =
        new RunWithCustomExecutorRule(VitamThreadPoolExecutor.getDefaultExecutor());


    private static final long timeoutScanDelay = 60000;

    @Before
    public void setUp() {
        // nothing to do here 
    }

    @RunWithCustomExecutor
    @Test
    public void givenVirusFoundThenKo()
        throws Exception {
        final IngestExternalConfiguration config = new IngestExternalConfiguration();
        config.setPath(PATH);
        config.setAntiVirusScriptName(SCRIPT_SCAN_CLAMAV_VIRUS);
        config.setTimeoutScanDelay(timeoutScanDelay);
        IngestExternalImpl ingestExternalImpl = new IngestExternalImpl(config);
        PowerMockito.mockStatic(FormatIdentifierFactory.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model =
            ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }
    
    @RunWithCustomExecutor
    @Test
    public void givenVirusFoundFixedThenKo()
        throws Exception {
        final IngestExternalConfiguration config = new IngestExternalConfiguration();
        config.setPath(PATH);
        config.setAntiVirusScriptName(SCRIPT_SCAN_CLAMAV_VIRUS_FIXED);
        config.setTimeoutScanDelay(timeoutScanDelay);
        IngestExternalImpl ingestExternalImpl = new IngestExternalImpl(config);
        PowerMockito.mockStatic(FormatIdentifierFactory.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model =
            ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }
    
    @RunWithCustomExecutor
    @Test
    public void givenClamavUnknwonThenKo()
        throws Exception {
        final IngestExternalConfiguration config = new IngestExternalConfiguration();
        config.setPath(PATH);
        config.setAntiVirusScriptName(SCRIPT_SCAN_CLAMAV_UNKNOWN);
        config.setTimeoutScanDelay(timeoutScanDelay);
        IngestExternalImpl ingestExternalImpl = new IngestExternalImpl(config);
        PowerMockito.mockStatic(FormatIdentifierFactory.class);
        VitamThreadUtils.getVitamSession().setTenantId(TENANT_ID);
        stream = PropertiesUtils.getResourceAsStream("unfixed-virus.txt");
        final GUID guid = GUIDFactory.newEventGUID(ParameterHelper.getTenantParameter());
        final AsyncResponseJunitTest responseAsync = new AsyncResponseJunitTest();
        PreUploadResume model =
            ingestExternalImpl.preUploadAndResume(stream, CONTEXT_ID, guid, responseAsync);

        StatusCode statusCode = ingestExternalImpl.upload(model, EXECUTION_MODE, guid);
        Assert.assertTrue(statusCode.equals(StatusCode.KO));
    }
}
