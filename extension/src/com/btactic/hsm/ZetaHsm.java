/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.btactic.hsm;

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.account.Provisioning;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ZetaHsm {

    private boolean inProgress = false;
    private boolean stopProcessing = false;

    private final static ZetaHsm SINGLETON = new ZetaHsm();

    private ZetaHsm() {
    }

    public static ZetaHsm getInstance() {
        return SINGLETON;
    }

    public synchronized void stopProcessing() {
        if (inProgress) {
            ZimbraLog.misc.info("Setting stopProcessing flag.");
            stopProcessing = true;
        }
    }

    private synchronized boolean isStopProcessing() {
        return stopProcessing;
    }
    
    public synchronized boolean isRunning() {
        return inProgress;
    }

    private synchronized void resetProgress() {
        inProgress = false;
        stopProcessing = false;
    }

    public void process() throws ServiceException, IOException {
        synchronized (this) {
            if (inProgress) {
                throw MailServiceException.TRY_AGAIN("ZetaHsm is already in progress. Only one request can be run at a time.");
            }
            inProgress = true;
        }
        Thread thread = new ZetaHsmThread();
        thread.setName("ZetaHsm");
        thread.start();
    }

    private class ZetaHsmThread extends Thread {

        public ZetaHsmThread() {
        }

        public void run() {
            // TODO: Sleep 1 minute as a proof of concept
            resetProgress();
            ZimbraLog.misc.info("DEBUG: ZetaHsm RUN function was run.");
            try {
                String[] zimbraHsmPolicyList = Provisioning.getInstance().getLocalServer().getMultiAttr("zimbraHsmPolicy");

                if (zimbraHsmPolicyList.length == 0) {
                    ZimbraLog.misc.info("'zimbraHsmPolicy' attribute is empty. Nothing to do. Aborting.");
                    return;
                }

                for (String nZimbraHsmPolicy: zimbraHsmPolicyList) {
                    Pattern hsmPolicyPattern = Pattern.compile("^(message|document|task|appointment|contact)(,(message|document|task|appointment|contact))*:(?<hsmSearch>.+)$");
                    Matcher hsmPolicyMatcher = hsmPolicyPattern.matcher(nZimbraHsmPolicy);
                    boolean validHsmPolicySyntax = hsmPolicyMatcher.matches();
                    ZimbraLog.misc.info("DEBUG: Is: '" + nZimbraHsmPolicy + "' policy a valid one? " + ( validHsmPolicySyntax ? "Yes!" : "No!") );
                }

            }
            catch (ServiceException e) {
                ZimbraLog.misc.info("Unable to get 'zimbraHsmPolicy' attribute. Aborting.");
                return;
            }
        }
    }
}
