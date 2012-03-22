// ============================================================================
//
// Copyright (C) 2006-2012 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.xmlmap.commands;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.commands.Command;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.talend.core.model.utils.TalendTextUtils;
import org.talend.designer.xmlmap.dnd.DragAndDrogDialog;
import org.talend.designer.xmlmap.dnd.TransferedObject;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractInOutTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.AbstractNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.Connection;
import org.talend.designer.xmlmap.model.emf.xmlmap.FilterConnection;
import org.talend.designer.xmlmap.model.emf.xmlmap.InputLoopNodesTable;
import org.talend.designer.xmlmap.model.emf.xmlmap.LookupConnection;
import org.talend.designer.xmlmap.model.emf.xmlmap.NodeType;
import org.talend.designer.xmlmap.model.emf.xmlmap.OutputTreeNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.OutputXmlTree;
import org.talend.designer.xmlmap.model.emf.xmlmap.TreeNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.VarNode;
import org.talend.designer.xmlmap.model.emf.xmlmap.VarTable;
import org.talend.designer.xmlmap.model.emf.xmlmap.XmlMapData;
import org.talend.designer.xmlmap.model.emf.xmlmap.XmlmapFactory;
import org.talend.designer.xmlmap.parts.AbstractNodePart;
import org.talend.designer.xmlmap.parts.InputXmlTreeEditPart;
import org.talend.designer.xmlmap.parts.OutputTreeNodeEditPart;
import org.talend.designer.xmlmap.parts.OutputXmlTreeEditPart;
import org.talend.designer.xmlmap.parts.TreeNodeEditPart;
import org.talend.designer.xmlmap.parts.VarNodeEditPart;
import org.talend.designer.xmlmap.util.XmlMapUtil;

/**
 * wchen class global comment. Detailled comment
 */
public class CreateNodeAndConnectionCommand extends Command {

    private Object newObjects;

    private EditPart targetEditPart;

    private XmlMapData xmlMapData;

    private TreeNode loopParentTreeNode;

    private List<InputLoopNodesTable> listInputLoopNodesTablesEntry;

    /*
     * if true update expression,else create a new child
     */
    private boolean update;

    public CreateNodeAndConnectionCommand(Object newObjects, EditPart targetEditPart, boolean update) {
        this.newObjects = newObjects;
        this.targetEditPart = targetEditPart;
        this.update = update;

    }

