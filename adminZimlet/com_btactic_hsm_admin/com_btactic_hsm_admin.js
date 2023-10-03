/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra OSE HSM Extension
 * Copyright (C) 2023 BTACTIC, S.C.C.L.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * ***** END LICENSE BLOCK *****
 */

if(ZaSettings && ZaSettings.EnabledZimlet["com_btactic_hsm_admin"]){

    function com_btactic_hsm_ext () {

    }

    if (window.console && console.log) {
        console.log("Start loading com_btactic_hsm_admin.js");
    }

    // Show additional HSM attributes for GlobalConfig
    if (ZaGlobalConfig && ZaGlobalConfig.myXModel && ZaGlobalConfig.myXModel.items) {
        ZaGlobalConfig.myXModel.items.push({id: "zimbraHsmPolicy", type: _STRING_, ref: "attrs/" + "zimbraHsmPolicy"});
    }

    if(ZaTabView.XFormModifiers["GlobalConfigXFormView"]) {
        com_btactic_hsm_ext.GlobalConfigXFormModifier= function (xFormObject,entry) {
            var cnt = xFormObject.items.length;
            var i = 0;
            for(i = 0; i <cnt; i++) {
                if(xFormObject.items[i].type=="switch")
                    break;
            }
            var tabBar = xFormObject.items[1] ;
            var hsmTabIx = ++this.TAB_INDEX;
            tabBar.choices.push({value:hsmTabIx, label:com_btactic_hsm_admin.zetaHSMTab});

            var hsmAccountTab={
                type:_ZATABCASE_,
                numCols:1,
                caseKey:hsmTabIx,
                items: [
                    {label: null, type: _OUTPUT_, value: com_btactic_hsm_admin.zetaPromo, colSpan:"*", cssStyle:"font-size:20pt; font-weight: bold;"},
                    {type:_SPACER_, colSpan:"*"},
                    {type:_ZAGROUP_,
                        items:[
                            {ref: "zimbraHsmPolicy", type: _TEXTFIELD_, label: com_btactic_hsm_admin.HSMPolicy, msgName: com_btactic_hsm_admin.HSMPolicy}
                        ]
                    }
                ]
            };

            xFormObject.items[i].items.push(hsmAccountTab);
        }
        ZaTabView.XFormModifiers["GlobalConfigXFormView"].push(com_btactic_hsm_ext.GlobalConfigXFormModifier);
    }

}
