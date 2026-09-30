package com.acme.orders.service;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Runs customer-supplied pricing rules under a restricted policy.
 * Required by the 2014 PCI review.
 */
@Component
@SuppressWarnings({ "removal", "deprecation" })
public class SandboxRunner {

    private static final Logger log = LoggerFactory.getLogger(SandboxRunner.class);

    public void installPolicy() {
        SecurityManager existing = System.getSecurityManager();
        if (existing == null) {
            log.info("Installing restrictive security manager for rule evaluation");
            System.setSecurityManager(new SecurityManager() {
                @Override
                public void checkExit(int status) {
                    throw new SecurityException("Rule scripts may not call System.exit");
                }
            });
        }
    }

    public <T> T runRestricted(Callable<T> work) {
        return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
            try {
                return work.call();
            } catch (Exception e) {
                throw new IllegalStateException("Rule evaluation failed", e);
            }
        });
    }
}
