/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.action;

import com.liferay.portal.kernel.json.JSONFactoryUtil;
import com.liferay.portal.kernel.json.JSONObject;
import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.servlet.ServletResponseUtil;
import com.liferay.portal.kernel.servlet.SessionErrors;
import com.liferay.portal.kernel.util.Constants;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.security.auth.PrincipalException;
import com.liferay.portal.setup.SetupWizardUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PropsValues;
import com.liferay.portal.util.WebKeys;

import java.sql.SQLException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * @author Manuel de la Peña
 * @author Brian Wing Shun Chan
 */
public class SetupWizardAction extends Action {

	@Override
	public ActionForward execute(
			ActionMapping mapping, ActionForm form, HttpServletRequest request,
			HttpServletResponse response)
		throws Exception {

		ThemeDisplay themeDisplay =
			(ThemeDisplay)request.getAttribute(WebKeys.THEME_DISPLAY);

		if (!PropsValues.SETUP_WIZARD_ENABLED) {
			response.sendRedirect(themeDisplay.getPathMain());
		}

		String cmd = ParamUtil.getString(request, Constants.CMD);

		if (Validator.isNull(cmd)) {
			return mapping.findForward("portal.setup_wizard");
		}

		try {
			if (cmd.equals(Constants.TRANSLATE)) {
				SetupWizardUtil.processPortalLanguage(request, response);

				return mapping.findForward("portal.setup_wizard");
			}
			else if (cmd.equals(Constants.TEST)) {

				JSONObject jsonObject = JSONFactoryUtil.createJSONObject();

				try {
					SetupWizardUtil.testDatabase(request) ;

					jsonObject.put("tested", Boolean.TRUE);
					jsonObject.put(
						"message",
						LanguageUtil.get(
							themeDisplay.getLocale(),
							"database-connection-has-been-established-" +
								"sucessfully"));
				}
				catch(ClassNotFoundException cnfe) {
					jsonObject.put("tested", Boolean.FALSE);
					jsonObject.put(
						"message", LanguageUtil.format(
							themeDisplay.getLocale(),
							"database-driver-x-is-not-present",
							cnfe.getLocalizedMessage()));
				}
				catch(SQLException sqle) {
					jsonObject.put("tested", Boolean.FALSE);
					jsonObject.put(
						"message", LanguageUtil.get(
							themeDisplay.getLocale(),
							"database-connection-has-not-been-established"));
				}

				response.setContentType(ContentTypes.TEXT_JAVASCRIPT);

				ServletResponseUtil.write(response, jsonObject.toString());

				return null;
			}
			else if (cmd.equals(Constants.UPDATE)) {
				SetupWizardUtil.processSetup(request);
			}

			response.sendRedirect(
				themeDisplay.getPathMain() + "/portal/setup_wizard");

			return null;
		}
		catch (Exception e) {
			if (e instanceof PrincipalException) {
				SessionErrors.add(request, e.getClass().getName());

				return mapping.findForward("portal.setup_wizard");
			}
			else {
				PortalUtil.sendError(e, request, response);

				return null;
			}
		}
	}

}