    @Override
    public void execute() {
        if (targetEditPart == null) {
            return;
        }
        xmlMapData = getXmlMapData(targetEditPart.getModel());
        if (xmlMapData == null) {
            return;
        }
        if (newObjects instanceof TransferedObject) {
            TransferedObject tranceferedObj = (TransferedObject) newObjects;
            // this node type is only used when drag leaf element or attribute or varnode to create output node
            NodeType selectedNodeType = NodeType.ELEMENT;
            if (!update && targetEditPart instanceof OutputTreeNodeEditPart) {
                OutputTreeNode targetOutputNode = (OutputTreeNode) ((OutputTreeNodeEditPart) targetEditPart).getModel();
                Shell shell = targetEditPart.getViewer().getControl().getShell();

                // if allNamespace , create output as namespace , if allsubTree , create output subtree , no need prompt
                boolean needPrompt = false;
                boolean hasSubTree = false;
                for (Object o : tranceferedObj.getToTransfer()) {
                    if (o instanceof VarNodeEditPart) {
                        needPrompt = true;
                    } else if (o instanceof TreeNodeEditPart) {
                        TreeNode treeNode = (TreeNode) ((TreeNodeEditPart) o).getModel();
                        if (NodeType.ATTRIBUT.equals(treeNode.getNodeType())) {
                            needPrompt = true;
                        }
                        if (NodeType.ELEMENT.equals(treeNode.getNodeType())) {
                            if (treeNode.getChildren().isEmpty()) {
                                needPrompt = true;
                            } else {
                                hasSubTree = true;
                            }
                        }
                    }
                }

                if (needPrompt) {
                    DragAndDrogDialog selectDialog = new DragAndDrogDialog(shell, !targetOutputNode.getChildren().isEmpty());
                    int open = selectDialog.open();
                    if (open == Window.OK) {
                        if (DragAndDrogDialog.CREATE_AS_SUBELEMENT.equals(selectDialog.getSelectValue())) {
                            selectedNodeType = NodeType.ELEMENT;
                        } else if (DragAndDrogDialog.CREATE_AS_ATTRIBUTE.equals(selectDialog.getSelectValue())) {
                            selectedNodeType = NodeType.ATTRIBUT;
                        } else if (DragAndDrogDialog.CREATE_AS_SUBELEMENT.equals(selectDialog.getSelectValue())) {
                            selectedNodeType = NodeType.NAME_SPACE;
                        } else if (DragAndDrogDialog.CREATE_AS_TEXT.equals(selectDialog.getSelectValue())) {
                            update = true;
                        }
                    } else {
                        return;
                    }
                }

                if (!update) {
                    if (!targetOutputNode.getIncomingConnections().isEmpty()
                            && ((selectedNodeType != NodeType.ATTRIBUT && selectedNodeType != NodeType.NAME_SPACE) || hasSubTree)) {
                        boolean canContinue = MessageDialog
                                .openConfirm(null, "Warning",
                                        "Do you want to disconnect the existing linker and then add an sub element for the selected element ?");
                        if (canContinue) {
                            XmlMapUtil.detachNodeConnections(targetOutputNode, xmlMapData, false);
                        } else {
                            return;
                        }
                    }
                }
            }

            for (Object o : (tranceferedObj.getToTransfer())) {
                if (!(o instanceof AbstractNodePart)) {
                    continue;
                }
                AbstractNode sourceNode = (AbstractNode) ((AbstractNodePart) o).getModel();
                if (o instanceof TreeNodeEditPart) {
                    if (((TreeNode) sourceNode).isLoop()) {
                        loopParentTreeNode = (TreeNode) sourceNode;
                    } else {
                        loopParentTreeNode = XmlMapUtil.getLoopParentNode((TreeNode) sourceNode);
                    }
                }
                if (update) {
                    doUpdate(sourceNode);
                } else {
                    // only drop output can create a new node now
                    if (targetEditPart instanceof OutputTreeNodeEditPart) {
                        OutputTreeNode targetOutputNode = (OutputTreeNode) ((OutputTreeNodeEditPart) targetEditPart).getModel();
                        OutputTreeNode targetNode = XmlmapFactory.eINSTANCE.createOutputTreeNode();
                        targetNode.setName(sourceNode.getName());
                        targetNode.setType(XmlMapUtil.DEFAULT_DATA_TYPE);
                        if (sourceNode instanceof TreeNode) {
                            NodeType nodeType = selectedNodeType;
                            if (NodeType.NAME_SPACE.equals(((TreeNode) sourceNode).getNodeType())) {
                                // namespace and only be droped as namespace
                                nodeType = NodeType.NAME_SPACE;
                                targetNode.setDefaultValue(((TreeNode) sourceNode).getDefaultValue());
                            } else if (!((TreeNode) sourceNode).getChildren().isEmpty()) {
                                nodeType = ((TreeNode) sourceNode).getNodeType();
                            }

                            targetNode.setXpath(XmlMapUtil.getXPath(targetOutputNode.getXpath(), targetNode.getName(), nodeType));
                            targetNode.setNodeType(nodeType);
                            targetNode.setExpression(XmlMapUtil.convertToExpression(((TreeNode) sourceNode).getXpath()));

                            EList<TreeNode> sourceChildren = ((TreeNode) sourceNode).getChildren();
                            if (!sourceChildren.isEmpty()) {
                                // children must be attribute or namespace
                                for (TreeNode child : sourceChildren) {
                                    OutputTreeNode childTarget = XmlmapFactory.eINSTANCE.createOutputTreeNode();
                                    childTarget.setName(child.getName());
                                    childTarget.setType(child.getType());
                                    childTarget.setNodeType(child.getNodeType());
                                    childTarget.setXpath(XmlMapUtil.getXPath(targetNode.getXpath(), childTarget.getName(),
                                            childTarget.getNodeType()));
                                    targetNode.getChildren().add(childTarget);

                                    if (NodeType.NAME_SPACE.equals(child.getNodeType())) {
                                        childTarget.setDefaultValue(child.getDefaultValue());
                                        // default value is already set as from source , no need the expression to get
                                        // default value
                                        childTarget.setExpression("");
                                    } else {
                                        childTarget.setExpression(XmlMapUtil.convertToExpression(((TreeNode) child).getXpath()));
                                        Connection conn = XmlmapFactory.eINSTANCE.createConnection();
                                        conn.setSource(child);
                                        conn.setTarget(childTarget);
                                        // attach source and target
                                        childTarget.getIncomingConnections().add(conn);
                                        child.getOutgoingConnections().add(conn);
                                        if (xmlMapData != null) {
                                            xmlMapData.getConnections().add(conn);
                                        }
                                    }
                                }
                            }

                        } else if (sourceNode instanceof VarNode) {
                            String variable = sourceNode.getName();
                            targetNode.setXpath(XmlMapUtil.getXPath(targetOutputNode.getXpath(), targetNode.getName(),
                                    selectedNodeType));
                            targetNode.setNodeType(selectedNodeType);
                            if (sourceNode.eContainer() instanceof VarTable) {
                                VarTable container = (VarTable) sourceNode.eContainer();
                                variable = container.getName() + TalendTextUtils.JAVA_END_STRING + variable;
                            }
                            targetNode.setExpression(variable);
                        }

                        targetOutputNode.getChildren().add(targetNode);
                        // add connection
                        Connection conn = XmlmapFactory.eINSTANCE.createConnection();
                        conn.setSource(sourceNode);
                        conn.setTarget(targetNode);
                        // attach source and target
                        targetNode.getIncomingConnections().add(conn);
                        sourceNode.getOutgoingConnections().add(conn);
                        if (xmlMapData != null) {
                            xmlMapData.getConnections().add(conn);
                        }

                        createInputLoopTable(targetNode, (OutputTreeNodeEditPart) targetEditPart);
                    }
                }

            }
        }
        if (targetEditPart instanceof OutputTreeNodeEditPart) {
            OutputTreeNode model = (OutputTreeNode) targetEditPart.getModel();
            if (NodeType.NAME_SPACE.equals(model.getNodeType()) && model.getExpression() != null
                    && !"".equals(model.getExpression())) {
                model.setDefaultValue("");
            }
            if (!XmlMapUtil.isExpressionEditable(model)) {
                model.setExpression("");
                if (model.isAggregate()) {
                    model.setAggregate(false);
                }
            }
        }
    }

