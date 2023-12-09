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

import com.zimbra.soap.JaxbUtil;

import java.util.ArrayList;
import java.util.List;

public class DbBlobFilter {

    public List<Integer> filterItemsByVolume (SoapProvisioning prov, List<Integer> zimbraQueryPreFilterItemsChunk, String validOriginVolumeIdsString) {
        List<Integer> filteredItems = new ArrayList<Integer>();

        filteredItems.addAll(zimbraQueryPreFilterItemsChunk);
        // TODO: Do the actual filter

        return filteredItems;

    }

}
