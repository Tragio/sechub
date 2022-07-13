// SPDX-License-Identifier: MIT
package com.mercedesbenz.sechub.domain.scan.product.pds;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.mercedesbenz.sechub.adapter.AdapterExecutionResult;
import com.mercedesbenz.sechub.adapter.pds.PDSAdapter;
import com.mercedesbenz.sechub.adapter.pds.PDSInfraScanConfig;
import com.mercedesbenz.sechub.adapter.pds.PDSInfraScanConfigImpl;
import com.mercedesbenz.sechub.adapter.pds.PDSMetaDataID;
import com.mercedesbenz.sechub.commons.model.ScanType;
import com.mercedesbenz.sechub.domain.scan.InfraScanNetworkLocationProvider;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetProductServerDataAdapterConfigurationStrategy;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetRegistry.NetworkTargetInfo;
import com.mercedesbenz.sechub.domain.scan.NetworkTargetType;
import com.mercedesbenz.sechub.domain.scan.product.AbstractProductExecutor;
import com.mercedesbenz.sechub.domain.scan.product.ProductExecutorContext;
import com.mercedesbenz.sechub.domain.scan.product.ProductExecutorData;
import com.mercedesbenz.sechub.domain.scan.product.ProductIdentifier;
import com.mercedesbenz.sechub.domain.scan.product.ProductResult;
import com.mercedesbenz.sechub.sharedkernel.configuration.SecHubConfiguration;
import com.mercedesbenz.sechub.sharedkernel.execution.SecHubExecutionContext;

@Service
public class PDSInfraScanProductExecutor extends AbstractProductExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(PDSInfraScanProductExecutor.class);

    @Autowired
    PDSAdapter pdsAdapter;

    @Autowired
    PDSInstallSetup installSetup;

    @Autowired
    PDSExecutorConfigSuppportServiceCollection serviceCollection;

    @Autowired
    PDSStorageContentProviderFactory contentProviderFactory;

    public PDSInfraScanProductExecutor() {
        super(ProductIdentifier.PDS_INFRASCAN, 1, ScanType.INFRA_SCAN);
    }

    @Override
    protected List<ProductResult> executeByAdapter(ProductExecutorData data) throws Exception {
        NetworkTargetInfo info = data.getCurrentNetworkTargetInfo();

        Set<URI> targetURIs = info.getURIs();
        if (targetURIs.isEmpty()) {
            /* no targets defined */
            return Collections.emptyList();
        }
        NetworkTargetType targetType = info.getTargetType();
        ProductExecutorContext executorContext = data.getProductExecutorContext();

        /* we reuse config support created inside customize method */
        PDSExecutorConfigSuppport configSupport = (PDSExecutorConfigSuppport) data.getNetworkTargetDataProvider();

        if (configSupport.isTargetTypeForbidden(targetType)) {
            LOG.info("pds adapter does not accept target type:{} so cancel execution");
            return Collections.emptyList();
        }
        LOG.debug("Trigger pds infra scan adapter execution for target {}", targetType);

        List<ProductResult> results = new ArrayList<>();

        SecHubExecutionContext context = data.getSechubExecutionContext();
        PDSStorageContentProvider contentProvider = contentProviderFactory.createContentProvider(context, configSupport, getScanType());

        for (URI targetURI : targetURIs) {
            /* @formatter:off */

            /* special behavior, because having multiple results here, we must find former result corresponding to
             * target URI.
             */
            executorContext.useFirstFormerResultHavingMetaData(PDSMetaDataID.KEY_TARGET_URI, targetURI);

            PDSInfraScanConfig pdsInfraScanConfig = PDSInfraScanConfigImpl.builder().
                    configure(PDSAdapterConfigurationStrategy.builder().
                            setScanType(getScanType()).
                            setProductExecutorData(data).
                            setConfigSupport(configSupport).
                            setContentProvider(contentProvider).
                            setInstallSetup(installSetup).
                            build()).
                    /* additional:*/
                    configure(new NetworkTargetProductServerDataAdapterConfigurationStrategy(configSupport,data.getCurrentNetworkTargetInfo().getTargetType())).

                    setTargetIPs(info.getIPs()).
                    setTargetURIs(info.getURIs()).

                    build();
            /* @formatter:on */

            /* execute PDS by adapter and return product result */
            AdapterExecutionResult adapterResult = pdsAdapter.start(pdsInfraScanConfig, executorContext.getCallback());

            ProductResult currentProductResult = updateCurrentProductResult(adapterResult, executorContext);
            results.add(currentProductResult);

        }
        return results;
    }

    @Override
    protected void customize(ProductExecutorData data) {
        SecHubConfiguration secHubConfiguration = data.getSechubExecutionContext().getConfiguration();
        data.setNetworkLocationProvider(new InfraScanNetworkLocationProvider(secHubConfiguration));

        ProductExecutorContext executorContext = data.getProductExecutorContext();
        PDSExecutorConfigSuppport configSupport = PDSExecutorConfigSuppport.createSupportAndAssertConfigValid(executorContext.getExecutorConfig(),
                serviceCollection);
        data.setNetworkTargetDataProvider(configSupport);
    }

}
