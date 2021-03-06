/* Copyright (C) 2013-2015 BMW Group
 * Author: Manfred Bathelt (manfred.bathelt@bmw.de)
 * Author: Juergen Gehring (juergen.gehring@bmw.de)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package de.bmw.yamaica.franca.ui;

import de.bmw.yamaica.common.ui.dialogs.YamaicaNewFileWizard;

public class DeploymentDefinitionNewWizard extends YamaicaNewFileWizard
{
    private static final String YAMAICA_FRANCA_NEW_DEPLOYMENT_DEFINITION_WIZARD = "YamaicaFrancaNewDeploymentDefinitionWizard";
    private static final String NEW_FRANCA_DEPLOYMENT_DEFINITION_FILE = "New Franca Deployment Definition File";

    public DeploymentDefinitionNewWizard()
    {
        super(YAMAICA_FRANCA_NEW_DEPLOYMENT_DEFINITION_WIZARD);

        setWindowTitle(NEW_FRANCA_DEPLOYMENT_DEFINITION_FILE);
    }

    @Override
    public void addPages()
    {
        yamaicaWizardNewFilePage = new DeploymentDefinitionNewPage(workbench, structuredSelection);

        addPage(yamaicaWizardNewFilePage);
    }
}
