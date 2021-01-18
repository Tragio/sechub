// SPDX-License-Identifier: MIT
package com.daimler.sechub.adapter;

import static com.daimler.sechub.sharedkernel.util.Assert.*;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.daimler.sechub.domain.scan.product.config.ProductExecutorConfig;
import com.daimler.sechub.domain.scan.product.config.ProductExecutorConfigSetupCredentials;
import com.daimler.sechub.domain.scan.product.config.ProductExecutorConfigSetupJobParameter;
import com.daimler.sechub.sharedkernel.SystemEnvironment;
import com.daimler.sechub.sharedkernel.validation.AssertValidation;
import com.daimler.sechub.sharedkernel.validation.Validation;

/**
 * A standard execution config support implementation. Supports environment entry evaluation, simple key value checks etc.
 * 
 * @author Albert Tregnaghi
 *
 */
public class DefaultExecutionConfigSupport {

    private static final String ENV_PREFIX_ID = "env:";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultExecutionConfigSupport.class);

    protected Map<String, String> configuredExecutorParameters = new TreeMap<>();

    protected ProductExecutorConfig config;
    private SystemEnvironment systemEnvironment;

    public DefaultExecutionConfigSupport(ProductExecutorConfig config, SystemEnvironment systemEnvironment, Validation<ProductExecutorConfig> validation) {
        notNull(config, "config may not be null!");
        notNull(systemEnvironment, "systemEnvironment may not be null!");

        this.config = config;
        this.systemEnvironment = systemEnvironment;

        if (validation != null) {
            AssertValidation.assertValid(config, validation);
        }

        /* create a simple map containing parameters */
        List<ProductExecutorConfigSetupJobParameter> jobParameters = config.getSetup().getJobParameters();
        for (ProductExecutorConfigSetupJobParameter jobParameter : jobParameters) {
            configuredExecutorParameters.put(jobParameter.getKey(), jobParameter.getValue());
        }

    }
    
    public String getPasswordOrAPIToken() {
        ProductExecutorConfigSetupCredentials credentials = config.getSetup().getCredentials();
        String pwd = credentials.getPassword();
        return evaluateEnvironmentEntry(pwd);
    }

    public String getProductBaseURL() {
        return config.getSetup().getBaseURL();
    }

    public String getUser() {
        ProductExecutorConfigSetupCredentials credentials = config.getSetup().getCredentials();
        String user = credentials.getUser();
        return evaluateEnvironmentEntry(user);
    }

    private String evaluateEnvironmentEntry(String data) {
        if (data == null) {
            return null;
        }
        if (!data.startsWith(ENV_PREFIX_ID)) {
            return data;
        }
        String key = data.substring(ENV_PREFIX_ID.length());
        String value = systemEnvironment.getEnv(key);
        if (value == null) {
            LOG.warn("No environment entry defined for variable:{}", key);
        }
        return value;
    }

    protected boolean getParameterBooleanValue(String key) {
        String asText = getParameter(key);
        return Boolean.parseBoolean(asText);
    }

    protected String getParameter(String key) {
        if (key == null) {
            return null;
        }
        return configuredExecutorParameters.get(key);
    }

    protected int getParameterIntValue(String key) {
        String asText = getParameter(key);
        if (asText == null) {
            return -1;
        }
        try {
            return Integer.parseInt(asText);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

}
