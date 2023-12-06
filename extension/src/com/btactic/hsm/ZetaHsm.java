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

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.XMLElement;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.volume.Volume;

import com.zimbra.soap.admin.message.GetAllMailboxesRequest;
import com.zimbra.soap.admin.message.GetAllMailboxesResponse;
import com.zimbra.soap.admin.message.GetAllVolumesRequest;
import com.zimbra.soap.admin.message.GetAllVolumesResponse;

import com.zimbra.soap.admin.type.MailboxInfo;
import com.zimbra.soap.admin.type.VolumeInfo;

import com.zimbra.soap.JaxbUtil;

import java.util.ArrayList;
import java.util.List;

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

        private List<Integer> mailboxIds;

        private List<Integer> getAllMailboxIds(SoapProvisioning prov)
        throws ServiceException {
            List<Integer> ids = new ArrayList<Integer>();
            GetAllMailboxesRequest request = new GetAllMailboxesRequest();
            Element requestElement = JaxbUtil.jaxbToElement(request);
            Element respElem = prov.invoke(requestElement);
            GetAllMailboxesResponse response = JaxbUtil.elementToJaxb(respElem);
            for (MailboxInfo mailboxInfo : response.getMboxes()) {
                ids.add(mailboxInfo.getId());
            }
            return ids;
        }

        private boolean isValidHsmPolicySyntaxList(String[] zimbraHsmPolicyList) {
            boolean validHsmPolicySyntaxList = true;
            for (String nZimbraHsmPolicy: zimbraHsmPolicyList) {
                Pattern hsmPolicyPattern = Pattern.compile("^(message|document|task|appointment|contact)(,(message|document|task|appointment|contact))*:(?<hsmSearch>.+)$");
                Matcher hsmPolicyMatcher = hsmPolicyPattern.matcher(nZimbraHsmPolicy);
                boolean validHsmPolicySyntax = hsmPolicyMatcher.matches();
                if (!(validHsmPolicySyntax)) {
                    validHsmPolicySyntaxList = false;
                    ZimbraLog.misc.error("zimbraHsmPolicy: '" + nZimbraHsmPolicy + "' syntax is not valid!");
                }
                // TODO: Check also if the search is valid or not at this point
                // TODO: Seems quite difficult to implement because you usually need an actual mailbox for testing it
            }
            return validHsmPolicySyntaxList;
        }

        private short getDestinationVolumeId(SoapProvisioning prov) throws ServiceException {
            short destinationVolumeId = -1;

            GetAllVolumesRequest request = new GetAllVolumesRequest();
            Element requestElement = JaxbUtil.jaxbToElement(request);
            Element respElem = prov.invoke(requestElement);
            GetAllVolumesResponse response = JaxbUtil.elementToJaxb(respElem);
            for (VolumeInfo volumeInfo : response.getVolumes()) {
                if (
                       // TODO: Make sure to use this in ZCS 10 and ?ZCS9?
                       // Not sure it's worth using reflection here
                       // in order to have an unique codebase.
                       // Related commit on zm-mailbox: 6e01a80383a9c8a3a1f94831c48c8309c177bbb0
                       //
                       // (volumeInfo.getStoreType() == Volume.StoreType.INTERNAL) &&
                       // volumeInfo.getStoreManagerClass() == 'WhateverMakesSense'
                       (volumeInfo.isCurrent()) &&
                       (volumeInfo.getType() == Volume.TYPE_MESSAGE_SECONDARY)
                   ) {
                       destinationVolumeId = volumeInfo.getId();
                }
            }

            return destinationVolumeId;
        }

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

                if (!(isValidHsmPolicySyntaxList(zimbraHsmPolicyList))) {
                    ZimbraLog.misc.error("One or more of the 'zimbraHsmPolicy' values does not have a valid syntax. Aborting.");
                    return;
                }

                SoapProvisioning prov = SoapProvisioning.getAdminInstance();
                prov.soapZimbraAdminAuthenticate();

                short destinationVolumeId = getDestinationVolumeId(prov);
                if (destinationVolumeId == -1) {
                    ZimbraLog.misc.error("We did not find an expected (Secondary, internal and current) destination volume. Aborting.");
                    return;
                }

                ZimbraLog.misc.info("DEBUG: destinationVolumeId: " + destinationVolumeId);

                mailboxIds = getAllMailboxIds(prov);

                for (int mboxId : mailboxIds) {
                    int zimbraHsmPolicyCounter = 0;
                    for (String nZimbraHsmPolicy: zimbraHsmPolicyList) {
                        zimbraHsmPolicyCounter = zimbraHsmPolicyCounter + 1 ;
                        ZimbraLog.misc.info("DEBUG: mailbox: " + mboxId + " - ZimbraHsmPolicy - (" + zimbraHsmPolicyCounter + "/" + zimbraHsmPolicyList.length +")" + " '" + nZimbraHsmPolicy + "' " + ".");
                    }
                }

            }
            catch (ServiceException e) {
                ZimbraLog.misc.info("Unable to get 'zimbraHsmPolicy' attribute. Aborting.");
                return;
            }
        }
    }
}