    private XmlMapData getXmlMapData(Object obj) {
        if (obj instanceof AbstractNode) {
            return XmlMapUtil.getXmlMapData((AbstractNode) obj);
        } else if (obj instanceof AbstractInOutTree) {
            return (XmlMapData) ((AbstractInOutTree) obj).eContainer();
        }
        return null;
    }

    private void createInputLoopTable(OutputTreeNode targetOutputNode, OutputTreeNodeEditPart treeNodeEditPart) {
        OutputTreeNodeEditPart outputTreeNodeEditPart = null;
        if (treeNodeEditPart instanceof OutputTreeNodeEditPart) {
            outputTreeNodeEditPart = XmlMapUtil.getParentLoopNodeEditPart(treeNodeEditPart);
        }
        //
        if (loopParentTreeNode != null) {
            InputLoopNodesTable inputLoopNodesTable = null;
            AbstractInOutTree abstractTree = XmlMapUtil.getAbstractInOutTree(targetOutputNode);
            if (abstractTree != null && abstractTree instanceof OutputXmlTree) {
                listInputLoopNodesTablesEntry = ((OutputXmlTree) abstractTree).getInputLoopNodesTables();
                if (!XmlMapUtil.hasDocument(abstractTree)) {
                    if (listInputLoopNodesTablesEntry != null && listInputLoopNodesTablesEntry.size() == 0) {
                        inputLoopNodesTable = XmlmapFactory.eINSTANCE.createInputLoopNodesTable();
                        inputLoopNodesTable.getInputloopnodes().add(loopParentTreeNode);
                        listInputLoopNodesTablesEntry.add(inputLoopNodesTable);
                    } else if (listInputLoopNodesTablesEntry != null && listInputLoopNodesTablesEntry.size() == 1) {
                        inputLoopNodesTable = listInputLoopNodesTablesEntry.get(0);
                        boolean isContain = false;
                        for (TreeNode treeNode : inputLoopNodesTable.getInputloopnodes()) {
                            if (treeNode.getXpath().equals(loopParentTreeNode.getXpath())) {
                                isContain = true;
                            }
                        }
                        if (!isContain) {
                            inputLoopNodesTable.getInputloopnodes().add(loopParentTreeNode);
                        }
                    }
                } else {
                    OutputTreeNode loopParentOutputTreeNode;
                    if (targetOutputNode.isLoop()) {
                        loopParentOutputTreeNode = targetOutputNode;
                    } else {
                        loopParentOutputTreeNode = (OutputTreeNode) XmlMapUtil.getLoopParentNode(targetOutputNode);
                    }
                    if (loopParentOutputTreeNode != null) {
                        if (loopParentOutputTreeNode.getInputLoopNodesTable() == null) {
                            inputLoopNodesTable = XmlmapFactory.eINSTANCE.createInputLoopNodesTable();
                            inputLoopNodesTable.getInputloopnodes().add(loopParentTreeNode);
                            inputLoopNodesTable.eAdapters().add(outputTreeNodeEditPart);
                            loopParentOutputTreeNode.setInputLoopNodesTable(inputLoopNodesTable);
                            listInputLoopNodesTablesEntry.add(inputLoopNodesTable);
                        } else {
                            loopParentOutputTreeNode.getInputLoopNodesTable().getInputloopnodes().add(loopParentTreeNode);
                        }
                    }
                }
            }
        }
    }

