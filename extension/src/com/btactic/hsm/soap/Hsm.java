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

package com.btactic.hsm.soap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.Pair;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.store.StoreManager;

import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.cs.volume.Volume;
import com.zimbra.cs.volume.VolumeManager;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.ZimbraSoapContext;

import com.zimbra.cs.service.admin.AdminDocumentHandler;
import com.zimbra.cs.service.admin.AdminRightCheckPoint;

import com.btactic.hsm.ZetaHsm;

public final class Hsm extends AdminDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        checkRight(zsc, context, null, AdminRight.PR_SYSTEM_ADMIN_ONLY);
        StoreManager sm = StoreManager.getInstance();
        if (!(sm instanceof FileBlobStore)) {
            throw ServiceException.INVALID_REQUEST(sm.getClass().getName()
                    + " is not supported", null);
        }
        HsmRequest req = zsc.elementToJaxb(request);
        ZetaHsm zetahsm = ZetaHsm.getInstance();
        HsmResponse resp = new HsmResponse();

        if ((req.getAction() == HsmRequest.HsmAction.start) ||
                (req.getAction() == HsmRequest.HsmAction.reset)) {
            // reset TODO
        }
        if (req.getAction() == HsmRequest.HsmAction.start) {
                try {
                    zetahsm.process();
                } catch (IOException e) {
                    throw ServiceException.FAILURE("error while deduping", e);
                }
        } else if (req.getAction() == HsmRequest.HsmAction.stop) {
            zetahsm.stopProcessing();
        } else if (req.getAction() == HsmRequest.HsmAction.reset) {
            // Reset TODO
        }
        // return the stats for all actions.
        boolean isRunning = zetahsm.isRunning();
        if (isRunning) {
            resp.setStatus(HsmResponse.HsmStatus.running);
        } else {
            resp.setStatus(HsmResponse.HsmStatus.stopped);
        }

        return zsc.jaxbToElement(resp);
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        notes.add(AdminRightCheckPoint.Notes.SYSTEM_ADMINS_ONLY);
    }
}
