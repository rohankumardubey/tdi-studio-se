// ============================================================================
//
// Copyright (C) 2006-2020 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.model.migration;

import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;

import org.talend.commons.exception.ExceptionHandler;
import org.talend.commons.exception.PersistenceException;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.model.components.ModifyComponentsAction;
import org.talend.core.model.components.conversions.IComponentConversion;
import org.talend.core.model.components.filters.IComponentFilter;
import org.talend.core.model.components.filters.NameComponentFilter;
import org.talend.core.model.migration.AbstractJobMigrationTask;
import org.talend.core.model.properties.Item;
import org.talend.designer.core.model.utils.emf.talendfile.ElementParameterType;
import org.talend.designer.core.model.utils.emf.talendfile.NodeType;
import org.talend.designer.core.model.utils.emf.talendfile.ProcessType;

/**
 * Migration task to change default load method.
 *
 */

public class ChangeDefaultLoadMethod4tSynapseBulkExec extends AbstractJobMigrationTask {

	@Override
	public ExecutionResult execute(Item item) {
		ProcessType processType = getProcessType(item);
		IComponentFilter filter = new NameComponentFilter("tAzureSynapseBulkExec");
		try {
			ModifyComponentsAction.searchAndModify(item, processType, filter,
					Arrays.<IComponentConversion>asList(new IComponentConversion() {

						public void transform(NodeType node) {
							ElementParameterType loadMethod = ComponentUtilities.getNodeProperty(node, "LOAD_METHOD");
							if (loadMethod == null) {
								ComponentUtilities.addNodeProperty(node, "LOAD_METHOD", "CLOSED_LIST");
								ComponentUtilities.getNodeProperty(node, "LOAD_METHOD").setValue("POLYBASE");
							}
						}
					}));
		} catch (PersistenceException e) {
			ExceptionHandler.process(e);
			return ExecutionResult.FAILURE;
		}

		return ExecutionResult.SUCCESS_NO_ALERT;

	}

	public Date getOrder() {
		GregorianCalendar gc = new GregorianCalendar(2020, 4, 12, 12, 0, 0);
		return gc.getTime();
	}
}