    private void doUpdate(AbstractNode sourceNode) {
        if (targetEditPart instanceof OutputTreeNodeEditPart) {
            OutputTreeNode targetOutputNode = (OutputTreeNode) ((OutputTreeNodeEditPart) targetEditPart).getModel();

            String expression = targetOutputNode.getExpression();
            if (sourceNode instanceof TreeNode) {
                if (expression == null) {
                    expression = XmlMapUtil.convertToExpression(((TreeNode) sourceNode).getXpath());
                } else {
                    expression = expression + " " + XmlMapUtil.convertToExpression(((TreeNode) sourceNode).getXpath());
                }
            } else if (sourceNode instanceof VarNode) {
                String tableName = "Var";
                if (sourceNode.eContainer() instanceof VarTable) {
                    tableName = ((VarTable) sourceNode.eContainer()).getName();
                }
                if (expression == null) {
                    expression = tableName + "." + sourceNode.getName();
                } else {
                    expression = expression + " " + tableName + "." + sourceNode.getName();
                }
            }

            createInputLoopTable(targetOutputNode, (OutputTreeNodeEditPart) targetEditPart);

            targetOutputNode.setExpression(expression);
            Connection conn = XmlmapFactory.eINSTANCE.createConnection();
            conn.setSource(sourceNode);
            conn.setTarget(targetOutputNode);
            targetOutputNode.getIncomingConnections().add(conn);
            sourceNode.getOutgoingConnections().add(conn);
            if (xmlMapData != null) {
                xmlMapData.getConnections().add(conn);
            }

        } else if (targetEditPart instanceof TreeNodeEditPart) {
            /* for lookup connections */
            if (sourceNode instanceof TreeNode) {
                TreeNode targetTreeNode = (TreeNode) targetEditPart.getModel();
                String expression = targetTreeNode.getExpression();
                if (expression == null) {
                    expression = "";
                }
                expression = expression + " " + XmlMapUtil.convertToExpression(((TreeNode) sourceNode).getXpath());
                targetTreeNode.setExpression(expression);

                LookupConnection conn = XmlmapFactory.eINSTANCE.createLookupConnection();
                conn.setSource(sourceNode);
                conn.setTarget(targetTreeNode);
                targetTreeNode.getLookupIncomingConnections().add(conn);
                ((TreeNode) sourceNode).getLookupOutgoingConnections().add(conn);
                if (xmlMapData != null) {
                    xmlMapData.getConnections().add(conn);
                }
            }
        } else if (targetEditPart instanceof VarNodeEditPart) {
            /* for varTable drag drop */
            if (sourceNode instanceof TreeNode) {
                VarNodeEditPart targetPart = (VarNodeEditPart) targetEditPart;
                VarNode targetNode = (VarNode) targetPart.getModel();
                String expression = targetNode.getExpression();
                if (expression == null) {
                    expression = "";
                }
                expression = expression + " " + XmlMapUtil.convertToExpression(((TreeNode) sourceNode).getXpath());
                if (targetNode.getName() == null || "".equals(targetNode.getName())) {
                    String findUniqueVarColumnName = XmlMapUtil.findUniqueVarColumnName(sourceNode.getName(), xmlMapData
                            .getVarTables().get(0));
                    targetNode.setName(findUniqueVarColumnName);
                }
                targetNode.setExpression(expression.trim());
                targetNode.setType(sourceNode.getType());
                Connection conn = XmlmapFactory.eINSTANCE.createConnection();
                conn.setSource(sourceNode);
                conn.setTarget(targetNode);
                targetNode.getIncomingConnections().add(conn);
                sourceNode.getOutgoingConnections().add(conn);
                if (xmlMapData != null) {
                    xmlMapData.getConnections().add(conn);
                }
            }

        } else if (targetEditPart instanceof InputXmlTreeEditPart || targetEditPart instanceof OutputXmlTreeEditPart) {
            AbstractInOutTree treeModel = (AbstractInOutTree) targetEditPart.getModel();
            String expression = treeModel.getExpressionFilter();
            if (expression == null) {
                expression = XmlMapUtil.convertToExpression(((TreeNode) sourceNode).getXpath());
            } else {
                expression = expression + " " + XmlMapUtil.convertToExpression(((TreeNode) sourceNode).getXpath());
            }
            treeModel.setExpressionFilter(expression);
            FilterConnection connection = XmlmapFactory.eINSTANCE.createFilterConnection();
            connection.setSource(sourceNode);
            connection.setTarget(treeModel);
            treeModel.getFilterIncomingConnections().add(connection);
            sourceNode.getFilterOutGoingConnections().add(connection);

            xmlMapData.getConnections().add(connection);

        }

    }
}
