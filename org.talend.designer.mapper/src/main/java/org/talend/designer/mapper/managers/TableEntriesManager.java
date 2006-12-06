// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.mapper.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.talend.commons.ui.swt.extended.table.IExtendedControlEventType;
import org.talend.designer.mapper.model.table.AbstractDataMapTable;
import org.talend.designer.mapper.model.table.OutputTable;
import org.talend.designer.mapper.model.tableentry.FilterTableEntry;
import org.talend.designer.mapper.model.tableentry.IColumnEntry;
import org.talend.designer.mapper.model.tableentry.ITableEntry;
import org.talend.designer.mapper.model.tableentry.TableEntryLocation;
import org.talend.designer.mapper.ui.visualmap.TableEntryProperties;
import org.talend.designer.mapper.ui.visualmap.table.DataMapTableView;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class TableEntriesManager {

    private Map<TableEntryLocation, ITableEntry> tableEntries;

    private Map<ITableEntry, TableEntryProperties> dataMapTableEntryToProperties;

    MapperManager mapperManager;

    static final int HEIGHT_REACTION = 2;

    /**
     * 
     * Event type.
     * <br/>
     *
     * $Id$
     *
     */
    public enum EVENT_TYPE implements IExtendedControlEventType {
        REMOVE_ALL,
        REMOVE,
        ADD,
        ADD_ALL,
    };
    
    TableEntriesManager(MapperManager mapperManager) {
        super();
        this.tableEntries = new HashMap<TableEntryLocation, ITableEntry>();
        this.dataMapTableEntryToProperties = new HashMap<ITableEntry, TableEntryProperties>();
        this.mapperManager = mapperManager;
    }

    void removeAll(List<? extends ITableEntry> dataMapTableEntriesGroup) {

        for (ITableEntry dataMapTableEntry : new ArrayList<ITableEntry>(dataMapTableEntriesGroup)) {
            remove(dataMapTableEntry);
        }

//        TableEntriesManagerEvent event = new TableEntriesManagerEvent(EVENT_TYPE.REMOVE_ALL);
//        event.entries = new ArrayList<ITableEntry>(dataMapTableEntriesGroup);
//        fireEvent(event);
        
    }

    /**
     * DOC amaumont Comment method "load".
     * 
     * @param tableEntries2
     */
    void addAll(List<? extends ITableEntry> dataMapTableEntriesGroup) {
        for (ITableEntry dataMapTableEntry : dataMapTableEntriesGroup) {
            add(dataMapTableEntry);
        }
        
//        TableEntriesManagerEvent event = new TableEntriesManagerEvent(EVENT_TYPE.ADD_ALL);
//        event.entries = new ArrayList<ITableEntry>(dataMapTableEntriesGroup);
//        fireEvent(event);
    }

    void addTableEntry(ITableEntry dataMapTableEntry) {
        addTableEntry(dataMapTableEntry, null);
    }

    /**
     * DOC amaumont Comment method "addTableEntry".
     * 
     * @param dataMapTableEntry
     * @param index
     */
    public void addTableEntry(ITableEntry dataMapTableEntry, Integer index) {
        if (dataMapTableEntry == null) {
            throw new IllegalArgumentException("dataMapTableEntry can't be null.");
        }
        add(dataMapTableEntry);
        AbstractDataMapTable dataMapTable = dataMapTableEntry.getParent();
        if (dataMapTableEntry instanceof IColumnEntry) {
            if (index == null) {
                dataMapTable.addColumnEntry((IColumnEntry) dataMapTableEntry);
            } else {
                dataMapTable.addColumnEntry((IColumnEntry) dataMapTableEntry, index);
            }
        } else if (dataMapTableEntry instanceof FilterTableEntry) {
            if (index == null) {
                ((OutputTable) dataMapTable).addFilterEntry((FilterTableEntry) dataMapTableEntry);
            } else {
                ((OutputTable) dataMapTable).addFilterEntry((FilterTableEntry) dataMapTableEntry, index);
            }
        } else {
            throw new IllegalArgumentException("Type '" + dataMapTableEntry.getClass() + "' is not a valid type");
        }
//        TableEntriesManagerEvent event = new TableEntriesManagerEvent(EVENT_TYPE.ADD);
//        event.entry = dataMapTableEntry;
//        fireEvent(event);
    }

    /**
     * DOC amaumont Comment method "add".
     * 
     * @param dataMapTableEntry
     */
    private void add(ITableEntry dataMapTableEntry) {
        tableEntries.put(TableEntryLocation.getNewInstance(dataMapTableEntry), dataMapTableEntry);
    }

    public void remove(ITableEntry dataMapTableEntry) {
        if (dataMapTableEntry != null) {
            mapperManager.removeLinksOf(dataMapTableEntry);
            tableEntries.remove(TableEntriesManager.buildLocation(dataMapTableEntry));
            dataMapTableEntryToProperties.remove(dataMapTableEntry);
            AbstractDataMapTable dataMapTable = dataMapTableEntry.getParent();
            if (dataMapTableEntry instanceof IColumnEntry) {
                dataMapTableEntry.getParent().removeColumnEntry((IColumnEntry) dataMapTableEntry);
            } else if (dataMapTableEntry instanceof FilterTableEntry) {
                if (dataMapTable instanceof OutputTable) {
                    ((OutputTable) dataMapTable).removeConstraintEntry((FilterTableEntry) dataMapTableEntry);
                }
            } else {
                throw new IllegalArgumentException("Type '" + dataMapTableEntry.getClass() + "' is not a valid type");
            }
        }
    }

    /**
     * DOC amaumont Comment method "searchTableEntry".
     * 
     * @param tableName
     * @param columnName
     */
    ITableEntry retrieveTableEntry(TableEntryLocation location) {
        return tableEntries.get(location);
    }

    /**
     * DOC amaumont Comment method "getTableEntryProperties".
     * 
     * @param dataMapTableEntry
     * @return
     */
    TableEntryProperties getTableEntryProperties(ITableEntry dataMapTableEntry) {
        TableEntryProperties tableEntryProperties = dataMapTableEntryToProperties.get(dataMapTableEntry);
        if (tableEntryProperties == null) {
            tableEntryProperties = new TableEntryProperties();
            dataMapTableEntryToProperties.put(dataMapTableEntry, tableEntryProperties);
        }
        return tableEntryProperties;
    }

    TableItem retrieveTableItem(ITableEntry dataMapTableEntry) {
        DataMapTableView dataMapTableView = this.mapperManager.retrieveAbstractDataMapTableView(dataMapTableEntry.getParent());
        TableItem[] tableItems = new TableItem[0];
        if (dataMapTableEntry instanceof IColumnEntry) {
            tableItems = dataMapTableView.getTableViewerCreatorForColumns().getTable().getItems();
        } else if (dataMapTableEntry instanceof FilterTableEntry) {
            tableItems = dataMapTableView.getTableViewerCreatorForFilters().getTable().getItems();
        } else {
            throw new IllegalArgumentException("case not found");
        }
        TableItem tableItem = null;
        for (int i = 0; i < tableItems.length; i++) {
            if (tableItems[i].getData() == dataMapTableEntry) {
                tableItem = tableItems[i];
                break;
            }
        }
        getTableEntryProperties(dataMapTableEntry).setTableItem(tableItem);
        return tableItem;
    }

    /**
     * DOC amaumont Comment method "retrieveTableFromTableEntry".
     * 
     * @param dataMapTableEntry
     * @return
     */
    Table retrieveTable(ITableEntry dataMapTableEntry) {
        return retrieveTableItem(dataMapTableEntry).getParent();
    }

    /**
     * DOC amaumont Comment method "renameEntryName".
     * 
     * @param dataMapTableEntry
     * @param newColumnName
     * @param newColumnName
     */
    public void renameEntryName(ITableEntry dataMapTableEntry, String previousColumnName, String newColumnName) {
        TableEntryLocation tableEntryLocationKey = new TableEntryLocation(dataMapTableEntry.getParentName(), previousColumnName);
        // TableEntriesManager.buildLocation(dataMapTableEntry);
        ITableEntry entry = tableEntries.get(tableEntryLocationKey);
        if (entry != dataMapTableEntry) {
            throw new IllegalStateException("tableEntries are not the same !");
        }
        tableEntries.remove(tableEntryLocationKey);
        tableEntryLocationKey.columnName = newColumnName;
        tableEntries.put(tableEntryLocationKey, dataMapTableEntry);
        dataMapTableEntry.setName(newColumnName);
    }

    public static TableEntryLocation buildLocation(ITableEntry dataMapTableEntry) {
        return new TableEntryLocation(dataMapTableEntry.getParentName(), dataMapTableEntry.getName());
    }

    /**
     * 
     * Event for TableEntriesManager.
     * <br/>
     *
     * $Id$
     *
     */
//    public class TableEntriesManagerEvent extends ExtendedModelEvent {
//
//        /**
//         * DOC amaumont TableEntriesManagerEvent constructor comment.
//         * @param type
//         */
//        public TableEntriesManagerEvent(IExtendedControlEventType type) {
//            super(type);
//        }
//
//        public ITableEntry entry;
//        public List<ITableEntry> entries;
//        
//    }
}
