/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
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

import com.zimbra.common.service.ServiceException;

import com.zimbra.common.soap.Element;

import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.soap.SoapProvisioning;

import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;

import com.zimbra.soap.admin.message.GetAllMailboxesRequest;
import com.zimbra.soap.admin.message.GetAllMailboxesResponse;
import com.zimbra.soap.JaxbUtil;

import com.zimbra.soap.admin.type.MailboxInfo;

import java.util.ArrayList;
import java.util.List;

public class BlobMover {

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

    public void moveItems(SoapProvisioning prov, String hsmTypesString, String hsmSearchQueryString, short destinationVolumeId) throws ServiceException {
        DbConnection conn = null;
        try {
            conn = DbPool.getConnection();

            List<Integer> mailboxIds = getAllMailboxIds(prov);
            for (int mboxId : mailboxIds) {
                ZimbraLog.misc.info("DEBUG: mailbox: " + mboxId + " - hsmTypesString: '" + hsmTypesString + "' - hsmSearchQueryString: '" + hsmSearchQueryString + "' - destinationVolumeId: " + destinationVolumeId + ".");
            }
        } finally {
            DbPool.quietClose(conn);
        }
    }

}
